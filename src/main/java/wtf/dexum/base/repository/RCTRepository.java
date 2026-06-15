package wtf.dexum.base.repository;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.base.notify.NotifyManager;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;
import wtf.dexum.utility.game.server.ServerHandler;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.math.StopWatch;

public class RCTRepository implements IClient {
   private final StopWatch stopWatch = new StopWatch();
   private boolean lobby;
   private int anarchy;

   public RCTRepository() {
      try {
         EventManager.register(this);
      } catch (Exception e) {

      }
   }

   @EventTarget
   @Native
   public void onPacket(EventPacket e) {
      if (this.anarchy != 0) {
         Packet var3 = e.getPacket();
         if (var3 instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket message = (GameMessageS2CPacket)var3;
            if (e.isReceive()) {
               String text = message.content().getString().toLowerCase();
               if (!text.contains("хаб") && text.contains("не удалось")) {
                  NotifyManager var10000 = NotifyManager.getInstance();
                  String var10002 = String.valueOf(Formatting.RED);
                  var10000.addNotification("[RCT]️", Text.literal(" На данную анархию " + var10002 + "нельзя" + String.valueOf(Formatting.RESET) + " зайти"));
                  this.anarchy = 0;
               }
            }
         }
      }

   }

   @EventTarget
   @Native
   public void onTick(EventUpdate e) {
      if (this.anarchy != 0) {
         ServerHandler serverHandler = Dexum.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            this.anarchy = 0;
         } else {
            int currentAnarchy = serverHandler.getAnarchy();
            if (this.lobby) {
               if (currentAnarchy == -1) {
                  this.lobby = false;
               } else {
                  mc.player.networkHandler.sendChatCommand("hub");
               }

            } else if (currentAnarchy == this.anarchy) {
               this.anarchy = 0;
            } else {
               Screen var5 = mc.currentScreen;
               if (var5 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)var5;
                  if (screen.getTitle().getString().equals("Выбор Лайт анархии:")) {
                     boolean secondScreen = ((GenericContainerScreenHandler)screen.getScreenHandler()).getInventory().size() < 10;
                     int[] slots = this.anarchy < 15 ? new int[]{0, 0} : (this.anarchy < 33 ? new int[]{1, 14} : (this.anarchy < 48 ? new int[]{2, 32} : new int[]{3, 47}));
                     if (secondScreen) {
                        PlayerInventoryUtil.clickSlot(slots[0], 0, SlotActionType.PICKUP, false);
                     } else {
                        PlayerInventoryUtil.clickSlot(17 + this.anarchy - slots[1], 0, SlotActionType.PICKUP, false);
                     }

                     return;
                  }
               }

               if (this.stopWatch.every(500L)) {
                  mc.player.networkHandler.sendChatCommand("lite");
               }

            }
         }
      }
   }

   @Native
   public void reconnect(int anarchy) {
      if (anarchy > 0 && anarchy < 64) {
         this.anarchy = anarchy;
         this.lobby = true;
      } else {
         NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не верный " + String.valueOf(Formatting.RED) + "лайт"));
      }

   }
}