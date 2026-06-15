package wtf.dexum.client.modules.api.setting;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;

public abstract class Setting {
   protected final String name;
   private final Animation animationAlpha;
   protected Supplier<Boolean> visible;

   public Setting(String name) {
      this.animationAlpha = new Animation(250L, Easing.CUBIC_OUT);
      this.name = name;
      this.setVisible(() -> {
         return true;
      });
   }

   public abstract void safe(JsonObject var1);

   public abstract void load(JsonObject var1);

   public boolean isVisible() {
      return (Boolean)this.visible.get();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public Animation getAnimationAlpha() {
      return this.animationAlpha;
   }

   @Generated
   public Supplier<Boolean> getVisible() {
      return this.visible;
   }

   @Generated
   public void setVisible(Supplier<Boolean> visible) {
      this.visible = visible;
   }
}