package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.base.events.impl.other.EventTickMovement;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
        name = "ServerHelper",
        category = Category.MISC,
        description = "Отдельные бинды на использование каждого предмета"
)
public final class ServerHelper extends Module {

    public static final ServerHelper INSTANCE = new ServerHelper();

    // === Отдельные бинды на каждый предмет ===
    private final KeySetting keyEyeOfEnder = new KeySetting("Eye of Ender");
    private final KeySetting keyNetheritePickaxe = new KeySetting("Незеритовый лом");
    private final KeySetting keySnowball = new KeySetting("Snowball");
    private final KeySetting keySugar = new KeySetting("Sugar");
    private final KeySetting keyDriedKelp = new KeySetting("Dried Kelp");

    private boolean trigger = false;
    private Item selectedItem = null;

    @EventTarget
    private void onKey(EventKey e) {
        if (mc.currentScreen != null) return;
        if (e.getAction() != 1) return;

        if (e.getKeyCode() == keyEyeOfEnder.getKeyCode()) {
            selectedItem = Items.ENDER_EYE;
            trigger = true;
        } else if (e.getKeyCode() == keyNetheritePickaxe.getKeyCode()) {
            selectedItem = Items.NETHERITE_PICKAXE;
            trigger = true;
        } else if (e.getKeyCode() == keySnowball.getKeyCode()) {
            selectedItem = Items.SNOWBALL;
            trigger = true;
        } else if (e.getKeyCode() == keySugar.getKeyCode()) {
            selectedItem = Items.SUGAR;
            trigger = true;
        } else if (e.getKeyCode() == keyDriedKelp.getKeyCode()) {
            selectedItem = Items.DRIED_KELP;
            trigger = true;
        }
    }

    @EventTarget
    @Native
    private void onTick(EventTickMovement e) {
        if (!trigger || selectedItem == null) return;
        trigger = false;

        Item item = selectedItem;

        if (mc.player.getOffHandStack().getItem() == item) {
            useItemInOffhand();
            selectedItem = null;
            return;
        }

        int hotbarSlot = PlayerInventoryUtil.find(item, 0, 8);
        int inventorySlot = PlayerInventoryUtil.find(item, 9, 45);

        boolean wasSprinting = false;

        if (mc.player.isSprinting()) {
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
            mc.player.setSprinting(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

            if (!AutoSprint.INSTANCE.isEnabled()) {
                mc.options.sprintKey.setPressed(false);
            }
            wasSprinting = true;
        }

        if (hotbarSlot != -1) {
            swapAndUse(hotbarSlot, true, item);
        } else if (inventorySlot != -1) {
            swapAndUse(inventorySlot, false, item);
        }

        if (wasSprinting) {
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
        }

        selectedItem = null;
    }

    private void swapAndUse(int slot, boolean isHotbar, Item item) {
        int offhandSlot = 45;

        if (isHotbar) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, offhandSlot, slot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
        }
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));

        useItemInOffhand();

        // Возврат обратно
        if (isHotbar) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, offhandSlot, slot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
        }
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
    }

    private void useItemInOffhand() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.OFF_HAND,
                0,
                mc.player.getYaw(),
                mc.player.getPitch()
        ));
    }
}