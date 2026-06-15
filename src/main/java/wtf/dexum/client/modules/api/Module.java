package wtf.dexum.client.modules.api;

import com.darkmagician6.eventapi.EventManager;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;
import org.jetbrains.annotations.NotNull;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.events.impl.other.EventModuleToggle;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.utility.interfaces.IClient;

public class Module implements IClient, Comparable<Module> {
   protected ModuleAnnotation info = (ModuleAnnotation)this.getClass().getAnnotation(ModuleAnnotation.class);
   private String name;
   private final Category category;
   private volatile boolean enabled;
   private int keyCode;
   private Animation animation;
   private Animation descAnimation;

   protected Module() {
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.descAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.name = this.info.name();
      this.category = this.info.category();
      this.enabled = false;
      this.keyCode = -1;
   }

   public void setToggled(boolean state) {
      if (state) {
         this.onEnable();
      } else {
         this.onDisable();
      }

      this.enabled = state;
   }

   public void toggle() {
      this.enabled = !this.enabled;
      if (this.enabled) {
         this.onEnable();
      } else {
         this.onDisable();
      }
   }

   public void onEnable() {
      EventManager.register(this);
      EventManager.call(new EventModuleToggle(this, this.enabled));
   }

   public void onDisable() {
      EventManager.unregister(this);
      EventManager.call(new EventModuleToggle(this, this.enabled));
   }

   public List<Setting> getSettings() {
      return Arrays.stream(this.getClass().getDeclaredFields()).map((field) -> {
         try {
            field.setAccessible(true);
            return field.get(this);
         } catch (IllegalAccessException var3) {

            return null;
         }
      }).filter((field) -> {
         return field instanceof Setting;
      }).map((field) -> {
         return (Setting)field;
      }).collect(Collectors.toList());
   }

   public JsonObject save() {
      JsonObject object = new JsonObject();
      object.addProperty("enabled", this.enabled);
      object.addProperty("keyCode", this.keyCode);
      JsonObject propertiesObject = new JsonObject();
      Iterator var3 = this.getSettings().iterator();

      while(var3.hasNext()) {
         Setting setting = (Setting)var3.next();
         setting.safe(propertiesObject);
      }

      object.add("Settings", propertiesObject);
      return object;
   }

   public void load(JsonObject object) {
      try {
         if (object != null) {
            if (object.has("enabled")) {
               boolean enable = object.get("enabled").getAsBoolean();
               if (enable && !this.isEnabled()) {
                  this.toggle();
               }

               if (!enable && this.isEnabled()) {
                  this.toggle();
               }
            }

            if (object.has("keyCode")) {
               this.keyCode = object.get("keyCode").getAsInt();
            }

            Iterator var7 = this.getSettings().iterator();

            while(var7.hasNext()) {
               Setting setting = (Setting)var7.next();
               String valueOf = setting.getName();
               JsonObject propertiesObject = object.getAsJsonObject("Settings");
               if (propertiesObject != null && propertiesObject.has(valueOf)) {
                  setting.load(propertiesObject);
               }
            }
         }
      } catch (Exception var6) {

      }

   }

   public int compareTo(@NotNull Module o) {
      return o.getName().compareTo(this.name);
   }

   @Generated
   public ModuleAnnotation getInfo() {
      return this.info;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public Category getCategory() {
      return this.category;
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public int getKeyCode() {
      return this.keyCode;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getDescAnimation() {
      return this.descAnimation;
   }

   @Generated
   public void setInfo(ModuleAnnotation info) {
      this.info = info;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Generated
   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   @Generated
   public void setAnimation(Animation animation) {
      this.animation = animation;
   }

   @Generated
   public void setDescAnimation(Animation descAnimation) {
      this.descAnimation = descAnimation;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Module)) {
         return false;
      } else {
         Module other = (Module)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.isEnabled() != other.isEnabled()) {
            return false;
         } else if (this.getKeyCode() != other.getKeyCode()) {
            return false;
         } else {
            label76: {
               Object this$info = this.getInfo();
               Object other$info = other.getInfo();
               if (this$info == null) {
                  if (other$info == null) {
                     break label76;
                  }
               } else if (this$info.equals(other$info)) {
                  break label76;
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

            label62: {
               Object this$category = this.getCategory();
               Object other$category = other.getCategory();
               if (this$category == null) {
                  if (other$category == null) {
                     break label62;
                  }
               } else if (this$category.equals(other$category)) {
                  break label62;
               }

               return false;
            }

            label55: {
               Object this$animation = this.getAnimation();
               Object other$animation = other.getAnimation();
               if (this$animation == null) {
                  if (other$animation == null) {
                     break label55;
                  }
               } else if (this$animation.equals(other$animation)) {
                  break label55;
               }

               return false;
            }

            Object this$descAnimation = this.getDescAnimation();
            Object other$descAnimation = other.getDescAnimation();
            if (this$descAnimation == null) {
               if (other$descAnimation != null) {
                  return false;
               }
            } else if (!this$descAnimation.equals(other$descAnimation)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof Module;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + (this.isEnabled() ? 79 : 97);
      result = result * 59 + this.getKeyCode();
      Object $info = this.getInfo();
      result = result * 59 + ($info == null ? 43 : $info.hashCode());
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $category = this.getCategory();
      result = result * 59 + ($category == null ? 43 : $category.hashCode());
      Object $animation = this.getAnimation();
      result = result * 59 + ($animation == null ? 43 : $animation.hashCode());
      Object $descAnimation = this.getDescAnimation();
      result = result * 59 + ($descAnimation == null ? 43 : $descAnimation.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getInfo());
      return "Module(info=" + var10000 + ", name=" + this.getName() + ", category=" + String.valueOf(this.getCategory()) + ", enabled=" + this.isEnabled() + ", keyCode=" + this.getKeyCode() + ", animation=" + String.valueOf(this.getAnimation()) + ", descAnimation=" + String.valueOf(this.getDescAnimation()) + ")";
   }
}