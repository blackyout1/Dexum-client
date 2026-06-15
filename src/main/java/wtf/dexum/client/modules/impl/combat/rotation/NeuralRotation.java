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

    private OrtEnvironment env;
    private OrtSession session;
    private float[] meanX, scaleX, meanY, scaleY;

    private static final int SEQ_LEN = 10;
    private LinkedList<float[]> buffer = new LinkedList<>();

    // Гибридный порог
    private static final float HYBRID_THRESHOLD = 10.0f;
    // Ограничения для нейросети
    private static final float MAX_NEURAL_YAW = 4.0f;
    private static final float MAX_NEURAL_PITCH = 1.8f;

    // Сглаживание выхода
    private float lastSmoothedYaw = 0f, lastSmoothedPitch = 0f;
    private static final float SMOOTH_YAW = 0.55f;
    private static final float SMOOTH_PITCH = 0.2f;

    // Сглаживание координат цели
    private float smoothDx, smoothDy, smoothDz, smoothDist;

    // Человеческие факторы
    private final Random rng = new Random();
    private int reactionDelay = 0;
    private Vec3d aimOffset = Vec3d.ZERO;
    private int offsetTimer = 0;

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
        if (target == null) return;

        if (reactionDelay > 0) {
            reactionDelay--;
            return;
        }
        if (rng.nextFloat() < 0.06f) return;

        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();

        offsetTimer--;
        if (offsetTimer <= 0) {
            offsetTimer = 5 + rng.nextInt(8);
            double rX = (rng.nextDouble() - 0.5) * 0.15;
            double rY = (rng.nextDouble() - 0.5) * 0.15;
            double rZ = (rng.nextDouble() - 0.5) * 0.15;
            aimOffset = new Vec3d(
                    MathHelper.clamp(rX, box.minX - box.getCenter().x, box.maxX - box.getCenter().x),
                    MathHelper.clamp(rY, box.minY - box.getCenter().y, box.maxY - box.getCenter().y),
                    MathHelper.clamp(rZ, box.minZ - box.getCenter().z, box.maxZ - box.getCenter().z)
            );
        }

        Vec3d center = box.getCenter().add(aimOffset);
        float dx = (float)(center.x - eye.x);
        float dy = (float)(center.y - eye.y);
        float dz = (float)(center.z - eye.z);
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float alpha = 0.3f;
        smoothDx = smoothDx * (1 - alpha) + dx * alpha;
        smoothDy = smoothDy * (1 - alpha) + dy * alpha;
        smoothDz = smoothDz * (1 - alpha) + dz * alpha;
        smoothDist = smoothDist * (1 - alpha) + dist * alpha;

        float idealYaw = (float) Math.toDegrees(Math.atan2(smoothDz, smoothDx)) - 90.0F;
        float idealPitch = (float) -Math.toDegrees(Math.atan2(smoothDy,
                Math.sqrt(smoothDx*smoothDx + smoothDz*smoothDz)));
        idealYaw = MathHelper.wrapDegrees(idealYaw);
        idealPitch = MathHelper.clamp(idealPitch, -90.0F, 90.0F);

        float errorYaw = MathHelper.wrapDegrees(idealYaw - currentYaw);
        float errorPitch = idealPitch - currentPitch;
        float totalError = (float) Math.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);

        float deltaYaw = 0, deltaPitch = 0;

        if (totalError > HYBRID_THRESHOLD) {
            float speed = 5.0f + totalError * 0.25f;
            speed *= 0.85f + rng.nextFloat() * 0.3f;
            float maxYaw = Math.min(speed, 12.0f);
            float maxPitch = Math.min(speed * 0.5f, 4.0f);
            float scaleYaw = Math.min(1.0f, maxYaw / Math.max(Math.abs(errorYaw), 0.01f));
            float scalePitch = Math.min(1.0f, maxPitch / Math.max(Math.abs(errorPitch), 0.01f));
            deltaYaw = errorYaw * scaleYaw;
            deltaPitch = errorPitch * scalePitch;
        } else if (session != null) {
            float[] neural = getNeuralDeltas(smoothDx, smoothDy, smoothDz, smoothDist);
            float neuralYaw = MathHelper.clamp(neural[0], -MAX_NEURAL_YAW, MAX_NEURAL_YAW);
            float neuralPitch = MathHelper.clamp(neural[1], -MAX_NEURAL_PITCH, MAX_NEURAL_PITCH);
            deltaYaw = neuralYaw + errorYaw * 0.03f;
            deltaPitch = neuralPitch + errorPitch * 0.01f;
        } else {
            deltaYaw = errorYaw * 0.5f;
            deltaPitch = errorPitch * 0.5f;
        }

        deltaYaw += rng.nextGaussian() * 0.15;
        deltaPitch += rng.nextGaussian() * 0.08;

        if (Math.abs(errorPitch) < 0.5f) deltaPitch *= 0.2f;

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

        Rotation finalRot = new Rotation(newYaw, newPitch);
        float visualSpeed = 25.0f + totalError * 0.5f;
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);

        currentYaw = newYaw;
        currentPitch = newPitch;
        lastYaw = newYaw;
        lastPitch = newPitch;
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

    public void onTargetChange() {
        reactionDelay = 2 + rng.nextInt(4);
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
    }
}