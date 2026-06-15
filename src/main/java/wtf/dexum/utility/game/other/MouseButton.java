package wtf.dexum.utility.game.other;

import lombok.Generated;
import ru.nexusguard.protection.annotations.Native;

public enum MouseButton {
   LEFT(0),
   RIGHT(1),
   MIDDLE(2),
   BUTTON_4(3),
   BUTTON_5(4),
   BUTTON_6(5),
   BUTTON_7(6);

   private final int buttonIndex;

   @Native
   public static MouseButton fromButtonIndex(int index) {
      MouseButton[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         MouseButton button = var1[var3];
         if (button.getButtonIndex() == index) {
            return button;
         }
      }

      return LEFT;
   }

   @Generated
   private MouseButton(final int buttonIndex) {
      this.buttonIndex = buttonIndex;
   }

   @Generated
   public int getButtonIndex() {
      return this.buttonIndex;
   }

   private static MouseButton[] $values() {
      return new MouseButton[]{LEFT, RIGHT, MIDDLE, BUTTON_4, BUTTON_5, BUTTON_6, BUTTON_7};
   }
}