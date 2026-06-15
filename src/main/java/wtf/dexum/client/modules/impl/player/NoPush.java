package wtf.dexum.client.modules.impl.player;

import java.util.List;
import lombok.Generated;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;

@ModuleAnnotation(
   name = "NoPush",
   description = "Убирает отталкивание от обьектов",
   category = Category.PLAYER
)
public class NoPush extends Module {
   public static final NoPush INSTANCE = new NoPush();
   private final MultiBooleanSetting objects = MultiBooleanSetting.create("Обьекты", List.of("Игроки", "Блоки", "Вода", "Удочки"));

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof NoPush)) {
         return false;
      } else {
         NoPush other = (NoPush)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else {
            Object this$objects = this.getObjects();
            Object other$objects = other.getObjects();
            if (this$objects == null) {
               if (other$objects != null) {
                  return false;
               }
            } else if (!this$objects.equals(other$objects)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof NoPush;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = super.hashCode();
      Object $objects = this.getObjects();
      result = result * 59 + ($objects == null ? 43 : $objects.hashCode());
      return result;
   }

   @Generated
   public MultiBooleanSetting getObjects() {
      return this.objects;
   }

   @Generated
   public String toString() {
      return "NoPush(objects=" + String.valueOf(this.getObjects()) + ")";
   }
}