package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.input.EventSetScreen;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.other.EventTick;

@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin {
   @Unique
   private long lastHookTime = Util.getMeasuringTimeNano();
   @Unique
   private int accumulatedCalls = 0;

   @Inject(
      method = {"<init>"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/MinecraftClient$1;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/RunArgs;)V"
)}
   )
   public void init(RunArgs args, CallbackInfo ci) {
      Dexum.getInstance().init();
   }

   @ModifyVariable(
      method = {"setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private Screen mixin$modifySetScreenArg(Screen original) {
      EventSetScreen event = new EventSetScreen(original);
      EventManager.call(event);
      return event.getScreen();
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void render(boolean tick, CallbackInfo ci) {
      long now = Util.getMeasuringTimeNano();
      long delta = now - this.lastHookTime;
      this.accumulatedCalls += (int)(delta / 4166666L);
      this.lastHookTime += (long)this.accumulatedCalls * 4166666L;

      for(this.accumulatedCalls = Math.min(this.accumulatedCalls, 240); this.accumulatedCalls > 0; --this.accumulatedCalls) {
         EventManager.call(new EventGameUpdate());
      }

   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventTick event = new EventTick();
      EventManager.call(event);
   }
}