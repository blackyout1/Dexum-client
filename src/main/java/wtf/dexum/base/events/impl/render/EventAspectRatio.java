package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Generated;

public class EventAspectRatio extends EventCancellable {
   private float ratio;

   @Generated
   public float getRatio() {
      return this.ratio;
   }

   @Generated
   public void setRatio(float ratio) {
      this.ratio = ratio;
   }
}