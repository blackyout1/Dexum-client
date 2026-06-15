package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
        name = "NoFall",
        category = Category.MOVEMENT,
        description = "Предотвращает получение урона от падения"
)
public class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();

    private final ModeSetting mode = new ModeSetting("Режим", "Grim", "Vanilla", "Packet", "GrimNew");

    private NoFall() {
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (mode.is("GrimNew")) {
            if (!mc.player.isOnGround() && mc.player.fallDistance > 1f) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true, false));
                mc.player.onLanding();
            }
            return;
        }

        if (mc.player.fallDistance > 3.0F) {
            switch (mode.get()) {
                case "Grim":

                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true, mc.player.horizontalCollision));
                    mc.player.fallDistance = 0;
                    break;
                case "Vanilla":
                    mc.player.fallDistance = 0;
                    break;
                case "Packet":
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                    break;
            }
        }
    }
}