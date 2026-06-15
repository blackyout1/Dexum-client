package wtf.dexum.base.theme;

import java.awt.Color;
import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class Theme {
   private Animation animation;
   private Animation checkAnimation;
   private float x;
   private float y;
   private float width;
   private float height;
   private String name;
   private int color1;
   private int color2;
   private int fromColor1;
   private int fromColor2;

   public Theme(String name, int color1, int color2) {
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.checkAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.name = name;
      this.color1 = this.saturateColor(color1);
      this.color2 = this.saturateColor(color2);
      this.fromColor1 = this.color1;
      this.fromColor2 = this.color2;
   }

   public void startAnimation(int oldColor1, int oldColor2) {
      this.fromColor1 = oldColor1;
      this.fromColor2 = oldColor2;
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
   }

   public ColorRGBA getColor() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getForegroundLight() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getForegroundColor() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getWhite() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getGrayLight() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getForegroundStroke() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getGray() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getWhiteGray() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor1), new ColorRGBA(this.color1), progress).brighter(0.1F);
   }

   public ColorRGBA getSecondColor() {
      float progress = this.animation.getValue();
      return ColorUtil.interpolate(new ColorRGBA(this.fromColor2), new ColorRGBA(this.color2), progress).brighter(0.1F);
   }

   public ColorRGBA getFriendColor() {
      return new ColorRGBA(32, 255, 32, 255);
   }

   public void drawTheme(MatrixStack matrixStack, double alpha) {
   }

   private int saturateColor(int color) {
      float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, (float[])null);
      hsb[1] = Math.min(1.0F, hsb[1] * 1.8F);
      hsb[2] = Math.min(1.0F, hsb[2] * 1.1F);
      return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getCheckAnimation() {
      return this.checkAnimation;
   }

   @Generated
   public float getX() {
      return this.x;
   }

   @Generated
   public float getY() {
      return this.y;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public int getColor1() {
      return this.color1;
   }

   @Generated
   public int getColor2() {
      return this.color2;
   }

   @Generated
   public int getFromColor1() {
      return this.fromColor1;
   }

   @Generated
   public int getFromColor2() {
      return this.fromColor2;
   }

   @Generated
   public void setAnimation(Animation animation) {
      this.animation = animation;
   }

   @Generated
   public void setCheckAnimation(Animation checkAnimation) {
      this.checkAnimation = checkAnimation;
   }

   @Generated
   public void setX(float x) {
      this.x = x;
   }

   @Generated
   public void setWidth(float width) {
      this.width = width;
   }

   @Generated
   public void setHeight(float height) {
      this.height = height;
   }

   @Generated
   public float getWidth() {
      return this.width;
   }

   @Generated
   public float getHeight() {
      return this.height;
   }

   @Generated
   public void setY(float y) {
      this.y = y;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setColor1(int color1) {
      this.color1 = color1;
   }

   @Generated
   public void setColor2(int color2) {
      this.color2 = color2;
   }

   @Generated
   public void setFromColor1(int fromColor1) {
      this.fromColor1 = fromColor1;
   }

   @Generated
   public void setFromColor2(int fromColor2) {
      this.fromColor2 = fromColor2;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Theme)) {
         return false;
      } else {
         Theme other = (Theme)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Float.compare(this.getX(), other.getX()) != 0) {
            return false;
         } else if (Float.compare(this.getY(), other.getY()) != 0) {
            return false;
         } else if (this.getColor1() != other.getColor1()) {
            return false;
         } else if (this.getColor2() != other.getColor2()) {
            return false;
         } else if (this.getFromColor1() != other.getFromColor1()) {
            return false;
         } else if (this.getFromColor2() != other.getFromColor2()) {
            return false;
         } else {
            Object this$animation = this.getAnimation();
            Object other$animation = other.getAnimation();
            if (this$animation == null) {
               if (other$animation != null) {
                  return false;
               }
            } else if (!this$animation.equals(other$animation)) {
               return false;
            }

            label55: {
               Object this$checkAnimation = this.getCheckAnimation();
               Object other$checkAnimation = other.getCheckAnimation();
               if (this$checkAnimation == null) {
                  if (other$checkAnimation == null) {
                     break label55;
                  }
               } else if (this$checkAnimation.equals(other$checkAnimation)) {
                  break label55;
               }

               return false;
            }

            Object this$name = this.getName();
            Object other$name = other.getName();
            if (this$name == null) {
               if (other$name != null) {
                  return false;
               }
            } else if (!this$name.equals(other$name)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof Theme;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + Float.floatToIntBits(this.getX());
      result = result * 59 + Float.floatToIntBits(this.getY());
      result = result * 59 + this.getColor1();
      result = result * 59 + this.getColor2();
      result = result * 59 + this.getFromColor1();
      result = result * 59 + this.getFromColor2();
      Object $animation = this.getAnimation();
      result = result * 59 + ($animation == null ? 43 : $animation.hashCode());
      Object $checkAnimation = this.getCheckAnimation();
      result = result * 59 + ($checkAnimation == null ? 43 : $checkAnimation.hashCode());
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getAnimation());
      return "Theme(animation=" + var10000 + ", checkAnimation=" + String.valueOf(this.getCheckAnimation()) + ", x=" + this.getX() + ", y=" + this.getY() + ", name=" + this.getName() + ", color1=" + this.getColor1() + ", color2=" + this.getColor2() + ", fromColor1=" + this.getFromColor1() + ", fromColor2=" + this.getFromColor2() + ")";
   }
}