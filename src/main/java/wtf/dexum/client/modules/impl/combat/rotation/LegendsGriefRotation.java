package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.predict.PredictUtils;

public class LegendsGriefRotation extends RotationBase {
    private float acceleration;
    private boolean isBack;

    public void update(LivingEntity target, Rotation angle, float distance, float predict, boolean elytraVisual) {
        if (mc.player.isGliding()) {
            if (this.noCircling) {
                this.acceleration = 0.17F;
            } else if (mc.player.isGliding() && target.isGliding()) {
                if (this.isBack) {
                    if (this.acceleration >= -0.02F) {
                        float decel = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw)) > 80.0F ? 0.1F : rng.nextFloat(0.001f, 0.005F);
                        decel *= (0.95F + rng.nextFloat() * 0.1F);
                        this.acceleration -= decel;
                    }
                    if (this.acceleration <= -0.02F) this.isBack = false;
                } else {
                    float accel = rng.nextFloat(0.005F, 0.012F);
                    accel *= (0.92F + rng.nextFloat() * 0.16F);
                    this.acceleration += accel;
                    if (this.acceleration >= 0.17F || RaytracingUtil.rayTrace(mc.player.getRotationVector(), 3.0D, target.getBoundingBox().offset(mc.player.isGliding() && target instanceof PlayerEntity && target.isGliding() ? PredictUtils.predict(target, target.getPos(), predict) : Vec3d.ZERO))) {
                        this.isBack = true;
                    }
                }
            } else {
                if (this.isBack) {
                    if (this.acceleration >= -0.01F) {
                        float decel = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw)) > 80.0F ? 0.1F : rng.nextFloat(0.001f, 0.005F);
                        decel *= (0.94F + rng.nextFloat() * 0.12F);
                        this.acceleration -= decel;
                    }
                    if (this.acceleration <= -0.01F) this.isBack = false;
                } else {
                    float accel = rng.nextFloat(0.001F, 0.007F);
                    accel *= (0.9F + rng.nextFloat() * 0.2F);
                    this.acceleration += accel;
                    if (this.acceleration >= 0.22F || RaytracingUtil.rayTrace(mc.player.getRotationVector(), 3.0D, target.getBoundingBox().offset(mc.player.isGliding() && target instanceof PlayerEntity && target.isGliding() ? PredictUtils.predict(target, target.getPos(), predict) : Vec3d.ZERO).expand(-0.5D))) {
                        this.isBack = true;
                    }
                }
            }

            float currentAcc = MathHelper.clamp(this.acceleration, 0.0F, 1.0F);

            if (mc.player.hurtTime > 0 && (mc.player.velocityDirty || mc.player.getVelocity().lengthSquared() > 0.001)) {
                currentAcc *= 0.45F;
            }

            float deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw);
            float deltaPitch = angle.getPitch() - this.lastPitch;
            float smoothFactor = MathHelper.clamp(currentAcc, 0.08F, 1.0F);
            smoothFactor = MathHelper.clamp(smoothFactor * 1.55F, 0.08F, 1.0F);

            float microYawNoise = this.noCircling ? 0 : (rng.nextFloat() - 0.5F) * 0.022F;
            float microPitchNoise = this.noCircling ? 0 : (rng.nextFloat() - 0.5F) * 0.013F;

            float newYaw = this.lastYaw + deltaYaw * smoothFactor + microYawNoise;

            float pitchStep = 0.72F;
            float pitchSteps = (float) Math.floor(Math.abs(deltaPitch) / pitchStep);
            float pitchMove = pitchSteps * pitchStep * Math.signum(deltaPitch) * smoothFactor;
            float newPitch = this.lastPitch + pitchMove + microPitchNoise;

            float gcd = Rotation.gcd();
            newYaw -= (newYaw - this.lastYaw) % gcd;
            newPitch -= (newPitch - this.lastPitch) % gcd;

            Rotation smoothRot = new Rotation(newYaw, newPitch);

            float cameraDeltaYaw = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
            float cameraDeltaPitch = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getPitch() - this.lastPitch);
            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                cameraDeltaYaw = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastYaw);
                cameraDeltaPitch = -mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
            }

            float rotationStep = (Math.abs(cameraDeltaYaw) < 5.0F && Math.abs(cameraDeltaPitch) < 5.0F) ? 360.0F : 120.0F;
            RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), rotationStep, rotationStep, rotationStep, rotationStep, 0, 1, elytraVisual);
            this.lastYaw = smoothRot.getYaw();
            this.lastPitch = smoothRot.getPitch();

        } else {
            double distanceToTarget = mc.player.distanceTo(target);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw));

            float speedConstraint = 1.0F;
            if ((distanceToTarget < 0.7D || yawDiff > 85.0F) && yawDiff > 40.0F) {
                speedConstraint = 0.3F;
            }

            if (this.isBack) {
                if (this.acceleration >= -0.01F) {
                    float decel = 0.007F * (0.93F + rng.nextFloat() * 0.14F);
                    this.acceleration -= decel;
                }
                if (this.acceleration <= -0.01F) this.isBack = false;
            } else {
                float speedMultiplier = MathHelper.clamp(yawDiff / 25.0F, 0.5F, 1.0F);
                float accel = 0.0035F * speedMultiplier * (0.9F + rng.nextFloat() * 0.2F);
                this.acceleration += accel;
                if (this.acceleration >= 0.12F) this.isBack = true;
            }

            float currentAcc = MathHelper.clamp(this.acceleration, 0.0F, 1.0F);

            if (mc.player.hurtTime > 0 && (mc.player.velocityDirty || mc.player.getVelocity().lengthSquared() > 0.001)) {
                currentAcc *= 0.45F;
            }

            float smoothFactor = MathHelper.clamp(currentAcc, 0.08F, 1.0F);

            float microYawNoise = this.noCircling ? 0 : (rng.nextFloat() - 0.5F) * 0.018F;
            float microPitchNoise = this.noCircling ? 0 : (rng.nextFloat() - 0.5F) * 0.009F;

            float newYaw = this.lastYaw + (MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw)) * (smoothFactor * speedConstraint) + microYawNoise;

            float rawPitchDelta = angle.getPitch() - this.lastPitch;
            float pitchStep = 0.72F;
            float pitchSteps = (float) Math.floor(Math.abs(rawPitchDelta) / pitchStep);
            float pitchMove = pitchSteps * pitchStep * Math.signum(rawPitchDelta) * (smoothFactor * speedConstraint);
            float newPitch = this.lastPitch + pitchMove + microPitchNoise;

            float gcd = Rotation.gcd();
            newYaw -= (newYaw - this.lastYaw) % gcd;
            newPitch -= (newPitch - this.lastPitch) % gcd;

            Rotation smoothRot = new Rotation(newYaw, newPitch);

            float rotationSpeedLimit = (speedConstraint < 1.0F) ? 45.0F : 110.0F;

            float deltaYaw2_2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
            float deltaPitch2_2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getPitch() - this.lastPitch);

            RotationComponent.update(
                    new Rotation(smoothRot.getYaw(), smoothRot.getPitch()),
                    rotationSpeedLimit,
                    rotationSpeedLimit,
                    !(Math.abs(deltaYaw2_2) > 3.0F) && !(Math.abs(deltaPitch2_2) > 3.0F) ? 360.0F : 0.0F,
                    !(Math.abs(deltaYaw2_2) > 3.0F) && !(Math.abs(deltaPitch2_2) > 3.0F) ? 360.0F : 0.0F,
                    0, 1, elytraVisual
            );

            this.lastYaw = smoothRot.getYaw();
            this.lastPitch = smoothRot.getPitch();
        }
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}