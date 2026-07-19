package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.ThreadLocalRandom;

public class HumanAimUtil {
    private static int lastTargetId = -1;
    private static long reactionStart = 0L;
    private static long reactionDelay = 0L;
    private static long rampDuration = 160L;
    private static float speedJitterValue = 1.0f;

    public static float reactionFactor(Entity target) {
        long now = System.currentTimeMillis();
        int id = target == null ? -1 : target.getId();
        if (id != lastTargetId) {
            lastTargetId = id;
            reactionStart = now;
            reactionDelay = ThreadLocalRandom.current().nextLong(90L, 230L);
            rampDuration = ThreadLocalRandom.current().nextLong(120L, 220L);
        }
        long elapsed = now - reactionStart;
        if (elapsed <= reactionDelay) return 0.0f;
        float t = (float)(elapsed - reactionDelay) / (float) rampDuration;
        if (t >= 1.0f) return 1.0f;
        return (float)(1.0 - Math.pow(1.0 - t, 3.0));
    }

    public static void reset() {
        lastTargetId = -1;
        reactionStart = 0L;
        reactionDelay = 0L;
    }

    public static float noiseYaw() {
        double t = (double) System.nanoTime() / 1.0E9;
        return (float)(Math.sin(t * 2.3) * 0.16 + Math.sin(t * 5.7 + 1.1) * 0.05);
    }

    public static float noisePitch() {
        double t = (double) System.nanoTime() / 1.0E9;
        return (float)(Math.sin(t * 1.9 + 0.7) * 0.09 + Math.sin(t * 4.3 + 2.2) * 0.03);
    }

    public static float bezierEase(float t) {
        float c = MathHelper.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }

    public static float fovFactor(float diffYaw, float diffPitch) {
        float dist = (float) Math.hypot(diffYaw, diffPitch);
        if (dist <= 30.0f) return 1.0f;
        if (dist >= 120.0f) return 0.55f;
        return 1.0f - (dist - 30.0f) / 90.0f * 0.45f;
    }

    public static float speedJitter() {
        float target = (float)(1.0 + ThreadLocalRandom.current().nextGaussian() * 0.08);
        target = MathHelper.clamp(target, 0.85f, 1.15f);
        speedJitterValue += (target - speedJitterValue) * 0.08f;
        return speedJitterValue;
    }
}