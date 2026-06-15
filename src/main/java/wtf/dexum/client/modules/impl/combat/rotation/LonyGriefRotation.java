package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class LonyGriefRotation extends RotationBase {
    private float acceleration;
    private boolean isBack;

    public void update(LivingEntity target, Rotation angle, boolean elytraVisual) {
        if (mc.player.isGliding()) {
            if (!this.isBack) {
                this.acceleration += 0.005F;
                if (this.acceleration >= 0.13F) {
                    this.isBack = true;
                }
            } else {
                if (this.acceleration >= -0.02F) {
                    this.acceleration -= 0.005F;
                }

                if (this.acceleration <= -0.02F) {
                    this.isBack = false;
                }
            }
        } else if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), 1488.0D, target.getBoundingBox())) {
            this.acceleration += 0.0015F;
        } else if (this.acceleration > 0.0F) {
            this.acceleration -= 0.01F;
        }

        float deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw);
        float deltaPitch = angle.getPitch() - this.lastPitch;
        float smooth = Math.max(this.acceleration, 0.0F);
        float newYaw = this.lastYaw + deltaYaw * Math.min(Math.max(smooth, 0.0F), 1.0F);
        float newPitch = this.lastPitch + deltaPitch * Math.min(Math.max(smooth / 2.0F, 0.0F), 1.0F);
        newYaw -= (newYaw - this.lastYaw) % Rotation.gcd();
        newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();

        Rotation smoothRot = new Rotation(newYaw, newPitch);
        float deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
        float deltaPitch2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getPitch() - this.lastPitch);
        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastYaw);
            deltaPitch2 = -mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
        }

        RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), 360.0F, 360.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, 0, 1, elytraVisual);
        this.lastYaw = smoothRot.getYaw();
        this.lastPitch = smoothRot.getPitch();
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}