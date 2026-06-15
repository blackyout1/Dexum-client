package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dexum.base.events.impl.render.EventFog;
import wtf.dexum.client.modules.impl.render.NoRender;
import wtf.dexum.utility.render.display.base.color.ColorUtil;

@Mixin({BackgroundRenderer.class})
public class BackGroundRendererMixin {
   @Inject(
      method = {"getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      NoRender noRender = NoRender.INSTANCE;
      if (noRender.isRemoveBadEffect()) {
         info.setReturnValue((Object)null);
      }

   }

    @Inject(
      method = {"getFogColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getFogColorHook(Camera camera, float tickDelta, ClientWorld world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
      EventFog event = new EventFog();
      EventManager.call(event);
      if (event.isCancelled()) {
         int color = event.getColor();
         float alpha = ((color >> 24) & 0xFF) / 255.0f;
         if (alpha == 0.0f) alpha = 1.0f;
         cir.setReturnValue(new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), alpha));
      }

   }

   @Inject(
      method = {"applyFog"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void modifyFog(Camera camera, FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
      EventFog event = new EventFog();
      EventManager.call(event);
      if (event.isCancelled()) {
         int color1 = event.getColor();
         float alpha = ((color1 >> 24) & 0xFF) / 255.0f;
         if (alpha <= 0.0f) alpha = 1.0f;

         cir.setReturnValue(new Fog(0.0F, event.getDistance(), FogShape.CYLINDER,
            ((color1 >> 16) & 0xFF) / 255.0f,
            ((color1 >> 8) & 0xFF) / 255.0f,
            (color1 & 0xFF) / 255.0f,
            alpha));
      }

   }
}