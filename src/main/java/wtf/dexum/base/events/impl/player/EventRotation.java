package wtf.dexum.base.events.impl.player;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventRotation extends EventCancellable {
   private float yaw;
   private float pitch;
   private float partialTicks;

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }

   @Generated
   public float getPartialTicks() {
      return this.partialTicks;
   }

   @Generated
   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Generated
   public void setPartialTicks(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   @Generated
   public EventRotation(float yaw, float pitch, float partialTicks) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.partialTicks = partialTicks;
   }
}