package wtf.dexum.base.events.impl.render;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventFov extends EventCancellable {
   private int fov;

   @Generated
   public int getFov() {
      return this.fov;
   }

   @Generated
   public void setFov(int fov) {
      this.fov = fov;
   }
}