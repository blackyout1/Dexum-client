package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.interfaces.IMinecraft;

import java.util.Random;

/**
 * Упрощенная модель для CLAMP DELTAS - основной элемент обхода Polar/VonTam.
 * Ограничивает скорость поворота по гайду:
 * - yawDelta <= 60 + random() * 1.0329834f
 * - pitchDelta <= random(23.133F, 26.477F)
 */
public class HumanMovementModel implements IMinecraft {
    private final Random rng = new Random();

    // CLAMP DELTAS параметры по гайду
    private static final float MAX_YAW_DELTA_BASE = 60.0f;
    private static final float YAW_JITTER = 1.0329834f;
    private static final float PITCH_MIN = 23.133f;
    private static final float PITCH_MAX = 26.477f;

    /**
     * Получает максимальный yaw delta для clamping.
     */
    public float getMaxYawDelta() {
        return MAX_YAW_DELTA_BASE + rng.nextFloat() * YAW_JITTER;
    }

    /**
     * Получает максимальный pitch delta для clamping.
     */
    public float getMaxPitchDelta() {
        return rng.nextFloat(PITCH_MIN, PITCH_MAX);
    }

    /**
     * Применяет clamping к deltaYaw и deltaPitch.
     */
    public void clampDeltas(float[] deltas) {
        float deltaYaw = deltas[0];
        float deltaPitch = deltas[1];
        
        float maxYaw = getMaxYawDelta();
        float maxPitch = getMaxPitchDelta();
        
        deltas[0] = MathHelper.clamp(deltaYaw, -maxYaw, maxYaw);
        deltas[1] = MathHelper.clamp(deltaPitch, -maxPitch, maxPitch);
    }

    /**
     * Применяет micro-jitter к deltaYaw и deltaPitch для реалистичности.
     */
    public void applyJitter(float[] deltas) {
        float deltaYaw = deltas[0];
        float deltaPitch = deltas[1];
        
        // Очень слабый шум (почти незаметный)
        float jitterYaw = (rng.nextFloat() - 0.5f) * 0.05f;
        float jitterPitch = (rng.nextFloat() - 0.5f) * 0.03f;
        
        deltas[0] = deltaYaw + jitterYaw;
        deltas[1] = deltaPitch + jitterPitch;
    }

    /**
     * Сброс состояния.
     */
    public void reset() {
        // Нет состояния для сброса
    }
}
