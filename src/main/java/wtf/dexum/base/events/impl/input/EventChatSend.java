package wtf.dexum.base.events.impl.input;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;

public final class EventChatSend extends EventCancellable {
   private String message;

   @Generated
   public String getMessage() {
      return this.message;
   }

   @Generated
   public EventChatSend(String message) {
      this.message = message;
   }

   @Generated
   public void setMessage(String message) {
      this.message = message;
   }
}