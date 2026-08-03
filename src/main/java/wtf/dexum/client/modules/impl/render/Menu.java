package wtf.dexum.client.modules.impl.render;

import wtf.dexum.Dexum;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ColorSetting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
        name = "ClickGUI",
        category = Category.RENDER,
        description = "Меню чита"
)
public final class Menu extends Module {
   public static final Menu INSTANCE = new Menu();

   public final ColorSetting guiColor = new ColorSetting("Цвет GUI", new ColorRGBA(56, 58, 61));

   private Menu() {
      this.setKeyCode(344);
   }

   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
      } else {
         Dexum.getInstance().getMenuScreen().needToClose = false;
         if (mc.currentScreen != Dexum.getInstance().getMenuScreen()) {
            mc.setScreen(Dexum.getInstance().getMenuScreen());
            super.onEnable();
         }
      }
   }

   public void onDisable() {
      if (mc.currentScreen == Dexum.getInstance().getMenuScreen()) {
         Dexum.getInstance().getMenuScreen().needToClose = true;
      }
      super.onDisable();
   }

   public void setKeyCode(int keyCode) {
      if (keyCode != -1) {
         super.setKeyCode(keyCode);
      }
   }
}