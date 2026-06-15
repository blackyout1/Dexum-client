package wtf.dexum.base.events.impl.player;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventMotion extends EventCancellable {
   private float yaw;
   private float pitch;
   public static float lastYaw;
   public static float lastPitch;

   public void setYaw(float yaw) {
      this.yaw = yaw;
      lastYaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
      lastPitch = pitch;
   }

   @Generated
   public EventMotion(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
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