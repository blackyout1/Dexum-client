package wtf.dexum.utility.game.server;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import wtf.dexum.base.autobuy.enchantes.Enchant;
import wtf.dexum.base.autobuy.enchantes.custom.EnchantCustom;
import wtf.dexum.base.autobuy.enchantes.minecraft.EnchantVanilla;
import wtf.dexum.utility.mixin.accessors.ItemStackAccessor;

public final class AutoBuyUtil {
   private static Pattern patternFuntime = Pattern.compile("\\$\\s*([0-9][\\d,]*)");
   private static Pattern patternHollyWorld = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");
   private static final Map<ItemStack, NbtCompound> nbtCompoundMap = new HashMap();
   public static List<String> testBypass = new ArrayList();

   public static int getPrice(String nbt) {
      Matcher matcher = patternFuntime.matcher(nbt);
      String amount;
      if (matcher.find()) {
         amount = matcher.group(1);
         String price = amount.replace(",", "");
         return Integer.parseInt(price);
      } else {
         matcher = patternHollyWorld.matcher(nbt);
         if (matcher.find()) {
            amount = matcher.group(1).replaceAll(" ", "");
            return Integer.parseInt(amount);
         } else {
            return Integer.MAX_VALUE;
         }
      }
   }

   public static boolean checkDon(ItemStack itemStack) {
      return itemStack.getCustomName().getString().contains("★");
   }

   public static int getPrice(ItemStack itemStack) {
      String nbt = getNBT(itemStack);
      return getPrice(nbt);
   }

   public static String getNBT(ItemStack itemStack) {
      return getTag(itemStack).toString();
   }

   public static String getKey(ItemStack itemStack) {

      NbtComponent customData = (NbtComponent)itemStack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData != null) {

         if (customData.getNbt().contains("kringeItems")) {
            NbtElement customEnchants = customData.getNbt().get("kringeItems");
            MinecraftClient.getInstance().keyboard.setClipboard(customEnchants.toString());
            return customEnchants.toString();
         }
      }

      return "";
   }

   public static NbtCompound getTag(ItemStack stack) {
      MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
      ComponentChanges changes = components.getChanges();
      World world = MinecraftClient.getInstance().world;
      return world == null ? new NbtCompound() : (NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
         return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
      });
   }

   public static String getTagFuntimeNotTempElements(ItemStack stack) {
      MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
      ComponentChanges changes = components.getChanges();
      World world = MinecraftClient.getInstance().world;
      return world == null ? "" : ((NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
         return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
      })).toString().replaceAll(",?\\s*PublicBukkitValues:\\{[^}]*\\}", "").replaceAll("'\\{[^']*Истeкaeт:[^']*\\}',?", "").replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "").replaceAll("minecraft:[0-9a-f\\-]{36}", "minecraft:UUID");
   }

   public static ArrayList<Enchant> getEnchants(ItemStack stack) {
      ArrayList<Enchant> enchantsBuy = new ArrayList();
      NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      String type;
      if (customData != null && customData.getNbt().contains("Enchantments", 9)) {
         NbtList customEnchants = customData.getNbt().getList("Enchantments", 10);

         for(int i = 0; i < customEnchants.size(); ++i) {
            NbtCompound ench = customEnchants.getCompound(i);
            type = ench.getString("id");
            int level = ench.getInt("lvl");
            enchantsBuy.add(new EnchantCustom(type, type, level));
         }
      }

      ItemEnchantmentsComponent enchants = stack.getEnchantments();
      Iterator var9 = enchants.getEnchantmentEntries().iterator();

      while(var9.hasNext()) {
         Entry<RegistryEntry<Enchantment>> entry = (Entry)var9.next();
         type = ((RegistryKey)((RegistryEntry)entry.getKey()).getKey().get()).getValue().toString();
         enchantsBuy.add(new EnchantVanilla(type, type, entry.getIntValue()));
      }

      return enchantsBuy;
   }

   public static boolean isAuction(ScreenHandler handledScreen) {
      return handledScreen.slots.size() == 90 && handledScreen.getSlot(49).getStack().getItem() == Items.NETHER_STAR;
   }

   public static boolean isWaitBuy(ScreenHandler handledScreen) {
      return handledScreen.slots.size() == 63 && handledScreen.getSlot(0).getStack().getItem() == Items.LIME_STAINED_GLASS_PANE;
   }

   public static void test(int slotId) {
   }

   @Generated
   private AutoBuyUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}