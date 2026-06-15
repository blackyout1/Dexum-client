package wtf.dexum.utility.mixin.client.render;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.dexum.base.events.impl.entity.EventEntityColor;
import wtf.dexum.client.modules.impl.render.TotemPop;
import wtf.dexum.utility.interfaces.IMinecraft;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {
   @Shadow
   @Nullable
   protected abstract RenderLayer method_24302(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
)
   )
   private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      if (TotemPop.ghostAlpha >= 0.0f) {
         return this.method_24302(state, showBody, true, showOutline);
      }

      if (!translucent && state.width == 0.6F) {
         EventEntityColor event = new EventEntityColor(-1);
         EventManager.call(event);
         if (event.isCancelled()) {
            translucent = true;
         }
      }

      return this.method_24302(state, showBody, translucent, showOutline);
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
)
   )
   private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0,argsOnly = true) LivingEntityRenderState renderState) {
      if (TotemPop.ghostAlpha >= 0.0f) {
         int alpha = (int) (TotemPop.ghostAlpha * 255.0f);
         int ghostColor = (alpha << 24) | (TotemPop.ghostRgb & 0x00FFFFFF);

         instance.render(matrixStack, vertexConsumer, 0xF000F0, 0x100010, ghostColor);
         return;
      }

      EventEntityColor event = new EventEntityColor(l);
      if (renderState.invisibleToPlayer) {
         EventManager.call(event);
      }

      instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
   }
}