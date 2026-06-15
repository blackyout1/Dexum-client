package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Generated;

public class EventFog extends EventCancellable {
   private float distance;
   private int color;

   @Generated
   public float getDistance() {
      return this.distance;
   }

   @Generated
   public int getColor() {
      return this.color;
   }

   @Generated
   public void setDistance(float distance) {
      this.distance = distance;
   }

   @Generated
   public void setColor(int color) {
      this.color = color;
   }
}