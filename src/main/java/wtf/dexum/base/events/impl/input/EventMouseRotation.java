package wtf.dexum.base.events.impl.input;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Generated;

public class EventMouseRotation extends EventCancellable {
   float cursorDeltaX;
   float cursorDeltaY;

   @Generated
   public float getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   @Generated
   public float getCursorDeltaY() {
      return this.cursorDeltaY;
   }

   @Generated
   public void setCursorDeltaX(float cursorDeltaX) {
      this.cursorDeltaX = cursorDeltaX;
   }

   @Generated
   public void setCursorDeltaY(float cursorDeltaY) {
      this.cursorDeltaY = cursorDeltaY;
   }

   @Generated
   public EventMouseRotation(float cursorDeltaX, float cursorDeltaY) {
      this.cursorDeltaX = cursorDeltaX;
      this.cursorDeltaY = cursorDeltaY;
   }
}