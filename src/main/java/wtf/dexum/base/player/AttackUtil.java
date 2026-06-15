package wtf.dexum.base.player;

import lombok.Generated;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.client.modules.impl.misc.FreeCam;
import wtf.dexum.client.modules.impl.movement.AirStuck;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.game.player.SimulatedPlayer;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.math.Timer;

public final class AttackUtil implements IClient {
    private static final Timer attackTimer = new Timer();
    private static int count = 0;

    public static void attackEntity(Entity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
        ++count;
    }

    public static boolean canAttack() {
        if (Aura.INSTANCE.isEnabled() && Aura.INSTANCE.critsOnlyWithSpace.isEnabled()) {
            if (hasPreAttackRestrictions()) {
                return false;
            }
            if (mc.player.isOnGround()) {
                return false;
            }
            if (mc.player.getVelocity().y >= 0.0D) {
                return false;
            }
            return isPlayerInCriticalState() || mc.player.fallDistance > 0.0F;
        }

        double effectiveJumpHeight = (double)mc.player.getStepHeight();
        Vec3d jumpVec = new Vec3d(0.0D, effectiveJumpHeight, 0.0D);
        Vec3d allowedMovement = mc.player.adjustMovementForCollisions(jumpVec);
        boolean notCrit = hasPreAttackRestrictions()
                || allowedMovement.y < (double)mc.player.getStepHeight() - 0.5D && mc.player.isOnGround()
                || mc.player.getVelocity().y == -0.005D && mc.player.isTouchingWater();
        return notCrit || mc.player.fallDistance > 0.0F && mc.player.prevY - mc.player.getY() != 0.12160004615783748D && mc.player.prevY - mc.player.getY() != 0.37663049823865435D && mc.player.prevY - mc.player.getY() != 0.30431682745754074D && mc.player.prevY - mc.player.getY() != 0.3739040364667261D;
    }

    private static boolean hasPreAttackRestrictions() {
        return mc.player.isInLava()
                || mc.player.isClimbing()
                || mc.player.isTouchingWater() && mc.player.isSubmergedInWater()
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || PlayerIntersectionUtil.isPlayerInBlock(Blocks.COBWEB)
                || mc.player.isGliding()
                || mc.player.hasVehicle()
                || mc.player.getAbilities().flying
                || FreeCam.INSTANCE.isEnabled()
                || AirStuck.INSTANCE.frozen;
    }

    public static boolean hasPreMovementRestrictions(SimulatedPlayer simulatedPlayer) {
        return simulatedPlayer.hasStatusEffect(StatusEffects.BLINDNESS) || simulatedPlayer.hasStatusEffect(StatusEffects.LEVITATION) || PlayerIntersectionUtil.isBoxInBlock(simulatedPlayer.boundingBox, Blocks.COBWEB) || simulatedPlayer.isSubmergedInWater() || simulatedPlayer.isInLava() || simulatedPlayer.isClimbing() || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING) && mc.player.isInSneakingPose() || mc.player.getAbilities().flying;
    }

    public static boolean isPlayerInCriticalState() {
        boolean crit = mc.player.fallDistance > 0.0F && ((double)mc.player.fallDistance < 0.08D || !SimulatedPlayer.simulateLocalPlayer(1).onGround);
        return !mc.player.isOnGround() && crit;
    }

    public static boolean isPrePlayerInCriticalState(SimulatedPlayer simulatedPlayer) {
        boolean crit = simulatedPlayer.fallDistance > 0.0F && ((double)simulatedPlayer.fallDistance < 0.08D || !SimulatedPlayer.simulateLocalPlayer(2).onGround);
        return !simulatedPlayer.onGround && crit;
    }

    @Generated
    private AttackUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}