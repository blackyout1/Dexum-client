package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffects;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.utility.game.player.MovingUtil;

@ModuleAnnotation(
        name = "AutoSprint",
        category = Category.MOVEMENT,
        description = "Автоматически включает спринт"
)
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();

    private final BooleanSetting sprintInWater = new BooleanSetting("Спринт в воде", true);

    private static int sprintResetPauseTicks = 0;

    private AutoSprint() {
    }

    public static void pauseForSprintReset(int ticks) {
        sprintResetPauseTicks = Math.max(sprintResetPauseTicks, ticks);
    }

    public boolean shouldKeepSprintInWater() {
        return isEnabled() && sprintInWater.isEnabled();
    }

    @Override
    public void onDisable() {
        sprintResetPauseTicks = 0;
        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventTick event) {
        if (mc.player == null) {
            return;
        }

        mc.options.sprintKey.setPressed(false);

        if (sprintResetPauseTicks > 0) {
            sprintResetPauseTicks--;
            mc.player.setSprinting(false);
            return;
        }

        boolean inWater = mc.player.isTouchingWater() || mc.player.isSubmergedInWater();
        boolean waterSprint = sprintInWater.isEnabled() && inWater && MovingUtil.hasPlayerMovement();

        boolean canSprint = waterSprint || (
                MovingUtil.hasPlayerMovement()
                        && mc.player.canSprint()
                        && !mc.player.isSneaking()
                        && !mc.player.isUsingItem()
                        && !inWater
                        && !mc.player.isGliding()
                        && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                        && !mc.player.horizontalCollision
        );

        mc.player.setSprinting(canSprint);
    }
}