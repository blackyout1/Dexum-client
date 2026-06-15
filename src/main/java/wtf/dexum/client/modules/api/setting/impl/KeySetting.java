package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.utility.render.display.Keyboard;

public class KeySetting extends Setting {
   private String nameKey;
   private int keyCode;

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
      this.nameKey = Keyboard.getKeyName(keyCode);
   }

   public KeySetting(String name, Supplier<Boolean> visible) {
      super(name);
      this.setVisible(visible);
      this.keyCode = -1;
      this.nameKey = Keyboard.getKeyName(this.keyCode);
   }

   public KeySetting(String name, int keyCode, Supplier<Boolean> visible) {
      super(name);
      this.setVisible(visible);
      this.keyCode = keyCode;
      this.nameKey = Keyboard.getKeyName(keyCode);
   }

   public KeySetting(String name, int keyCode) {
      super(name);
      this.keyCode = keyCode;
      this.nameKey = Keyboard.getKeyName(keyCode);
   }

   public KeySetting(String name) {
      super(name);
      this.keyCode = -1;
      this.nameKey = "";
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(String.valueOf(this.name), this.getKeyCode());
   }

   public void load(JsonObject propertiesObject) {
      this.setKeyCode(propertiesObject.get(String.valueOf(this.name)).getAsInt());
   }

   @Generated
   public String getNameKey() {
      return this.nameKey;
   }

   @Generated
   public int getKeyCode() {
      return this.keyCode;
   }
}