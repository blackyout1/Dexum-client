package wtf.dexum.client.modules.impl.misc;

import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ColorSetting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "AH Helper",
   category = Category.MISC,
   description = "помощник в поиске дешевых предметов"
)
public final class AHHelper extends Module {
   public static final AHHelper INSTANCE = new AHHelper();
   private final ColorSetting cheapSlotColor = new ColorSetting("Цвет дешевого", new ColorRGBA(64, 255, 64, 140));
   private final ColorSetting goodSlotColor = new ColorSetting("Цвет выгодного", new ColorRGBA(255, 255, 64, 140));

   private AHHelper() {
   }

   @Native
   public void renderCheat(DrawContext context, Slot slot) {
      context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, this.cheapSlotColor.getIntColor());
   }

   @Native
   public void renderGood(DrawContext context, Slot slot) {
      context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, this.goodSlotColor.getIntColor());
   }

   @Generated
   public ColorSetting getCheapSlotColor() {
      return this.cheapSlotColor;
   }

   @Generated
   public ColorSetting getGoodSlotColor() {
      return this.goodSlotColor;
   }
}