package wtf.dexum.utility.math;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.interfaces.IClient;

public class MultipointUtils implements IClient {
   private static Vec3d rotationPoint;
   private static Vec3d rotationMotion;

   public static Vec3d getNearestPoint(Entity target, double distance) {
      Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
      double maxDistSq = distance * distance;
      Box box = target.getBoundingBox();
      Vec3d boxCenter = box.getCenter();
      double stepXZ = 0.1D;
      double stepY = 0.1D;
      Vec3d bestPoint = null;
      double bestScore = Double.POSITIVE_INFINITY;

      for(double x = box.minX; x <= box.maxX; x += stepXZ) {
         for(double y = box.minY; y <= box.maxY; y += stepY) {
            for(double z = box.minZ; z <= box.maxZ; z += stepXZ) {
               Vec3d point = new Vec3d(x, y, z);
               if (!(eyePos.squaredDistanceTo(point) > maxDistSq)) {
                  RaycastContext context = new RaycastContext(eyePos, point, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
                  HitResult result = mc.world.raycast(context);
                  boolean visible;
                  if (result.getType() == Type.MISS) {
                     visible = true;
                  } else {
                     visible = result.getType() != Type.BLOCK;
                  }

                  if (visible) {
                     double score = boxCenter.squaredDistanceTo(point);
                     if (score < bestScore) {
                        bestScore = score;
                        bestPoint = point;
                     }
                  }
               }
            }
         }
      }

      return bestPoint == null ? target.getBoundingBox().getCenter() : bestPoint;
   }

   public static Vec3d getMultipoint(Entity target, double distance) {
      float minMotionXZ = 0.01F;
      float maxMotionXZ = 0.03F;
      float minMotionY = 0.01F;
      float maxMotionY = 0.03F;
      double lenghtX = target.getBoundingBox().getLengthX();
      double lenghtY = target.getBoundingBox().getLengthY();
      double lenghtZ = target.getBoundingBox().getLengthZ();
      if (rotationMotion.equals(Vec3d.ZERO)) {
         rotationMotion = new Vec3d((double)MathUtil.random(-0.05000000074505806D, 0.05000000074505806D), (double)MathUtil.random(-0.05000000074505806D, 0.05000000074505806D), (double)MathUtil.random(-0.05000000074505806D, 0.05000000074505806D));
      }

      rotationPoint = rotationPoint.add(rotationMotion);
      if (rotationPoint.x >= (lenghtX - 0.05D) / 2.0D) {
         rotationMotion = new Vec3d((double)(-MathUtil.random((double)minMotionXZ, (double)maxMotionXZ)), rotationMotion.getY(), rotationMotion.getZ());
      }

      if (rotationPoint.y >= lenghtY / 2.0D) {
         rotationMotion = new Vec3d(rotationMotion.getX(), (double)(-MathUtil.random((double)minMotionY, (double)maxMotionY)), rotationMotion.getZ());
      }

      if (rotationPoint.z >= (lenghtZ - 0.05D) / 2.0D) {
         rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), (double)(-MathUtil.random((double)minMotionXZ, (double)maxMotionXZ)));
      }

      if (rotationPoint.x <= -(lenghtX - 0.05D) / 2.0D) {
         rotationMotion = new Vec3d((double)MathUtil.random((double)minMotionXZ, 0.029999999329447746D), rotationMotion.getY(), rotationMotion.getZ());
      }

      if (rotationPoint.y <= 0.1D) {
         rotationMotion = new Vec3d(rotationMotion.getX(), (double)MathUtil.random((double)minMotionY, (double)maxMotionY), rotationMotion.getZ());
      }

      if (rotationPoint.z <= -(lenghtZ - 0.05D) / 2.0D) {
         rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), (double)MathUtil.random((double)minMotionXZ, (double)maxMotionXZ));
      }

      rotationPoint.add((double)MathUtil.random(-0.029999999329447746D, 0.029999999329447746D), 0.0D, (double)MathUtil.random(-0.029999999329447746D, 0.029999999329447746D));
      if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance, target.getBoundingBox())) {
         float halfBox = (float)(lenghtX / 2.0D);

         for(float x1 = -halfBox; x1 <= halfBox; x1 += 0.05F) {
            for(float z1 = -halfBox; z1 <= halfBox; z1 += 0.05F) {
               for(float y1 = 0.05F; (double)y1 <= target.getBoundingBox().getLengthY(); y1 += 0.15F) {
                  Vec3d v1 = new Vec3d(target.getX() + (double)x1, target.getY() + (double)y1, target.getZ() + (double)z1);
                  Rotation rotation = RotationUtil.fromVec3d(v1);
                  if (RaytracingUtil.rayTrace(rotation.toVector(), distance, target.getBoundingBox())) {
                     rotationPoint = new Vec3d((double)x1, (double)y1, (double)z1);
                     break;
                  }
               }
            }
         }
      }

      return target.getPos().add(rotationPoint);
   }

   static {
      rotationPoint = Vec3d.ZERO;
      rotationMotion = Vec3d.ZERO;
   }
}