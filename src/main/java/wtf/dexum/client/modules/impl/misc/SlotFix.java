package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
        name = "SlotFix",
        category = Category.MISC,
        description = "Защищает от перестановки предметов сервером"
)
public class SlotFix extends Module {
    public static final SlotFix INSTANCE = new SlotFix();

    private SlotFix() {
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!e.isReceive()) return;
        
        // Блокируем серверные обновления слотов в инвентаре игрока
        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            // syncId == 0 это инвентарь игрока
            if (packet.getSyncId() == 0) {
                int slot = packet.getSlot();
                
                // Слоты 36-44 это хотбар (9 слотов)
                // Если это клиентское действие - разрешаем
                if (slot >= 36 && slot <= 44) {
                    if (PlayerInventoryUtil.isClientSwap()) {
                        PlayerInventoryUtil.onServerSlotUpdate();
                        return;
                    }
                    
                    // Блокируем серверное изменение хотбара
                    e.cancel();
                }
            }
        }
    }
}
