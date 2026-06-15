package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

import java.util.Random;

@ModuleAnnotation(
        name = "TriggerBot",
        category = Category.COMBAT,
        description = "Авто-атака при наведении на цель"
)
public class TriggerBot extends Module {

    public static final TriggerBot INSTANCE = new TriggerBot();

    private final NumberSetting distance = new NumberSetting("Дистанция", 4.0F, 1.0F, 6.0F, 0.1F);
    private final BooleanSetting rayTraceCheck = new BooleanSetting("Проверка видимости", true);
    private final BooleanSetting requireClick = new BooleanSetting("Только при зажатой атаке", false);
    private final BooleanSetting legitDelay = new BooleanSetting("Человеческая задержка", true);
    private final BooleanSetting crits = new BooleanSetting("Криты (только при падении)", false);

    private final Random rng = new Random();
    private int pendingDelay = 0;

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (requireClick.isEnabled() && !mc.options.attackKey.isPressed()) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        double dist = mc.player.squaredDistanceTo(target);
        if (dist > distance.getCurrent() * distance.getCurrent()) return;

        if (rayTraceCheck.isEnabled() && !mc.player.canSee(target)) return;

        if (crits.isEnabled()) {
            if (mc.player.getVelocity().y >= -0.1f) return;
        }

        if (legitDelay.isEnabled()) {
            if (pendingDelay > 0) {
                pendingDelay--;
                return;
            }
        }

        if (mc.player.getAttackCooldownProgress(0.5f) < 0.95f) return;

        if (rng.nextFloat() < 0.05f) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (legitDelay.isEnabled()) {
            pendingDelay = 1 + rng.nextInt(3);
        }
    }
}