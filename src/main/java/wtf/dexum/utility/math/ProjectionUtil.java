package wtf.dexum.utility.math;

import lombok.Generated;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.level.Render3DUtil;

public final class ProjectionUtil implements IMinecraft {
   @NotNull
   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      double cameraX = mc.getEntityRenderDispatcher().camera.getPos().x;
      double cameraY = mc.getEntityRenderDispatcher().camera.getPos().y;
      double cameraZ = mc.getEntityRenderDispatcher().camera.getPos().z;

      float dx = (float) (pos.x - cameraX);
      float dy = (float) (pos.y - cameraY);
      float dz = (float) (pos.z - cameraZ);

      int[] viewport = new int[4];
      GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

      Vector4f transformedCoordinates = new Vector4f(dx, dy, dz, 1.0F).mul(Render3DUtil.getLastWorldSpaceMatrix());

      Matrix4f matrixProj = Render3DUtil.getLastProjMat();
      Vector3f target = new Vector3f();
      matrixProj.project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

      double scaleFactor = mc.getWindow().getScaleFactor();
      return new Vec3d(
         (double)target.x / scaleFactor,
         (double)((float)mc.getWindow().getHeight() - target.y) / scaleFactor,
         (double)target.z
      );
   }

   public static boolean canSee(Vec3d vec3d) {
      Camera camera = mc.getEntityRenderDispatcher().camera;

      Vec3d cameraPos = camera.getPos();
      Vec3d relPos = vec3d.subtract(cameraPos);

      float pitch = camera.getPitch();
      float yaw = camera.getYaw();
      float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
      float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
      float f2 = -MathHelper.cos(-pitch * 0.017453292F);
      float f3 = MathHelper.sin(-pitch * 0.017453292F);
      Vec3d lookVec = new Vec3d(f1 * f2, f3, f * f2);

      if (relPos.dotProduct(lookVec) < 0) return false;

      Frustum frustum = mc.worldRenderer.frustum;
      return frustum != null && frustum.isVisible(new Box(vec3d.x - 0.1, vec3d.y - 0.1, vec3d.z - 0.1, vec3d.x + 0.1, vec3d.y + 0.1, vec3d.z + 0.1));
   }

   public static boolean canSee(Box box) {
      Frustum frustum = mc.worldRenderer.frustum;
      return box != null && frustum != null && frustum.isVisible(box);
   }

   public static boolean canSee(Vector4d vec) {
      return vec == null || vec.x < 0.0D && vec.z < 1.0D || vec.y < 0.0D && vec.w < 1.0D;
   }

   public static double centerX(Vector4d vec) {
      return vec.x + (vec.z - vec.x) / 2.0D;
   }

   @NotNull
   public static Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + pos.x - 0.10000000149011612D, axisAlignedBB2.minY - ent.getY() + pos.y - 0.10000000149011612D, axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.10000000149011612D, axisAlignedBB2.maxX - ent.getX() + pos.x + 0.10000000149011612D, axisAlignedBB2.maxY - ent.getY() + pos.y + 0.10000000149011612D, axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.10000000149011612D);
      return new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
   }

   public static Vector4d getVector4D(Entity ent) {
      Vector4d position = null;
      Vec3d[] var2 = getVec3ds(ent, MathUtil.interpolate(ent));
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Vec3d vector = var2[var4];
         vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (vector.z > 0.0D && vector.z < 1.0D) {
            if (position == null) {
               position = new Vector4d(vector.x, vector.y, vector.z, 0.0D);
            }

            position.x = Math.min(vector.x, position.x);
            position.y = Math.min(vector.y, position.y);
            position.z = Math.max(vector.x, position.z);
            position.w = Math.max(vector.y, position.w);
         }
      }

      return position;
   }

   @Generated
   private ProjectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}