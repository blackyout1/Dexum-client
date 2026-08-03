package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Comparator;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.game.player.PlayerInventoryComponent;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
   name = "AutoSwap",
   category = Category.COMBAT,
   description = "Свап предметов по бинду"
)
public final class AutoSwap extends Module {
   public static final AutoSwap INSTANCE = new AutoSwap();
   private final ModeSetting itemType = new ModeSetting("Предмет", new String[]{"Щит", "Геплы", "Тотем", "Шар"});
   private final ModeSetting swapType = new ModeSetting("Свапать на", new String[]{"Щит", "Геплы", "Тотем", "Шар"});
   private final KeySetting keyToSwap = new KeySetting("Кнопка свапа", -1);
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Simple", "Multi (2)", "Multi (3)"});
   private boolean swap;
   private int swapStep = 0;

   @EventTarget
   public void onKey(EventKey event) {
      if (mc.currentScreen == null) {
         if (event.getAction() == 1) {
            if (event.is(this.keyToSwap.getKeyCode())) {
               this.swap = true;
               this.swapStep = 0;
            }
         }
      }
   }

   @EventTarget
   @Native
   public void onTick(EventUpdate event) {
      if (this.swap) {
         int maxSteps = this.mode.get().equals("Multi (2)") ? 2 : (this.mode.get().equals("Multi (3)") ? 3 : 1);
         
         for (int step = this.swapStep; step < maxSteps && step < 3; step++) {
            Item targetItem = this.getItemByStep(step);
            Slot slot = PlayerInventoryUtil.getSlot(targetItem, Comparator.comparing((s) -> {
               return s.getStack().hasEnchantments();
            }), (s) -> {
               return s.id != 46 && s.id != 45 && s.id != 5 && s.id != 6 && s.id != 7 && s.id != 8;
            });
            
            if (slot != null) {
               PlayerInventoryComponent.addTask(() -> {
                  if (mc.player.isSprinting()) {
                     mc.player.setSprinting(false);
                     mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                     if (!AutoSprint.INSTANCE.isEnabled()) {
                        mc.options.sprintKey.setPressed(false);
                     }
                  }
                  PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, false);
               });
            }
         }
         
         this.swapStep = maxSteps;
         if (this.swapStep >= maxSteps) {
            this.swap = false;
            this.swapStep = 0;
         }
      }
   }

   private Item getItemByStep(int step) {
      switch (step) {
         case 0: return this.getItemByType(this.itemType.get());
         case 1: return this.getItemByType(this.swapType.get());
         case 2: return this.getItemByType(this.itemType.get());
         default: return Items.AIR;
      }
   }

   @Native
   private Item getItemByType(String itemType) {
      byte var3 = -1;
      switch(itemType.hashCode()) {
      case 1056824:
         if (itemType.equals("Шар")) {
            var3 = 3;
         }
         break;
      case 1058035:
         if (itemType.equals("Щит")) {
            var3 = 0;
         }
         break;
      case 996396589:
         if (itemType.equals("Геплы")) {
            var3 = 2;
         }
         break;
      case 1010520205:
         if (itemType.equals("Тотем")) {
            var3 = 1;
         }
      }

      Item var10000;
      switch(var3) {
      case 0:
         var10000 = Items.SHIELD;
         break;
      case 1:
         var10000 = Items.TOTEM_OF_UNDYING;
         break;
      case 2:
         var10000 = Items.GOLDEN_APPLE;
         break;
      case 3:
         var10000 = Items.PLAYER_HEAD;
         break;
      default:
         var10000 = Items.AIR;
      }

      return var10000;
   }
}