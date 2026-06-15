package wtf.dexum.utility.render.display;

import java.io.InputStream;
import java.net.URL;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class BufferUtil {
   public static Identifier registerDynamicTexture(String prefix, NativeImage image) {
      if (image == null) {
         return null;
      } else {
         Identifier id = Identifier.of("dexum", prefix + System.currentTimeMillis());
         MinecraftClient mc = MinecraftClient.getInstance();
         mc.execute(() -> {
            try {
               NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
               mc.getTextureManager().registerTexture(id, texture);
            } catch (Exception var4) {

            }

         });
         return id;
      }
   }

   public static NativeImage getHeadFromURL(String urlString) {
      try {
         URL url = new URL(urlString);
         InputStream stream = url.openStream();
         NativeImage image = NativeImage.read(stream);
         stream.close();
         return image;
      } catch (Exception var4) {

         return null;
      }
   }
}