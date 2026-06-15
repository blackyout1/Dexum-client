package wtf.dexum.base.font;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import wtf.dexum.Dexum;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class ResourceProvider implements IMinecraft {
   private static final ResourceManager RESOURCE_MANAGER;
   private static final Gson GSON;

   public static Identifier getShaderIdentifier(String name) {
      return Dexum.id("core/" + name);
   }

   public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
      return GSON.fromJson(toString(identifier), clazz);
   }

   public static String toString(Identifier identifier) {
      return toString(identifier, "\n");
   }

   public static String toString(Identifier identifier, String delimiter) {
      try {
         InputStream inputStream = RESOURCE_MANAGER.open(identifier);

         String var4;
         try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try {
               var4 = (String)reader.lines().collect(Collectors.joining(delimiter));
            } catch (Throwable var8) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            reader.close();
         } catch (Throwable var9) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }
            }

            throw var9;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var4;
      } catch (IOException var10) {
         throw new RuntimeException(var10);
      }
   }

   static {
      RESOURCE_MANAGER = mc.getResourceManager();
      GSON = new Gson();
   }
}