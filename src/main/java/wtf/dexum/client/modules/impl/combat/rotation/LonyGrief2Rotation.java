package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;

import java.util.ArrayList;
import java.util.List;

public class LonyGrief2Rotation extends RotationBase {
    private final List<Vec3d> targetHistory = new ArrayList<>();
    private float tension = 0.0F;
    private long lastMouseUpdateTime = 0L;
    private long lastRotationUpdate = 0L;
    private float momentumYaw = 0.0F;
    private float momentumPitch = 0.0F;
    private int mistakeTicks = 0;
    private float mistakeYaw = 0.0F;
    private int postAttackTicks = 0;

    public void update(LivingEntity target, Vec3d point, Rotation targetAngle, boolean elytraVisual, boolean canAttack) {
        long now = System.currentTimeMillis();
        if (this.lastRotationUpdate == 0L) this.lastRotationUpdate = now;
        float dt = (now - this.lastRotationUpdate) / 50.0F;
        this.lastRotationUpdate = now;

        long mouseDelta = now - this.lastMouseUpdateTime;
        boolean isInputTick = mouseDelta > (4 + rng.nextInt(12));
        if (isInputTick) this.lastMouseUpdateTime = now;

        this.targetHistory.add(point);
        if (this.targetHistory.size() > 20) this.targetHistory.remove(0);

        this.tension = MathHelper.lerp(0.05F * dt, this.tension, canAttack ? 1.0F : 0.0F);
        int neuralLag = 4 + (int)(this.tension * 5.0) + rng.nextInt(3);
        int delayIdx = MathHelper.clamp(this.targetHistory.size() - neuralLag, 0, this.targetHistory.size() - 1);
        Vec3d focusedPoint = this.targetHistory.get(delayIdx);

        Rotation goalRot = RotationUtil.fromVec3d(focusedPoint.subtract(mc.player.getEyePos()));
        float dYaw = MathHelper.wrapDegrees(goalRot.getYaw() - this.lastYaw);
        float dPitch = goalRot.getPitch() - this.lastPitch;
        float totalDist = (float) Math.sqrt(dYaw * dYaw + dPitch * dPitch);

        double bioNoiseX = this.noCircling ? 0 : Math.sin(now * 0.0013) * 0.15 + Math.sin(now * 0.012) * 0.08 + (rng.nextFloat() - 0.5) * 0.04;
        double bioNoiseY = this.noCircling ? 0 : Math.cos(now * 0.0011) * 0.12 + Math.cos(now * 0.015) * 0.06 + (rng.nextFloat() - 0.5) * 0.03;

        float friction = 0.74F + (rng.nextFloat() * 0.12F);
        this.momentumYaw *= friction;
        this.momentumPitch *= friction;

        if (isInputTick) {
            float muscleForce = 0.28F + (float)(Math.tanh(totalDist / 20.0F) * 0.35F);
            muscleForce *= (1.0F + (rng.nextFloat() - 0.5F) * 0.25F);

            this.momentumYaw += dYaw * muscleForce * dt;
            this.momentumPitch += dPitch * muscleForce * dt;
        }

        if (this.mistakeTicks > 0) {
            this.mistakeTicks--;
            this.momentumYaw += this.mistakeYaw * 0.18F;
        } else if (totalDist > 12.0F && rng.nextFloat() < 0.08F) {
            this.mistakeTicks = 3 + rng.nextInt(3);
            this.mistakeYaw = (rng.nextFloat() - 0.5F) * 3.5F;
        }

        float recoilX = 0.0F;
        if (this.postAttackTicks > 0) {
            float impulse = (float) Math.pow((float)this.postAttackTicks / 7.0F, 1.5);
            recoilX = (float) (Math.sin(this.postAttackTicks * 3.5) * impulse * 2.2F);
            this.postAttackTicks--;
        }

        float finalYawMove = (this.momentumYaw + (float)bioNoiseX + recoilX);
        float finalPitchMove = (this.momentumPitch + (float)bioNoiseY);

        float finalYaw = this.lastYaw + finalYawMove;
        float finalPitch = this.lastPitch + finalPitchMove;

        float baseGcd = Rotation.gcd();
        float shift = (float)(Math.sin(now * 0.0005) * 0.05);
        float organicGcd = baseGcd * (0.94F + rng.nextFloat() * 0.12F + shift);

        if (rng.nextFloat() > 0.04F) {
            finalYaw -= (finalYaw - this.lastYaw) % organicGcd;
            finalPitch -= (finalPitch - this.lastPitch) % organicGcd;
        }

        Rotation smoothRot = new Rotation(finalYaw, finalPitch);

        float visualStep = 100.0F + rng.nextFloat() * 160.0F;
        if (totalDist < 0.8F) visualStep = 360.0F;

        RotationComponent.update(smoothRot, visualStep, visualStep, visualStep, visualStep, 0, 1, elytraVisual);

        this.lastYaw = smoothRot.getYaw();
        this.lastPitch = smoothRot.getPitch();
    }

    public void setPostAttackTicks(int ticks) {
        this.postAttackTicks = ticks;
    }

    public int getPostAttackTicks() {
        return this.postAttackTicks;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
    }
}