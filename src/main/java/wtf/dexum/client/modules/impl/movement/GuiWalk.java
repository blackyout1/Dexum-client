package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.utility.game.player.MovingUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleAnnotation(
        name = "GuiMove",
        category = Category.MOVEMENT,
        description = "Позволяет двигаться с открытым инвентарем"
)
public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    private final ModeSetting mode = new ModeSetting("Режим", "default", "bypass");
    private final List<net.minecraft.network.packet.Packet<?>> delayedPackets = new CopyOnWriteArrayList<>();
    private boolean processingPackets = false;
    private boolean movedInGui = false;

    private GuiWalk() {
    }

    @Override
    public void onDisable() {
        cleanup();
        super.onDisable();
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) {
            cleanup();
            return;
        }

        if (!(mc.currentScreen instanceof InventoryScreen)) {
            if (!processingPackets && delayedPackets.isEmpty()) movedInGui = false;
            return;
        }

        movedInGui |= movementKeysDown() && !delayedPackets.isEmpty();

        for (KeyBinding binding : movementKeys(false)) {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey()).getCode())) {
                binding.setPressed(true);
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (mode.get().equals("default")) return;

        boolean moving = movedInGui || movementKeysDown();
        movedInGui |= moving && !delayedPackets.isEmpty();

        if (e.isSent()) {
            if (e.getPacket() instanceof ClickSlotC2SPacket && mc.currentScreen instanceof InventoryScreen && moving && shouldAllowMovement()) {
                delayedPackets.add(e.getPacket());
                e.cancel();
            } else if (e.getPacket() instanceof CloseHandledScreenC2SPacket && ((CloseHandledScreenC2SPacket)e.getPacket()).getSyncId() == 0 && moving && !processingPackets) {
                if (delayedPackets.isEmpty()) {
                    e.cancel();
                } else {
                    delayedPackets.add(e.getPacket());
                    e.cancel();
                    processDelayedPackets();
                }
            }
        }

        if (processingPackets && e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket) {
            e.cancel();
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false)));
        }

        if (!delayedPackets.isEmpty() && processingPackets) {
            if (e.getPacket() instanceof net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
                    || e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
                    || e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
                    || e.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket) {
                e.cancel();
            }
        }
    }

    private void processDelayedPackets() {
        processingPackets = true;
        // Сбрасываем задержку после 2 тиков
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false)));
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false)));

        for (net.minecraft.network.packet.Packet<?> p : delayedPackets) {
            mc.getNetworkHandler().sendPacket(p);
        }
        delayedPackets.clear();
        processingPackets = false;
        movedInGui = false;
    }

    private boolean movementKeysDown() {
        if (mc == null || mc.getWindow() == null || mc.options == null) return false;
        if (mc.currentScreen instanceof InventoryScreen) {
            for (KeyBinding binding : movementKeys(true)) {
                if (binding == mc.options.sneakKey) continue;
                if (binding == mc.options.sprintKey && !mc.options.forwardKey.equals(mc.options.sprintKey)) continue;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey()).getCode())) return true;
            }
        }
        return false;
    }

    private KeyBinding[] movementKeys(boolean includeModifiers) {
        return includeModifiers
                ? new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey, mc.options.jumpKey, mc.options.sneakKey, mc.options.sprintKey}
                : new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey, mc.options.jumpKey};
    }

    private boolean shouldAllowMovement() {
        return mc.player != null && mc.player.currentScreenHandler != null && mc.player.currentScreenHandler.slots.size() >= 27;
    }

    private void cleanup() {
        delayedPackets.clear();
        processingPackets = false;
        movedInGui = false;
    }
}
