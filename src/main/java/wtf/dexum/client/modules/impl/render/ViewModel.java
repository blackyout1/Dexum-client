package wtf.dexum.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "ViewModel",
   category = Category.RENDER,
   description = "Настройка позиции"
)
public final class ViewModel extends Module {
   public static final ViewModel INSTANCE = new ViewModel();
   public final NumberSetting leftX = new NumberSetting("Левая рука X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftY = new NumberSetting("Левая рука Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftZ = new NumberSetting("Левая рука Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftScale = new NumberSetting("Левая рука размер", 1.0F, 0.5F, 1.5F, 0.05F);
   public final NumberSetting rightX = new NumberSetting("Правая рука X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightY = new NumberSetting("Правая рука Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightZ = new NumberSetting("Правая рука Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightScale = new NumberSetting("Правая рука размер", 1.0F, 0.5F, 1.5F, 0.05F);

   private ViewModel() {
   }

   public void applyHandScale(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.scale(this.rightScale.getCurrent(), this.rightScale.getCurrent(), this.rightScale.getCurrent());
         } else {
            matrices.scale(this.leftScale.getCurrent(), this.leftScale.getCurrent(), this.leftScale.getCurrent());
         }
      } else {
         matrices.scale(1.0F, 1.0F, 1.0F);
      }

   }

   public void applyHandPosition(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
         } else {
            matrices.translate(-this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
         }
      } else {
         matrices.translate(0.0F, 0.0F, 0.0F);
      }

   }
}