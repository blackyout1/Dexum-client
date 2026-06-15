package wtf.dexum.utility.render.display.base;

import lombok.Generated;
import net.minecraft.util.Identifier;
import wtf.dexum.Dexum;

public class CustomSprite {
   private final Identifier texture;

   public CustomSprite(String path) {
      if (path.contains(":")) {
         this.texture = Identifier.of(path);
      } else if (path.contains("/")) {
         this.texture = Dexum.id(path);
      } else {
         this.texture = Dexum.id("icons/category/" + path);
      }

   }

   @Generated
   public Identifier getTexture() {
      return this.texture;
   }
}