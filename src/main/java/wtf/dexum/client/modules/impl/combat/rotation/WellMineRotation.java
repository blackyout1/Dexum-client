package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.math.MultipointUtils;
import wtf.dexum.utility.predict.PredictUtils;

import java.util.concurrent.ThreadLocalRandom;

public class WellMineRotation extends RotationBase {
    private static final float RANDOM_STRENGTH = 0.75F;

    private float speedAcceleration;
    private boolean back;

    public void update(LivingEntity target, float predict, boolean elytraVisual, boolean turnaroundActive, boolean predictOnElytra, boolean useNewPredict) {
        Vec3d vector = MultipointUtils.getMultipoint(target, 6.0D);

        if (target.isGliding() && predictOnElytra && !turnaroundActive) {
            vector = useNewPredict
                    ? PredictUtils.getPredicted(target, predict)
                    : PredictUtils.predict(target, target.getPos(), predict);
        }

        Rotation angle = RotationUtil.fromVec3d(vector.subtract(mc.player.getEyePos()));
        float targetYaw = angle.getYaw();
        float targetPitch = angle.getPitch();
        Box box = target.getBoundingBox();

        if (!this.back) {
            if (this.speedAcceleration >= 1.0F) {
                this.speedAcceleration = 0.0F;
            } else if (mc.player.isGliding()) {
                float diff = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
                this.speedAcceleration += diff > 40.0F ? 0.0025F : 0.005F;
            } else {
                this.speedAcceleration += 0.005F;
            }

            Vec3d offset = Vec3d.ZERO;
            if (mc.player.isGliding() && target instanceof PlayerEntity && target.isGliding()) {
                offset = useNewPredict
                        ? PredictUtils.getPredicted(target, predict)
                        : PredictUtils.predict(target, target.getPos(), predict);
            }

            if (this.speedAcceleration >= 0.18F || RaytracingUtil.rayTrace(mc.player.getRotationVector(), 6.0D, box.offset(offset).expand(-0.5D, -1.0D, -0.5D))) {
                this.back = true;
            }
        } else {
            if (this.speedAcceleration >= -0.01F) {
                float diff = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
                this.speedAcceleration -= diff > 40.0F ? 0.04F : 0.01F;
            }

            if (this.speedAcceleration <= -0.01F) {
                this.back = false;
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        targetYaw += (float) random.nextDouble(-RANDOM_STRENGTH, RANDOM_STRENGTH);
        targetPitch += (float) random.nextDouble(-RANDOM_STRENGTH, RANDOM_STRENGTH);

        float smoothVal = MathHelper.clamp(this.speedAcceleration, -1.0F, 1.0F);
        float changeYaw = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()) * smoothVal;
        float changePitch = (targetPitch - mc.player.getPitch()) * (smoothVal / 2.0F);

        Rotation smoothRot = new Rotation(
                mc.player.getYaw() + changeYaw,
                MathHelper.clamp(mc.player.getPitch() + changePitch, -90.0F, 90.0F)
        );

        RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);
        this.lastYaw = smoothRot.getYaw();
        this.lastPitch = smoothRot.getPitch();
    }

    public void reset() {
        this.speedAcceleration = 0.0F;
        this.back = false;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {

    }
}