package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class UniversalRotation extends RotationBase {
    private float momentumYaw = 0.0F;
    private float momentumPitch = 0.0F;
    private long lastRotationUpdate = 0L;

    private float currentSpeed = 0.0F;
    private float aimFatigue = 0.0F;
    private int idleTicks = 0;
    private int reactionDelayTicks = 0;
    private float tension = 0.0F;

    private float targetXOffset = 0.0F;
    private float targetYOffset = 0.0F;
    private long lastOffsetUpdate = 0L;

    public void update(net.minecraft.entity.LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        long now = System.currentTimeMillis();
        if (lastRotationUpdate == 0L) {
            lastRotationUpdate = now;
            return;
        }

        float dt = (now - lastRotationUpdate) / 1000.0F;
        lastRotationUpdate = now;

        boolean hasTrace = wtf.dexum.utility.game.player.RaytracingUtil.rayTrace(mc.player.getRotationVector(), 999.0, target.getBoundingBox());
        boolean isElytra = mc.player.isGliding();

        if (now - lastOffsetUpdate > 800 + rng.nextInt(1200)) {
            targetXOffset = (rng.nextFloat() - 0.5F) * 0.12F;
            targetYOffset = (rng.nextFloat() - 0.5F) * 0.18F;
            lastOffsetUpdate = now;
        }

        float goalYaw = MathHelper.wrapDegrees(targetAngle.getYaw() + targetXOffset);
        float goalPitch = targetAngle.getPitch() + targetYOffset;

        float deltaYaw = MathHelper.wrapDegrees(goalYaw - this.lastYaw);
        float deltaPitch = goalPitch - this.lastPitch;
        float dist = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (!hasTrace) {

            int reactionThreshold = 3 + Math.min(idleTicks / 4, 6) + rng.nextInt(3);
            if (reactionDelayTicks < reactionThreshold) {
                reactionDelayTicks++;

                deltaYaw += (rng.nextFloat() - 0.5F) * 0.8F;
                deltaPitch += (rng.nextFloat() - 0.5F) * 0.4F;
            } else {

                float speedGoal = dist > 15.0F ? 0.85F : 0.45F;
                currentSpeed = MathHelper.lerp(0.04F, currentSpeed, speedGoal);
            }
            idleTicks = 0;
        } else {

            reactionDelayTicks = 0;
            currentSpeed = MathHelper.lerp(0.45F, currentSpeed, 0.0F);
            idleTicks++;
        }

        aimFatigue = MathHelper.clamp(aimFatigue + (dist * 0.00005F) + (dt * 0.003F), 0.0F, 0.3F);
        if (hasTrace) aimFatigue *= 0.98F;

        tension = MathHelper.lerp(0.1F, tension, hasTrace ? 0.2F : 0.8F);

        float baseYawSpeed = (12.0F + rng.nextFloat() * 10.0F) / (isElytra ? 2.2F : 1.0F);
        float basePitchSpeed = (6.0F + rng.nextFloat() * 6.0F) / (isElytra ? 2.2F : 1.0F);

        float speedCurve = currentSpeed * currentSpeed * (3.0F - 2.0F * currentSpeed);
        float finalYawSpeed = baseYawSpeed * speedCurve * (1.0F - aimFatigue);
        float finalPitchSpeed = basePitchSpeed * speedCurve * (1.0F - aimFatigue);

        double time = now / 1000.0;
        float tremorX = this.noCircling ? 0 : (float) (Math.sin(time * 18.0) * 0.025F * tension);
        float tremorY = this.noCircling ? 0 : (float) (Math.cos(time * 15.0) * 0.015F * tension);

        float clampedYaw = MathHelper.clamp(deltaYaw, -finalYawSpeed, finalYawSpeed);
        float clampedPitch = MathHelper.clamp(deltaPitch, -finalPitchSpeed, finalPitchSpeed);

        if (hasTrace && Math.abs(clampedYaw) < 0.4F) clampedYaw = 0.0F;
        if (hasTrace && Math.abs(clampedPitch) < 0.25F) clampedPitch = 0.0F;

        float newYaw = this.lastYaw + clampedYaw + tremorX;
        float newPitch = this.lastPitch + clampedPitch + tremorY;

        float gcd = Rotation.gcd();
        newYaw -= (newYaw - this.lastYaw) % gcd;
        newPitch -= (newPitch - this.lastPitch) % gcd;

        Rotation finalRot = new Rotation(newYaw, MathHelper.clamp(newPitch, -90.0F, 90.0F));
        RotationComponent.update(finalRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);

        this.lastYaw = mc.player.getYaw();
        this.lastPitch = mc.player.getPitch();
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {

    }

    private int getPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return 50;
    }
}