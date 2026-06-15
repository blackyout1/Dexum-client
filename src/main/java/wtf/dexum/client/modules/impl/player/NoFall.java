package wtf.dexum.client.modules.impl.player;

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
        description = "Убирает урон от падения"
)
public class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();

    private NoFall() {
    }

    @EventTarget
    public void onUpdate(final EventUpdate ignored) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!mc.player.isOnGround() && mc.player.fallDistance > 1f) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true, false));
            mc.player.onLanding();
        }
    }
}