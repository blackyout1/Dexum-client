package wtf.dexum.client.modules.impl.combat.rotation;

import ai.onnxruntime.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class NeuralRotation extends RotationBase {
    private float currentYaw, currentPitch;
    private boolean initialized;

    // ONNX
    private OrtEnvironment env;
    private OrtSession session;
    private float[] meanX, scaleX, meanY, scaleY;

    private static final int SEQ_LEN = 16;
    private LinkedList<float[]> buffer = new LinkedList<>();

    // Гибридный порог
    private static final float HYBRID_THRESHOLD = 12.0f;
    // Ограничения нейросети
    private static final float MAX_NEURAL_YAW = 4.5f;
    private static final float MAX_NEURAL_PITCH = 2.0f;

    // Инерция мыши
    private float velocityYaw = 0f;
    private float velocityPitch = 0f;
    private static final float ACCEL = 0.35f;
    private static final float FRICTION = 0.82f;
    private static final float MAX_SPEED = 10.0f;

    // Сглаживание выхода
    private float lastSmoothedYaw = 0f, lastSmoothedPitch = 0f;
    private static final float SMOOTH_YAW = 0.5f;
    private static final float SMOOTH_PITCH = 0.12f;     // экстремально сильная инерция вертикали

    // Сглаживание координат цели
    private float smoothDx, smoothDy, smoothDz, smoothDist;

    // Плавающая точка прицела
    private Vec3d aimOffset = Vec3d.ZERO;
    private int offsetTimer = 0;
    private static final double OFFSET_SPEED = 0.07;
    private static final double OFFSET_FRICTION = 0.83;

    // Овершут
    private boolean overshooting = false;
    private float overshootYaw = 0f, overshootPitch = 0f;
    private int overshootTicks = 0;

    // Отвлечение
    private int distractionTimer = 0;
    private boolean distracted = false;
    private float distractionYaw = 0f, distractionPitch = 0f;

    // Человеческие факторы
    private final Random rng = new Random();
    private int reactionDelay = 0;
    private long sessionStart = System.currentTimeMillis();
    private long lastAttackTime = 0;

    public NeuralRotation() {
        try {
            env = OrtEnvironment.getEnvironment();
            byte[] modelBytes = getClass().getResourceAsStream("/assets/dexum/neural/aim_model.onnx").readAllBytes();
            session = env.createSession(modelBytes, new OrtSession.SessionOptions());

            Reader reader = new InputStreamReader(getClass().getResourceAsStream("/assets/dexum/neural/scalers.json"));
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            meanX = toFloatArray(json.getAsJsonArray("meanX"));
            scaleX = toFloatArray(json.getAsJsonArray("scaleX"));
            meanY = toFloatArray(json.getAsJsonArray("meanY"));
            scaleY = toFloatArray(json.getAsJsonArray("scaleY"));
        } catch (Exception e) {
            System.err.println("Neural model not loaded, using pure algorithm aim.");
        }
    }

    private float[] toFloatArray(JsonArray arr) {
        float[] res = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) res[i] = arr.get(i).getAsFloat();
        return res;
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!initialized) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            initialized = true;
        }
        if (target == null) {
            velocityYaw *= FRICTION;
            velocityPitch *= FRICTION;
            distracted = false;
            return;
        }

        // 1. Задержка реакции
        if (reactionDelay > 0) {
            reactionDelay--;
            return;
        }

        // 2. Пропуск тика
        if (rng.nextFloat() < 0.07f) return;

        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // 3. Плавающая точка прицела (random walk)
        offsetTimer--;
        if (offsetTimer <= 0) {
            offsetTimer = 1 + rng.nextInt(3);
            double ax = (rng.nextDouble() - 0.5) * OFFSET_SPEED;
            double ay = (rng.nextDouble() - 0.5) * OFFSET_SPEED;
            double az = (rng.nextDouble() - 0.5) * OFFSET_SPEED;
            aimOffset = aimOffset.add(ax, ay, az);
            aimOffset = new Vec3d(
                    MathHelper.clamp(aimOffset.x, box.minX - box.getCenter().x, box.maxX - box.getCenter().x),
                    MathHelper.clamp(aimOffset.y, box.minY - box.getCenter().y, box.maxY - box.getCenter().y),
                    MathHelper.clamp(aimOffset.z, box.minZ - box.getCenter().z, box.maxZ - box.getCenter().z)
            );
            aimOffset = aimOffset.multiply(OFFSET_FRICTION);
        }

        Vec3d center = box.getCenter().add(aimOffset);
        float dx = (float)(center.x - eye.x);
        float dy = (float)(center.y - eye.y);
        float dz = (float)(center.z - eye.z);
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // 4. Сильное сглаживание координат цели
        float alpha = 0.25f;
        smoothDx = smoothDx * (1 - alpha) + dx * alpha;
        smoothDy = smoothDy * (1 - alpha) + dy * alpha;
        smoothDz = smoothDz * (1 - alpha) + dz * alpha;
        smoothDist = smoothDist * (1 - alpha) + dist * alpha;

        float idealYaw = (float) Math.toDegrees(Math.atan2(smoothDz, smoothDx)) - 90.0F;
        float idealPitch = (float) -Math.toDegrees(Math.atan2(smoothDy,
                Math.sqrt(smoothDx*smoothDx + smoothDz*smoothDz)));
        idealYaw = MathHelper.wrapDegrees(idealYaw);
        idealPitch = MathHelper.clamp(idealPitch, -90.0F, 90.0F);

        // 5. Овершут
        if (overshooting) {
            idealYaw += overshootYaw;
            idealPitch += overshootPitch;
            overshootTicks--;
            if (overshootTicks <= 0) overshooting = false;
        }

        // 6. Отвлечение
        long now = System.currentTimeMillis();
        boolean targetMoving = target.getVelocity().lengthSquared() > 0.01;
        boolean recentlyAttacked = (now - lastAttackTime) < 1500;
        if (!targetMoving && !recentlyAttacked) {
            distractionTimer++;
            if (distractionTimer > 25) {
                distracted = true;
                distractionYaw = (rng.nextFloat() - 0.5f) * 18.0f;
                distractionPitch = (rng.nextFloat() - 0.5f) * 6.0f;
            }
        } else {
            distractionTimer = 0;
            distracted = false;
        }
        if (distracted) {
            idealYaw += distractionYaw;
            idealPitch += distractionPitch;
        }

        float errorYaw = MathHelper.wrapDegrees(idealYaw - currentYaw);
        float errorPitch = idealPitch - currentPitch;
        float totalError = (float) Math.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);

        // 7. Усталость
        long sessionDuration = (System.currentTimeMillis() - sessionStart) / 1000;
        float fatigue = 1.0f - Math.min(0.25f, sessionDuration * 0.00015f);

        float deltaYaw = 0, deltaPitch = 0;

        if (totalError > HYBRID_THRESHOLD) {
            // Алгоритмический доворот с инерцией
            float speed = (6.0f + totalError * 0.3f) * fatigue;
            speed *= 0.85f + rng.nextFloat() * 0.3f;
            float targetSpeedYaw = Math.min(speed, MAX_SPEED);
            float targetSpeedPitch = Math.min(speed * 0.5f, 5.0f);

            velocityYaw += (targetSpeedYaw - velocityYaw) * ACCEL;
            velocityPitch += (targetSpeedPitch - velocityPitch) * ACCEL;
            velocityYaw = MathHelper.clamp(velocityYaw, -MAX_SPEED, MAX_SPEED);
            velocityPitch = MathHelper.clamp(velocityPitch, -MAX_SPEED * 0.5f, MAX_SPEED * 0.5f);

            deltaYaw = Math.signum(errorYaw) * velocityYaw;
            deltaPitch = Math.signum(errorPitch) * velocityPitch;
        } else if (session != null) {
            // Нейросеть
            float[] neural = getNeuralDeltas(smoothDx, smoothDy, smoothDz, smoothDist);
            float neuralYaw = MathHelper.clamp(neural[0], -MAX_NEURAL_YAW, MAX_NEURAL_YAW);
            float neuralPitch = MathHelper.clamp(neural[1], -MAX_NEURAL_PITCH, MAX_NEURAL_PITCH);
            deltaYaw = neuralYaw + errorYaw * 0.04f;
            deltaPitch = neuralPitch + errorPitch * 0.02f;
        } else {
            deltaYaw = errorYaw * 0.4f;
            deltaPitch = errorPitch * 0.4f;
        }

        // 8. Вертикальная стабилизация
        if (Math.abs(errorPitch) < 1.2f) {
            deltaPitch = 0;
        } else {
            deltaPitch *= 0.25f;
        }

        // 9. Многослойный шум
        deltaYaw += rng.nextGaussian() * 0.3f;
        deltaPitch += rng.nextGaussian() * 0.15f;

        // 10. Экспоненциальное сглаживание
        deltaYaw = lastSmoothedYaw * (1 - SMOOTH_YAW) + deltaYaw * SMOOTH_YAW;
        deltaPitch = lastSmoothedPitch * (1 - SMOOTH_PITCH) + deltaPitch * SMOOTH_PITCH;
        lastSmoothedYaw = deltaYaw;
        lastSmoothedPitch = deltaPitch;

        deltaYaw = MathHelper.clamp(deltaYaw, -12.0f, 12.0f);
        deltaPitch = MathHelper.clamp(deltaPitch, -4.0f, 4.0f);

        float gcd = Rotation.gcd();
        int mX = Math.round(deltaYaw / gcd);
        int mY = Math.round(deltaPitch / gcd);
        if (mX == 0 && mY == 0) return;

        float newYaw = currentYaw + mX * gcd;
        float newPitch = MathHelper.clamp(currentPitch + mY * gcd, -90.0F, 90.0F);

        // 11. Финальная рандомизация точки попадания
        newYaw += rng.nextGaussian() * 0.5f;
        newPitch += rng.nextGaussian() * 0.25f;
        newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);

        Rotation finalRot = new Rotation(newYaw, newPitch);
        float visualSpeed = 25.0f + totalError * 0.5f;
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);

        currentYaw = newYaw;
        currentPitch = newPitch;
        lastYaw = newYaw;
        lastPitch = newPitch;

        // 12. Запуск овершута
        if (!overshooting && totalError < 6.0f && rng.nextFloat() < 0.05f) {
            overshooting = true;
            overshootYaw = (rng.nextFloat() - 0.5f) * 8.0f;
            overshootPitch = (rng.nextFloat() - 0.5f) * 4.0f;
            overshootTicks = 2 + rng.nextInt(4);
        }
    }

    private float[] getNeuralDeltas(float dx, float dy, float dz, float dist) {
        float[] feat = new float[6];
        feat[0] = MathHelper.wrapDegrees(currentYaw);
        feat[1] = currentPitch;
        feat[2] = dx;
        feat[3] = dy;
        feat[4] = dz;
        feat[5] = dist;

        for (int i = 0; i < feat.length; i++)
            feat[i] = (feat[i] - meanX[i]) / scaleX[i];

        buffer.addLast(feat);
        if (buffer.size() > SEQ_LEN) buffer.removeFirst();
        if (buffer.size() < SEQ_LEN) return new float[]{0, 0};

        float[][][] input = new float[1][SEQ_LEN][6];
        int idx = 0;
        for (float[] f : buffer) input[0][idx++] = f;

        try (var tensor = OnnxTensor.createTensor(env, input);
             var output = session.run(Collections.singletonMap("input", tensor))) {
            var optionalValue = output.get("output");
            if (optionalValue.isPresent()) {
                float[][] raw = (float[][]) optionalValue.get().getValue();
                float deltaYaw = raw[0][0] * scaleY[0] + meanY[0];
                float deltaPitch = raw[0][1] * scaleY[1] + meanY[1];
                return new float[]{deltaYaw, deltaPitch};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new float[]{0, 0};
    }

    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        distracted = false;
        distractionTimer = 0;
    }

    public void onTargetChange() {
        reactionDelay = 2 + rng.nextInt(5);
        sessionStart = System.currentTimeMillis();
        overshooting = false;
        distracted = false;
        distractionTimer = 0;
        velocityYaw = 0;
        velocityPitch = 0;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        initialized = true;
        buffer.clear();
        lastSmoothedYaw = 0;
        lastSmoothedPitch = 0;
        smoothDx = smoothDy = smoothDz = smoothDist = 0;
        reactionDelay = 0;
        aimOffset = Vec3d.ZERO;
        distracted = false;
        distractionTimer = 0;
        velocityYaw = 0;
        velocityPitch = 0;
    }
}