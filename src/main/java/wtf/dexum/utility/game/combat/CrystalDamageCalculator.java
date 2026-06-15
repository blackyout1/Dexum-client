package wtf.dexum.utility.game.combat;

import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.ShapeType;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.interfaces.IClient;

public final class CrystalDamageCalculator implements IClient {
   private static final float CRYSTAL_EXPLOSION_POWER = 6.0F;

   @Native
   public static float calculateCrystalDamage(EndCrystalEntity crystal, PlayerEntity player, boolean checkBlocks) {
      if (crystal != null && !crystal.isRemoved()) {
         if (player != null && mc.world != null) {
            Vec3d crystalPos = crystal.getPos();
            Vec3d playerEyes = player.getEyePos();
            if (checkBlocks && isLineBlocked(playerEyes, crystalPos, mc.world)) {
               return 0.0F;
            } else {
               Vec3d playerPos = player.getPos();
               float rawDamage = calculateRawExplosionDamage(crystalPos, playerPos, 6.0F);
               float protectionFactor = getProtectionFactor(player);
               float finalDamage = rawDamage * (1.0F - protectionFactor);
               return Math.max(0.0F, finalDamage);
            }
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   @Native
   public static float getMaxPotentialDamage(List<EndCrystalEntity> crystals, PlayerEntity player, boolean checkBlocks) {
      if (crystals != null && !crystals.isEmpty()) {
         if (player == null) {
            return 0.0F;
         } else {
            float maxDamage = 0.0F;
            Iterator var4 = crystals.iterator();

            while(var4.hasNext()) {
               EndCrystalEntity crystal = (EndCrystalEntity)var4.next();
               float damage = calculateCrystalDamage(crystal, player, checkBlocks);
               if (damage > maxDamage) {
                  maxDamage = damage;
               }
            }

            return maxDamage;
         }
      } else {
         return 0.0F;
      }
   }

   @Native
   private static float getProtectionFactor(PlayerEntity player) {
      if (player == null) {
         return 0.0F;
      } else {
         float armorValue = (float)player.getAttributeValue(EntityAttributes.ARMOR);
         float armorProtection = Math.min(0.5F, armorValue * 0.02F);
         float resistanceProtection = 0.0F;
         StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
         if (resistance != null) {
            int amplifier = resistance.getAmplifier();
            resistanceProtection = (float)(amplifier + 1) * 0.2F;
         }

         float totalProtection = armorProtection + resistanceProtection;
         return Math.min(0.8F, totalProtection);
      }
   }

   @Native
   private static float calculateRawExplosionDamage(Vec3d explosionPos, Vec3d targetPos, float explosionPower) {
      double distance = explosionPos.distanceTo(targetPos);
      double impact = 1.0D - distance / ((double)explosionPower * 2.0D);
      if (impact <= 0.0D) {
         return 0.0F;
      } else {
         double rawDamage = (impact * impact + impact) / 2.0D * 7.0D * (double)explosionPower * 2.0D + 1.0D;
         return (float)rawDamage;
      }
   }

   private static boolean isLineBlocked(Vec3d playerEyes, Vec3d crystalPos, World world) {
      if (world == null) {
         return true;
      } else {
         BlockHitResult result = RaytracingUtil.raycast(playerEyes, crystalPos, ShapeType.OUTLINE);
         return result != null && result.getType() == Type.BLOCK;
      }
   }

   @Generated
   private CrystalDamageCalculator() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}