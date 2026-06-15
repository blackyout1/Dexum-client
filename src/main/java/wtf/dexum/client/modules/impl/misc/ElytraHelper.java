package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.other.EventTickMovement;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
   name = "ElytraHelper",
   description = "Помощник для элитр",
   category = Category.MISC
)
public final class ElytraHelper extends Module {
   public static final ElytraHelper INSTANCE = new ElytraHelper();
   private final KeySetting elytraSetting = new KeySetting("Кнопка свапа");
   private final KeySetting fireworkSetting = new KeySetting("Кнопка фейерверка");
   private final ModeSetting mode = new ModeSetting("Мод пуска феера", new String[]{"Хвх", "Легит"});
   private final BooleanSetting fromInventory = new BooleanSetting("С инвентаря", true);
   private boolean swap;
   private int slowdownTicks = 0;
   private boolean useFirework;

   @EventTarget
   public void onKey(EventKey e) {
      if (e.isKeyDown(this.elytraSetting.getKeyCode())) {
         this.swap = true;
      } else if (e.isKeyDown(this.fireworkSetting.getKeyCode()) && mc.player.isGliding()) {
         this.useFirework = true;
      }

   }

   @EventTarget
   @Native
   private void onTick(EventTick e) {
      if (this.swap) {
         if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            int slotHotbar = PlayerInventoryUtil.find(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE), 0, 8);
            if (slotHotbar != -1) {
               boolean wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, 6, slotHotbar, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }

            int slotInventory = PlayerInventoryUtil.find(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE), 0, 8);
            if (slotHotbar == -1 && slotInventory != -1) {
               boolean wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
               mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
               mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }
         } else {
            int slotHotbar = PlayerInventoryUtil.find(Items.ELYTRA, 0, 8);
            if (slotHotbar != -1) {
               boolean wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, 6, slotHotbar, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }

            int slotInventory = PlayerInventoryUtil.find(Items.ELYTRA, 0, 8);
            if (slotHotbar == -1 && slotInventory != -1) {
               boolean wasSprinting = false;
               if (mc.player.isSprinting()) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
                  mc.player.setSprinting(false);
                  mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  if (!AutoSprint.INSTANCE.isEnabled()) {
                     mc.options.sprintKey.setPressed(false);
                  }

                  wasSprinting = true;
               }

               mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
               mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
               mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               if (wasSprinting) {
                  mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
               }
            }
         }

         this.swap = false;
      }
   }

   @EventTarget
   @Native
   private void onTickMovement(EventTickMovement e) {
      if (this.useFirework) {
         if (this.fromInventory.isEnabled() && PlayerInventoryUtil.find(Items.FIREWORK_ROCKET, 0, 8) == -1) {
            this.useFireworkFromInventory();
         } else if (this.mode.is("Хвх")) {
            PlayerInventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
         } else {
            PlayerInventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
         }

         this.useFirework = false;
      }
   }

   private void useFireworkFromInventory() {
      int fireworkSlot = PlayerInventoryUtil.find(Items.FIREWORK_ROCKET, 9, 35);
      if (fireworkSlot == -1) {
         if (this.mode.is("Хвх")) {
            PlayerInventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
         } else {
            PlayerInventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
         }
         return;
      }

      int prevSlot = mc.player.getInventory().selectedSlot;
      mc.interactionManager.clickSlot(0, fireworkSlot, prevSlot, SlotActionType.SWAP, mc.player);
      mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
      mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
      mc.interactionManager.clickSlot(0, fireworkSlot, prevSlot, SlotActionType.SWAP, mc.player);
      mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
   }

   private Slot chestPlate() {
      return ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) ? PlayerInventoryUtil.getSlot(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE)) : PlayerInventoryUtil.getSlot(Items.ELYTRA);
   }
}