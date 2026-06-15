package wtf.dexum.utility.mixin.client.render.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dexum.client.modules.impl.misc.AHHelper;
import wtf.dexum.client.modules.impl.misc.ItemScroller;
import wtf.dexum.utility.game.server.AutoBuyUtil;

import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

@Mixin({HandledScreen.class})
public abstract class HandledScreenMixin {
   @Unique
   @Mutable
   private boolean isAuc;
   @Unique
   @Mutable
   private Slot lowSumSlotId = null;
   @Unique
   @Mutable
   private Slot lowAllSumSlotId = null;
   @Shadow
   @Final
   protected ScreenHandler field_2797;

   @Shadow
   public abstract ScreenHandler method_17577();

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickScreen(CallbackInfo ci) {
      if (!this.isAuc && AHHelper.INSTANCE.isEnabled()) {
         this.isAuc = AutoBuyUtil.isAuction(this.field_2797);
      }

      if (this.isAuc && AHHelper.INSTANCE.isEnabled()) {
         int lowSum = Integer.MAX_VALUE;
         int allSum = Integer.MAX_VALUE;

         for(int i = 0; i < 44; ++i) {
            Slot slot = (Slot)this.method_17577().slots.get(i);
            if (!slot.getStack().isEmpty()) {
               int sum = AutoBuyUtil.getPrice(slot.getStack());
               if (sum < lowSum) {
                  this.lowSumSlotId = slot;
                  lowSum = sum;
               }

               if (sum / slot.getStack().getCount() < allSum) {
                  allSum = sum / slot.getStack().getCount();
                  this.lowAllSumSlotId = slot;
               }
            }
         }
      }

   }

   @Inject(
      method = {"drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V"},
      at = {@At("HEAD")}
   )
   private void onDrawSlotInject(DrawContext context, Slot slot, CallbackInfo ci) {
      if (AHHelper.INSTANCE.isEnabled()) {
         if (slot == this.lowSumSlotId) {
            AHHelper.INSTANCE.renderCheat(context, slot);
         } else if (slot == this.lowAllSumSlotId) {
            AHHelper.INSTANCE.renderGood(context, slot);
         }
      }

   }

   @Shadow
   protected abstract boolean method_2387(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void method_2383(Slot var1, int var2, int var3, SlotActionType var4);

   @Unique
   private boolean isDropAllHovered;

   @Unique
   private void dropAllItems() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.interactionManager == null) return;
      ScreenHandler handler = mc.player.currentScreenHandler;
      for (Slot slot : handler.slots) {
         if (slot.hasStack()) {
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
         }
      }
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void mouseClickedHook(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (this.isDropAllHovered && button == 0) {
         dropAllItems();
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void renderDropAllButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) return;

      int x = 10;
      int y = 10;
      int width = 80;
      int height = 15;

      this.isDropAllHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

      context.fill(x, y, x + width, y + height, this.isDropAllHovered ? 0xFF555555 : 0xFF333333);
      context.drawText(mc.textRenderer, "Выбросить все", x + 5, y + 4, 0xFFFFFFFF, false);
   }

   @Unique
   private boolean attack() {
      return ItemScroller.INSTANCE.mouseHold;
   }

   @Unique
   private boolean shit() {
      return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 340) || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 344);
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void drawScreenHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (MinecraftClient.getInstance().player == null) {
         return;
      }

      for(int i1 = 0; i1 < MinecraftClient.getInstance().player.currentScreenHandler.slots.size(); ++i1) {
         Slot slot = (Slot)MinecraftClient.getInstance().player.currentScreenHandler.slots.get(i1);
         if (this.method_2387(slot, (double)mouseX, (double)mouseY) && slot.isEnabled() && ItemScroller.INSTANCE.isEnabled() && this.shit() && this.attack() && !slot.getStack().isEmpty()) {
            this.method_2383(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
         }
      }
   }

}