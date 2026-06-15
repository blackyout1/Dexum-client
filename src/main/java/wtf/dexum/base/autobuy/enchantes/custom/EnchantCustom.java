package wtf.dexum.base.autobuy.enchantes.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import wtf.dexum.base.autobuy.enchantes.Enchant;

public class EnchantCustom extends Enchant {
   public EnchantCustom(String name, String checked, int minLevel) {
      super(name, checked, minLevel);
   }

   public boolean isEnchanted(ItemStack stack) {
      NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData != null && customData.getNbt().contains("custom-enchantments", 9)) {
         NbtList customEnchants = customData.getNbt().getList("custom-enchantments", 10);

         for(int i = 0; i < customEnchants.size(); ++i) {
            NbtCompound ench = customEnchants.getCompound(i);
            String type = ench.getString("type");
            int level = ench.getInt("level");
            if (type.equals(this.checked)) {
               return level >= this.minLevel;
            }
         }
      }

      return false;
   }
}