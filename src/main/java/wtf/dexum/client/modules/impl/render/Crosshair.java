package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult.Type;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "Crosshair",
   category = Category.RENDER,
   description = "Кастомный прицел"
)
public final class Crosshair extends Module {
   public static final Crosshair INSTANCE = new Crosshair();
   private final NumberSetting thickness = new NumberSetting("Толщина", 1.0F, 0.5F, 3.0F, 0.1F);
   private final NumberSetting length = new NumberSetting("Длина", 3.0F, 1.0F, 8.0F, 0.5F);
   private final NumberSetting gap = new NumberSetting("Разрыв", 2.0F, 0.0F, 5.0F, 0.5F);
   private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false);
   private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);
   private final ColorRGBA entityColor = new ColorRGBA(255, 0, 0, 255);

   private Crosshair() {
   }

   @EventTarget
   public void onRender(EventHudRender event) {
      if (mc.player != null && mc.world != null) {
         if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            CustomDrawContext ctx = event.getContext();
            float x = (float)mc.getWindow().getScaledWidth() / 2.0F;
            float y = (float)mc.getWindow().getScaledHeight() / 2.0F;
            float currentGap = this.gap.getCurrent();
            float currentThickness;
            if (this.dynamicGap.isEnabled()) {
               currentThickness = 1.0F - mc.player.getAttackCooldownProgress(0.0F);
               currentGap += 8.0F * currentThickness;
            }

            currentThickness = this.thickness.getCurrent();
            float currentLength = this.length.getCurrent();
            ColorRGBA color = this.useEntityColor.isEnabled() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY ? this.entityColor : new ColorRGBA(255, 255, 255, 255);
            this.drawLine(ctx, x - currentThickness / 2.0F, y - currentGap - currentLength, currentThickness, currentLength, color);
            this.drawLine(ctx, x - currentThickness / 2.0F, y + currentGap, currentThickness, currentLength, color);
            this.drawLine(ctx, x - currentGap - currentLength, y - currentThickness / 2.0F, currentLength, currentThickness, color);
            this.drawLine(ctx, x + currentGap, y - currentThickness / 2.0F, currentLength, currentThickness, color);
         }
      }
   }

   private void drawLine(CustomDrawContext ctx, float x, float y, float width, float height, ColorRGBA color) {
      ctx.drawRect(x, y, width, height, color);
   }
}