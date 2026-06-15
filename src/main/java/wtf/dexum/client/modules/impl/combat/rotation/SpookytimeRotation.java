package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.RaytracingUtil;

public class SpookytimeRotation extends RotationBase {
    private float currentYaw = 0.0F;
    private float currentPitch = 0.0F;
    private boolean isInitialized = false;

    private LivingEntity lastTarget = null;
    private float lastStepYaw = 0.0F;
    private float lastStepPitch = 0.0F;

    // Точка прицела с плавным блужданием
    private Vec3d aimPoint = null;
    private Vec3d aimVelocity = Vec3d.ZERO;
    private static final double AIM_FRICTION = 0.85;
    private static final double AIM_FORCE = 0.025;
    private int aimJitterTimer = 0;

    // Параметры человеческого движения
    private int reactionDelay = 0;          // задержка перед стартом движения
    private boolean isAccelerating = false;
    private double currentSpeed = 0.0;     // текущая угловая скорость (градусов/тик)
    private double targetSpeed = 0.0;
    private static final double MAX_SPEED = 3.2;    // ~19°/сек при 20 tps
    private static final double ACCEL = 0.4;        // скорость разгона
    private static final double DECEL = 0.35;       // скорость торможения
    private int speedChangeTimer = 0;

    // Овершут и коррекция
    private float overshootYaw = 0.0F;
    private float overshootPitch = 0.0F;
    private int overshootTicks = 0;

    // Тряска (очень низкая амплитуда, хаотичная)
    private double noiseTime = 0.0;
    private final double noiseSeedX, noiseSeedY;

    // Генератор некратных GCD шумов
    private float gcdNoiseYaw = 0.0f;
    private float gcdNoisePitch = 0.0f;

    public SpookytimeRotation() {
        noiseSeedX = rng.nextDouble() * 100.0;
        noiseSeedY = rng.nextDouble() * 100.0;
    }

    private double noise(double x, double y) {
        double n = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
        return n - Math.floor(n);
    }

    private double smoothNoise(double x, double y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        double fx = x - ix;
        double fy = y - iy;
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sy = fy * fy * (3.0 - 2.0 * fy);
        return (1-sy)*((1-sx)*noise(ix,iy) + sx*noise(ix+1,iy)) +
                sy*((1-sx)*noise(ix,iy+1) + sx*noise(ix+1,iy+1));
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!isInitialized) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            this.isInitialized = true;
        }

        // Сброс при смене цели
        if (target != lastTarget) {
            this.lastTarget = target;
            Box box = target.getBoundingBox();
            this.aimPoint = box.getCenter();
            this.aimVelocity = Vec3d.ZERO;
            this.lastStepYaw = 0.0F;
            this.lastStepPitch = 0.0F;
            // Реакция человека: 100-250 мс задержка
            this.reactionDelay = 2 + rng.nextInt(4); // 2-5 тиков
            this.currentSpeed = 0.0;
            this.targetSpeed = 0.0;
            this.isAccelerating = false;
            this.speedChangeTimer = 10 + rng.nextInt(15);
            this.overshootYaw = 0.0F;
            this.overshootPitch = 0.0F;
            this.overshootTicks = 0;
        }

        if (target == null || aimPoint == null) return;

        // Задержка реакции
        if (reactionDelay > 0) {
            reactionDelay--;
            // Микро-дрожание (человек не держит прицел идеально)
            float microShake = (rng.nextFloat() - 0.5f) * 0.03f;
            this.currentYaw = MathHelper.wrapDegrees(this.currentYaw + microShake);
            return;
        }

        // Обновление точки прицела (плавное блуждание по телу)
        Box box = target.getBoundingBox();
        aimJitterTimer++;
        if (aimJitterTimer > 8 + rng.nextInt(10)) {
            aimJitterTimer = 0;
            // Случайный толчок
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
        this.aimPoint = candidate;

        // Целевые углы
        Vec3d eyePos = mc.player.getEyePos();
        double dx = aimPoint.x - eyePos.x;
        double dy = aimPoint.y - eyePos.y;
        double dz = aimPoint.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx*dx + dz*dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
        targetYaw = MathHelper.wrapDegrees(targetYaw);
        targetPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);

        // Если активен overshoot, корректируем цель
        if (overshootTicks > 0) {
            targetYaw = MathHelper.wrapDegrees(targetYaw + overshootYaw);
            targetPitch = MathHelper.clamp(targetPitch + overshootPitch, -90.0F, 90.0F);
            overshootTicks--;
            if (overshootTicks == 0) {
                overshootYaw = 0;
                overshootPitch = 0;
            }
        }

        // Ошибка
        float errorYaw = MathHelper.wrapDegrees(targetYaw - this.currentYaw);
        float errorPitch = targetPitch - this.currentPitch;
        double totalError = Math.sqrt(errorYaw*errorYaw + errorPitch*errorPitch);

        // Адаптивная целевая скорость: на большом расстоянии быстрее, вблизи медленнее
        boolean hasTrace = RaytracingUtil.rayTrace(mc.player.getRotationVector(), 3.0, box);
        double dist = eyePos.distanceTo(box.getCenter());
        double desiredMaxSpeed;
        if (hasTrace) {
            desiredMaxSpeed = 1.0 + rng.nextDouble() * 1.5; // 1.0-2.5
        } else {
            double distFactor = Math.min(1.0, dist / 6.0);
            desiredMaxSpeed = 1.8 + distFactor * 1.5 + rng.nextDouble() * 0.8; // 1.8-4.1
        }
        desiredMaxSpeed = MathHelper.clamp(desiredMaxSpeed, 0.8, MAX_SPEED);

        // Ограничение скорости если ошибка маленькая (точная доводка)
        if (totalError < 3.0) {
            desiredMaxSpeed *= totalError / 3.0 * 0.7 + 0.3;
        }

        // Плавное изменение целевой скорости (естественное ускорение/замедление)
        speedChangeTimer--;
        if (speedChangeTimer <= 0) {
            speedChangeTimer = 8 + rng.nextInt(15);
            targetSpeed = desiredMaxSpeed * (0.7 + rng.nextDouble() * 0.6);
        }

        // Имитация разгона/торможения
        if (currentSpeed < targetSpeed) {
            currentSpeed += ACCEL;
            if (currentSpeed > targetSpeed) currentSpeed = targetSpeed;
        } else {
            currentSpeed -= DECEL;
            if (currentSpeed < targetSpeed) currentSpeed = targetSpeed;
        }
        currentSpeed = MathHelper.clamp(currentSpeed, 0.0, MAX_SPEED);

        // Вычисляем шаг
        double maxStep = currentSpeed;
        double stepScale = Math.min(1.0, maxStep / Math.max(totalError, 0.01));
        double rawStepYaw = errorYaw * stepScale;
        double rawStepPitch = errorPitch * stepScale;

        // Сглаживание (инерция мыши)
        float smoothYaw = (float)(rawStepYaw * 0.7 + lastStepYaw * 0.3);
        float smoothPitch = (float)(rawStepPitch * 0.7 + lastStepPitch * 0.3);
        lastStepYaw = smoothYaw;
        lastStepPitch = smoothPitch;

        // GCD fix + неидеальный шум
        float gcd = Rotation.gcd();
        int mX = Math.round((smoothYaw + gcdNoiseYaw) / gcd);
        int mY = Math.round((smoothPitch + gcdNoisePitch) / gcd);
        if (mX == 0 && mY == 0) return;

        // Обновляем шум для следующего тика (медленно меняющийся)
        gcdNoiseYaw += (rng.nextFloat() - 0.5f) * 0.02f;
        gcdNoisePitch += (rng.nextFloat() - 0.5f) * 0.02f;
        gcdNoiseYaw = MathHelper.clamp(gcdNoiseYaw, -0.1f, 0.1f);
        gcdNoisePitch = MathHelper.clamp(gcdNoisePitch, -0.08f, 0.08f);

        float steppedYaw = this.currentYaw + mX * gcd;
        float steppedPitch = MathHelper.clamp(this.currentPitch + mY * gcd, -90.0F, 90.0F);

        // Очень лёгкая тряска (почти незаметная)
        noiseTime += 0.01 + rng.nextDouble() * 0.005;
        double n1 = noiseTime * 0.7 + noiseSeedX;
        double n2 = noiseTime * 0.9 + noiseSeedY;
        double shakeYaw = (smoothNoise(n1, n2) * 2.0 - 1.0) * 0.04;
        double shakePitch = (smoothNoise(n2, n1) * 2.0 - 1.0) * 0.03;

        float finalYaw = MathHelper.wrapDegrees(steppedYaw + (float)shakeYaw);
        float finalPitch = MathHelper.clamp(steppedPitch + (float)shakePitch, -90.0F, 90.0F);

        // Иногда создаём искусственный overshoot (человек перелетел цель)
        if (totalError > 6.0 && rng.nextFloat() < 0.03 && overshootTicks == 0) {
            overshootYaw = (rng.nextFloat() - 0.5f) * 1.2f;
            overshootPitch = (rng.nextFloat() - 0.5f) * 0.8f;
            overshootTicks = 2 + rng.nextInt(3);
        }

        correctMovement(finalYaw);

        Rotation finalRot = new Rotation(finalYaw, finalPitch);
        float visualSpeed = 25.0f + (float)totalError * 0.8f + rng.nextFloat() * 5.0f;
        visualSpeed = MathHelper.clamp(visualSpeed, 25.0f, 50.0f);
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);

        this.currentYaw = finalYaw;
        this.currentPitch = finalPitch;
        this.lastYaw = finalYaw;
        this.lastPitch = finalPitch;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        if (mc.player != null) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
        }
        this.isInitialized = false;
        this.lastStepYaw = 0.0F;
        this.lastStepPitch = 0.0F;
        this.aimPoint = null;
        this.lastTarget = null;
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
}