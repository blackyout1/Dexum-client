package wtf.dexum.utility.render.display;

import net.minecraft.util.Identifier;
import wtf.dexum.Dexum;

public class Texture {
   final Identifier id;

   public Texture(String path) {
      this.id = Dexum.id(this.validatePath(path));
   }

   public Texture(Identifier i) {
      this.id = Identifier.of(i.getNamespace(), i.getPath());
   }

   String validatePath(String path) {
      if (Identifier.isPathValid(path)) {
         return path;
      } else {
         StringBuilder ret = new StringBuilder();
         char[] var3 = path.toLowerCase().toCharArray();
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            char c = var3[var5];
            if (Identifier.isPathCharacterValid(c)) {
               ret.append(c);
            }
         }

         return ret.toString();
      }
   }

   public Identifier getId() {
      return this.id;
   }
}