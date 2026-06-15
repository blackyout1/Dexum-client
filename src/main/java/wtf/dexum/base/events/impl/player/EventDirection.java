package wtf.dexum.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;

public class EventDirection implements Event {
   private float yaw;
   private float pitch;

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
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
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventDirection)) {
         return false;
      } else {
         EventDirection other = (EventDirection)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
            return false;
         } else {
            return Float.compare(this.getPitch(), other.getPitch()) == 0;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof EventDirection;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + Float.floatToIntBits(this.getYaw());
      result = result * 59 + Float.floatToIntBits(this.getPitch());
      return result;
   }

   @Generated
   public String toString() {
      float var10000 = this.getYaw();
      return "EventDirection(yaw=" + var10000 + ", pitch=" + this.getPitch() + ")";
   }

   @Generated
   public EventDirection(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }
}