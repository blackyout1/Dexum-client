package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

public class ColorSetting extends Setting {
   private ColorRGBA color;
   private final ColorSetting.ColorGetter colorGetter;

   public ColorSetting(String name, ColorRGBA color, Supplier<Boolean> visible, ColorSetting.ColorGetter colorGetter) {
      this(name, color, colorGetter);
      this.setVisible(visible);
   }

   public ColorSetting(String name, ColorRGBA color, ColorSetting.ColorGetter colorGetter) {
      super(name);
      if (color == null) {
         throw new RuntimeException(name + " color is null");
      } else {
         this.color = color;
         this.setColor(color);
         this.colorGetter = colorGetter;
      }
   }

   public ColorSetting(String name, ColorRGBA color) {
      this(name, color, () -> {
         return color;
      });
   }

   public ColorSetting(String name, ColorSetting.ColorGetter color) {
      this(name, color.getDefaultColor(), color);
   }

   public ColorSetting(String name, ColorRGBA color, Supplier<Boolean> visible) {
      this(name, color, visible, () -> {
         return color;
      });
   }

   public int getIntColor() {
      return this.color.getRGB();
   }

   public void setColor(int color) {
      this.color = new ColorRGBA(color);
   }

   public void setColor(ColorRGBA color) {
      this.color = color;
   }

   public void update() {
   }

   public void reset() {
      this.color = this.colorGetter.getDefaultColor();
   }

   public ColorRGBA getColor(float alpha) {
      return this.color.mulAlpha(alpha);
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(String.valueOf(this.name), this.getIntColor());
   }

   public void load(JsonObject propertiesObject) {
      this.setColor(propertiesObject.get(String.valueOf(this.name)).getAsInt());
   }

   @Generated
   public ColorRGBA getColor() {
      return this.color;
   }

   public interface ColorGetter {
      ColorRGBA getDefaultColor();
   }
}