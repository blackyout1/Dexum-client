package wtf.dexum.utility.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.dexum.client.modules.impl.render.FullBright;

@Mixin({LightmapTextureManager.class})
public class LightmapTextureManagerMixin {
   @ModifyExpressionValue(
      method = {"update(F)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
)}
   )
   private Object injectXRayFullBright(Object original) {
      FullBright tweaks = FullBright.INSTANCE;
      return tweaks.isEnabled() ? Math.max((Double)original, 10.0D) : original;
   }
}