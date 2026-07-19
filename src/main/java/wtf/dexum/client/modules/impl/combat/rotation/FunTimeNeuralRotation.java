package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

import java.util.Random;

public class FunTimeNeuralRotation extends RotationBase {

    private float currentYaw, currentPitch;
    private boolean initialized = false;

    private float velocityYaw = 0f;
    private float velocityPitch = 0f;

    private final Random rng = new Random();
    private long lastAttackTime = 0;
    private LivingEntity lastTarget = null;

    private float prevErrorMag = 0f;
    private float errorDerivative = 0f;

    private int reactionDelayTicks = 0;

    private float fatigue = 0f;
    private long combatStartTime = 0;

    private double tremorTime = 0;
    private final float tremorFreqYaw = 9.3f + rng.nextFloat() * 2.5f;
    private final float tremorFreqPitch = 8.1f + rng.nextFloat() * 2.2f;
    private final float tremorPhaseYaw = rng.nextFloat() * 6.2832f;
    private final float tremorPhasePitch = rng.nextFloat() * 6.2832f;

    public FunTimeNeuralRotation() {}


    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        initialized = true;
        // Сбрасываем скорость, чтобы не накапливалась без цели
        velocityYaw = velocityPitch = 0f;
        fatigue = 0f;
        combatStartTime = 0;
    }


    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!initialized) update(null, false);
        if (target == null || !Aura.INSTANCE.isEnabled()) {
            resetToVanilla(elytraVisual);
            return;
        }

        if (target != lastTarget) {
            onTargetChange();
            lastTarget = target;
        }

        if (reactionDelayTicks > 0) {
            reactionDelayTicks--;
            return;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d center = target.getBoundingBox().getCenter();
        float dx = (float)(center.x - eye.x);
        float dy = (float)(center.y - eye.y);
        float dz = (float)(center.z - eye.z);
        float idealYaw = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float idealPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));

        long now = System.currentTimeMillis();
        if (combatStartTime == 0) combatStartTime = now;
        float fightDuration = (now - combatStartTime) / 1000f;
        fatigue = Math.min(1f, fightDuration / 15f);
        if (now - lastAttackTime > 5000) fatigue = Math.max(0, fatigue - 0.02f);

        boolean lowHP = mc.player.getHealth() < 4f;
        boolean underPressure = (now - lastAttackTime < 2000);
        float stress = (lowHP || underPressure) ? 1f : 0f;

        float k = 2.8f - 0.7f * fatigue + 1.2f * stress;
        float b = 1.8f + 0.5f * fatigue;

        float errorYaw = MathHelper.wrapDegrees(idealYaw - currentYaw);
        float errorPitch = idealPitch - currentPitch;
        float errorMag = (float)Math.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);

        // Мёртвая зона + дополнительное торможение при малой ошибке
        if (errorMag < 0.8f && !(stress > 0.5f)) {
            // Сильно тормозим скорость, чтобы избежать раскачки
            velocityYaw *= 0.4f;
            velocityPitch *= 0.4f;
            // Микро‑дрейф только если скорость уже почти ноль
            if (Math.abs(velocityYaw) < 0.5f && Math.abs(velocityPitch) < 0.5f) {
                errorYaw += (rng.nextFloat() - 0.5f) * 0.3f;
                errorPitch += (rng.nextFloat() - 0.5f) * 0.15f;
            }
        }

        float dt = 0.05f;
        float accelYaw = k * errorYaw - b * velocityYaw;
        float accelPitch = k * errorPitch - b * velocityPitch;

        velocityYaw += accelYaw * dt;
        velocityPitch += accelPitch * dt;

        // Жёсткое ограничение скорости
        velocityYaw = MathHelper.clamp(velocityYaw, -15f, 15f);
        velocityPitch = MathHelper.clamp(velocityPitch, -10f, 10f);

        // Максимальный поворот за кадр
        float maxStep = 12f;
        float stepYaw = MathHelper.clamp(velocityYaw * dt, -maxStep, maxStep);
        float stepPitch = MathHelper.clamp(velocityPitch * dt, -maxStep, maxStep);

        float newYaw = currentYaw + stepYaw;
        float newPitch = currentPitch + stepPitch;

        // Тремор (лёгкий)
        tremorTime += dt;
        float tremorAmp = 0.06f * (1f + fatigue); // уменьшил до 0.06
        newYaw += tremorAmp * (float)Math.sin(2 * Math.PI * tremorFreqYaw * tremorTime + tremorPhaseYaw);
        newPitch += tremorAmp * (float)Math.sin(2 * Math.PI * tremorFreqPitch * tremorTime + tremorPhasePitch);

        newPitch = MathHelper.clamp(newPitch, -90, 90);

        // Стресс‑промах
        if (stress > 0.5f && rng.nextFloat() < 0.15f) {
            Vec3d vel = target.getVelocity();
            if (vel.lengthSquared() > 0.01) {
                Vec3d offset = vel.normalize().multiply(3 + rng.nextFloat() * 2);
                float missYaw = (float)Math.toDegrees(Math.atan2(offset.z, offset.x));
                newYaw += missYaw * 0.3f;
            }
        }

        float gcd = Rotation.gcd();
        newYaw = currentYaw + Math.round((newYaw - currentYaw) / gcd) * gcd;

        RotationComponent.update(new Rotation(newYaw, newPitch), 27f, 27f, 27f, 27f, 0, 1, elytraVisual);
        currentYaw = newYaw;
        currentPitch = newPitch;

        errorDerivative = (errorMag - prevErrorMag) / dt;
        prevErrorMag = errorMag;
    }

    public boolean shouldAttack(LivingEntity target) {
        if (target == null) return false;
        Rotation ideal = Rotation.getRotations(target.getBoundingBox().getCenter());
        float errorMag = (float)Math.sqrt(
                Math.pow(MathHelper.wrapDegrees(ideal.getYaw() - currentYaw), 2) +
                        Math.pow(ideal.getPitch() - currentPitch, 2)
        );
        return errorMag < 10f;
    }

    private void resetToVanilla(boolean elytraVisual) {
        if (mc.player == null) return;
        // Плавный возврат к ванильным углам
        currentYaw = MathHelper.lerp(0.55f, currentYaw, mc.player.getYaw());
        currentPitch = MathHelper.lerp(0.55f, currentPitch, mc.player.getPitch());
        // Сброс скоростей, чтобы не было вращения
        velocityYaw = 0f;
        velocityPitch = 0f;
        fatigue = 0f;
        combatStartTime = 0;
        RotationComponent.update(new Rotation(currentYaw, currentPitch), 360, 360, 360, 360, 0, 2, elytraVisual);
    }

    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
        reactionDelayTicks = 3 + rng.nextInt(4);
    }

    public void onTargetChange() {
        velocityYaw = velocityPitch = 0f;
        prevErrorMag = 0f;
        errorDerivative = 0f;
        reactionDelayTicks = 5 + rng.nextInt(6);
        combatStartTime = System.currentTimeMillis();
    }

    public float getFatigue() { return fatigue; }
    public float getStress() { return (mc.player != null && mc.player.getHealth() < 4f) ? 1f : 0f; }
}