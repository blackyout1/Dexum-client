package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.MinecraftClient;
import wtf.dexum.utility.render.display.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.client.modules.impl.movement.GuiWalk;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin extends Input {
   @Shadow
   @Final
   private GameOptions settings;

   @Unique
   private float abobaGetMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
   value = "FIELD",
   target = "Lnet/minecraft/client/input/KeyboardInput;playerInput:Lnet/minecraft/util/PlayerInput;",
   ordinal = 0,
   shift = Shift.AFTER
)},
      cancellable = true
   )
   public void injectInputEvent(CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();

      if (GuiWalk.INSTANCE.isEnabled() && client.currentScreen != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) {
          boolean f = client.options.forwardKey.isPressed();
          boolean b = client.options.backKey.isPressed();
          boolean l = client.options.leftKey.isPressed();
          boolean r = client.options.rightKey.isPressed();
          this.movementForward = abobaGetMovementMultiplier(f, b);
          this.movementSideways = abobaGetMovementMultiplier(l, r);
          boolean jumping = client.options.jumpKey.isPressed();
          boolean sneaking = client.options.sneakKey.isPressed();

          this.playerInput = new PlayerInput(f, b, l, r, jumping, sneaking, client.options.sprintKey.isPressed());
          ci.cancel();
          return;
      }

      EventMoveInput event = new EventMoveInput(this.playerInput, this.abobaGetMovementMultiplier(this.playerInput.forward(), this.playerInput.backward()), this.abobaGetMovementMultiplier(this.playerInput.left(), this.playerInput.right()));
      EventManager.call(event);
      if (!event.isCancelled()) {
         this.movementForward = event.getForward();
         this.movementSideways = event.getStrafe();
         this.playerInput = new PlayerInput(this.movementForward > 0.0F, this.movementForward < 0.0F, this.movementSideways > 0.0F, this.movementSideways < 0.0F, client.options.jumpKey.isPressed(), client.options.sneakKey.isPressed(), client.options.sprintKey.isPressed());
         ci.cancel();
      }
   }
}