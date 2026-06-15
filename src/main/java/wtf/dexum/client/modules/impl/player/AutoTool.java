package wtf.dexum.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
   name = "AutoTool",
   category = Category.PLAYER,
   description = "При копании берет лучший предмет"
)
public final class AutoTool extends Module {
   public static final AutoTool INSTANCE = new AutoTool();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Обычный", "Скрытный"});

   private int originalHotbarSlot = -1;
   private int swappedInventorySlot = -1;
   private int currentHotbarSlot = -1;
   private int toolOriginalHotbarIndex = -1;
   private boolean isSwapped = false;
   private boolean toolFromHotbar = false;

   private AutoTool() {
   }

   @Override
   public void onDisable() {
      restore();
      super.onDisable();
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isCreative()) {
         restore();
         return;
      }

      if (mc.interactionManager.isBreakingBlock()) {
         Block block = getTargetBlock();
         if (block == null) {
            restore();
            return;
         }

         if (mode.is("Скрытный")) {
            handleStealthSwap(block);
         } else {
            int bestHotbarSlot = findBestToolInRange(block, 0, 8);
            if (bestHotbarSlot != -1 && bestHotbarSlot != mc.player.getInventory().selectedSlot) {
               if (originalHotbarSlot == -1) {
                  originalHotbarSlot = mc.player.getInventory().selectedSlot;
               }
               mc.player.getInventory().selectedSlot = bestHotbarSlot;
            }
         }
      } else {
         restore();
      }
   }

   private void handleStealthSwap(Block block) {
      int currentSlot = mc.player.getInventory().selectedSlot;
      int bestInventorySlot = findBestToolInRange(block, 9, 35);

      if (bestInventorySlot != -1) {
         if (bestInventorySlot == currentSlot) {
            return;
         }

         if (!isSwapped || swappedInventorySlot != bestInventorySlot || currentHotbarSlot != currentSlot || toolFromHotbar) {
            if (isSwapped) {
               restore();
            }

            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, bestInventorySlot, currentSlot, SlotActionType.SWAP, mc.player);
            this.swappedInventorySlot = bestInventorySlot;
            this.currentHotbarSlot = currentSlot;
            this.toolFromHotbar = false;
            this.toolOriginalHotbarIndex = -1;
            this.isSwapped = true;
            if (originalHotbarSlot == -1) {
               originalHotbarSlot = currentSlot;
            }
         }
         return;
      }

      int bestHotbarSlot = findBestToolInRange(block, 0, 8);
      if (bestHotbarSlot == -1 || bestHotbarSlot == currentSlot) {
         return;
      }

      if (!isSwapped || toolOriginalHotbarIndex != bestHotbarSlot || currentHotbarSlot != currentSlot || !toolFromHotbar) {
         if (isSwapped) {
            restore();
         }

         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, toScreenSlot(bestHotbarSlot), currentSlot, SlotActionType.SWAP, mc.player);
         this.swappedInventorySlot = toScreenSlot(bestHotbarSlot);
         this.currentHotbarSlot = currentSlot;
         this.toolFromHotbar = true;
         this.toolOriginalHotbarIndex = bestHotbarSlot;
         this.isSwapped = true;
         if (originalHotbarSlot == -1) {
            originalHotbarSlot = currentSlot;
         }
      }
   }

   private void restore() {
      if (mc.player == null || mc.interactionManager == null) {
         resetState();
         return;
      }

      if (isSwapped) {
         if (toolFromHotbar) {
            int emptyInventorySlot = findEmptyInventorySlot();
            if (emptyInventorySlot != -1) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, emptyInventorySlot, currentHotbarSlot, SlotActionType.SWAP, mc.player);
            }
            if (toolOriginalHotbarIndex != -1 && toolOriginalHotbarIndex != currentHotbarSlot) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, toScreenSlot(toolOriginalHotbarIndex), currentHotbarSlot, SlotActionType.SWAP, mc.player);
            }
         } else if (swappedInventorySlot != -1) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, swappedInventorySlot, currentHotbarSlot, SlotActionType.SWAP, mc.player);
         }
      }

      resetState();
   }

   private void resetState() {
      isSwapped = false;
      swappedInventorySlot = -1;
      currentHotbarSlot = -1;
      toolOriginalHotbarIndex = -1;
      toolFromHotbar = false;
      originalHotbarSlot = -1;
   }

   private int toScreenSlot(int inventoryIndex) {
      return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
   }

   private int findEmptyInventorySlot() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }
      return -1;
   }

   private Block getTargetBlock() {
      HitResult hit = mc.crosshairTarget;
      if (hit instanceof BlockHitResult blockHit) {
         return mc.world.getBlockState(blockHit.getBlockPos()).getBlock();
      }
      return null;
   }

   private int findBestToolInRange(Block block, int start, int end) {
      int bestSlot = -1;
      float bestSpeed = 1.1F;

      for (int i = start; i <= end; i++) {
         float speed = getSpeed(i, block);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = i;
         }
      }
      return bestSlot;
   }

   private float getSpeed(int slot, Block block) {
      ItemStack stack = mc.player.getInventory().getStack(slot);
      return stack.getMiningSpeedMultiplier(block.getDefaultState());
   }
}