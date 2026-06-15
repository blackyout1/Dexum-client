package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.client.modules.impl.movement.Speed;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class HolyWorld2Rotation extends RotationBase {
    private float acceleration;
    private boolean isBack;
    private double randomOffsetX;
    private double randomOffsetY;
    private double randomOffsetZ;

    public void update(LivingEntity target, boolean elytraVisual) {
        if (target == null || mc.player == null) {
            return;
        }

        Box box = target.getBoundingBox();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d aimPoint = this.computeHolyWorld2AimPoint(target).add(this.randomOffsetX, this.randomOffsetY, this.randomOffsetZ);
        Vec3d toTarget = aimPoint.subtract(eyePos);
        float centerYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float centerPitch = (float) (-Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))));
        boolean bothGliding = mc.player.isGliding() && target.isGliding();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d endVec = eyePos.add(lookVec.multiply(bothGliding ? 1488.0 : 999.0));
        Box shrunkBox = box.expand(bothGliding ? 0.0 : -0.5);
        boolean inBox = shrunkBox.raycast(eyePos, endVec).isPresent();

        if (bothGliding) {
            if (this.isBack) {
                if (this.acceleration >= -0.02F) {
                    this.acceleration -= Math.abs(MathHelper.wrapDegrees(centerYaw - this.lastYaw)) > 80.0F ? 0.15F : 0.02F;
                }
                if (this.acceleration <= -0.02F) {
                    this.isBack = false;
                    this.updateRandomOffset(target);
                }
            } else {
                this.acceleration += 0.0105F;
                if (this.acceleration >= 0.305F || inBox) {
                    this.isBack = true;
                }
            }
        } else if (this.isBack) {
            if (this.acceleration >= -0.15F) {
                float slowdownSpeed = Math.abs(MathHelper.wrapDegrees(centerYaw - this.lastYaw)) > 80.0F ? 0.1F : 0.01F;
                slowdownSpeed *= 0.9F + (float) Math.random() * 0.2F;
                this.acceleration -= slowdownSpeed;
            }
            if (this.acceleration <= -0.15F) {
                this.isBack = false;
                this.updateRandomOffset(target);
            }
        } else {
            float accelSpeed = 0.0082F + ((float) Math.random() * 0.002F - 0.001F);
            this.acceleration += accelSpeed;
            float threshold = 0.184F + ((float) Math.random() * 0.03F - 0.015F);
            if (this.acceleration >= threshold || inBox) {
                this.isBack = true;
            }
        }

        float deltaYaw = MathHelper.wrapDegrees(centerYaw - this.lastYaw);
        float deltaPitch = centerPitch - this.lastPitch;
        float smooth = Math.max(this.acceleration, 0.0F);
        float humanYawOffset = (float) (Math.sin((double) System.currentTimeMillis() * 0.001) * 0.04);
        float humanPitchOffset = (float) (Math.cos((double) System.currentTimeMillis() * 0.0015) * 0.025);
        if (Math.abs(deltaYaw) > 1.0F || Math.abs(deltaPitch) > 1.0F) {
            humanYawOffset += ((float) Math.random() - 0.5F) * 0.035F;
            humanPitchOffset += ((float) Math.random() - 0.5F) * 0.02F;
        }

        float newYaw = this.lastYaw + deltaYaw * MathHelper.clamp(smooth * 1.12F, 0.0F, 1.0F) + humanYawOffset;
        float newPitch = this.lastPitch + deltaPitch * MathHelper.clamp(smooth / 1.88F, 0.0F, 1.0F) + humanPitchOffset;
        float gcd = Rotation.gcd();
        newYaw -= (newYaw - this.lastYaw) % gcd;
        newPitch -= (newPitch - this.lastPitch) % gcd;
        newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);

        this.lastYaw = newYaw;
        this.lastPitch = newPitch;

        boolean speedEnabled = Speed.INSTANCE.isEnabled();
        float rotSpeed = speedEnabled ? 360.0F : 120.0F;
        float returnSpeed = speedEnabled ? 360.0F : 180.0F;
        RotationComponent.update(new Rotation(newYaw, newPitch), rotSpeed, rotSpeed, returnSpeed, returnSpeed, 0, 1, elytraVisual);
    }

    private Vec3d computeHolyWorld2AimPoint(LivingEntity target) {
        float targetYaw = target.getYaw();
        double offset = 0.28;
        double ox = -MathHelper.sin(targetYaw * ((float) Math.PI / 180F)) * offset;
        double oz = MathHelper.cos(targetYaw * ((float) Math.PI / 180F)) * offset;
        boolean speedEnabled = Speed.INSTANCE.isEnabled();

        if (speedEnabled) {
            return new Vec3d(
                    target.getX() + ox,
                    target.getY() + target.getHeight() * 0.75F,
                    target.getZ() + oz
            );
        }
        return new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.75F, target.getZ());
    }

    private void updateRandomOffset(LivingEntity target) {
        Box box = target.getBoundingBox();
        double boxWidth = box.maxX - box.minX;
        double boxHeight = box.maxY - box.minY;
        double boxDepth = box.maxZ - box.minZ;
        this.randomOffsetX = (Math.random() - 0.5) * boxWidth * 0.15;
        this.randomOffsetY = (Math.random() - 0.5) * boxHeight * 0.15;
        this.randomOffsetZ = (Math.random() - 0.5) * boxDepth * 0.15;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}