package wtf.dexum.base.autobuy.enchantes;

import lombok.Generated;
import net.minecraft.item.ItemStack;

public abstract class Enchant {
   protected final String name;
   protected final String checked;
   protected int minLevel;

   public abstract boolean isEnchanted(ItemStack var1);

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + " [checked=" + this.checked + ", level=" + this.minLevel + "]";
   }

   @Generated
   public Enchant(String name, String checked, int minLevel) {
      this.name = name;
      this.checked = checked;
      this.minLevel = minLevel;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getChecked() {
      return this.checked;
   }

   @Generated
   public int getMinLevel() {
      return this.minLevel;
   }

   @Generated
   public void setMinLevel(int minLevel) {
      this.minLevel = minLevel;
   }
}