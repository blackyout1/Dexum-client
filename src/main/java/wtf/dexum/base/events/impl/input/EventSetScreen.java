package wtf.dexum.base.events.impl.input;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;

public class EventSetScreen implements Event {
   private Screen screen;

   @Generated
   public EventSetScreen(Screen screen) {
      this.screen = screen;
   }

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventSetScreen)) {
         return false;
      } else {
         EventSetScreen other = (EventSetScreen)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$screen = this.getScreen();
            Object other$screen = other.getScreen();
            if (this$screen == null) {
               if (other$screen != null) {
                  return false;
               }
            } else if (!this$screen.equals(other$screen)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof EventSetScreen;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1;
      Object $screen = this.getScreen();
      result = result * 59 + ($screen == null ? 43 : $screen.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      return "EventSetScreen(screen=" + String.valueOf(this.getScreen()) + ")";
   }
}