package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.render.EventCamera;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;

@ModuleAnnotation(
   name = "NoRender",
   category = Category.RENDER,
   description = "Убирает лишние элементы с экрана"
)
public final class NoRender extends Module {
   public static final NoRender INSTANCE = new NoRender();
   private final MultiBooleanSetting settings = MultiBooleanSetting.create("Убрать", List.of("Огонь", "Плохие эффекты", "Камера клип", "Тряска", "Затуманивание", "Частицы", "Трава", "Тотемы"));

   @Native
   public boolean isRemoveFire() {
      return this.isEnabled() && this.settings.isEnable(0);
   }

   @Native
   public boolean isRemoveBadEffect() {
      return this.isEnabled() && this.settings.isEnable(1);
   }

   @Native
   public boolean isRemoveShake() {
      return this.isEnabled() && this.settings.isEnable("Тряска");
   }

   @Native
   public boolean isRemoveParticles() {
      return this.isEnabled() && this.settings.isEnable("Частицы");
   }

   @Native
   public boolean isRemoveGrass() {
      return this.isEnabled() && this.settings.isEnable("Трава");
   }

   @Native
   public boolean isRemoveTotem() {
      return this.isEnabled() && this.settings.isEnable("Тотемы");
   }

   @EventTarget
   @Native
   private void onCamera(EventCamera e) {
      e.setCameraClip(this.settings.isEnable("Камера клип"));
      e.cancel();
   }
}