package wtf.dexum.base.notify;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.other.EventModuleToggle;
import wtf.dexum.base.events.impl.player.EventPickupItem;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.hud.elements.component.NotifyComponent;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.utility.game.other.NetworkUtils;

public class NotifyManager {
   private static NotifyManager instance;
   private final List<NotifyComponent> notifyComponents = new ArrayList<>();
   private boolean notifedHelmet;
   private boolean notifedChestplate;
   private boolean notifedLeggings;
   private boolean notifedBoots;

   public NotifyManager() {
      EventManager.register(this);
   }

   public static NotifyManager getInstance() {
      if (instance == null) {
         instance = new NotifyManager();
      }

      return instance;
   }

   public void setNotifyComponent(NotifyComponent component) {
      this.notifyComponents.add(component);
   }

   @EventTarget
   @Native
   public void onModuleToggle(EventModuleToggle event) {
      for (NotifyComponent notifyComponent : this.notifyComponents) {
         notifyComponent.addNotification(event.getModule(), event.isEnabled());
      }
   }

   public void addNotification(Module module, boolean enabled) {
      for (NotifyComponent notifyComponent : this.notifyComponents) {
         notifyComponent.addNotification(module, enabled);
      }
   }

   public void addNotification(String title, Text text) {
      for (NotifyComponent notifyComponent : this.notifyComponents) {
         notifyComponent.addTextNotification("\uf05a", Text.literal(title + " " + text.getString()));
      }
   }
}