package wtf.dexum.utility.game.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.game.other.NetworkUtils;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.math.MathUtil;

public final class PlayerInventoryUtil implements IClient {
   public static final List<KeyBinding> moveKeys;

   public static void updateSlots() {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      ItemStack stack = ((Item)Registries.ITEM.get((int)MathUtil.getRandom(0.0D, 100.0D))).getDefaultStack();
      mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), 0, 0, SlotActionType.PICKUP_ALL, stack, Int2ObjectMaps.singleton(0, stack)));
   }

   public static void swapWithBypassGrim(Runnable runnable) {
      if (mc.player == null || mc.getNetworkHandler() == null) return;
      try {
         mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
         if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
            if (!AutoSprint.INSTANCE.isEnabled()) mc.options.sprintKey.setPressed(false);
         }
         runnable.run();
         NetworkUtils.sendSilentPacket(new CloseHandledScreenC2SPacket(0));
         mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
      } catch (Exception e) {

      }
   }

   public static void closeScreen(boolean packet) {
      if (packet) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      } else {
         mc.player.closeHandledScreen();
      }

   }

   public static void swapHand(Slot slot, Hand hand, boolean updateInventory) {
      if (slot != null && slot.id != -1 && (!hand.equals(Hand.OFF_HAND) || slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory)) {
         int button = hand.equals(Hand.MAIN_HAND) ? mc.player.getInventory().selectedSlot : 40;
         swapHand(slot, button, updateInventory);
      }
   }

   public static void swapHand(Slot slot, int button, boolean updateInventory) {
      clickSlot(slot, button, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   public static void swapHand(Slot slot, int button) {
      clickSlot(slot, button, SlotActionType.SWAP, false);
   }

   public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
      if (slot != null) {
         clickSlot(slot.id, button, clickType, silent);
      }

   }

   public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
      clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
   }

   public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
      mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, mc.player);
      if (silent) {
         mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
      }

   }

   public static Slot getSlot(ItemStack item) {
      return getSlot(item, (s) -> {
         return true;
      });
   }

   public static Slot getSlot(Item item) {
      return getSlot(item, (s) -> {
         return true;
      });
   }

   public static Slot getSlot(Item item, Predicate<Slot> filter) {
      return getSlot(item, Comparator.comparingInt((s) -> {
         return 0;
      }), filter);
   }

   public static Slot getSlot(ItemStack item, Predicate<Slot> filter) {
      return getSlot(item, Comparator.comparingInt((s) -> {
         return 0;
      }), filter);
   }

   public static Slot getSlot(Predicate<Slot> filter) {
      return (Slot)slots().filter(filter).findFirst().orElse(null);
   }

   public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
      return (Slot)slots().filter(filter).max(comparator).orElse(null);
   }

   public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
      return (Slot)slots().filter((s) -> {
         return s.getStack().getItem().equals(item);
      }).filter(filter).max(comparator).orElse(null);
   }

   public static Slot getSlot(ItemStack item, Comparator<Slot> comparator, Predicate<Slot> filter) {
      return (Slot)slots().filter((s) -> {
         return s.getStack().getName().equals(item.getName()) && s.getStack().getItem() == item.getItem();
      }).filter(filter).max(comparator).orElse(null);
   }

   public static Slot getFoodMaxSaturationSlot() {
      return (Slot)slots().filter((s) -> {
         return s.getStack().get(DataComponentTypes.FOOD) != null && !((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).canAlwaysEat();
      }).max(Comparator.comparingDouble((s) -> {
         return (double)((FoodComponent)s.getStack().get(DataComponentTypes.FOOD)).saturation();
      })).orElse(null);
   }

   public static Slot getSlot(List<Item> item) {
      return (Slot)slots().filter((s) -> {
         return item.contains(s.getStack().getItem());
      }).findFirst().orElse(null);
   }

   public static Slot getPotion(RegistryEntry<StatusEffect> effect) {
      return (Slot)slots().filter((s) -> {
         PotionContentsComponent component = (PotionContentsComponent)s.getStack().get(DataComponentTypes.POTION_CONTENTS);
         return component == null ? false : StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch((e) -> {
            return e.getEffectType().equals(effect);
         });
      }).findFirst().orElse(null);
   }

   public static Slot getPotionFromCategory(StatusEffectCategory category) {
      return (Slot)slots().filter((s) -> {
         ItemStack stack = s.getStack();
         PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (stack.getItem().equals(Items.SPLASH_POTION) && component != null) {
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> {
               return ((StatusEffect)e.getEffectType().value()).getCategory().equals(category);
            }).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter((e) -> {
               return ((StatusEffect)e.getEffectType().value()).getCategory().equals(category2);
            }).count();
            return effects >= effects2;
         } else {
            return false;
         }
      }).findFirst().orElse(null);
   }

   public static int getInventoryCount(Item item) {
      return IntStream.range(0, 45).filter((i) -> {
         return ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getInventory().getStack(i).getItem().equals(item);
      }).map((i) -> {
         return mc.player.getInventory().getStack(i).getCount();
      }).sum();
   }

   public static int getHotbarItems(List<Item> items) {
      return IntStream.range(0, 9).filter((i) -> {
         return items.contains(mc.player.getInventory().getStack(i).getItem());
      }).findFirst().orElse(-1);
   }

   public static int getHotbarSlotId(IntPredicate filter) {
      return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
   }

   public static int getCount(Predicate<Slot> filter) {
      return slots().filter(filter).mapToInt((s) -> {
         return s.getStack().getCount();
      }).sum();
   }

   public static Slot mainHandSlot() {
      long count = slots().count();
      int i = count == 46L ? 10 : 9;
      return (Slot)slots().toList().get(Math.toIntExact(count - (long)i + (long)mc.player.getInventory().selectedSlot));
   }

   public static boolean isServerScreen() {
      return slots().toList().size() != 46;
   }

   public static Stream<Slot> slots() {
      return mc.player.currentScreenHandler.slots.stream();
   }

   public static int find(Item item, int start, int end) {
      if (mc.player == null) {
         return -1;
      } else {
         for(int i = end; i >= start; --i) {
            if (mc.player.currentScreenHandler.syncId != 0 && mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == item) {
               return i;
            }

            if (mc.player.currentScreenHandler.syncId == 0 && mc.player.getInventory().getStack(i).getItem() == item) {
               return i;
            }
         }

         return -1;
      }
   }

   public static int find(List<Item> itemList, int start, int end) {
      if (mc.player == null) {
         return -1;
      } else {
         for(int i = end; i >= start; --i) {
            Iterator var4 = itemList.iterator();

            while(var4.hasNext()) {
               Item item = (Item)var4.next();
               if (mc.player.currentScreenHandler.syncId != 0 && mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == item) {
                  return i;
               }

               if (mc.player.currentScreenHandler.syncId == 0 && mc.player.getInventory().getStack(i).getItem() == item) {
                  return i;
               }
            }
         }

         return -1;
      }
   }

   public static void swapAndUseHvH(Item item) {
      int slot = find((Item)item, 9, 45);
      int slotHotbar = find((Item)item, 0, 8);
      int previousSlot = mc.player.getInventory().selectedSlot;
      if (mc.player.getMainHandStack().getItem() == item) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      } else if (mc.player.getOffHandStack().getItem() == item) {
         mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
      } else {
         if (slotHotbar != -1) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
         }

         if (slotHotbar == -1 && slot != -1) {
            int slotCorrectable = -1;

            for(int slotNone = 0; slotNone < 8; ++slotNone) {
               ItemStack stack = mc.player.getInventory().getStack(slotNone);
               if (stack.isEmpty()) {
                  slotCorrectable = slotNone;
               }

               UseAction action = stack.getUseAction();
               if (action == UseAction.NONE) {
                  slotCorrectable = slotNone;
               }
            }

            boolean wasSprinting;
            if (slotCorrectable == -1) {
               wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(8));
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            } else {
               wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slot, slotCorrectable, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotCorrectable));
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
               mc.interactionManager.clickSlot(0, slot, slotCorrectable, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }
         }

      }
   }

   public static void swapAndUseLegit(Item item) {
      int slot = find((Item)item, 9, 45);
      int slotHotbar = find((Item)item, 0, 8);
      int previousSlot = mc.player.getInventory().selectedSlot;
      if (mc.player.getMainHandStack().getItem() == item) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      } else if (mc.player.getOffHandStack().getItem() == item) {
         mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
      } else {
         if (slotHotbar != -1) {
            mc.player.getInventory().selectedSlot = slotHotbar;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = previousSlot;
         }

         if (slotHotbar == -1 && slot != -1) {
            int slotCorrectable = -1;

            for(int slotNone = 0; slotNone < 8; ++slotNone) {
               ItemStack stack = mc.player.getInventory().getStack(slotNone);
               if (stack.isEmpty()) {
                  slotCorrectable = slotNone;
               }

               UseAction action = stack.getUseAction();
               if (action == UseAction.NONE) {
                  slotCorrectable = slotNone;
               }
            }

            boolean wasSprinting;
            if (slotCorrectable == -1) {
               wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               mc.player.getInventory().selectedSlot = slotCorrectable;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.getInventory().selectedSlot = previousSlot;
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            } else {
               wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slot, slotCorrectable, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               mc.player.getInventory().selectedSlot = slotCorrectable;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.getInventory().selectedSlot = previousSlot;
               mc.interactionManager.clickSlot(0, slot, slotCorrectable, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }
         }

      }
   }

   public static void moveItem(Slot from, int to) {
      if (from != null) {
         moveItem(from.id, to, false, false);
      }

   }

   public static void moveItem(Slot from, int to, boolean task) {
      moveItem(from, to, task, false);
   }

   public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
      if (from != null) {
         moveItem(from.id, to, task, updateInventory);
      }

   }

   public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
      if (from != to && from != -1) {
         int count = Math.toIntExact(slots().count()) - 9;
         if (from >= count && count == 36) {
            if (task) {
               PlayerInventoryComponent.addTask(() -> {
                  clickSlot(to, from - count, SlotActionType.SWAP, false);
               });
            } else {
               clickSlot(to, from - count, SlotActionType.SWAP, false);
               closeScreen(true);
            }

         } else {
            if (task) {
               PlayerInventoryComponent.addTask(() -> {
                  moveItem(from, to, updateInventory);
               });
            } else {
               moveItem(from, to, updateInventory);
               closeScreen(true);
            }

         }
      }
   }

   public static void moveItem(int from, int to, boolean updateInventory) {
      clickSlot(from, 0, SlotActionType.SWAP, false);
      clickSlot(to, 0, SlotActionType.SWAP, false);
      clickSlot(from, 0, SlotActionType.SWAP, false);
      if (updateInventory) {
         updateSlots();
      }

   }

   @Generated
   private PlayerInventoryUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
   }
}