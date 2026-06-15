package wtf.dexum.base.events.impl.server;

import lombok.Generated;
import net.minecraft.text.Text;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventChatReceive extends EventCancellable {
   private Text message;

   @Generated
   public Text getMessage() {
      return this.message;
   }

   @Generated
   public void setMessage(Text message) {
      this.message = message;
   }

   @Generated
   public EventChatReceive(Text message) {
      this.message = message;
   }
}