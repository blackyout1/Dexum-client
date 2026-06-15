package wtf.dexum.utility.mixin.client.render.gui.hud;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.impl.render.Crosshair;
import wtf.dexum.client.modules.impl.render.Interface;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.particles.ParticleEngine;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin {
   @Inject(
      method = {"renderStatusBars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hideVanillaStatusBars(DrawContext context, CallbackInfo ci) {
       if (Interface.INSTANCE.isLiquidHudEnabled()) {
           ci.cancel();
       }
   }

   @Inject(
      method = {"renderExperienceBar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hideVanillaExpBar(DrawContext context, int x, CallbackInfo ci) {
       if (Interface.INSTANCE.isLiquidHudEnabled()) {
           ci.cancel();
       }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.player != null) {
          ParticleEngine.update();
      }
      CustomDrawContext customDrawContext = new CustomDrawContext(IMinecraft.mc.getBufferBuilders().getEntityVertexConsumers());
      EventManager.call(new EventRender2D(customDrawContext, tickCounter.getTickDelta(false)));
   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void removeVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      try {
         Module crosshairModule = Crosshair.INSTANCE;
         if (crosshairModule.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception var5) {
      }

   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderPlayerList"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void inject(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Interface interfaceModule = Interface.INSTANCE;
      if (interfaceModule.isEnabled() && interfaceModule.isEnableTab()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderOverlayMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderScoreboardSidebar*"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Interface interfaceModule = Interface.INSTANCE;
      if (interfaceModule.isEnabled() && interfaceModule.isEnableScoreBar()) {
         ci.cancel();
      }

   }

   @ModifyVariable(
      method = {"renderStatusBars"},
      at = @At("STORE"),
      ordinal = 3
   )
   private int modifyM(int original, DrawContext context) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            return context.getScaledWindowWidth() / 2 + 90 + 36;
         }
      }

      return original;
   }
}