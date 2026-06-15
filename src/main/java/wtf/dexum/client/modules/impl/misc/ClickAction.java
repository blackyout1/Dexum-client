package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;

@ModuleAnnotation(
   name = "ClickFriend",
   description = "Добавляет друга по бинду",
   category = Category.MISC
)
public final class ClickAction extends Module {
   private final KeySetting friendBind = new KeySetting("Добавить друга");
   public static final ClickAction INSTANCE = new ClickAction();

   @EventTarget
   @Native
   public void onKey(EventKey e) {
      if (e.isKeyDown(this.friendBind.getKeyCode())) {
         HitResult var4 = mc.crosshairTarget;
         if (var4 instanceof EntityHitResult) {
            EntityHitResult result = (EntityHitResult)var4;
            Entity var5 = result.getEntity();
            if (var5 instanceof PlayerEntity) {
               PlayerEntity player = (PlayerEntity)var5;
               if (Dexum.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) {
                  Dexum.getInstance().getFriendManager().removeFriend(player.getGameProfile().getName());
               } else {
                  Dexum.getInstance().getFriendManager().add(player.getGameProfile().getName());
               }
            }
         }
      }

   }
}