package wtf.dexum.utility.game.other.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.game.other.MouseButton;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.UIContext;

public abstract class CustomScreen extends Screen implements IMinecraft {
   protected CustomScreen() {
      super(Text.empty());
   }

   public abstract void render(UIContext var1, float var2, float var3);

   @Native
   public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
      UIContext uiContext = UIContext.of(context, mouseX, mouseY, delta);
      this.render(uiContext, (float)mouseX, (float)mouseY);
      super.render(context, mouseX, mouseY, delta);
   }

   public final boolean mouseClicked(double mouseX, double mouseY, int button) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(button);
      this.onMouseClicked(mouseX, mouseY, mouseButton);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void tick() {
   }

   public final boolean mouseReleased(double mouseX, double mouseY, int button) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(button);
      this.onMouseReleased(mouseX, mouseY, mouseButton);
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public final boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      MouseButton mouseButton = MouseButton.fromButtonIndex(button);
      this.onMouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
   }

   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
   }
}