package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
        name = "GuiMove",
        category = Category.MOVEMENT,
        description = "Позволяет двигаться с открытым инвентарем"
)
public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    private final ModeSetting mode = new ModeSetting("Обход", "Grim", "Matrix", "Vanilla");

    private boolean serverSideClosed = false;
    private int lastSyncId = -1;

    private GuiWalk() {
    }

    @Override
    public void onDisable() {
        serverSideClosed = false;
        lastSyncId = -1;
        super.onDisable();
    }

    @EventTarget
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
            serverSideClosed = false;
            lastSyncId = -1;
            return;
        }

        KeyBinding[] keys = {mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sneakKey};
        for (KeyBinding key : keys) {
            key.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()));
        }

        if (mc.currentScreen instanceof HandledScreen) {
            boolean isMoving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0 || mc.options.jumpKey.isPressed();
            int syncId = mc.player.currentScreenHandler.syncId;

            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 0) || InputUtil.isKeyPressed(mc.getWindow().getHandle(), 1)) {
                isMoving = false;
            }

            if (isMoving) {
                if (mode.is("Grim") && !serverSideClosed) {

                    mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));

                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
                    serverSideClosed = true;
                    lastSyncId = syncId;
                } else if (mode.is("Matrix")) {
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
                }
            }
        }
    }

    @EventTarget
    private void onPacket(EventPacket e) {
        if (mode.is("Grim") && serverSideClosed && e.isSent()) {

            if (e.getPacket() instanceof CloseHandledScreenC2SPacket p) {
                if (p.getSyncId() == lastSyncId) {
                    e.cancel();
                }
            }
        }
    }
}