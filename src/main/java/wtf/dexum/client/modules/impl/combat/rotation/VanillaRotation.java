package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class VanillaRotation extends RotationBase {
    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        float deltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw);
        float deltaPitch = targetAngle.getPitch() - this.lastPitch;

        float yaw = this.lastYaw + deltaYaw;
        float pitch = this.lastPitch + deltaPitch;

        float gcd = Rotation.gcd();
        yaw -= (yaw - this.lastYaw) % gcd;
        pitch -= (pitch - this.lastPitch) % gcd;

        Rotation smoothRot = new Rotation(yaw, pitch);
        RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);

        this.lastYaw = smoothRot.getYaw();
        this.lastPitch = smoothRot.getPitch();
    }
}