package wtf.dexum.base.animations.types;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.utility.render.display.base.Gradient;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

public class ColorCycleRGBA {
   private List<ColorRGBA> baseColors;
   private final List<ColorRGBA> colors;
   private int index = 0;
   private final Animation animation;
   private static final int TL = 0;
   private static final int BL = 1;
   private static final int TR = 2;
   private static final int BR = 3;

   public ColorCycleRGBA(List<ColorRGBA> initialColors, long durationMs) {
      if (initialColors.size() != 4) {
         throw new IllegalArgumentException("надо 4 цвета - Gradient.of thow expetion");
      } else {
         this.baseColors = new ArrayList(initialColors);
         this.colors = new ArrayList(initialColors);
         this.animation = new Animation(durationMs, Easing.LINEAR);
      }
   }

   public void update() {
      float t = this.animation.getValue();
      this.animation.setDuration(1000L);
      ColorRGBA color = ((ColorRGBA)this.baseColors.get(0)).mulAlpha(0.0F);
      if (this.index == 0) {
         this.colors.set(0, ((ColorRGBA)this.baseColors.get(0)).mix(color, t));
         this.colors.set(1, color.mix((ColorRGBA)this.baseColors.get(1), t));
      }

      if (this.index == 1) {
         this.colors.set(2, ((ColorRGBA)this.baseColors.get(2)).mix(color, t));
         this.colors.set(0, color.mix((ColorRGBA)this.baseColors.get(0), t));
      }

      if (this.index == 2) {
         this.colors.set(3, ((ColorRGBA)this.baseColors.get(3)).mix(color, t));
         this.colors.set(2, color.mix((ColorRGBA)this.baseColors.get(2), t));
      }

      if (this.index == 3) {
         this.colors.set(1, ((ColorRGBA)this.baseColors.get(1)).mix(color, t));
         this.colors.set(3, color.mix((ColorRGBA)this.baseColors.get(3), t));
      }

      if (this.animation.getValue() == 1.0F) {
         this.animation.reset();
         ++this.index;
         if (this.index >= this.colors.size()) {
            this.index = 0;
         }
      }

      this.animation.update(1.0F);
   }

   public Gradient toGradient() {
      return Gradient.of((ColorRGBA)this.colors.get(0), (ColorRGBA)this.colors.get(1), (ColorRGBA)this.colors.get(2), (ColorRGBA)this.colors.get(3));
   }

   @Generated
   public List<ColorRGBA> getBaseColors() {
      return this.baseColors;
   }
}