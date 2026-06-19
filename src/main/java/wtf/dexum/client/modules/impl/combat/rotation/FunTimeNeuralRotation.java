package wtf.dexum.client.modules.impl.combat.rotation;

import ai.onnxruntime.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class FunTimeNeuralRotation extends RotationBase {

    // --- нейросеть ---
    private OrtEnvironment env;
    private OrtSession session;
    private float[] meanX, scaleX, meanY, scaleY;
    private static final int SEQ_LEN = 16;
    private final LinkedList<float[]> buffer = new LinkedList<>();

    // --- константы ---
    private static final float HYBRID_THRESHOLD = 12.0f;
    private static final float MAX_NEURAL_YAW = 4.5f;
    private static final float MAX_NEURAL_PITCH = 2.0f;
    private static final float SMOOTH_YAW = 0.5f;
    private static final float SMOOTH_PITCH = 0.12f;
    private static final double OFFSET_SPEED = 0.07;
    private static final double OFFSET_FRICTION = 0.83;

    // --- сглаживание выхода ---
    private float lastSmoothedYaw = 0f;
    private float lastSmoothedPitch = 0f;

    // --- сглаживание координат цели ---
    private float smoothDx, smoothDy, smoothDz, smoothDist;
    private Vec3d aimOffset = Vec3d.ZERO;
    private int offsetTimer = 0;

    // --- человеческие факторы ---
    private boolean overshooting = false;
    private float overshootYaw = 0f, overshootPitch = 0f;
    private int overshootTicks = 0;
    private int distractionTimer = 0;
    private boolean distracted = false;
    private float distractionYaw = 0f, distractionPitch = 0f;
    private final Random rng = new Random();
    private int reactionDelay = 0;
    private long lastAttackTime = 0;
    private LivingEntity lastTarget = null;

    public FunTimeNeuralRotation() {
        try {
            env = OrtEnvironment.getEnvironment();

            try (InputStream modelStream = getClass().getResourceAsStream("/assets/dexum/neural/aim_model.onnx")) {
                if (modelStream == null) throw new IllegalStateException("aim_model.onnx not found");
                byte[] modelBytes = modelStream.readAllBytes();
                session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            }

            try (Reader reader = new InputStreamReader(
                    Objects.requireNonNull(getClass().getResourceAsStream("/assets/dexum/neural/scalers.json")))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                meanX = toFloatArray(json.getAsJsonArray("meanX"));
                scaleX = toFloatArray(json.getAsJsonArray("scaleX"));
                meanY = toFloatArray(json.getAsJsonArray("meanY"));
                scaleY = toFloatArray(json.getAsJsonArray("scaleY"));
            }
        } catch (Exception e) {
            System.err.println("[FunTimeNeural] модель не загружена, использую упрощённый доворот.");
        }
    }

    private float[] toFloatArray(JsonArray arr) {
        float[] res = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) res[i] = arr.get(i).getAsFloat();
        return res;
    }

    // Вызывается из Aura при пропадании цели
    public void update(Rotation targetAngle, boolean elytraVisual) {
        resetNeuralState();
        if (mc.player != null) {
            setYaw(mc.player.getYaw());
            setPitch(mc.player.getPitch());
        }
    }


    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (target == null || !Aura.INSTANCE.isEnabled()) {
            handleNoTarget(elytraVisual);
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            onTargetChange();
        }

        if (reactionDelay > 0) {
            reactionDelay--;
            return;
        }

        if (rng.nextFloat() < 0.07f) return;

        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        // плавающая точка прицела
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

        if (overshooting) {
            idealYaw += overshootYaw;
            idealPitch += overshootPitch;
            overshootTicks--;
            if (overshootTicks <= 0) overshooting = false;
        }

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

        float currentYaw = getYaw();
        float currentPitch = getPitch();
        float errorYaw = MathHelper.wrapDegrees(idealYaw - currentYaw);
        float errorPitch = idealPitch - currentPitch;
        float totalError = (float) Math.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);

        float deltaYaw, deltaPitch;

        // гибридная логика
        if (totalError > HYBRID_THRESHOLD) {
            // снап-доворот FunTime
            float capYaw = (Math.abs(errorYaw) / totalError) * 130f;
            float capPitch = (Math.abs(errorPitch) / totalError) * 130f;
            float snapYaw = currentYaw + MathHelper.clamp(errorYaw, -capYaw, capYaw);
            float snapPitch = currentPitch + MathHelper.clamp(errorPitch, -capPitch, capPitch);
            deltaYaw = (snapYaw - currentYaw) * 0.85f;
            deltaPitch = (snapPitch - currentPitch) * 0.85f;
        } else if (session != null) {
            // нейросетевой доворот
            float[] neural = getNeuralDeltas(smoothDx, smoothDy, smoothDz, smoothDist);
            float neuralYaw = MathHelper.clamp(neural[0], -MAX_NEURAL_YAW, MAX_NEURAL_YAW);
            float neuralPitch = MathHelper.clamp(neural[1], -MAX_NEURAL_PITCH, MAX_NEURAL_PITCH);
            deltaYaw = neuralYaw + errorYaw * 0.04f;
            deltaPitch = neuralPitch + errorPitch * 0.02f;
        } else {
            deltaYaw = errorYaw * 0.4f;
            deltaPitch = errorPitch * 0.4f;
        }

        if (Math.abs(errorPitch) < 1.2f) {
            deltaPitch = 0;
        } else {
            deltaPitch *= 0.25f;
        }

        deltaYaw += (float) rng.nextGaussian() * 0.3f;
        deltaPitch += (float) rng.nextGaussian() * 0.15f;

        deltaYaw = lastSmoothedYaw * (1 - SMOOTH_YAW) + deltaYaw * SMOOTH_YAW;
        deltaPitch = lastSmoothedPitch * (1 - SMOOTH_PITCH) + deltaPitch * SMOOTH_PITCH;
        lastSmoothedYaw = deltaYaw;
        lastSmoothedPitch = deltaPitch;

        deltaYaw = MathHelper.clamp(deltaYaw, -12.0f, 12.0f);
        deltaPitch = MathHelper.clamp(deltaPitch, -4.0f, 4.0f);

        float newYaw = currentYaw + deltaYaw;
        float newPitch = MathHelper.clamp(currentPitch + deltaPitch, -90.0F, 90.0F);

        // FunTime-флик УБРАН (раньше здесь был сброс в -90°)

        float gcd = Rotation.gcd();
        int mX = Math.round((newYaw - currentYaw) / gcd);
        int mY = Math.round((newPitch - currentPitch) / gcd);
        newYaw = currentYaw + mX * gcd;
        newPitch = currentPitch + mY * gcd;

        newYaw += (float) rng.nextGaussian() * 0.5f;
        newPitch += (float) rng.nextGaussian() * 0.25f;
        newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);

        Rotation finalRot = new Rotation(newYaw, newPitch);
        float visualSpeed = 25.0f + totalError * 0.5f;
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);

        setYaw(newYaw);
        setPitch(newPitch);

        if (!overshooting && totalError < 6.0f && rng.nextFloat() < 0.05f) {
            overshooting = true;
            overshootYaw = (rng.nextFloat() - 0.5f) * 8.0f;
            overshootPitch = (rng.nextFloat() - 0.5f) * 4.0f;
            overshootTicks = 2 + rng.nextInt(4);
        }
    }

    private void handleNoTarget(boolean elytraVisual) {
        // Простой возврат к ванильным углам, без дрожания
        if (mc.player == null) return;

        float currentYaw = getYaw();
        float currentPitch = getPitch();
        float vanillaYaw = mc.player.getYaw();
        float vanillaPitch = mc.player.getPitch();

        float deltaYaw = MathHelper.wrapDegrees(vanillaYaw - currentYaw);
        float deltaPitch = vanillaPitch - currentPitch;
        float totalBack = (float) Math.hypot(deltaYaw, deltaPitch);
        totalBack = Math.max(totalBack, 0.0001f);
        float returnSpeed = MathHelper.clamp(totalBack / 45f, 0.08f, 0.65f);
        float yawReturn = deltaYaw * returnSpeed;
        float pitchReturn = deltaPitch * returnSpeed;

        float newYaw = currentYaw + yawReturn;
        float newPitch = MathHelper.clamp(currentPitch + pitchReturn, -90f, 90f);

        setYaw(newYaw);
        setPitch(newPitch);

        Rotation finalRot = new Rotation(newYaw, newPitch);
        RotationComponent.update(finalRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, elytraVisual);
    }

    private float[] getNeuralDeltas(float dx, float dy, float dz, float dist) {
        if (session == null || mc.player == null) return new float[]{0, 0};
        float[] feat = new float[6];
        feat[0] = MathHelper.wrapDegrees(mc.player.getYaw());
        feat[1] = mc.player.getPitch();
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
            System.err.println("[FunTimeNeural] ошибка инференса нейросети");
        }
        return new float[]{0, 0};
    }

    private void resetNeuralState() {
        buffer.clear();
        lastSmoothedYaw = 0;
        lastSmoothedPitch = 0;
        smoothDx = smoothDy = smoothDz = smoothDist = 0;
        aimOffset = Vec3d.ZERO;
        reactionDelay = 0;
        distracted = false;
        distractionTimer = 0;
        overshooting = false;
    }

    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        distracted = false;
        distractionTimer = 0;
    }

    public void onTargetChange() {
        reactionDelay = 2 + rng.nextInt(5);
        overshooting = false;
        distracted = false;
        distractionTimer = 0;
    }
}