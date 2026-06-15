package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.player.EventMove;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.combat.Aura;

@ModuleAnnotation(
        name = "ElytraMotion",
        category = Category.MOVEMENT,
        description = "Зависает в воздухе на элитрах рядом с целью"
)
public final class ElytraMotion extends Module {
    public static final ElytraMotion INSTANCE = new ElytraMotion();

    public final NumberSetting attackDistance = new NumberSetting("Дистанция работы", 3.0F, 0.1F, 5.0F, 0.01F);

    public boolean freeze;

    private ElytraMotion() {
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || !mc.player.isGliding()) {
            freeze = false;
            return;
        }

        Aura aura = Aura.INSTANCE;
        freeze = check(aura);
    }

    @EventTarget
    public void onMove(EventMove event) {
        if (freeze) {
            event.setMovePos(new Vec3d(0, 0, 0));
        }
    }

    public boolean check(Aura aura) {
        if (!aura.isEnabled()) return false;

        LivingEntity target = aura.getTarget();
        if (target == null || mc.player == null || !mc.player.isGliding()) return false;

        return target.distanceTo(mc.player) < attackDistance.getCurrent();
    }

    @Override
    public void onEnable() {
        freeze = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        freeze = false;
        super.onDisable();
    }
}