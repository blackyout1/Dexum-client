package wtf.dexum.utility.game.player;

import java.util.Objects;
import java.util.function.Predicate;
import lombok.Generated;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.interfaces.IClient;

public final class RaytracingUtil implements IClient {
   public static BlockHitResult raycast(double range, Rotation angle, boolean includeFluids) {
      return raycast(((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getCameraPosVec(1.0F), range, angle, includeFluids);
   }

   public static BlockHitResult raycast(Vec3d vec, double range, Rotation angle, boolean includeFluids) {
      Entity entity = mc.cameraEntity;
      if (entity == null) {
         return null;
      } else {
         Vec3d rotationVec = angle.toVector();
         Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         World world = mc.world;
         if (world == null) {
            return null;
         } else {
            FluidHandling fluidHandling = includeFluids ? FluidHandling.ANY : FluidHandling.NONE;
            RaycastContext context = new RaycastContext(vec, end, ShapeType.OUTLINE, fluidHandling, entity);
            return world.raycast(context);
         }
      }
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      return raycast(start, end, shapeType, mc.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
      return mc.world.raycast(new RaycastContext(start, end, shapeType, FluidHandling.NONE, entity));
   }

   public static EntityHitResult raytraceEntity(double range, Rotation angle, Predicate<Entity> filter) {
      Entity entity = mc.cameraEntity;
      if (entity == null) {
         return null;
      } else {
         Vec3d cameraVec = entity.getCameraPosVec(1.0F);
         Vec3d rotationVec = angle.toVector();
         Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
         Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0D, 1.0D, 1.0D);
         return ProjectileUtil.raycast(entity, cameraVec, vec3d3, box, (e) -> {
            return !e.isSpectator() && filter.test(e);
         }, range * range);
      }
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      Vec3d cameraVec = ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getEyePos();
      return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
   }

   @Generated
   private RaytracingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}