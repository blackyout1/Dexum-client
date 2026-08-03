package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
        name = "SlotFix",
        category = Category.MISC,
        description = "Защищает от принудительной смены слота сервером"
)
public class SlotFix extends Module {
    public static final SlotFix INSTANCE = new SlotFix();

    private SlotFix() {
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!e.isReceive()) return;
        if (!(e.getPacket() instanceof UpdateSelectedSlotS2CPacket packet)) return;

        int serverSlot = PlayerInventoryUtil.getServerSlot(packet);
        int currentSlot = mc.player.getInventory().selectedSlot;

        if (serverSlot == currentSlot) return;

        if (PlayerInventoryUtil.isClientSwap()) {
            PlayerInventoryUtil.onServerSlotUpdate();
            return;
        }

        e.cancel();
    }
}
