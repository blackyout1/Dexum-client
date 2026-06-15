package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.math.Timer;

@ModuleAnnotation(
   name = "AntiBot",
   category = Category.COMBAT,
   description = "Убирает бота вызванного от античита"
)
public final class AntiBot extends Module {
   public static final AntiBot INSTANCE = new AntiBot();
   private final List<PlayerEntity> bots = new ArrayList();
   private final Timer timer = new Timer();

   private AntiBot() {
   }

   @EventTarget
   @Native
   public void onTick(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         if (this.timer.finished(10000L) && !this.bots.isEmpty()) {
            this.bots.clear();
            this.timer.reset();
         }

         Iterator var2 = mc.world.getPlayers().iterator();

         while(var2.hasNext()) {
            PlayerEntity player = (PlayerEntity)var2.next();
            if (player != null && player != mc.player && this.armorCheck(player) && !this.bots.contains(player)) {
               this.bots.add(player);
            }
         }

      }
   }

   private boolean armorCheck(PlayerEntity entity) {
      return this.getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && this.isNotColored(entity, 3) && !this.getArmor(entity, 3).hasEnchantments() || this.getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && this.isNotColored(entity, 2) && !this.getArmor(entity, 2).hasEnchantments() || this.getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && this.isNotColored(entity, 1) && !this.getArmor(entity, 1).hasEnchantments() || this.getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && this.isNotColored(entity, 0) && !this.getArmor(entity, 0).hasEnchantments() || this.getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !this.getArmor(entity, 2).hasEnchantments() || this.getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !this.getArmor(entity, 1).hasEnchantments();
   }

   private ItemStack getArmor(PlayerEntity entity, int slot) {
      return entity.getInventory().getArmorStack(slot);
   }

   private boolean isNotColored(PlayerEntity entity, int slot) {
      return !this.getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
   }

   public void onEnable() {
      super.onEnable();
      if (!this.bots.isEmpty()) {
         this.bots.clear();
      }

   }

   public void onDisable() {
      super.onDisable();
      if (!this.bots.isEmpty()) {
         this.bots.clear();
      }

   }

   public boolean isBot(PlayerEntity player) {
      return this.bots.contains(player);
   }
}