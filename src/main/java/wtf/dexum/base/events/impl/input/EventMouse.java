package wtf.dexum.base.events.impl.input;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;

public class EventMouse implements Event {
   private final int button;
   private final int action;

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public EventMouse(int button, int action) {
      this.button = button;
      this.action = action;
   }
}