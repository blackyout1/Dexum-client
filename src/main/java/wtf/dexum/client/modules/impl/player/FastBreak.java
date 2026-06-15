package wtf.dexum.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
   name = "FastBreak",
   category = Category.PLAYER,
   description = "Ускоряет добычу блоков"
)
public final class FastBreak extends Module {
   public static final FastBreak INSTANCE = new FastBreak();
   public final BooleanSetting speedMine = new BooleanSetting("Speed Mine", false);

   private FastBreak() {
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         if (this.speedMine.isEnabled()) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20, 1, false, false));
         } else {
            mc.player.removeStatusEffect(StatusEffects.HASTE);
         }

      }
   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.player.removeStatusEffect(StatusEffects.HASTE);
      }

   }
}