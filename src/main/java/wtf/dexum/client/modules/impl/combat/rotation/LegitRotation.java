package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class LegitRotation extends RotationBase {

    private static final float FOLLOW_YAW = 0.15f;
    private static final float FOLLOW_PITCH = 0.08f;
    private static final float MAX_ACCEL_YAW = 1.8f;
    private static final float MAX_ACCEL_PITCH = 0.7f;
    private static final float AIM_SMOOTH_YAW = 0.5f;
    private static final float AIM_SMOOTH_PITCH = 0.06f;

    private long lastUpdateMs = 0L;
    private int lastTargetId = -1;
    private float velYaw = 0.0f;
    private float velPitch = 0.0f;
    private float commitSmooth = 0.0f;
    private float aimYaw = 0.0f;
    private float aimPitch = 0.0f;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private boolean haveAim = false;
    private float errorDerivative = 0f;

    // Для совместимости с Aura
    private float fatigue = 0f;
    private float stress = 0f;

    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            setYaw(currentYaw);
            setPitch(currentPitch);
        }
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (target == null || !Aura.INSTANCE.isEnabled()) {
            resetToVanilla(elytraVisual);
            return;
        }
        
        // Обновляем currentYaw и currentPitch из игрока
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }

        long now = System.currentTimeMillis();
        float dt = lastUpdateMs == 0L ? 1.0f : MathHelper.clamp((float)(now - lastUpdateMs) / 50.0f, 0.5f, 2.0f);
        this.lastUpdateMs = now;

        if (target.getId() != this.lastTargetId) {
            this.lastTargetId = target.getId();
            this.velYaw = 0.0f;
            this.velPitch = 0.0f;
            this.haveAim = false;
        }

        Vec3d aimPoint = this.getPredictedPoint(target, getAimPoint(target));
        Rotation desired = Rotation.getRotations(aimPoint);
        float desiredYaw = desired.getYaw();
        float desiredPitch = desired.getPitch();

        if (!this.haveAim) {
            this.aimYaw = desiredYaw;
            this.aimPitch = desiredPitch;
            this.haveAim = true;
        } else {
            this.aimYaw += MathHelper.wrapDegrees(desiredYaw - this.aimYaw) * MathHelper.clamp(0.5f * dt, 0.0f, 1.0f);
            this.aimPitch += (desiredPitch - this.aimPitch) * MathHelper.clamp(0.06f * dt, 0.0f, 1.0f);
        }

        this.aimYaw = MathHelper.wrapDegrees(this.aimYaw);
        this.aimPitch = MathHelper.clamp(this.aimPitch, -89.0f, 89.0f);

        float baseDiffYaw = MathHelper.wrapDegrees(this.aimYaw - currentYaw);
        float baseDiffPitch = this.aimPitch - currentPitch;
        float baseDist = (float)Math.hypot(baseDiffYaw, baseDiffPitch);

        // Упрощенная логика commitSmooth - убираем зависимость от errorDerivative
        boolean commit = Aura.INSTANCE != null && Aura.INSTANCE.isLegitAimCommit();
        this.commitSmooth += ((commit ? 1.0f : 0.0f) - this.commitSmooth) * MathHelper.clamp(0.15f * dt, 0.0f, 1.0f);

        // Уменьшенный tremorScale для более агрессивного поведения
        float hitChance = HitChanceUtil.get(target);
        float tremorScale = (1.0f - 0.5f * this.commitSmooth) * (1.0f - 0.2f * hitChance);

        float jitterYaw = HumanAimUtil.noiseYaw() * tremorScale;
        float jitterPitch = HumanAimUtil.noisePitch() * tremorScale * 0.25f;

        float diffYaw = MathHelper.wrapDegrees(this.aimYaw + jitterYaw - currentYaw);
        float diffPitch = MathHelper.wrapDegrees(this.aimPitch + jitterPitch - currentPitch);

        float reaction = HumanAimUtil.reactionFactor(target);
        float fov = HumanAimUtil.fovFactor(baseDiffYaw, baseDiffPitch);
        float speedJitter = HumanAimUtil.speedJitter();
        float gate = reaction * fov * speedJitter;

        float speedFrac = HumanAimUtil.bezierEase(MathHelper.clamp(baseDist / 60.0f, 0.0f, 1.0f));
        // Увеличенные maxSpeed для более быстрого поворота
        float maxSpeedYaw = (2.5f + speedFrac * 15.0f) * gate;
        float maxSpeedPitch = (0.8f + speedFrac * 3.5f) * gate;

        float stepYaw = this.follow(diffYaw, true, FOLLOW_YAW, maxSpeedYaw, MAX_ACCEL_YAW, dt);
        float stepPitch = this.follow(diffPitch, false, FOLLOW_PITCH, maxSpeedPitch, MAX_ACCEL_PITCH, dt);

        float newYaw = currentYaw + stepYaw;
        float newPitch = MathHelper.clamp(currentPitch + stepPitch, -89.0f, 89.0f);

        RotationComponent.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, elytraVisual);
        setYaw(newYaw);
        setPitch(newPitch);
        
        // Обновляем currentYaw и currentPitch после поворота
        this.currentYaw = newYaw;
        this.currentPitch = newPitch;
        
        // Вычисляем errorDerivative как разницу между идеальным углом и текущим углом
        Vec3d targetPos = getAimPoint(target);
        Rotation ideal = Rotation.getRotations(targetPos);
        float idealYaw = ideal.getYaw();
        float idealPitch = ideal.getPitch();
        float errorMag = (float)Math.hypot(
                MathHelper.wrapDegrees(idealYaw - newYaw),
                idealPitch - newPitch
        );
        this.errorDerivative = (errorMag - this.errorDerivative) / Math.max(dt, 0.01f);
        this.errorDerivative = MathHelper.clamp(this.errorDerivative, -20f, 20f);
    }

    private Vec3d getPredictedPoint(LivingEntity target, Vec3d basePoint) {
        return basePoint;
    }

    private Vec3d getAimPoint(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    private float follow(float error, boolean yawAxis, float followGain, float maxSpeed, float maxAccel, float dt) {
        float vel = yawAxis ? this.velYaw : this.velPitch;
        float desiredVel = MathHelper.clamp(error * followGain, -maxSpeed, maxSpeed);
        float dv = MathHelper.clamp(desiredVel - vel, -maxAccel * dt, maxAccel * dt);
        vel += dv;
        if (yawAxis) {
            this.velYaw = vel;
        } else {
            this.velPitch = vel;
        }
        return vel * dt;
    }

    public boolean shouldAttack(LivingEntity target) {
        if (target == null) return false;
        
        // Вычисляем идеальный угол к цели
        Vec3d targetPos = getAimPoint(target);
        Rotation ideal = Rotation.getRotations(targetPos);
        float idealYaw = ideal.getYaw();
        float idealPitch = ideal.getPitch();
        
        // Используем текущий угол изLegitRotation, а не из mc.player
        float errorYaw = MathHelper.wrapDegrees(idealYaw - currentYaw);
        float errorPitch = idealPitch - currentPitch;
        float errorMag = (float)Math.hypot(errorYaw, errorPitch);
        
        // Атака разрешена, если ошибка маленькая (простой порог без дополнительных проверок)
        // Это делает поведение более агрессивным и похожим на настоящего игрока
        return errorMag < 3.0f;
    }

    public void onAttack() {
        // вызывается после атаки, можно сбросить что-то
    }

    public void onTargetChange() {
        HumanAimUtil.reset();
        this.velYaw = this.velPitch = 0f;
        this.haveAim = false;
        this.currentYaw = 0f;
        this.currentPitch = 0f;
    }

    private void resetToVanilla(boolean elytraVisual) {
        if (mc.player == null) return;
        // Плавный возврат к ванильным углам
        float targetYaw = mc.player.getYaw();
        float targetPitch = mc.player.getPitch();
        float lerpYaw = MathHelper.lerp(0.55f, currentYaw, targetYaw);
        float lerpPitch = MathHelper.lerp(0.55f, currentPitch, targetPitch);
        setYaw(lerpYaw);
        setPitch(lerpPitch);
        this.currentYaw = lerpYaw;
        this.currentPitch = lerpPitch;
        this.velYaw = 0f;
        this.velPitch = 0f;
        this.fatigue = 0f;
        this.stress = 0f;
        this.errorDerivative = 0f;
        RotationComponent.update(new Rotation(lerpYaw, lerpPitch), 360, 360, 360, 360, 0, 2, elytraVisual);
    }

    public float getFatigue() { return this.fatigue; }
    public float getStress() { return 0f; }

    public float getAimYaw() { return aimYaw; }
    public float getAimPitch() { return aimPitch; }
    public float getErrorDerivative() { return errorDerivative; }
}
