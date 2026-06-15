package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.utility.render.display.Keyboard;
import wtf.dexum.client.modules.api.setting.Setting;

public class BooleanSetting extends Setting {
   private boolean enabled;
   private final String description;
   private final Animation animation;
   private int keyCode = -1;
   private String nameKey = "";

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
      this.nameKey = Keyboard.getKeyName(keyCode);
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public String getNameKey() {
      return this.nameKey;
   }

   public BooleanSetting(String name, boolean state) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.description = "";
   }

   public BooleanSetting(String name, String description, boolean state) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.description = description;
   }

   public BooleanSetting(String name, String description, boolean state, Supplier<Boolean> supplier) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.setVisible(supplier);
      this.description = description;
   }

   public BooleanSetting(String name, boolean state, Supplier<Boolean> visible) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.setVisible(visible);
      this.description = "";
   }

   public static BooleanSetting of(String name, boolean state) {
      return new BooleanSetting(name, state);
   }

   public static BooleanSetting of(String name) {
      return new BooleanSetting(name, true);
   }

   public void toggle() {
      this.enabled = !this.enabled;
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(String.valueOf(this.name), this.isEnabled());
      propertiesObject.addProperty(this.name + "_bind", this.getKeyCode());
   }

   public void load(JsonObject propertiesObject) {
      this.setEnabled(propertiesObject.get(String.valueOf(this.name)).getAsBoolean());
      if (propertiesObject.has(this.name + "_bind")) {
         this.setKeyCode(propertiesObject.get(this.name + "_bind").getAsInt());
      }
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Generated
   public String getDescription() {
      return this.description;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }
}