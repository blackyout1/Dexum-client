package wtf.dexum.utility.game.other;

import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.interfaces.IMinecraft;

@Deprecated
public class InventoryUtil implements IMinecraft {
   public static int findItem(Item item) {
      return findItem(item, 0, 35);
   }

   public static int findHotbar(Item item) {
      return findItem(item, 0, 8);
   }

   public static int findInventory(Item item) {
      return findItem(item, 9, 35);
   }

   @Native
   public static int findItem(Item item, int start, int end) {
      for(int i = end; i >= start; --i) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static int findEmptySlot(int start, int end) {
      for(int i = end; i >= start; --i) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static void switchSlot(InventoryUtil.Switch mode, int slot, int previousSlot) {
      if (slot != -1 && previousSlot != -1) {
         switch(mode.ordinal()) {
         case 0:
            mc.player.getInventory().selectedSlot = slot;
            break;
         case 1:
            mc.player.getInventory().selectedSlot = slot;
            NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            break;
         case 2:
            swapItems(slot, previousSlot);
         }

      }
   }

   public static void switchBack(InventoryUtil.Switch mode, int slot, int previousSlot) {
      if (slot != -1 && previousSlot != -1) {
         switch(mode.ordinal()) {
         case 0:
            mc.player.getInventory().selectedSlot = previousSlot;
            break;
         case 1:
            mc.player.getInventory().selectedSlot = previousSlot;
            NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            break;
         case 2:
            swapItems(slot, previousSlot);
         }

      }
   }

   @Native
   public static void swapItems(int slot, int targetSlot) {
      if (slot != -1 && targetSlot != -1) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   @Native
   public static void swap(int slot, int targetSlot) {
      if (slot != -1 && targetSlot != -1) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(targetSlot), 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public static void bypassSwap(int slot, int targetSlot) {
      if (slot != -1 && targetSlot != -1) {
         swap(slot, targetSlot);
      }
   }

   public static int indexToSlot(int index) {
      return index >= 0 && index <= 8 ? 36 + index : index;
   }

   public static void swing(InventoryUtil.Swing mode) {
      switch(mode.ordinal()) {
      case 0:
         mc.player.swingHand(Hand.MAIN_HAND);
         break;
      case 1:
         mc.player.swingHand(Hand.OFF_HAND);
         break;
      case 2:
         NetworkUtils.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

   }

   public static enum Switch {
      Normal,
      Silent,
      Alternative,
      None;

      private static InventoryUtil.Switch[] $values() {
         return new InventoryUtil.Switch[]{Normal, Silent, Alternative, None};
      }
   }

   public static enum Swing {
      MainHand,
      OffHand,
      Packet,
      None;

      private static InventoryUtil.Swing[] $values() {
         return new InventoryUtil.Swing[]{MainHand, OffHand, Packet, None};
      }
   }
}