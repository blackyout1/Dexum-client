package wtf.dexum.utility.game.player.rotation;

import net.minecraft.util.math.Vec2f;

public class RotationDelta {
   private final float deltaYaw;
   private final float deltaPitch;

   public RotationDelta(float deltaYaw, float deltaPitch) {
      this.deltaYaw = deltaYaw;
      this.deltaPitch = deltaPitch;
   }

   public float length() {
      return (float)Math.sqrt((double)(this.deltaYaw * this.deltaYaw + this.deltaPitch * this.deltaPitch));
   }

   public float getDeltaYaw() {
      return this.deltaYaw;
   }

   public float getDeltaPitch() {
      return this.deltaPitch;
   }

   public Vec2f toVec2f() {
      return new Vec2f(this.deltaYaw, this.deltaPitch);
   }

   public boolean isInRange(float delta) {
      return this.isInRange(delta, delta);
   }

   public boolean isInRange(float maxDeltaYaw, float maxDeltaPitch) {
      return Math.abs(this.deltaYaw) < maxDeltaYaw && Math.abs(this.deltaPitch) < maxDeltaPitch;
   }
}