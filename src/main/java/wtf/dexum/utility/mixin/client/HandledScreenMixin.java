package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.render.EventHandledScreen;

@Mixin({HandledScreen.class})
public abstract class HandledScreenMixin {
   @Shadow
   protected int field_2792;
   @Shadow
   protected int field_2779;
   @Shadow
   @Nullable
   protected Slot field_2787;

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      EventManager.call(new EventHandledScreen(context, this.field_2787, this.field_2792, this.field_2779));
   }
}