package wtf.dexum.utility.game.player.rotation;

import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
    private final float yaw;
    private final float pitch;
    private boolean isNormalized;
    public static final Rotation ZERO = new Rotation(0.0F, 0.0F);

    public Rotation(float yaw, float pitch) {
        this(yaw, pitch, false);
    }

    public Rotation(float yaw, float pitch, boolean isNormalized) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.isNormalized = isNormalized;
    }

    public static Rotation lookingAt(Vec3d point, Vec3d from) {
        return fromRotationVec(point.subtract(from));
    }

    public static Rotation fromRotationVec(Vec3d lookVec) {
        double diffX = lookVec.x;
        double diffY = lookVec.y;
        double diffZ = lookVec.z;
        return new Rotation((float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D), (float)MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))));
    }

    public static Rotation getRotations(Vec3d vec3d) {
        double deltaX = vec3d.x - MinecraftClient.getInstance().player.getX();
        double deltaY = vec3d.y - MinecraftClient.getInstance().player.getEyeY();
        double deltaZ = vec3d.z - MinecraftClient.getInstance().player.getZ();
        double distance = (double)MathHelper.sqrt((float)(deltaX * deltaX + deltaZ * deltaZ));
        float yaw = (float)(MathHelper.atan2(deltaZ, deltaX) * 57.29577951308232D - 90.0D);
        float pitch = (float)(-MathHelper.atan2(deltaY, distance) * 57.29577951308232D);
        return new Rotation(yaw, pitch);
    }

    public float angleTo(Rotation other) {
        return Math.min(this.rotationDeltaTo(other).length(), 180.0F);
    }

    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(this.angleDifference(other.yaw, this.yaw), this.angleDifference(other.pitch, this.pitch));
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - this.yaw);
        float pitchDelta = target.getPitch() - this.pitch;
        return (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
    }

    private float angleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    public boolean approximatelyEquals(Rotation other, float tolerance) {
        return this.angleTo(other) <= tolerance;
    }

    public boolean isNormalized() {
        return this.isNormalized;
    }

    public Vec3d getDirectionVector() {
        return Vec3d.fromPolar(this.pitch, this.yaw);
    }

    public final Vec3d toVector() {
        float f = this.pitch * 0.017453292F;
        float g = -this.yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    public Rotation towardsLinear(Rotation other, float horizontalFactor, float verticalFactor) {
        RotationDelta diff = this.rotationDeltaTo(other);
        float rotationDifference = diff.length();
        float straightLineYaw = Math.abs(diff.getDeltaYaw() / rotationDifference) * horizontalFactor;
        float straightLinePitch = Math.abs(diff.getDeltaPitch() / rotationDifference) * verticalFactor;
        float limitedYaw = MathHelper.clamp(diff.getDeltaYaw(), -straightLineYaw, straightLineYaw);
        float limitedPitch = MathHelper.clamp(diff.getDeltaPitch(), -straightLinePitch, straightLinePitch);
        return new Rotation(this.yaw + limitedYaw, this.pitch + limitedPitch);
    }

    public boolean check() {
        return Float.isInfinite(this.yaw) || Float.isNaN(this.yaw) || Float.isInfinite(this.pitch) || Float.isNaN(this.pitch);
    }

    public static float gcd() {
        double f = (Double)MinecraftClient.getInstance().options.getMouseSensitivity().getValue() * 0.6000000238418579D + 0.20000000298023224D;
        return (float)(f * f * f * 8.0D * 0.15000000596046448D);
    }

    public Rotation normalize(Rotation currentRotation) {
        if (!this.isNormalized && !this.equals(currentRotation)) {
            RotationDelta rotationDelta = currentRotation.rotationDeltaTo(this);
            double gcd = (double)gcd();
            int targetX = (int)((double)rotationDelta.getDeltaYaw() / gcd);
            int targetY = (int)((double)rotationDelta.getDeltaPitch() / gcd);
            return new Rotation((float)((double)currentRotation.getYaw() + (double)targetX * gcd), (float)((double)currentRotation.getPitch() + (double)targetY * gcd), true);
        } else {
            return this;
        }
    }

    public Rotation add(RotationDelta diff) {
        return new Rotation(this.yaw + diff.getDeltaYaw(), this.pitch + diff.getDeltaPitch());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Rotation)) {
            return false;
        } else {
            Rotation o2 = (Rotation)obj;
            return o2.yaw == this.yaw && o2.pitch == this.pitch;
        }
    }

    @Generated
    public float getYaw() {
        return this.yaw;
    }

    @Generated
    public float getPitch() {
        return this.pitch;
    }
}