package wtf.dexum.base.events.impl.input;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Generated;

public class EventHotBarScroll extends EventCancellable {
   private double horizontal;
   private double vertical;

   @Generated
   public double getHorizontal() {
      return this.horizontal;
   }

   @Generated
   public double getVertical() {
      return this.vertical;
   }

   @Generated
   public void setHorizontal(double horizontal) {
      this.horizontal = horizontal;
   }

   @Generated
   public void setVertical(double vertical) {
      this.vertical = vertical;
   }

   @Generated
   public EventHotBarScroll(double horizontal, double vertical) {
      this.horizontal = horizontal;
      this.vertical = vertical;
   }
}