package wtf.dexum.base.events.impl.entity;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventEntityColor extends EventCancellable {
   private int color;

   @Generated
   public int getColor() {
      return this.color;
   }

   @Generated
   public void setColor(int color) {
      this.color = color;
   }

   @Generated
   public EventEntityColor(int color) {
      this.color = color;
   }
}