package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class CakeWorldRotation extends RotationBase {
    private final LegendsGriefRotation legendsGriefRotation = new LegendsGriefRotation();

    public void update(LivingEntity target, Rotation targetAngle, float distance, float predict, boolean elytraVisual) {
        legendsGriefRotation.setYaw(this.lastYaw);
        legendsGriefRotation.setPitch(this.lastPitch);
        legendsGriefRotation.update(target, targetAngle, distance, predict, elytraVisual);
        this.lastYaw = legendsGriefRotation.getYaw();
        this.lastPitch = legendsGriefRotation.getPitch();
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}