package wtf.dexum.utility.game.player;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import wtf.dexum.utility.interfaces.IClient;

public class SimulatedPlayer implements IClient {
   public final PlayerEntity player;
   public final SimulatedPlayer.SimulatedPlayerInput input;
   public Vec3d pos;
   public Vec3d velocity;
   public Box boundingBox;
   public float yaw;
   public float pitch;
   public boolean sprinting;
   public float fallDistance;
   public int jumpingCooldown;
   public boolean isJumping;
   public boolean isFallFlying;
   public boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean touchingWater;
   public boolean isSwimming;
   public boolean submergedInWater;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeight;
   private final HashSet<TagKey<Fluid>> submergedFluidTag;
   private int simulatedTicks = 0;
   private boolean clipLedged = false;
   private static final double STEP_HEIGHT = 0.5D;

   public SimulatedPlayer(PlayerEntity player, SimulatedPlayer.SimulatedPlayerInput input, Vec3d pos, Vec3d velocity, Box boundingBox, float yaw, float pitch, boolean sprinting, float fallDistance, int jumpingCooldown, boolean isJumping, boolean isFallFlying, boolean onGround, boolean horizontalCollision, boolean verticalCollision, boolean touchingWater, boolean isSwimming, boolean submergedInWater, Object2DoubleMap<TagKey<Fluid>> fluidHeight, HashSet<TagKey<Fluid>> submergedFluidTag) {
      this.player = player;
      this.input = input;
      this.pos = pos;
      this.velocity = velocity;
      this.boundingBox = boundingBox;
      this.yaw = yaw;
      this.pitch = pitch;
      this.sprinting = sprinting;
      this.fallDistance = fallDistance;
      this.jumpingCooldown = jumpingCooldown;
      this.isJumping = isJumping;
      this.isFallFlying = isFallFlying;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.verticalCollision = verticalCollision;
      this.touchingWater = touchingWater;
      this.isSwimming = isSwimming;
      this.submergedInWater = submergedInWater;
      this.fluidHeight = fluidHeight;
      this.submergedFluidTag = submergedFluidTag;
   }

   public static SimulatedPlayer simulateLocalPlayer(int ticks) {
      SimulatedPlayer simulatedPlayer = fromClientPlayer(SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(mc.player.input.playerInput));

      for(int i = 0; i < ticks; ++i) {
         simulatedPlayer.tick();
      }

      return simulatedPlayer;
   }

   public static SimulatedPlayer simulateOtherPlayer(PlayerEntity player, int ticks) {
      SimulatedPlayer simulatedPlayer = fromOtherPlayer(player, SimulatedPlayer.SimulatedPlayerInput.guessInput(player));

      for(int i = 0; i < ticks; ++i) {
         simulatedPlayer.tick();
      }

      return simulatedPlayer;
   }

   public static SimulatedPlayer fromClientPlayer(SimulatedPlayer.SimulatedPlayerInput input) {
      ClientPlayerEntity player = mc.player;
      return new SimulatedPlayer(player, input, player.getPos(), player.getVelocity(), player.getBoundingBox(), player.getYaw(), player.getPitch(), player.isSprinting(), player.fallDistance, player.jumpingCooldown, player.jumping, player.isGliding(), player.isOnGround(), player.horizontalCollision, player.verticalCollision, player.isTouchingWater(), player.isSwimming(), player.isSubmergedInWater(), new Object2DoubleArrayMap(player.fluidHeight), new HashSet(player.submergedFluidTag));
   }

   public static SimulatedPlayer fromOtherPlayer(PlayerEntity player, SimulatedPlayer.SimulatedPlayerInput input) {
      return new SimulatedPlayer(player, input, player.getPos(), player.getPos().subtract(new Vec3d(player.prevX, player.prevY, player.prevZ)), player.getBoundingBox(), player.getYaw(), player.getPitch(), player.isSprinting(), player.fallDistance, player.jumpingCooldown, player.jumping, player.isGliding(), player.isOnGround(), player.horizontalCollision, player.verticalCollision, player.isTouchingWater(), player.isSwimming(), player.isSubmergedInWater(), new Object2DoubleArrayMap(player.fluidHeight), new HashSet(player.submergedFluidTag));
   }

   public Vec3d pos() {
      return this.player.getPos();
   }

   public void tick() {
      ++this.simulatedTicks;
      this.clipLedged = false;
      if (!(this.pos.y <= -70.0D)) {
         this.input.update();
         this.checkWaterState();
         this.updateSubmergedInWaterState();
         this.updateSwimming();
         if (this.jumpingCooldown > 0) {
            --this.jumpingCooldown;
         }

         this.isJumping = this.input.playerInput.jump();
         double newX = this.velocity.x;
         double newY = this.velocity.y;
         double newZ = this.velocity.z;
         if (Math.abs(this.velocity.x) < 0.003D) {
            newX = 0.0D;
         }

         if (Math.abs(this.velocity.y) < 0.003D) {
            newY = 0.0D;
         }

         if (Math.abs(this.velocity.z) < 0.003D) {
            newZ = 0.0D;
         }

         if (this.onGround) {
            this.isFallFlying = false;
         }

         this.velocity = new Vec3d(newX, newY, newZ);
         if (this.isJumping) {
            double fluidLevel = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean inWater = this.isTouchingWater() && fluidLevel > 0.0D;
            double swimHeight = this.getSwimHeight();
            if (inWater && (!this.onGround || fluidLevel > swimHeight)) {
               this.swimUpward(FluidTags.WATER);
            } else if (this.isInLava() && (!this.onGround || fluidLevel > swimHeight)) {
               this.swimUpward(FluidTags.LAVA);
            } else if ((this.onGround || inWater && fluidLevel <= swimHeight) && this.jumpingCooldown == 0) {
               this.jump();
               this.jumpingCooldown = 10;
            }
         }

         float sidewaysSpeed = this.input.movementSideways * 0.98F;
         float forwardSpeed = this.input.movementForward * 0.98F;
         float upwardsSpeed = 0.0F;
         if (this.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.hasStatusEffect(StatusEffects.LEVITATION)) {
            this.onLanding();
         }

         this.travel(new Vec3d((double)sidewaysSpeed, (double)upwardsSpeed, (double)forwardSpeed));
      }
   }

   private void travel(Vec3d movementInput) {
      double beforeTravelVelocityY;
      double d;
      if (this.isSwimming && !this.player.hasVehicle()) {
         beforeTravelVelocityY = this.getRotationVector().y;
         d = beforeTravelVelocityY < -0.2D ? 0.085D : 0.06D;
         BlockPos posAbove = new BlockPos(MathHelper.floor(this.pos.x), MathHelper.floor(this.pos.y + 1.0D - 0.1D), MathHelper.floor(this.pos.z));
         if (beforeTravelVelocityY <= 0.0D || this.input.playerInput.jump() || !this.player.getWorld().getBlockState(posAbove).getFluidState().isEmpty()) {
            this.velocity = this.velocity.add(0.0D, (beforeTravelVelocityY - this.velocity.y) * d, 0.0D);
         }
      }

      beforeTravelVelocityY = this.velocity.y;
      d = 0.08D;
      boolean falling = this.velocity.y <= 0.0D;
      if (this.velocity.y <= 0.0D && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
         d = 0.01D;
         this.onLanding();
      }

      double k;
      float f;
      if (this.isTouchingWater() && this.player.shouldSwimInFluids()) {
         k = this.pos.y;
         f = this.isSprinting() ? 0.9F : 0.8F;
         float g = 0.02F;
         f = (float)this.getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.onGround) {
            f *= 0.5F;
         }

         if (f > 0.0F) {
            f += (0.54600006F - f) * f / 3.0F;
            g += (this.getMovementSpeed() - g) * f / 3.0F;
         }

         if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            f = 0.96F;
         }

         this.updateVelocity(g, movementInput);
         this.move(this.velocity);
         Vec3d tempVel = this.velocity;
         if (this.horizontalCollision && this.isClimbing()) {
            tempVel = new Vec3d(tempVel.x, 0.2D, tempVel.z);
         }

         this.velocity = tempVel.multiply((double)f, 0.8D, (double)f);
         Vec3d vec3d2 = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
         this.velocity = vec3d2;
         if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6D - this.pos.y + k, vec3d2.z)) {
            this.velocity = new Vec3d(vec3d2.x, 0.3D, vec3d2.z);
         }
      } else if (this.isInLava() && this.player.shouldSwimInFluids()) {
         k = this.pos.y;
         this.updateVelocity(0.02F, movementInput);
         this.move(this.velocity);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
            this.velocity = this.velocity.multiply(0.5D, 0.8D, 0.5D);
            this.velocity = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
         } else {
            this.velocity = this.velocity.multiply(0.5D);
         }

         if (!this.player.hasNoGravity()) {
            this.velocity = this.velocity.add(0.0D, -d / 4.0D, 0.0D);
         }

         if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6D - this.pos.y + k, this.velocity.z)) {
            this.velocity = new Vec3d(this.velocity.x, 0.3D, this.velocity.z);
         }
      } else {
         Vec3d vec3d3;
         if (this.isFallFlying) {
            Vec3d e = this.velocity;
            if (e.y > -0.5D) {
               this.fallDistance = 1.0F;
            }

            vec3d3 = this.getRotationVector();
            f = this.pitch * 0.017453292F;
            double g = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
            double horizontalSpeed = this.velocity.horizontalLength();
            double i = vec3d3.length();
            float j = MathHelper.cos(f);
            j = (float)((double)j * (double)j * Math.min(1.0D, i / 0.4D));
            e = this.velocity.add(0.0D, d * (-1.0D + (double)j * 0.75D), 0.0D);
            if (e.y < 0.0D && g > 0.0D) {
               k = e.y * -0.1D * (double)j;
               e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g);
            }

            if (f < 0.0F && g > 0.0D) {
               k = horizontalSpeed * (double)(-MathHelper.sin(f)) * 0.04D;
               e = e.add(-vec3d3.x * k / g, k * 3.2D, -vec3d3.z * k / g);
            }

            if (g > 0.0D) {
               e = e.add((vec3d3.x / g * horizontalSpeed - e.x) * 0.1D, 0.0D, (vec3d3.z / g * horizontalSpeed - e.z) * 0.1D);
            }

            this.velocity = e.multiply(0.99D, 0.98D, 0.99D);
            this.move(this.velocity);
         } else {
            BlockPos blockPos = this.getVelocityAffectingPos();
            float p = this.player.getWorld().getBlockState(blockPos).getBlock().getSlipperiness();
            f = this.onGround ? p * 0.91F : 0.91F;
            vec3d3 = this.applyMovementInput(movementInput, p);
            double q = vec3d3.y;
            if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
               StatusEffectInstance levitation = this.getStatusEffect(StatusEffects.LEVITATION);
               if (levitation != null) {
                  q += (0.05D * (double)(levitation.getAmplifier() + 1) - vec3d3.y) * 0.2D;
               }
            } else if (this.player.getWorld().isClient() && !this.player.getWorld().isChunkLoaded(blockPos)) {
               q = this.pos.y > (double)this.player.getWorld().getBottomY() ? -0.1D : 0.0D;
            } else if (!this.player.hasNoGravity()) {
               q -= d;
            }

            if (this.player.hasNoDrag()) {
               this.velocity = new Vec3d(vec3d3.x, q, vec3d3.z);
            } else {
               this.velocity = new Vec3d(vec3d3.x * (double)f, q * 0.9800000190734863D, vec3d3.z * (double)f);
            }
         }
      }

      if (this.player.getAbilities().flying && !this.player.hasVehicle()) {
         this.velocity = new Vec3d(this.velocity.x, beforeTravelVelocityY * 0.6D, this.velocity.z);
         this.onLanding();
      }

   }

   private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
      this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
      this.velocity = this.applyClimbingSpeed(this.velocity);
      this.move(this.velocity);
      Vec3d result = this.velocity;
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      if ((this.horizontalCollision || this.isJumping) && (this.isClimbing() || state != null && state.isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this.player))) {
         result = new Vec3d(result.x, 0.2D, result.z);
      }

      return result;
   }

   private void updateVelocity(float speed, Vec3d movementInput) {
      Vec3d vec = Entity.movementInputToVelocity(movementInput, speed, this.yaw);
      this.velocity = this.velocity.add(vec);
   }

   private float getMovementSpeed(float slipperiness) {
      return this.onGround ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirStrafingSpeed();
   }

   private float getAirStrafingSpeed() {
      float speed = 0.02F;
      return this.input.playerInput.sprint() ? speed + 0.006F : speed;
   }

   private float getMovementSpeed() {
      return 0.1F;
   }

   private void move(Vec3d movement) {
      Vec3d modifiedMovement = this.adjustMovementForSneaking(movement);
      Vec3d adjustedMovement = this.adjustMovementForCollisions(modifiedMovement);
      if (adjustedMovement.lengthSquared() > 1.0E-7D) {
         this.pos = this.pos.add(adjustedMovement);
         this.boundingBox = this.player.dimensions.getBoxAt(this.pos);
      }

      boolean xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x);
      boolean zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z);
      this.horizontalCollision = xCollision || zCollision;
      this.verticalCollision = movement.y != adjustedMovement.y;
      this.onGround = this.verticalCollision && movement.y < 0.0D;
      if (!this.isTouchingWater()) {
         this.checkWaterState();
      }

      if (this.onGround) {
         this.onLanding();
      } else if (movement.y < 0.0D) {
         this.fallDistance -= (float)movement.y;
      }

      Vec3d currentVel = this.velocity;
      if (this.horizontalCollision || this.verticalCollision) {
         this.velocity = new Vec3d(xCollision ? 0.0D : currentVel.x, this.onGround ? 0.0D : currentVel.y, zCollision ? 0.0D : currentVel.z);
      }

   }

   private Vec3d adjustMovementForCollisions(Vec3d movement) {
      Box box = (new Box(-0.3D, 0.0D, -0.3D, 0.3D, 1.8D, 0.3D)).offset(this.pos);
      List<VoxelShape> collisionShapes = Collections.emptyList();
      Vec3d adjusted;
      if (movement.lengthSquared() == 0.0D) {
         adjusted = movement;
      } else {
         adjusted = Entity.adjustMovementForCollisions(this.player, movement, box, this.player.getWorld(), collisionShapes);
      }

      boolean xCollide = movement.x != adjusted.x;
      boolean yCollide = movement.y != adjusted.y;
      boolean zCollide = movement.z != adjusted.z;
      boolean stepPossible = this.onGround || yCollide && movement.y < 0.0D;
      if (this.player.getStepHeight() > 0.0F && stepPossible && (xCollide || zCollide)) {
         Vec3d stepAdjust = Entity.adjustMovementForCollisions(this.player, new Vec3d(movement.x, (double)this.player.getStepHeight(), movement.z), box, this.player.getWorld(), collisionShapes);
         Vec3d stepOffset = Entity.adjustMovementForCollisions(this.player, new Vec3d(0.0D, (double)this.player.getStepHeight(), 0.0D), box.stretch(movement.x, 0.0D, movement.z), this.player.getWorld(), collisionShapes);
         Vec3d combined = Entity.adjustMovementForCollisions(this.player, new Vec3d(movement.x, 0.0D, movement.z), box.offset(stepOffset), this.player.getWorld(), collisionShapes).add(stepOffset);
         if (stepOffset.y < (double)this.player.getStepHeight() && combined.horizontalLengthSquared() > stepAdjust.horizontalLengthSquared()) {
            stepAdjust = combined;
         }

         if (stepAdjust.horizontalLengthSquared() > adjusted.horizontalLengthSquared()) {
            return stepAdjust.add(Entity.adjustMovementForCollisions(this.player, new Vec3d(0.0D, -stepAdjust.y + movement.y, 0.0D), box.offset(stepAdjust), this.player.getWorld(), collisionShapes));
         }
      }

      return adjusted;
   }

   private void onLanding() {
      this.fallDistance = 0.0F;
   }

   public void jump() {
      this.velocity = this.velocity.add(0.0D, (double)this.getJumpVelocity() - this.velocity.y, 0.0D);
      if (this.isSprinting()) {
         float rad = (float)Math.toRadians((double)this.yaw);
         this.velocity = this.velocity.add((double)(-MathHelper.sin(rad)) * 0.2D, 0.0D, (double)MathHelper.cos(rad) * 0.2D);
      }

   }

   private Vec3d applyClimbingSpeed(Vec3d motion) {
      if (!this.isClimbing()) {
         return motion;
      } else {
         this.onLanding();
         double clampedX = MathHelper.clamp(motion.x, -0.15000000596046448D, 0.15000000596046448D);
         double clampedZ = MathHelper.clamp(motion.z, -0.15000000596046448D, 0.15000000596046448D);
         double clampedY = Math.max(motion.y, -0.15000000596046448D);
         if (clampedY < 0.0D && !this.getState(this.posToBlockPos(this.pos)).isOf(Blocks.SCAFFOLDING) && this.player.isHoldingOntoLadder()) {
            clampedY = 0.0D;
         }

         return new Vec3d(clampedX, clampedY, clampedZ);
      }
   }

   public boolean isClimbing() {
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      if (state.isIn(BlockTags.CLIMBABLE)) {
         return true;
      } else {
         return state.getBlock() instanceof TrapdoorBlock && this.canEnterTrapdoor(posBlock, state);
      }
   }

   private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
      if (!(Boolean)state.get(TrapdoorBlock.OPEN)) {
         return false;
      } else {
         BlockState below = this.player.getWorld().getBlockState(pos.down());
         return below.isOf(Blocks.LADDER) && ((Direction)below.get(LadderBlock.FACING)).equals(state.get(TrapdoorBlock.FACING));
      }
   }

   private Vec3d adjustMovementForSneaking(Vec3d movement) {
      if (movement.y <= 0.0D && this.method_30263()) {
         double dx = movement.x;
         double dz = movement.z;

         double step;
         for(step = 0.05D; dx != 0.0D && this.player.getWorld().isSpaceEmpty(this.player, this.boundingBox.offset(dx, -0.5D, 0.0D)); dx += dx > 0.0D ? -step : step) {
            if (dx < step && dx >= -step) {
               dx = 0.0D;
               break;
            }
         }

         while(dz != 0.0D && this.player.getWorld().isSpaceEmpty(this.player, this.boundingBox.offset(0.0D, -0.5D, dz))) {
            if (dz < step && dz >= -step) {
               dz = 0.0D;
               break;
            }

            dz += dz > 0.0D ? -step : step;
         }

         while(dx != 0.0D && dz != 0.0D && this.player.getWorld().isSpaceEmpty(this.player, this.boundingBox.offset(dx, -0.5D, dz))) {
            dx = dx < step && dx >= -step ? 0.0D : (dx > 0.0D ? dx - step : dx + step);
            if (dz < step && dz >= -step) {
               dz = 0.0D;
               break;
            }

            dz += dz > 0.0D ? -step : step;
         }

         if (movement.x != dx || movement.z != dz) {
            this.clipLedged = true;
         }

         if (this.shouldClipAtLedge()) {
            movement = new Vec3d(dx, movement.y, dz);
         }
      }

      return movement;
   }

   protected boolean shouldClipAtLedge() {
      return this.input.playerInput.sneak() || this.input.forceSafeWalk;
   }

   private boolean method_30263() {
      return this.onGround || (double)this.fallDistance < 0.5D && !this.player.getWorld().isSpaceEmpty(this.player, this.boundingBox.offset(0.0D, (double)this.fallDistance - 0.5D, 0.0D));
   }

   private boolean isSprinting() {
      return this.sprinting;
   }

   private float getJumpVelocity() {
      return 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
   }

   private float getJumpBoostVelocityModifier() {
      if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
         StatusEffectInstance boost = this.getStatusEffect(StatusEffects.JUMP_BOOST);
         return 0.1F * (float)(boost.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private float getJumpVelocityMultiplier() {
      float multiplier1 = 0.0F;
      Block block = this.getState(this.posToBlockPos(this.pos)).getBlock();
      if (block != null) {
         multiplier1 = block.getJumpVelocityMultiplier();
      }

      float multiplier2 = 0.0F;
      Block block2 = this.getState(this.getVelocityAffectingPos()).getBlock();
      if (block2 != null) {
         multiplier2 = block2.getJumpVelocityMultiplier();
      }

      return multiplier1 == 1.0F ? multiplier2 : multiplier1;
   }

   private boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
      return this.doesNotCollide(this.boundingBox.offset(offsetX, offsetY, offsetZ));
   }

   private boolean doesNotCollide(Box box) {
      return this.player.getWorld().isSpaceEmpty(this.player, box) && !this.player.getWorld().containsFluid(box);
   }

   private void swimUpward(TagKey<Fluid> fluidTag) {
      this.velocity = this.velocity.add(0.0D, 0.03999999910593033D, 0.0D);
   }

   private BlockPos getVelocityAffectingPos() {
      return BlockPos.ofFloored(this.pos.x, this.boundingBox.minY - 0.5000001D, this.pos.z);
   }

   private double getSwimHeight() {
      return (double)this.player.getStandingEyeHeight() < 0.4D ? 0.0D : 0.4D;
   }

   private boolean isTouchingWater() {
      return this.touchingWater;
   }

   public boolean isInLava() {
      return this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0D;
   }

   private void checkWaterState() {
      if (this.player.getVehicle() instanceof BoatEntity) {
         BoatEntity boat = (BoatEntity)this.player.getVehicle();
         if (!boat.isSubmergedInWater()) {
            this.touchingWater = false;
            return;
         }
      }

      if (this.updateMovementInFluid(FluidTags.WATER, 0.014D)) {
         this.onLanding();
         this.touchingWater = true;
      } else {
         this.touchingWater = false;
      }

   }

   private void updateSwimming() {
      if (this.isSwimming) {
         this.isSwimming = this.isSprinting() && this.isTouchingWater() && !this.player.hasVehicle();
      } else {
         this.isSwimming = this.isSprinting() && this.isSubmergedInWater() && !this.player.hasVehicle() && this.player.getWorld().getFluidState(this.posToBlockPos(this.pos)).isIn(FluidTags.WATER);
      }

   }

   private void updateSubmergedInWaterState() {
      this.submergedInWater = this.submergedFluidTag.contains(FluidTags.WATER);
      this.submergedFluidTag.clear();
      double eyeLevel = this.getEyeY() - 0.1111111119389534D;
      Entity vehicle = this.player.getVehicle();
      if (vehicle instanceof BoatEntity) {
         BoatEntity boat = (BoatEntity)vehicle;
         if (!boat.isSubmergedInWater() && boat.getBoundingBox().maxY >= eyeLevel && boat.getBoundingBox().minY <= eyeLevel) {
            return;
         }
      }

      BlockPos posEye = BlockPos.ofFloored(this.pos.x, eyeLevel, this.pos.z);
      FluidState fluidState = this.player.getWorld().getFluidState(posEye);
      double height = (double)((float)posEye.getY() + fluidState.getHeight(this.player.getWorld(), posEye));
      if (height > eyeLevel) {
         this.submergedFluidTag.addAll(fluidState.streamTags().toList());
      }

   }

   private double getEyeY() {
      return this.pos.y + (double)this.player.getStandingEyeHeight();
   }

   public boolean isSubmergedInWater() {
      return this.submergedInWater && this.isTouchingWater();
   }

   private double getFluidHeight(TagKey<Fluid> tag) {
      return this.fluidHeight.getDouble(tag);
   }

   private boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
      if (this.isRegionUnloaded()) {
         return false;
      } else {
         Box box = this.boundingBox.contract(0.001D);
         int i = MathHelper.floor(box.minX);
         int j = MathHelper.ceil(box.maxX);
         int k = MathHelper.floor(box.minY);
         int l = MathHelper.ceil(box.maxY);
         int m = MathHelper.floor(box.minZ);
         int n = MathHelper.ceil(box.maxZ);
         double d = 0.0D;
         boolean pushedByFluids = true;
         boolean foundFluid = false;
         Vec3d fluidVelocity = Vec3d.ZERO;
         int count = 0;
         Mutable mutable = new Mutable();

         for(int p = i; p < j; ++p) {
            for(int q = k; q < l; ++q) {
               for(int r = m; r < n; ++r) {
                  mutable.set(p, q, r);
                  FluidState fluidState = this.player.getWorld().getFluidState(mutable);
                  if (fluidState.isIn(tag)) {
                     double e = (double)((float)q + fluidState.getHeight(this.player.getWorld(), mutable));
                     if (e >= box.minY) {
                        foundFluid = true;
                        d = Math.max(e - box.minY, d);
                        if (pushedByFluids) {
                           Vec3d vel = fluidState.getVelocity(this.player.getWorld(), mutable);
                           if (d < 0.4D) {
                              vel = vel.multiply(d);
                           }

                           fluidVelocity = fluidVelocity.add(vel);
                           ++count;
                        }
                     }
                  }
               }
            }
         }

         if (fluidVelocity.length() > 0.0D) {
            if (count > 0) {
               fluidVelocity = fluidVelocity.multiply(1.0D / (double)count);
            }

            fluidVelocity = fluidVelocity.multiply(speed);
            if (Math.abs(this.velocity.x) < 0.003D && Math.abs(this.velocity.z) < 0.003D && fluidVelocity.length() < 0.0045D) {
               fluidVelocity = fluidVelocity.normalize().multiply(0.0045D);
            }

            this.velocity = this.velocity.add(fluidVelocity);
         }

         this.fluidHeight.put(tag, d);
         return foundFluid;
      }
   }

   private boolean isRegionUnloaded() {
      Box box = this.boundingBox.expand(1.0D);
      int i = MathHelper.floor(box.minX);
      int j = MathHelper.ceil(box.maxX);
      int k = MathHelper.floor(box.minZ);
      int l = MathHelper.ceil(box.maxZ);
      return !this.player.getWorld().isRegionLoaded(i, k, j, l);
   }

   private Vec3d getRotationVector() {
      return this.getRotationVector(this.pitch, this.yaw);
   }

   private Vec3d getRotationVector(float pitch, float yaw) {
      float f = (float)((double)pitch * 3.141592653589793D / 180.0D);
      float g = (float)((double)(-yaw) * 3.141592653589793D / 180.0D);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }

   public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance instance = this.player.getStatusEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks;
   }

   private StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance instance = this.player.getStatusEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks ? instance : null;
   }

   public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
      return this.player.getAttributes().getValue(attribute);
   }

   public SimulatedPlayer clone() {
      return new SimulatedPlayer(this.player, this.input, this.pos, this.velocity, this.boundingBox, this.yaw, this.pitch, this.sprinting, this.fallDistance, this.jumpingCooldown, this.isJumping, this.isFallFlying, this.onGround, this.horizontalCollision, this.verticalCollision, this.touchingWater, this.isSwimming, this.submergedInWater, new Object2DoubleArrayMap(this.fluidHeight), new HashSet(this.submergedFluidTag));
   }

   public BlockPos posToBlockPos(Vec3d pos) {
      return new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z));
   }

   public BlockState getState(BlockPos pos) {
      return this.player.getWorld().getBlockState(pos);
   }

   public static class SimulatedPlayerInput extends Input {
      public boolean forceSafeWalk = false;
      public float movementForward;
      public float movementSideways;
      public PlayerInput playerInput;
      public static final double MAX_WALKING_SPEED = 0.121D;

      public SimulatedPlayerInput(PlayerInput input) {
         this.playerInput = input;
      }

      public void update() {
         if (this.playerInput.forward() != this.playerInput.backward()) {
            this.movementForward = this.playerInput.forward() ? 1.0F : -1.0F;
         } else {
            this.movementForward = 0.0F;
         }

         if (this.playerInput.left() == this.playerInput.right()) {
            this.movementSideways = 0.0F;
         } else {
            this.movementSideways = this.playerInput.left() ? 1.0F : -1.0F;
         }

         if (this.playerInput.sneak()) {
            this.movementSideways *= 0.3F;
            this.movementForward *= 0.3F;
         }

      }

      public String toString() {
         boolean var10000 = this.playerInput.forward();
         return "SimulatedPlayerInput(forwards={" + var10000 + "}, backwards={" + this.playerInput.backward() + "}, left={" + this.playerInput.left() + "}, right={" + this.playerInput.right() + "}, jumping={" + this.playerInput.jump() + "}, sprinting=" + this.playerInput.sprint() + ", slowDown=" + this.playerInput.sneak() + ")";
      }

      public static SimulatedPlayer.SimulatedPlayerInput fromClientPlayer(PlayerInput input) {
         return new SimulatedPlayer.SimulatedPlayerInput(input);
      }

      public static SimulatedPlayer.SimulatedPlayerInput guessInput(PlayerEntity entity) {
         Vec3d velocity = entity.getPos().subtract(new Vec3d(entity.prevX, entity.prevY, entity.prevZ));
         double horizontalVelocity = velocity.horizontalLengthSquared();
         PlayerInput input = new PlayerInput(false, false, false, false, !entity.isOnGround(), entity.isSneaking(), horizontalVelocity >= 0.014641D);
         if (horizontalVelocity > 0.0025000000000000005D) {
            double velocityAngle = MovingUtil.getDegreesRelativeToView(velocity, entity.getYaw());
            double wrappedAngle = MathHelper.wrapDegrees(velocityAngle);
            input = MovingUtil.getDirectionalInputForDegrees(input, wrappedAngle);
         }

         return new SimulatedPlayer.SimulatedPlayerInput(input);
      }
   }
}