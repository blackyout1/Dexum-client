package wtf.dexum.client.modules.impl.misc;

import java.util.Collection;
import java.util.Iterator;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.StringSetting;

@ModuleAnnotation(
   name = "NameProtect",
   category = Category.MISC,
   description = "Защищает имена игроков"
)
public final class NameProtect extends Module {
   public static final NameProtect INSTANCE = new NameProtect();
   private final StringSetting customName = new StringSetting("Имя", "dexum");
   private final StringSetting titlePrefix = new StringSetting("Префикс", "");
   private final BooleanSetting hideInWatermark = new BooleanSetting("Скрыть в Watermark", true);
   private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

   private NameProtect() {
   }

   @Native
   public static String getCustomName() {
      Module module = INSTANCE;
      if (module != null && module.isEnabled()) {
         String name = INSTANCE.customName.getValue().trim();
         return name.isEmpty() ? "dexum" : name;
      }
      return mc.player.getNameForScoreboard();
   }

   @Native
   public static String getTitlePrefix() {
      Module module = INSTANCE;
      if (module != null && module.isEnabled()) {
         return INSTANCE.titlePrefix.getValue().trim();
      }
      return "";
   }

   @Native
   public static String getProtectedDisplayName() {
      String prefix = getTitlePrefix();
      String name = getCustomName();
      if (!prefix.isEmpty()) {
         return prefix + name;
      }
      return name;
   }

   @Native
   public static String getWatermarkName() {
      if (INSTANCE.isEnabled() && INSTANCE.hideInWatermark.isEnabled()) {
         String prefix = getTitlePrefix();
         if (!prefix.isEmpty()) {
            return prefix;
         }
         return getCustomName();
      }
      return mc.player.getName().getString();
   }

   @Native
   public static String getCustomName(String originalName) {
      Module module = INSTANCE;
      if (module != null && module.isEnabled() && mc.player != null) {
         String me = mc.player.getNameForScoreboard();
         String replacement = getProtectedDisplayName();
         if (originalName.contains(me)) {
            return originalName.replace(me, replacement);
         } else {
            if (module instanceof NameProtect) {
               NameProtect nameProtect = (NameProtect)module;
               if (nameProtect.hideFriends.isEnabled()) {
                  Collection<String> friends = Dexum.getInstance().getFriendManager().getItems();
                  Iterator var5 = friends.iterator();

                  while(var5.hasNext()) {
                     String friend = (String)var5.next();
                     if (originalName.contains(friend)) {
                        return originalName.replace(friend, getCustomName());
                     }
                  }
               }
            }

            return originalName;
         }
      } else {
         return originalName;
      }
   }
}