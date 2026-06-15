package wtf.dexum.base.waypoint;

import lombok.Generated;

public class Waypoint {
   private String name;
   private final double x;
   private final double z;

   public Waypoint(String name, double x, double z) {
      this.name = name;
      this.x = x;
      this.z = z;
   }

   public Waypoint(double x, double z) {
      this.x = x;
      this.z = z;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public double getX() {
      return this.x;
   }

   @Generated
   public double getZ() {
      return this.z;
   }
}