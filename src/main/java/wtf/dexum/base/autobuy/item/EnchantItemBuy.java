package wtf.dexum.base.autobuy.item;

import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.item.ItemStack;
import wtf.dexum.base.autobuy.enchantes.Enchant;

public class EnchantItemBuy extends ItemBuy {
   protected final ArrayList<Enchant> enchants = new ArrayList();

   public EnchantItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category maxSumBuy) {
      super(itemStack, displayName, searchName, maxSumBuy);
   }

   public EnchantItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category maxSumBuy) {
      super(itemStack, searchName, maxSumBuy);
   }

   public boolean isBuy(ItemStack stack) {
      if (!super.isBuy(stack)) {
         return false;
      } else {
         Iterator var2 = this.enchants.iterator();

         Enchant enchant;
         do {
            if (!var2.hasNext()) {
               return true;
            }

            enchant = (Enchant)var2.next();
         } while(enchant.isEnchanted(stack));

         return false;
      }
   }

   public void addEnchant(Enchant enchant) {
      this.enchants.add(enchant);
   }

   public ArrayList<Enchant> getEnchants() {
      return this.enchants;
   }
}