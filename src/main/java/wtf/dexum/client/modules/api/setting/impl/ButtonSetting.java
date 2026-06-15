package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Generated;
import wtf.dexum.client.modules.api.setting.Setting;

public class ButtonSetting extends Setting {
   private Runnable runnable;

   public ButtonSetting(String name, Runnable runnable) {
      super(name);
      this.runnable = runnable;
   }

   public void toggle() {
      this.runnable.run();
   }

   public void safe(JsonObject propertiesObject) {
   }

   public void load(JsonObject propertiesObject) {
   }

   @Generated
   public Runnable getRunnable() {
      return this.runnable;
   }

   @Generated
   public void setRunnable(Runnable runnable) {
      this.runnable = runnable;
   }
}