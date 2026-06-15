package wtf.dexum.utility.math;

import java.util.concurrent.ThreadLocalRandom;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class MathUtil implements IMinecraft {
   public static double PI2 = 6.283185307179586D;
   private static final int TABLE_SIZE = 65536;
   private static final double TWO_PI = 6.283185307179586D;
   private static final double[] TRIG_TABLE = new double[65536];

   public static double sin(double radians) {
      int index = (int)(radians * 10430.378350470453D) & '\uffff';
      return TRIG_TABLE[index];
   }

   public static double cos(double radians) {
      int index = (int)(radians * 10430.378350470453D + 16384.0D) & '\uffff';
      return TRIG_TABLE[index];
   }

   public static float random(double min, double max) {
      return (float)(min + (max - min) * Math.random());
   }

   public static double cubicBezier(double t, double p0, double p1, double p2, double p3) {
      return Math.pow(1.0D - t, 3.0D) * p0 + 3.0D * t * Math.pow(1.0D - t, 2.0D) * p1 + 3.0D * Math.pow(t, 2.0D) * (1.0D - t) * p2 + Math.pow(t, 3.0D) * p3;
   }

   public static int levenshtein(String a, String b) {
      int n = a.length();
      int m = b.length();
      int[] dp = new int[m + 1];

      int i;
      for(i = 0; i <= m; dp[i] = i++) {
      }

      for(i = 1; i <= n; ++i) {
         int prev = dp[0];
         dp[0] = i;

         for(int j = 1; j <= m; ++j) {
            int tmp = dp[j];
            int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
            dp[j] = Math.min(Math.min(dp[j] + 1, dp[j - 1] + 1), prev + cost);
            prev = tmp;
         }
      }

      return dp[m];
   }

   public static float angleDifference(float angle1, float angle2) {
      float diff = (angle1 - angle2) % 360.0F;
      if (diff < -180.0F) {
         diff += 360.0F;
      } else if (diff > 180.0F) {
         diff -= 360.0F;
      }

      return diff;
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static boolean isHoveredByCords(double mouseX, double mouseY, int x, int y, int xEnd, int yEnd) {
      return mouseX >= (double)x && mouseX <= (double)xEnd && mouseY >= (double)y && mouseY <= (double)yEnd;
   }

   public static float interpolate(double oldValue, double newValue, double interpolationValue) {
      return (float)(oldValue + (newValue - oldValue) * interpolationValue);
   }

   public static float goodSubtract(float value1, float value2) {
      return Math.abs(value1 - value2);
   }

   public static double getRandom(double min, double max) {
      if (min == max) {
         return min;
      } else {
         if (min > max) {
            double d = min;
            min = max;
            max = d;
         }

         return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
      }
   }

   public static float round(float value) {
      return (float)Math.round(value * 10.0F) / 10.0F;
   }

   public static double round(double num, double increment) {
      double rounded = (double)Math.round(num / increment) * increment;
      return (double)Math.round(rounded * 100.0D) / 100.0D;
   }

   public static Vec3d cosSin(int i, int size, double width) {
      int index = Math.min(i, size);
      float cos = (float)(Math.cos((double)index * PI2 / (double)size) * width);
      float sin = (float)(-Math.sin((double)index * PI2 / (double)size) * width);
      return new Vec3d((double)cos, 0.0D, (double)sin);
   }

   public static Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
      return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
      return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static Vec3d interpolate(Entity entity) {
      return entity == null ? Vec3d.ZERO : new Vec3d(interpolate(entity.prevX, entity.getX()), interpolate(entity.prevY, entity.getY()), interpolate(entity.prevZ, entity.getZ()));
   }

   public static float interpolate(float prev, float orig) {
      return MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(false), prev, orig);
   }

   public static double interpolate(double prev, double orig) {
      return MathHelper.lerp((double)mc.getRenderTickCounter().getTickDelta(false), prev, orig);
   }

   public static int interpolateSmooth(double smooth, int prev, int orig) {
      return (int)MathHelper.lerp((double)mc.getRenderTickCounter().getLastDuration() / smooth, (double)prev, (double)orig);
   }

   public static float interpolateSmooth(double smooth, float prev, float orig) {
      return (float)MathHelper.lerp((double)mc.getRenderTickCounter().getLastDuration() / smooth, (double)prev, (double)orig);
   }

   public static double interpolateSmooth(double smooth, double prev, double orig) {
      return MathHelper.lerp((double)mc.getRenderTickCounter().getLastDuration() / smooth, prev, orig);
   }

   public static double getDistance(Vec3d pos1, Vec3d pos2) {
      double deltaX = pos1.getX() - pos2.getX();
      double deltaY = pos1.getY() - pos2.getY();
      double deltaZ = pos1.getZ() - pos2.getZ();
      return (double)MathHelper.sqrt((float)(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
   }

   @Generated
   private MathUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      for(int i = 0; i < 65536; ++i) {
         TRIG_TABLE[i] = Math.sin((double)i * 6.283185307179586D / 65536.0D);
      }

   }
}