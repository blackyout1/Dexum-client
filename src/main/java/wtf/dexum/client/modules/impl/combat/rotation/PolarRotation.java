package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class PolarRotation extends RotationBase {
    private float currentYaw, currentPitch;
    private boolean initialized;
    
    // Сглаживание и инерция
    private float velocityYaw = 0.0f;
    private float velocityPitch = 0.0f;
    private static final float ACCEL = 0.25f;
    private static final float FRICTION = 0.75f;
    
    // Параметры ротации (из гайда)
    private static final float AVG_YAW_DELTA = 60.0f;
    private static final float AVG_PITCH_DELTA = 23.0f;
    private static final float MAX_YAW_DELTA = 62.0f;
    private static final float MAX_PITCH_DELTA = 26.5f;
    
    // Точка прицела с плавным блужданием
    private Vec3d aimPoint = null;
    private Vec3d aimVelocity = Vec3d.ZERO;
    private static final double AIM_FRICTION = 0.85;
    private static final double AIM_FORCE = 0.025;
    private int aimUpdateTimer = 0;
    
    // Задержка реакции
    private int reactionDelay = 0;
    private boolean hasReactionDelay = false;
    
    // Овершут
    private float overshootYaw = 0.0f;
    private float overshootPitch = 0.0f;
    private int overshootTicks = 0;
    
    // GCD шум
    private float gcdNoiseYaw = 0.0f;
    private float gcdNoisePitch = 0.0f;
    
    private LivingEntity lastTarget = null;
    private float lastStepYaw = 0.0f;
    private float lastStepPitch = 0.0f;

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!initialized) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            initialized = true;
        }
        
        // Сброс при смене цели
        if (target != lastTarget) {
            this.lastTarget = target;
            Box box = target.getBoundingBox();
            aimPoint = box.getCenter();
            aimVelocity = Vec3d.ZERO;
            lastStepYaw = 0.0f;
            lastStepPitch = 0.0f;
            reactionDelay = 2 + rng.nextInt(4);
            hasReactionDelay = true;
            overshootYaw = 0.0f;
            overshootPitch = 0.0f;
            overshootTicks = 0;
        }
        
        if (target == null) {
            velocityYaw *= FRICTION;
            velocityPitch *= FRICTION;
            hasReactionDelay = false;
            return;
        }
        
        // Задержка реакции
        if (hasReactionDelay) {
            reactionDelay--;
            if (reactionDelay <= 0) {
                hasReactionDelay = false;
            }
            float microShake = (rng.nextFloat() - 0.5f) * 0.03f;
            currentYaw = MathHelper.wrapDegrees(currentYaw + microShake);
            return;
        }
        
        // Обновление точки прицела (плавное блуждание по хитбоксу)
        Box box = target.getBoundingBox();
        aimUpdateTimer++;
        if (aimUpdateTimer > 8 + rng.nextInt(10)) {
            aimUpdateTimer = 0;
            aimVelocity = new Vec3d(
                    (rng.nextDouble() - 0.5) * AIM_FORCE * 2.0,
                    (rng.nextDouble() - 0.5) * AIM_FORCE * 1.5,
                    (rng.nextDouble() - 0.5) * AIM_FORCE * 2.0
            );
        } else {
            aimVelocity = aimVelocity.multiply(AIM_FRICTION);
        }
        Vec3d candidate = aimPoint.add(aimVelocity);
        candidate = new Vec3d(
                MathHelper.clamp(candidate.x, box.minX, box.maxX),
                MathHelper.clamp(candidate.y, box.minY, box.maxY),
                MathHelper.clamp(candidate.z, box.minZ, box.maxZ)
        );
        aimPoint = candidate;
        
        // Целевые углы
        Vec3d eyePos = mc.player.getEyePos();
        double dx = aimPoint.x - eyePos.x;
        double dy = aimPoint.y - eyePos.y;
        double dz = aimPoint.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx*dx + dz*dz);
        
        float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float)-Math.toDegrees(Math.atan2(dy, horizontalDist));
        targetYaw = MathHelper.wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
        
        // Овершут
        if (overshootTicks > 0) {
            targetYaw = MathHelper.wrapDegrees(targetYaw + overshootYaw);
            targetPitch = MathHelper.clamp(targetPitch + overshootPitch, -90.0F, 90.0F);
            overshootTicks--;
        }
        
        // Ошибка
        float errorYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float errorPitch = targetPitch - currentPitch;
        float totalError = MathHelper.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);
        
        // Кламп дельт по гайду:
        // yawDelta = Math.min(Math.abs(yawDelta), 60 + (random() * 1.0329834f));
        // pitchDelta = Math.min(Math.abs(pitchDelta), random(23.133F, 26.477F));
        float maxYawStep = MAX_YAW_DELTA + rng.nextFloat() * 1.0329834f;
        float maxPitchStep = rng.nextFloat(23.133F, 26.477F);
        
        // Адаптивная скорость: 65-75% от максимальной ротации
        float speedFactor = Math.min(1.0f, totalError / 45.0f);
        float yawSpeed = AVG_YAW_DELTA * (0.65f + rng.nextFloat() * 0.1f) * speedFactor;
        float pitchSpeed = AVG_PITCH_DELTA * (0.65f + rng.nextFloat() * 0.1f) * speedFactor;
        
        // Ограничиваем
        yawSpeed = MathHelper.clamp(yawSpeed, 5.0f, maxYawStep);
        pitchSpeed = MathHelper.clamp(pitchSpeed, 3.0f, maxPitchStep);
        
        // Адаптивный разгон/торможение
        float currentYawSpeed = Math.abs(velocityYaw);
        float currentPitchSpeed = Math.abs(velocityPitch);
        
        if (currentYawSpeed < yawSpeed) {
            velocityYaw += ACCEL * Math.signum(errorYaw);
        } else {
            velocityYaw -= FRICTION * Math.signum(velocityYaw);
        }
        
        if (currentPitchSpeed < pitchSpeed) {
            velocityPitch += ACCEL * Math.signum(errorPitch);
        } else {
            velocityPitch -= FRICTION * Math.signum(velocityPitch);
        }
        
        velocityYaw = MathHelper.clamp(velocityYaw, -yawSpeed, yawSpeed);
        velocityPitch = MathHelper.clamp(velocityPitch, -pitchSpeed, pitchSpeed);
        
        // Вычисляем шаги
        float stepYaw = velocityYaw;
        float stepPitch = velocityPitch;
        
        // Сглаживание (инерция)
        stepYaw = stepYaw * 0.6f + lastStepYaw * 0.4f;
        stepPitch = stepPitch * 0.6f + lastStepPitch * 0.4f;
        lastStepYaw = stepYaw;
        lastStepPitch = stepPitch;
        
        // GCD fix с шумом
        float gcd = Rotation.gcd();
        int mX = Math.round((stepYaw + gcdNoiseYaw) / gcd);
        int mY = Math.round((stepPitch + gcdNoisePitch) / gcd);
        if (mX == 0 && mY == 0) return;
        
        // Обновляем GCD шум
        gcdNoiseYaw += (rng.nextFloat() - 0.5f) * 0.02f;
        gcdNoisePitch += (rng.nextFloat() - 0.5f) * 0.02f;
        gcdNoiseYaw = MathHelper.clamp(gcdNoiseYaw, -0.1f, 0.1f);
        gcdNoisePitch = MathHelper.clamp(gcdNoisePitch, -0.08f, 0.08f);
        
        float newYaw = currentYaw + mX * gcd;
        float newPitch = MathHelper.clamp(currentPitch + mY * gcd, -90.0F, 90.0F);
        
        // Очень лёгкая тряска (человеческий фактор)
        float shakeYaw = (rng.nextFloat() - 0.5f) * 0.08f;
        float shakePitch = (rng.nextFloat() - 0.5f) * 0.05f;
        
        newYaw = MathHelper.wrapDegrees(newYaw + shakeYaw);
        newPitch = MathHelper.clamp(newPitch + shakePitch, -90.0F, 90.0F);
        
        // Иногда создаём искусственный overshoot
        if (totalError > 8.0f && rng.nextFloat() < 0.02f && overshootTicks == 0) {
            overshootYaw = (rng.nextFloat() - 0.5f) * 3.5f;
            overshootPitch = (rng.nextFloat() - 0.5f) * 2.5f;
            overshootTicks = 2 + rng.nextInt(3);
        }
        
        correctMovement(newYaw);
        
        Rotation finalRot = new Rotation(newYaw, newPitch);
        float visualSpeed = 25.0f + totalError * 0.6f + rng.nextFloat() * 8.0f;
        visualSpeed = MathHelper.clamp(visualSpeed, 20.0f, 60.0f);
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);
        
        currentYaw = newYaw;
        currentPitch = newPitch;
        lastYaw = newYaw;
        lastPitch = newPitch;
    }
    
    private void correctMovement(float rotationYaw) {
        if (mc.player == null) return;
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0.0F && strafe == 0.0F) return;
        
        float clientYaw = mc.player.getYaw();
        double angleRad = Math.toRadians(MathHelper.wrapDegrees(rotationYaw - clientYaw));
        
        float newForward = (float)(forward * Math.cos(angleRad) + strafe * Math.sin(angleRad));
        float newStrafe = (float)(strafe * Math.cos(angleRad) - forward * Math.sin(angleRad));
        
        mc.player.input.movementForward = newForward;
        mc.player.input.movementSideways = newStrafe;
    }
    
    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
        }
        initialized = false;
        aimPoint = null;
        aimVelocity = Vec3d.ZERO;
        lastTarget = null;
    }
    
    public void onAttack() {
        velocityYaw *= 0.3f;
        velocityPitch *= 0.3f;
    }
    
    public void onTargetChange() {
        velocityYaw = 0.0f;
        velocityPitch = 0.0f;
        hasReactionDelay = true;
        reactionDelay = 2 + rng.nextInt(3);
        overshootTicks = 0;
        lastTarget = null;
    }
}
