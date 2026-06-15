package wtf.dexum.base.events.impl.input;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class EventKey implements Event {
   private final int action;
   private final int keyCode;

   public boolean is(int keyCode) {
      return keyCode == this.keyCode;
   }

   public boolean isKeyDown(int key) {
      return this.isKeyDown(key, IMinecraft.mc.currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean screen) {
      return this.keyCode == key && this.action == 1 && screen;
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, IMinecraft.mc.currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean screen) {
      return this.keyCode == key && this.action == 0 && screen;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public int getKeyCode() {
      return this.keyCode;
   }

   @Generated
   public EventKey(int action, int keyCode) {
      this.action = action;
      this.keyCode = keyCode;
   }
}