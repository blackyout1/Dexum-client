package wtf.dexum.utility.predict;

import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PredictUtils {
    public static Vec3d predict(LivingEntity entity, Vec3d pos, float ticks) {
        if (Math.hypot(entity.getX() - entity.prevX, entity.getZ() - entity.prevZ) * 20.0D <= 5.0D && (entity.getY() - entity.prevY) * 20.0D <= 5.0D) {
            return pos;
        } else {
            float f2 = (entity.getPitch() + (entity.prevPitch - entity.getPitch())) * 0.017453292F;
            float g = -(entity.getYaw() + (entity.prevHeadYaw - entity.getYaw())) * 0.017453292F;
            float h2 = MathHelper.cos(g);
            float i2 = MathHelper.sin(g);
            float j = MathHelper.cos(f2);
            float k = MathHelper.sin(f2);
            Vec3d oldVelocity = entity.getVelocity();
            Vec3d lookVec = new Vec3d((double)(i2 * j), (double)(-k), (double)(h2 * j));
            float f = (float)((double)entity.getPitch() * 0.017453293005625408D);
            double d = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
            double e = oldVelocity.horizontalLength();
            boolean slowFalling = entity.getVelocity().y <= 0.0D;
            double gravity = slowFalling && entity.hasStatusEffect(StatusEffects.SLOW_FALLING) ? Math.min(entity.getFinalGravity(), 0.01D) : entity.getFinalGravity();
            double h = MathHelper.square(Math.cos((double)f));
            oldVelocity = oldVelocity.add(0.0D, gravity * (-1.0D + h * 0.75D), 0.0D);
            double i;
            if (oldVelocity.y < 0.0D && d > 0.0D) {
                i = oldVelocity.y * -0.1D * h;
                oldVelocity = oldVelocity.add(lookVec.x * i / d, i, lookVec.z * i / d);
            }

            if (f < 0.0F && d > 0.0D) {
                i = e * (double)(-MathHelper.sin(f)) * 0.04D;
                oldVelocity = oldVelocity.add(-lookVec.x * i / d, i * 3.2D, -lookVec.z * i / d);
            }

            if (d > 0.0D) {
                oldVelocity = oldVelocity.add((lookVec.x / d * e - oldVelocity.x) * 0.1D, 0.0D, (lookVec.z / d * e - oldVelocity.z) * 0.1D);
            }

            Vec3d totalMotion = oldVelocity.multiply(0.9900000095367432D, 0.9800000190734863D, 0.9900000095367432D);
            return pos.add(totalMotion.multiply((double)ticks)).add(0.0D, 0.3499999940395355D, 0.0D);
        }
    }

    @Generated
    private PredictUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Vec3d getPredicted(LivingEntity entity, float ticks) {
        return predict(entity, entity.getPos(), ticks);
    }
}