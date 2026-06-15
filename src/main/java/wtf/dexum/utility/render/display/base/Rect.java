package wtf.dexum.utility.render.display.base;

import wtf.dexum.utility.math.MathUtil;

public record Rect(float x, float y, float width, float height) {
   public Rect(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public boolean contains(double mx, double my) {
      return MathUtil.isHovered(mx, my, (double)this.x, (double)this.y, (double)this.width, (double)this.height);
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }
}