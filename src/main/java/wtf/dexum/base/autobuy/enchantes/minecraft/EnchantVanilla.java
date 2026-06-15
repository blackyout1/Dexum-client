package wtf.dexum.base.autobuy.enchantes.minecraft;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Iterator;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import wtf.dexum.base.autobuy.enchantes.Enchant;

public class EnchantVanilla extends Enchant {
   public EnchantVanilla(String name, String checked, int minLevel) {
      super(name, checked, minLevel);
   }

   public boolean isEnchanted(ItemStack stack) {
      if (this.minLevel <= 0) {
         return true;
      } else {
         ItemEnchantmentsComponent enchants = stack.getEnchantments();
         Iterator var3 = enchants.getEnchantmentEntries().iterator();

         Entry entry;
         String id;
         do {
            if (!var3.hasNext()) {
               return false;
            }

            entry = (Entry)var3.next();
            id = ((RegistryEntry)entry.getKey()).getKey().toString();
         } while(!id.contains(this.checked));

         return entry.getIntValue() >= this.minLevel;
      }
   }
}