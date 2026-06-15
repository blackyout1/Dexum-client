package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import wtf.dexum.utility.render.display.base.CustomDrawContext;

public class EventHudRender implements Event {
   private final CustomDrawContext context;
   private final float tickDelta;

   @Generated
   public CustomDrawContext getContext() {
      return this.context;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public EventHudRender(CustomDrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
   }
}