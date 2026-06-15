package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Smoother;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.input.EventHotBarScroll;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.base.events.impl.input.EventMouse;
import wtf.dexum.base.events.impl.input.EventMouseRotation;
import wtf.dexum.base.events.impl.player.EventLook;
import wtf.dexum.utility.interfaces.IMinecraft;

@Mixin({Mouse.class})
public class MouseMixin {
   @Shadow
   @Final
   private MinecraftClient field_1779;
   @Shadow
   private double field_1789;
   @Shadow
   private double field_1787;
   @Shadow
   private Smoother field_1793;
   @Shadow
   private Smoother field_1782;
   @Shadow
   private int field_1780;

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")}
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      if (button != -1 && window == IMinecraft.mc.getWindow().getHandle()) {
         EventManager.call(new EventKey(action, button));
         EventManager.call(new EventMouse(button, action));
      }

   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
)},
      cancellable = true
   )
   public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
      EventHotBarScroll event = new EventHotBarScroll(horizontal, vertical);
      EventManager.call(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"tick"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/Mouse;isCursorLocked()Z"
)
   )
   public boolean onIsCursorLocked(Mouse instance) {
      return instance.isCursorLocked() || this.isAnim();
   }

   @WrapWithCondition(
      method = {"updateMouse"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
)},
      require = 1,
      allow = 1
   )
   private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      EventMouseRotation event = new EventMouseRotation((float)cursorDeltaX, (float)cursorDeltaY);
      EventManager.call(event);
      if (event.isCancelled()) {
         return false;
      } else {
         instance.changeLookDirection((double)event.getCursorDeltaX(), (double)event.getCursorDeltaY());
         return false;
      }
   }

   @Unique
   private boolean isAnim() {
      Screen screen = MinecraftClient.getInstance().currentScreen;
      return false;
   }

   @Inject(
      method = {"updateMouse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      try {
         if (this.field_1779.player == null) {
            return;
         }

         double sensitivity = (Double)this.field_1779.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
         double scaled = sensitivity * sensitivity * sensitivity * 8.0D;
         double i;
         double j;
         if (this.field_1779.options.smoothCameraEnabled) {
            i = this.field_1793.smooth(this.field_1789 * scaled, timeDelta * scaled);
            j = this.field_1782.smooth(this.field_1787 * scaled, timeDelta * scaled);
         } else if (this.field_1779.options.getPerspective().isFirstPerson() && this.field_1779.player.isUsingSpyglass()) {
            this.field_1793.clear();
            this.field_1782.clear();
            i = this.field_1789 * sensitivity * sensitivity * sensitivity;
            j = this.field_1787 * sensitivity * sensitivity * sensitivity;
         } else {
            this.field_1793.clear();
            this.field_1782.clear();
            i = this.field_1789 * scaled;
            j = this.field_1787 * scaled;
         }

         int invert = (Boolean)this.field_1779.options.getInvertYMouse().getValue() ? -1 : 1;
         EventLook event = new EventLook(i, j * (double)invert);
         EventManager.call(event);
         if (!event.isCancelled()) {
            this.field_1779.getTutorialManager().onUpdateMouse(event.getYaw(), event.getPitch());
            this.field_1779.player.changeLookDirection(event.getYaw(), event.getPitch());
         }

         this.field_1789 = 0.0D;
         this.field_1787 = 0.0D;
         ci.cancel();
      } catch (Exception var14) {
      }

   }
}