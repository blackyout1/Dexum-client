package wtf.dexum.utility.game.player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.impl.combat.AntiBot;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.interfaces.IMinecraft;

public class TargetSelector implements IClient {
   private final PointFinder pointFinder = new PointFinder();
   private LivingEntity currentTarget = null;
   private Stream<LivingEntity> potentialTargets;

   public void lockTarget(LivingEntity target) {
      if (this.currentTarget == null) {
         this.currentTarget = target;
      }

   }

   public void releaseTarget() {
      this.currentTarget = null;
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      this.findFirstMatch(predicate).ifPresent(this::lockTarget);
      if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
         this.releaseTarget();
      }

   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, boolean ignoreWalls) {
      if (this.currentTarget != null && !this.pointFinder.hasValidPoint(this.currentTarget, maxDistance, ignoreWalls)) {
         this.releaseTarget();
      }

      this.potentialTargets = this.createStreamFromEntities(entities, maxDistance, ignoreWalls);
   }

   private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance, boolean ignoreWalls) {
      return StreamSupport.stream(entities.spliterator(), false)
         .filter(LivingEntity.class::isInstance)
         .map(LivingEntity.class::cast)
         .filter((entity) -> {
            return this.pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls);
         })
         .sorted(Comparator.comparingDouble((entity) -> {
            return entity.distanceTo(mc.player);
         }));
   }

   public LivingEntity findClosestToCrosshair(Stream<LivingEntity> stream, double maxAngleDegrees) {
      Vec3d eyes = mc.player.getCameraPosVec(1.0F);
      Vec3d look = mc.player.getRotationVec(1.0F).normalize();
      return stream.map((entity) -> {
         Vec3d toEntity = entity.getBoundingBox().getCenter().subtract(eyes).normalize();
         double dot = look.dotProduct(toEntity);
         double angle = Math.toDegrees(Math.acos(dot));
         return new SimpleEntry<>(entity, angle);
      }).filter((entry) -> {
         return entry.getValue() <= maxAngleDegrees;
      }).min(Comparator.comparingDouble(Entry::getValue)).map(Entry::getKey).orElse(null);
   }

   private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
      return this.potentialTargets.filter(predicate).findFirst();
   }

   @Generated
   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   @Generated
   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   @Generated
   public Stream<LivingEntity> getPotentialTargets() {
      return this.potentialTargets;
   }

   public static class EntityFilter {
      private final List<String> targetSettings;

      public boolean isValid(LivingEntity entity) {
         if (this.isLocalPlayer(entity)) {
            return false;
         } else if (this.isInvalidHealth(entity)) {
            return false;
         } else {
            return this.isBotPlayer(entity) ? false : this.isValidEntityType(entity);
         }
      }

      private boolean isLocalPlayer(LivingEntity entity) {
         return entity == IMinecraft.mc.player;
      }

      private boolean isInvalidHealth(LivingEntity entity) {
         return !entity.isAlive() || entity.getHealth() <= 0.0F;
      }

      private boolean isBotPlayer(LivingEntity entity) {
         boolean var10000;
         if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (AntiBot.INSTANCE.isBot(player)) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }

      private boolean isNakedPlayer(LivingEntity entity) {
         return entity.isPlayer();
      }

      private boolean isValidEntityType(LivingEntity entity) {
         if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            return Dexum.getInstance().getFriendManager().isFriend(player.getGameProfile().getName()) ? false : this.targetSettings.contains("Игроков");
         } else if (entity instanceof MobEntity) {
            return this.targetSettings.contains("Мобов");
         } else if (entity instanceof AnimalEntity) {
            return this.targetSettings.contains("Животных");
         } else {
            return !(entity instanceof ArmorStandEntity);
         }
      }

      @Generated
      public EntityFilter(List<String> targetSettings) {
         this.targetSettings = targetSettings;
      }
   }
}