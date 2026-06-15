package wtf.dexum.utility.game.player;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationDelta;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.interfaces.IMinecraft;

public class PointFinder implements IMinecraft {
   private static final double MIN_GRID_SPACING = 0.15D;
   private static final int MAX_STEPS_XZ = 14;
   private static final int STEPS_Y = 10;
   private final Random random = new SecureRandom();
   private Vec3d offset;

   public PointFinder() {
      this.offset = Vec3d.ZERO;
   }

   public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Rotation initialRotation, Vec3d velocity, boolean ignoreWalls) {
      Pair<List<Vec3d>, Box> candidatePoints = this.generateCandidatePoints(entity, maxDistance, ignoreWalls);
      List<Vec3d> suitable = this.filterSuitable((List)candidatePoints.getLeft(), maxDistance);
      Vec3d bestPoint = this.findValidCenterOrNearest(suitable, maxDistance, ignoreWalls);
      if (bestPoint == null) {
         bestPoint = this.findValidCenterOrNearest((List)candidatePoints.getLeft(), maxDistance, ignoreWalls);
      }

      if (bestPoint == null) {
         bestPoint = this.findBestVectorByDistance((List)candidatePoints.getLeft());
      }

      this.updateOffset(velocity);
      Vec3d result = (bestPoint == null ? entity.getEyePos() : bestPoint).add(this.offset);
      return new Pair(result, (Box)candidatePoints.getRight());
   }

   public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box box = entity.getBoundingBox();
      Vec3d eye = mc.player.getEyePos();
      double lenX = box.getLengthX();
      double lenY = box.getLengthY();
      double lenZ = box.getLengthZ();
      int stepsX = this.computeSteps(lenX);
      int stepsZ = this.computeSteps(lenZ);
      int ySteps = Math.max(2, 10);
      double stepY = lenY / (double)(ySteps - 1);
      double minX = box.minX;
      double minY = box.minY;
      double minZ = box.minZ;
      double stepX = stepsX <= 1 ? 0.0D : lenX / (double)(stepsX - 1);
      double stepZ = stepsZ <= 1 ? 0.0D : lenZ / (double)(stepsZ - 1);
      List<Vec3d> list = new ArrayList(ySteps * stepsX * stepsZ);

      for(int iy = 0; iy < ySteps; ++iy) {
         double y = minY + (double)iy * stepY;

         for(int ix = 0; ix < stepsX; ++ix) {
            double x = minX + (double)ix * stepX;

            for(int iz = 0; iz < stepsZ; ++iz) {
               double z = minZ + (double)iz * stepZ;
               Vec3d p = new Vec3d(x, y, z);
               if (this.isValidPoint(eye, p, maxDistance, ignoreWalls)) {
                  list.add(p);
               }
            }
         }
      }

      return new Pair(list, box);
   }

   public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box box = entity.getBoundingBox();
      Vec3d eye = mc.player.getEyePos();
      double lenX = box.getLengthX();
      double lenY = box.getLengthY();
      double lenZ = box.getLengthZ();
      int stepsX = this.computeSteps(lenX);
      int stepsZ = this.computeSteps(lenZ);
      int ySteps = Math.max(2, 10);
      double stepY = lenY / (double)(ySteps - 1);
      double minX = box.minX;
      double minY = box.minY;
      double minZ = box.minZ;
      double stepX = stepsX <= 1 ? 0.0D : lenX / (double)(stepsX - 1);
      double stepZ = stepsZ <= 1 ? 0.0D : lenZ / (double)(stepsZ - 1);

      for(int iy = 0; iy < ySteps; ++iy) {
         double y = minY + (double)iy * stepY;

         for(int ix = 0; ix < stepsX; ++ix) {
            double x = minX + (double)ix * stepX;

            for(int iz = 0; iz < stepsZ; ++iz) {
               double z = minZ + (double)iz * stepZ;
               Vec3d p = new Vec3d(x, y, z);
               if (this.isValidPoint(eye, p, maxDistance, ignoreWalls)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isValidPoint(Vec3d startPoint, Vec3d endPoint, float maxDistance, boolean ignoreWalls) {
      if (startPoint.squaredDistanceTo(endPoint) > (double)maxDistance * (double)maxDistance) {
         return false;
      } else if (ignoreWalls) {
         return true;
      } else {
         BlockHitResult hit = RaytracingUtil.raycast(startPoint, endPoint, ShapeType.COLLIDER);
         return hit.getType() != Type.BLOCK;
      }
   }

   private Vec3d findValidCenterOrNearest(List<Vec3d> points, float maxDistance, boolean ignoreWalls) {
      if (points != null && !points.isEmpty()) {
         Vec3d eye = mc.player.getEyePos();
         Vec3d centroid = this.computeCentroid(points);
         return this.isValidPoint(eye, centroid, maxDistance, ignoreWalls) ? centroid : (Vec3d)points.stream().filter((p) -> {
            return this.isValidPoint(eye, p, maxDistance, ignoreWalls);
         }).min(Comparator.comparingDouble((p) -> {
            return p.squaredDistanceTo(centroid);
         })).orElse(null);
      } else {
         return null;
      }
   }

   private List<Vec3d> filterSuitable(List<Vec3d> points, float maxDistance) {
      if (points != null && !points.isEmpty()) {
         Vec3d eye = mc.player.getEyePos();
         double tight = Math.max(0.0D, (double)maxDistance - 0.3D);
         double tightSq = tight * tight;
         List<Vec3d> suitable = new ArrayList();
         Iterator var9 = points.iterator();

         while(var9.hasNext()) {
            Vec3d p = (Vec3d)var9.next();
            if (eye.squaredDistanceTo(p) < tightSq) {
               suitable.add(p);
            }
         }

         return suitable;
      } else {
         return List.of();
      }
   }

   private Vec3d computeCentroid(List<Vec3d> points) {
      double sx = 0.0D;
      double sy = 0.0D;
      double sz = 0.0D;
      int n = points.size();

      Vec3d p;
      for(Iterator var9 = points.iterator(); var9.hasNext(); sz += p.z) {
         p = (Vec3d)var9.next();
         sx += p.x;
         sy += p.y;
      }

      return new Vec3d(sx / (double)n, sy / (double)n, sz / (double)n);
   }

   private Vec3d findBestVectorByDistance(List<Vec3d> candidatePoints) {
      if (candidatePoints != null && !candidatePoints.isEmpty()) {
         Vec3d eye = mc.player.getEyePos();
         return (Vec3d)candidatePoints.stream().min(Comparator.comparingDouble((p) -> {
            return p.squaredDistanceTo(eye);
         })).orElse(null);
      } else {
         return null;
      }
   }

   private void updateOffset(Vec3d velocity) {
      this.offset = this.offset.add(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).multiply(velocity);
   }

   private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Rotation initialRotation) {
      if (initialRotation == null) {
         return Double.POSITIVE_INFINITY;
      } else {
         Rotation targetRotation = RotationUtil.fromVec3d(endPoint.subtract(startPoint));
         RotationDelta delta = initialRotation.rotationDeltaTo(targetRotation);
         return Math.hypot((double)delta.getDeltaYaw(), (double)delta.getDeltaPitch());
      }
   }

   private int computeSteps(double length) {
      if (length <= 0.0D) {
         return 1;
      } else {
         int bySpacing = (int)Math.ceil(length / 0.15D) + 1;
         int steps = Math.min(bySpacing, 14);
         return Math.max(2, steps);
      }
   }

   @Generated
   public Random getRandom() {
      return this.random;
   }

   @Generated
   public Vec3d getOffset() {
      return this.offset;
   }
}