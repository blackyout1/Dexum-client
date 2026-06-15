package wtf.dexum.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
        name = "LevitationControl",
        category = Category.PLAYER,
        description = "Контроль левитации через свап элитры"
)
public final class LevitationControl extends Module {
    public static final LevitationControl INSTANCE = new LevitationControl();

    private static final int CHEST_SLOT_ID = 6;

    private final BooleanSetting packetSwap = new BooleanSetting("Пакетный свап элитры", true);
    private final NumberSetting swapDelay = new NumberSetting("Задержка свапа", 80.0F, 25.0F, 200.0F, 5.0F);

    private long lastSwapTime = 0L;

    private int heldSwapSlotId = -1;
    private int packetRestoreSlotId = -1;
    private int pendingHeldSwapSlotId = -1;
    private boolean pendingHeldSwapBack = false;
    private int slowdownTicks = 0;

    private LevitationControl() {
    }

    @Override
    public void onEnable() {
        packetRestoreSlotId = -1;
        heldSwapSlotId = -1;
        pendingHeldSwapSlotId = -1;
        pendingHeldSwapBack = false;
        slowdownTicks = 0;
        lastSwapTime = 0L;
        super.onEnable();
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        if (slowdownTicks <= 0) {
            return;
        }
        event.setForward(0.0F);
        event.setStrafe(0.0F);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        tickSwapSlowdown();

        boolean levitating = mc.player.hasStatusEffect(StatusEffects.LEVITATION);
        boolean sneaking = mc.options.sneakKey.isPressed();
        boolean shouldDrop = levitating && sneaking;

        if (packetSwap.isEnabled()) {
            handlePacketSwap(shouldDrop);
            return;
        }

        handleHeldSwap(levitating, sneaking);
        if (shouldDrop && isElytraEquipped()) {
            applyLevitationDrop();
        }
    }

    private void handlePacketSwap(boolean shouldDrop) {
        if (isServerScreenOpen()) {
            return;
        }

        if (heldSwapSlotId != -1) {
            if (!canSwapNow()) {
                return;
            }
            if (swapChestWithSlot(heldSwapSlotId)) {
                heldSwapSlotId = -1;
                markSwap();
            }
            return;
        }

        if (packetRestoreSlotId != -1) {
            if (!canSwapNow()) {
                return;
            }
            if (swapChestWithSlot(packetRestoreSlotId)) {
                packetRestoreSlotId = -1;
                markSwap();
            }
            return;
        }

        if (!shouldDrop || !canSwapNow()) {
            return;
        }

        if (isElytraEquipped()) {
            applyLevitationDrop();
            return;
        }

        Slot elytraSlot = PlayerInventoryUtil.getSlot(Items.ELYTRA);
        if (elytraSlot == null) {
            return;
        }

        if (!swapChestWithSlot(elytraSlot.id)) {
            return;
        }

        applyLevitationDrop();

        if (!swapChestWithSlot(elytraSlot.id)) {
            packetRestoreSlotId = elytraSlot.id;
        }

        markSwap();
    }

    private void handleHeldSwap(boolean levitating, boolean sneaking) {
        if (isServerScreenOpen()) {
            return;
        }

        if (pendingHeldSwapSlotId != -1) {
            if (slowdownTicks > 0 || !canSwapNow()) {
                return;
            }
            if (swapChestWithSlot(pendingHeldSwapSlotId)) {
                heldSwapSlotId = pendingHeldSwapBack ? -1 : pendingHeldSwapSlotId;
                markSwap();
            }
            pendingHeldSwapSlotId = -1;
            pendingHeldSwapBack = false;
            return;
        }

        if (heldSwapSlotId != -1) {
            if (!levitating || !sneaking) {
                if (!canSwapNow()) {
                    return;
                }
                pendingHeldSwapSlotId = heldSwapSlotId;
                pendingHeldSwapBack = true;
                slowdownTicks = 2;
            }
            return;
        }

        if (!levitating || !sneaking || isElytraEquipped() || !canSwapNow()) {
            return;
        }

        Slot elytraSlot = PlayerInventoryUtil.getSlot(Items.ELYTRA);
        if (elytraSlot == null) {
            return;
        }

        pendingHeldSwapSlotId = elytraSlot.id;
        pendingHeldSwapBack = false;
        slowdownTicks = 2;
    }

    private void tickSwapSlowdown() {
        if (slowdownTicks <= 0 || mc.player == null) {
            return;
        }

        if (mc.player.input != null) {
            mc.player.input.movementForward = 0.0F;
            mc.player.input.movementSideways = 0.0F;
        }

        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            if (AutoSprint.INSTANCE.isEnabled()) {

            } else {
                mc.options.sprintKey.setPressed(false);
            }
        }

        slowdownTicks--;
    }

    private void applyLevitationDrop() {
        mc.player.setVelocity(
                mc.player.getVelocity().x,
                -0.22D,
                mc.player.getVelocity().z
        );

        if (!mc.player.isGliding()) {
            mc.getNetworkHandler().sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
            mc.player.startGliding();
        }

        for (int i = 0; i < 2; i++) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(),
                    mc.player.getY() - (0.05D * (i + 1)),
                    mc.player.getZ(),
                    false,
                    mc.player.horizontalCollision
            ));
        }
    }

    private boolean swapChestWithSlot(int slotId) {
        if (mc.player == null || mc.interactionManager == null || mc.player.currentScreenHandler == null) {
            return false;
        }

        if (slotId == CHEST_SLOT_ID) {
            return true;
        }

        if (slotId < 0 || !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            return false;
        }

        int windowId = mc.player.currentScreenHandler.syncId;

        mc.interactionManager.clickSlot(windowId, CHEST_SLOT_ID, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(windowId, slotId, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(windowId, CHEST_SLOT_ID, 0, SlotActionType.PICKUP, mc.player);

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(windowId, slotId, 0, SlotActionType.PICKUP, mc.player);
        }

        return mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private boolean canSwapNow() {
        return System.currentTimeMillis() - lastSwapTime >= (long) swapDelay.getCurrent();
    }

    private void markSwap() {
        lastSwapTime = System.currentTimeMillis();
    }

    private boolean isElytraEquipped() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    private boolean isServerScreenOpen() {
        return mc.player == null
                || mc.player.currentScreenHandler == null
                || !(mc.player.currentScreenHandler instanceof PlayerScreenHandler);
    }

    @Override
    public void onDisable() {
        if (mc.player != null && !isServerScreenOpen()) {
            if (packetRestoreSlotId != -1) {
                swapChestWithSlot(packetRestoreSlotId);
            }
            if (heldSwapSlotId != -1) {
                swapChestWithSlot(heldSwapSlotId);
            }
        }

        packetRestoreSlotId = -1;
        heldSwapSlotId = -1;
        pendingHeldSwapSlotId = -1;
        pendingHeldSwapBack = false;
        slowdownTicks = 0;
        super.onDisable();
    }
}