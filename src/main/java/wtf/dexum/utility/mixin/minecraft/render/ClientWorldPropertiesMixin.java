package wtf.dexum.utility.mixin.minecraft.render;

import net.minecraft.client.world.ClientWorld.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.client.modules.impl.render.Ambience;

@Mixin({Properties.class})
public class ClientWorldPropertiesMixin {
   @Shadow
   private long field_24439;

   @Inject(
      method = {"setTimeOfDay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
      Ambience tweaks = Ambience.INSTANCE;
      if (tweaks.isTimeEnabled()) {
         this.field_24439 = (long)(tweaks.timeSetting.getCurrent() * 1000.0F);
         ci.cancel();
      }
   }
}