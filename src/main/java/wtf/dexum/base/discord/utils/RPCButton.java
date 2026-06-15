package wtf.dexum.base.discord.utils;

import java.io.Serializable;
import lombok.Generated;

public class RPCButton implements Serializable {
   private final String url;
   private final String label;

   public static RPCButton create(String label, String url) {
      if (label != null && !label.isEmpty()) {
         if (url != null && !url.isEmpty()) {
            label = label.substring(0, Math.min(label.length(), 31));
            return new RPCButton(label, url);
         } else {
            throw new IllegalArgumentException("Button URL cannot be null or empty");
         }
      } else {
         throw new IllegalArgumentException("Button label cannot be null or empty");
      }
   }

   protected RPCButton(String label, String url) {
      this.label = label;
      this.url = url;
   }

   @Generated
   public String getUrl() {
      return this.url;
   }

   @Generated
   public String getLabel() {
      return this.label;
   }
}