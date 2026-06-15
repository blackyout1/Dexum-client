package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class LonyGriefv2Rotation extends RotationBase {
    private float momentumYaw = 0.0F;
    private float momentumPitch = 0.0F;
    private long lastRotationUpdate = 0L;

    @Override
    public void update(Rotation angle, boolean elytraVisual) {
        long now = System.currentTimeMillis();

        float targetYaw = angle.getYaw();
        float targetPitch = angle.getPitch();

        float dYaw = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
        float dPitch = targetPitch - this.lastPitch;
        float totalDelta = (float) Math.sqrt(dYaw * dYaw + dPitch * dPitch);

        if (this.lastRotationUpdate == 0L) this.lastRotationUpdate = now;
        float dt = Math.min((now - this.lastRotationUpdate) / 50.0F, 2.0F);
        this.lastRotationUpdate = now;

        double swayX = Math.sin(now * 0.0007) * 0.12 + Math.cos(now * 0.0015) * 0.07;
        double swayY = Math.cos(now * 0.0009) * 0.09 + Math.sin(now * 0.0012) * 0.06;

        double tremorX = Math.sin(now * 0.025) * 0.04 * (1.0 + rng.nextGaussian() * 0.2);
        double tremorY = Math.cos(now * 0.022) * 0.03 * (1.0 + rng.nextGaussian() * 0.15);

        double jitterX = (rng.nextFloat() - 0.5) * 0.015;
        double jitterY = (rng.nextFloat() - 0.5) * 0.012;

        float speedMod = (float) (1.0 - Math.exp(-totalDelta / 15.0F));
        float muscleForce = 0.15F + speedMod * 0.45F;

        float friction = 0.78F + (rng.nextFloat() * 0.08F);
        this.momentumYaw = this.momentumYaw * friction + (dYaw * muscleForce * dt);
        this.momentumPitch = this.momentumPitch * friction + (dPitch * muscleForce * dt);

        if (totalDelta > 5.0F && rng.nextFloat() < 0.03F) {
            float overshot = (rng.nextFloat() - 0.5F) * (totalDelta * 0.15F);
            this.momentumYaw += overshot;
        }

        float finalYaw = this.lastYaw + (this.momentumYaw + (float)(swayX + tremorX + jitterX));
        float finalPitch = this.lastPitch + (this.momentumPitch + (float)(swayY + tremorY + jitterY));

        float baseGcd = Rotation.gcd();
        float organicGcd = baseGcd * (0.98F + rng.nextFloat() * 0.04F);

        if (rng.nextFloat() > 0.02F) {
            finalYaw -= (finalYaw - this.lastYaw) % organicGcd;
            finalPitch -= (finalPitch - this.lastPitch) % organicGcd;
        }

        Rotation smoothRot = new Rotation(finalYaw, finalPitch);

        float visualStep = 110.0F + (totalDelta * 1.5F);
        if (mc.player.isGliding()) visualStep = 360.0F;

        RotationComponent.update(smoothRot, visualStep, visualStep, 360.0F, 360.0F, 0, 1, elytraVisual);

        this.lastYaw = smoothRot.getYaw();
        this.lastPitch = smoothRot.getPitch();
    }
}