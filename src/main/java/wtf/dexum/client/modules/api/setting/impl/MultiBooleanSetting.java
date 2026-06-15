package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.client.modules.api.setting.Setting;

public class MultiBooleanSetting extends Setting {
   private final List<MultiBooleanSetting.Value> booleanSettings;
   private final Animation expandAnimation = new Animation(200L, Easing.CUBIC_OUT);

   public MultiBooleanSetting(String name) {
      super(name);
      this.booleanSettings = new ArrayList();
   }

   public MultiBooleanSetting(String name, MultiBooleanSetting.Value... settings) {
      super(name);
      this.booleanSettings = new ArrayList(Arrays.asList(settings));
   }

   public MultiBooleanSetting.Value getValueByName(String settingName) {
      return (MultiBooleanSetting.Value)this.booleanSettings.stream().filter((s) -> {
         return s.getName().equalsIgnoreCase(settingName);
      }).findFirst().orElse(null);
   }

   public static MultiBooleanSetting create(String name, List<String> values) {
      MultiBooleanSetting.Value[] booleanSettings = (MultiBooleanSetting.Value[])values.stream().map((value) -> {
         return new MultiBooleanSetting.Value(value, true);
      }).toArray((x$0) -> {
         return new MultiBooleanSetting.Value[x$0];
      });
      return new MultiBooleanSetting(name, booleanSettings);
   }

   public MultiBooleanSetting.Value get(int index) {
      return (MultiBooleanSetting.Value)this.booleanSettings.get(index);
   }

   public boolean isEnable(String name) {
      MultiBooleanSetting.Value setting = this.getValueByName(name);
      return setting != null && setting.isEnabled();
   }

   public boolean isEnable(int index) {
      if (index >= this.getBooleanSettings().size()) {
         return false;
      } else {
         MultiBooleanSetting.Value setting = this.get(index);
         return setting != null && setting.isEnabled();
      }
   }

   public List<MultiBooleanSetting.Value> getSelectedValues() {
      return (List)this.booleanSettings.stream().filter(MultiBooleanSetting.Value::isEnabled).collect(Collectors.toList());
   }

   public List<String> getSelectedNames() {
      return (List)this.booleanSettings.stream().filter(MultiBooleanSetting.Value::isEnabled).map(MultiBooleanSetting.Value::getName).collect(Collectors.toList());
   }

   public void safe(JsonObject propertiesObject) {
      StringBuilder builder = new StringBuilder();
      int j = 0;

      for(Iterator var4 = this.getBooleanSettings().iterator(); var4.hasNext(); ++j) {
         MultiBooleanSetting.Value s = (MultiBooleanSetting.Value)var4.next();
         if (this.getValueByName(s.getName()).isEnabled()) {
            builder.append(s.getName()).append("\n");
         }
      }

      propertiesObject.addProperty(this.getName(), builder.toString());
   }

   public void load(JsonObject propertiesObject) {
      this.getBooleanSettings().forEach((booleanSettingx) -> {
         booleanSettingx.setEnabled(false);
      });
      String[] strs = propertiesObject.get(String.valueOf(this.name)).getAsString().split("\n");
      String[] var3 = strs;
      int var4 = strs.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         String str = var3[var5];
         MultiBooleanSetting.Value booleanSetting = this.getValueByName(str);
         if (booleanSetting != null) {
            this.getValueByName(str).setEnabled(true);
         }
      }

   }

   @Generated
   public List<MultiBooleanSetting.Value> getBooleanSettings() {
      return this.booleanSettings;
   }

   @Generated
   public Animation getExpandAnimation() {
      return this.expandAnimation;
   }

   public void updateExpandAnimation(boolean expanded) {
      this.expandAnimation.setEasing(expanded ? Easing.CUBIC_OUT : Easing.CUBIC_IN);
      this.expandAnimation.update(expanded);
   }

   public static class Value {
      private boolean enabled;
      private final String name;
      private final Animation animation;

      public Value(String name, boolean state) {
         this.animation = new Animation(250L, Easing.CUBIC_OUT);
         this.enabled = state;
         this.name = name;
      }

      public Value(MultiBooleanSetting parent, String name, boolean state) {
         this.animation = new Animation(250L, Easing.CUBIC_OUT);
         this.enabled = state;
         this.name = name;
         parent.booleanSettings.add(this);
      }

      public static MultiBooleanSetting.Value of(String name, boolean state) {
         return new MultiBooleanSetting.Value(name, state);
      }

      public static MultiBooleanSetting.Value of(String name) {
         return new MultiBooleanSetting.Value(name, true);
      }

      public void toggle() {
         this.enabled = !this.enabled;
      }

      @Generated
      public boolean isEnabled() {
         return this.enabled;
      }

      @Generated
      public String getName() {
         return this.name;
      }

      @Generated
      public Animation getAnimation() {
         return this.animation;
      }

      @Generated
      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }
   }
}