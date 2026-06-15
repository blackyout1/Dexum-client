package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.render.EventRenderSky;
import wtf.dexum.client.modules.impl.render.CustomSky;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderPass;setRenderer(Ljava/lang/Runnable;)V"
        )
    )
    private void onSetRenderer(RenderPass renderPass, Runnable originalRenderer) {
        if (CustomSky.INSTANCE.isEnabled()) {
            renderPass.setRenderer(() -> {
                CustomSky.INSTANCE.renderSkyShader();
            });
        } else {
            renderPass.setRenderer(originalRenderer);
        }
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        EventManager.call(new EventRenderSky(new MatrixStack(), new Matrix4f(), tickDelta));
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void onRenderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f matrix4f, Matrix4f matrix4f2, CloudRenderMode cloudRenderMode, Vec3d vec3d, float f, int i, float f2, CallbackInfo ci) {
        if (CustomSky.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}