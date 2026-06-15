package wtf.dexum.utility.mixin.minecraft.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.client.modules.impl.render.TotemPop;

@Mixin({HeldItemFeatureRenderer.class})
public class HeldItemFeatureRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/ArmedEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRender(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                          ArmedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (TotemPop.ghostAlpha >= 0.0f) {
            ci.cancel();
        }
    }
}