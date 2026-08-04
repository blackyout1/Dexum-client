package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.player.EventAttack;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "NoFriendDamage",
        category = Category.COMBAT,
        description = "Отменяет урон по друзьям из списка"
)
public class NoFriendDamage extends Module {
    public static final NoFriendDamage INSTANCE = new NoFriendDamage();

    private NoFriendDamage() {
        super();
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;
        
        Entity target = event.getTarget();
        
        // Если атакуем игрока
        if (target instanceof PlayerEntity player) {
            // Проверяем, является ли он другом
            if (Dexum.getInstance().getFriendManager().isFriend(player.getName().getString())) {
                // Отменяем атаку - не даём ударить друга
                event.setCancelled(true);
            }
        }
    }
}
