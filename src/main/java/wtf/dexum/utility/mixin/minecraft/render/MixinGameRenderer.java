package wtf.dexum.utility.mixin.minecraft.render;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import wtf.dexum.base.events.impl.render.EventAspectRatio;
import wtf.dexum.base.events.impl.render.EventFov;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.render.EventRenderScreen;
import wtf.dexum.client.modules.impl.render.Interface;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.UIContext;
import wtf.dexum.utility.render.level.Render3DUtil;
import wtf.dexum.utility.render.particles.ParticleEngine;

@Mixin({GameRenderer.class})
public abstract class MixinGameRenderer {
   @Shadow
   private float field_4005;
   @Shadow
   private float field_3988;
   @Shadow
   private float field_4004;

   @Shadow
   public abstract float method_32796();

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      EventAspectRatio eventAspectRatio = new EventAspectRatio();
      EventManager.call(eventAspectRatio);
      if (eventAspectRatio.isCancelled()) {
         Matrix4f matrix4f = new Matrix4f();
         if (this.field_4005 != 1.0F) {
            matrix4f.translate(this.field_3988, -this.field_4004, 0.0F);
            matrix4f.scale(this.field_4005, this.field_4005, 1.0F);
         }

         matrix4f.perspective(fovDegrees * 0.017453292F, eventAspectRatio.getRatio(), 0.05F, this.method_32796());
         cir.setReturnValue(matrix4f);
      }

   }

   @ModifyExpressionValue(
      method = {"getFov"},
      at = {@At(
   value = "INVOKE",
   target = "Ljava/lang/Integer;intValue()I",
   remap = false
)}
   )
   private int hookGetFov(int original) {
      EventFov event = new EventFov();
      EventManager.call(event);
      return event.isCancelled() ? event.getFov() : original;
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
   opcode = 180,
   ordinal = 0
)}
   )
   public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
      MatrixStack matrixStack = new MatrixStack();
      matrixStack.multiplyPositionMatrix(matrix4f);
      Render3DUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
      Render3DUtil.setLastModMat(RenderSystem.getModelViewMatrix());
      Render3DUtil.setLastWorldSpaceMatrix(matrix4f);
      EventRender3D event = new EventRender3D(matrixStack, tickCounter.getTickDelta(false));
      EventManager.call(event);
      if (IMinecraft.mc.world != null) {
          ParticleEngine.render3D(event.getMatrix());
      }
      Render3DUtil.onEventRender3D(event.getMatrix());
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
   opcode = 180,
   ordinal = 2
)},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void renderScreenHook(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, Profiler profiler, boolean bl, int i, int j, Window window, Matrix4f matrix4f, Matrix4fStack matrix4fStack, DrawContext drawContext) {
      EventManager.call(new EventRenderScreen(UIContext.of(drawContext, i, j, IMinecraft.mc.getRenderTickCounter().getTickDelta(false))));
   }

   @Inject(
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;draw()V",
   opcode = 180,
   shift = Shift.AFTER,
   ordinal = 0
)},
      method = {"render"}
   )
   void renderHudHook(RenderTickCounter tickCounter, boolean tick, CallbackInfo callbackInfo) {
      this.triggerHudRenderEvent(tickCounter);
   }

   @Unique
   private void triggerHudRenderEvent(RenderTickCounter tickCounter) {
      CustomDrawContext customDrawContext = new CustomDrawContext(IMinecraft.mc.getBufferBuilders().getEntityVertexConsumers());
      double saveScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
      this.setScaleFactorOutAllMods((double)Interface.INSTANCE.getCustomScale());
      RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)IMinecraft.mc.getWindow().getScaledWidth(), (float)IMinecraft.mc.getWindow().getScaledHeight(), 0.0F, 1000.0F, 21000.0F), ProjectionType.ORTHOGRAPHIC);
      RenderSystem.disableDepthTest();

      try {
         EventManager.call(new EventHudRender(customDrawContext, tickCounter.getTickDelta(false)));
      } catch (Exception var6) {

      }

      customDrawContext.draw();
      RenderSystem.enableDepthTest();
      this.setScaleFactorOutAllMods(saveScale);
      RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)IMinecraft.mc.getWindow().getScaledWidth(), (float)IMinecraft.mc.getWindow().getScaledHeight(), 0.0F, 1000.0F, 21000.0F), ProjectionType.ORTHOGRAPHIC);
   }

   @Unique
   public void setScaleFactorOutAllMods(double scaleFactor) {
      IMinecraft.mc.getWindow().scaleFactor = scaleFactor;
      int i = (int)((double)IMinecraft.mc.getWindow().framebufferWidth / scaleFactor);
      IMinecraft.mc.getWindow().scaledWidth = (double)IMinecraft.mc.getWindow().framebufferWidth / scaleFactor > (double)i ? i + 1 : i;
      int j = (int)((double)IMinecraft.mc.getWindow().framebufferHeight / scaleFactor);
      IMinecraft.mc.getWindow().scaledHeight = (double)IMinecraft.mc.getWindow().framebufferHeight / scaleFactor > (double)j ? j + 1 : j;
   }
}