package wtf.dexum.base.waypoint;

import lombok.Generated;

public class Waypoint {
   private String name;
   private final double x;
   private final double y;
   private final double z;

   public Waypoint(String name, double x, double y, double z) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Waypoint(double x, double z) {
      this("", x, 64.0, z); // дефолтная Y = 64
   }
   
   public Waypoint(double x, double y, double z) {
      this("", x, y, z);
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
   public double getY() {
      return this.y;
   }

   @Generated
   public double getZ() {
      return this.z;
   }
}