package wtf.dexum.client.modules.impl.combat.rotation;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MLRotation extends RotationBase {
    private float currentYaw, currentPitch;
    private boolean initialized = false;

    // ONNX сессии
    private OrtEnvironment env;
    private OrtSession sessionClick;
    private OrtSession sessionYaw;
    private OrtSession sessionPitch;

    // Параметры нормализации
    private float[] inputMean, inputScale;
    private float[] outputMeanClick, outputScaleClick;
    private float[] outputMeanYaw, outputScaleYaw;
    private float[] outputMeanPitch, outputScalePitch;

    // Гибридные настройки (как в NeuralRotation)
    private static final float HYBRID_THRESHOLD = 12.0f;
    private static final float MAX_NEURAL_YAW = 4.5f;
    private static final float MAX_NEURAL_PITCH = 2.0f;

    // Инерция мыши
    private float velocityYaw = 0f, velocityPitch = 0f;
    private static final float ACCEL = 0.35f;
    private static final float FRICTION = 0.82f;
    private static final float MAX_SPEED = 10.0f;

    // Сглаживание выхода
    private float lastSmoothedYaw = 0f, lastSmoothedPitch = 0f;
    private static final float SMOOTH_YAW = 0.5f;
    private static final float SMOOTH_PITCH = 0.12f;

    // Сглаживание координат цели
    private float smoothDx = 0, smoothDy = 0, smoothDz = 0, smoothDist = 0;

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

    private boolean loaded = false;

    public MLRotation() {
        try {
            loadModelsAndScalers();
            loaded = true;
            System.out.println("[MLRotation] Все три модели загружены успешно.");
        } catch (Exception e) {
            System.err.println("[MLRotation] Ошибка загрузки моделей!");
            e.printStackTrace();
        }
    }

    private void loadModelsAndScalers() throws Exception {
        env = OrtEnvironment.getEnvironment();

        try (InputStream clickStream = getClass().getResourceAsStream("/assets/dexum/neural/model_click.onnx");
             InputStream yawStream = getClass().getResourceAsStream("/assets/dexum/neural/model_deltaYaw.onnx");
             InputStream pitchStream = getClass().getResourceAsStream("/assets/dexum/neural/model_deltaPitch.onnx")) {

            if (clickStream == null || yawStream == null || pitchStream == null)
                throw new RuntimeException("ONNX файлы не найдены в /assets/dexum/neural/");

            sessionClick = env.createSession(clickStream.readAllBytes());
            sessionYaw = env.createSession(yawStream.readAllBytes());
            sessionPitch = env.createSession(pitchStream.readAllBytes());
        }

        try (InputStream scalerStream = getClass().getResourceAsStream("/assets/dexum/neural/scalers.json")) {
            if (scalerStream == null) throw new RuntimeException("scalers.json не найден");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(scalerStream, StandardCharsets.UTF_8)).getAsJsonObject();

            String[] meanIn = json.get("input_mean").getAsString().split(",");
            String[] scaleIn = json.get("input_scale").getAsString().split(",");
            inputMean = new float[meanIn.length];
            inputScale = new float[scaleIn.length];
            for (int i = 0; i < meanIn.length; i++) {
                inputMean[i] = Float.parseFloat(meanIn[i].trim());
                inputScale[i] = Float.parseFloat(scaleIn[i].trim());
            }

            String[] mC = json.get("output_mean_click").getAsString().split(",");
            String[] sC = json.get("output_scale_click").getAsString().split(",");
            outputMeanClick = new float[mC.length];
            outputScaleClick = new float[sC.length];
            for (int i = 0; i < mC.length; i++) {
                outputMeanClick[i] = Float.parseFloat(mC[i].trim());
                outputScaleClick[i] = Float.parseFloat(sC[i].trim());
            }

            String[] mY = json.get("output_mean_yaw").getAsString().split(",");
            String[] sY = json.get("output_scale_yaw").getAsString().split(",");
            outputMeanYaw = new float[mY.length];
            outputScaleYaw = new float[sY.length];
            for (int i = 0; i < mY.length; i++) {
                outputMeanYaw[i] = Float.parseFloat(mY[i].trim());
                outputScaleYaw[i] = Float.parseFloat(sY[i].trim());
            }

            String[] mP = json.get("output_mean_pitch").getAsString().split(",");
            String[] sP = json.get("output_scale_pitch").getAsString().split(",");
            outputMeanPitch = new float[mP.length];
            outputScalePitch = new float[sP.length];
            for (int i = 0; i < mP.length; i++) {
                outputMeanPitch[i] = Float.parseFloat(mP[i].trim());
                outputScalePitch[i] = Float.parseFloat(sP[i].trim());
            }
        }
    }

    /**
     * Основной метод, вызываемый из Aura с явной целью.
     */
    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!loaded || target == null) {
            if (target == null) {
                velocityYaw *= FRICTION;
                velocityPitch *= FRICTION;
                distracted = false;
            }
            return;
        }

        if (!initialized) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            initialized = true;
        }

        // Задержка реакции
        if (reactionDelay > 0) {
            reactionDelay--;
            return;
        }

        // Пропуск тика
        if (rng.nextFloat() < 0.07f) return;

        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // Плавающая точка прицела
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

        // Сглаживание координат цели
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

        // Овершут
        if (overshooting) {
            idealYaw += overshootYaw;
            idealPitch += overshootPitch;
            overshootTicks--;
            if (overshootTicks <= 0) overshooting = false;
        }

        // Отвлечение
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

        // Усталость
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
        } else {
            // Нейросеть
            float[] features = extractFeatures(target);
            float[] neural = getNeuralDeltas(features);
            float neuralYaw = MathHelper.clamp(neural[0], -MAX_NEURAL_YAW, MAX_NEURAL_YAW);
            float neuralPitch = MathHelper.clamp(neural[1], -MAX_NEURAL_PITCH, MAX_NEURAL_PITCH);
            deltaYaw = neuralYaw + errorYaw * 0.04f;
            deltaPitch = neuralPitch + errorPitch * 0.02f;
        }

        // Вертикальная стабилизация
        if (Math.abs(errorPitch) < 1.2f) {
            deltaPitch = 0;
        } else {
            deltaPitch *= 0.25f;
        }

        // Многослойный шум
        deltaYaw += rng.nextGaussian() * 0.3f;
        deltaPitch += rng.nextGaussian() * 0.15f;

        // Экспоненциальное сглаживание
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

        // Финальная рандомизация
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

        // Запуск овершута
        if (!overshooting && totalError < 6.0f && rng.nextFloat() < 0.05f) {
            overshooting = true;
            overshootYaw = (rng.nextFloat() - 0.5f) * 8.0f;
            overshootPitch = (rng.nextFloat() - 0.5f) * 4.0f;
            overshootTicks = 2 + rng.nextInt(4);
        }
    }

    /**
     * Сбор 30 фичей для модели.
     */
    private float[] extractFeatures(LivingEntity target) {
        PlayerEntity player = mc.player;
        if (player == null) return new float[30];

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float playerX = (float) player.getX();
        float playerY = (float) player.getY();
        float playerZ = (float) player.getZ();
        float motionX = (float) player.getVelocity().x;
        float motionY = (float) player.getVelocity().y;
        float motionZ = (float) player.getVelocity().z;
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float food = player.getHungerManager().getFoodLevel();
        float saturation = player.getHungerManager().getSaturationLevel();
        float onGround = player.isOnGround() ? 1f : 0f;
        float sprinting = player.isSprinting() ? 1f : 0f;
        float sneaking = player.isSneaking() ? 1f : 0f;

        float targetX = (float) target.getX();
        float targetY = (float) target.getY();
        float targetZ = (float) target.getZ();
        float targetMotionX = (float) target.getVelocity().x;
        float targetMotionY = (float) target.getVelocity().y;
        float targetMotionZ = (float) target.getVelocity().z;
        float targetHealth = (target instanceof PlayerEntity) ? ((PlayerEntity) target).getHealth() : 0f;
        float targetMaxHealth = (target instanceof PlayerEntity) ? ((PlayerEntity) target).getMaxHealth() : 0f;

        float dx = targetX - playerX;
        float dy = targetY + target.getEyeHeight(target.getPose()) - playerY - player.getEyeHeight(player.getPose());
        float dz = targetZ - playerZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float angleH = (float) Math.toDegrees(Math.atan2(dz, dx));
        float angleV = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float attackCooldown = player.getAttackCooldownProgress(0.0f);

        return new float[]{
                yaw, pitch, playerX, playerY, playerZ,
                motionX, motionY, motionZ,
                health, maxHealth, food, saturation,
                onGround, sprinting, sneaking,
                targetX, targetY, targetZ,
                targetMotionX, targetMotionY, targetMotionZ,
                targetHealth, targetMaxHealth,
                dist, dx, dy, dz,
                angleH, angleV,
                attackCooldown
        };
    }

    /**
     * Прогон фичей через три модели ONNX.
     */
    private float[] getNeuralDeltas(float[] features) {
        float[] normalized = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            normalized[i] = (features[i] - inputMean[i]) / inputScale[i];
        }

        try (OnnxTensor tensor = OnnxTensor.createTensor(env, new float[][]{normalized})) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("float_input", tensor);

            // Инференс click (не используем для поворота)
            OrtSession.Result resultClick = sessionClick.run(inputs);
            float[][] rawClick = (float[][]) resultClick.get(0).getValue();
            float probClick = rawClick[0].length > 1 ? rawClick[0][1] : rawClick[0][0];
            if (outputMeanClick.length > 0) probClick = probClick * outputScaleClick[0] + outputMeanClick[0];

            // Инференс deltaYaw
            OrtSession.Result resultYaw = sessionYaw.run(inputs);
            float deltaYaw = ((float[][]) resultYaw.get(0).getValue())[0][0];
            if (outputMeanYaw.length > 0) deltaYaw = deltaYaw * outputScaleYaw[0] + outputMeanYaw[0];

            // Инференс deltaPitch
            OrtSession.Result resultPitch = sessionPitch.run(inputs);
            float deltaPitch = ((float[][]) resultPitch.get(0).getValue())[0][0];
            if (outputMeanPitch.length > 0) deltaPitch = deltaPitch * outputScalePitch[0] + outputMeanPitch[0];

            return new float[]{deltaYaw, deltaPitch};
        } catch (OrtException e) {
            e.printStackTrace();
            return new float[]{0, 0};
        }
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
        // Этот метод вызывается из Aura только если не передан target, но мы всегда передаём target явно.
        // Оставляем пустым.
    }

    public boolean isLoaded() { return loaded; }
}