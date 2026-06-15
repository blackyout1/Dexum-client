package wtf.dexum.base.autobuy.item;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.ComponentChanges.Builder;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class SkinItemBuy extends ItemBuy {
   private final String skin;

   public SkinItemBuy(String name, ItemBuy.Category category, String skin) {
      this(skin, name, name, category);
   }

   public SkinItemBuy(String skin, String displayName, String searchName, ItemBuy.Category maxSumBuy) {
      super(Items.PLAYER_HEAD.getDefaultStack(), displayName, searchName, maxSumBuy);
      this.skin = skin;
      Builder datacomponentpatch$builder = ComponentChanges.builder();
      Optional<String> emptyString = Optional.of("");
      Optional<UUID> emptyUUID = Optional.of(UUID.randomUUID());
      PropertyMap propertyMap = new PropertyMap();
      propertyMap.put("textures", new Property("textures", skin));
      ProfileComponent resolvableProfile = new ProfileComponent(emptyString, emptyUUID, propertyMap);
      datacomponentpatch$builder.add(DataComponentTypes.PROFILE, resolvableProfile);
      ItemStackArgument input = new ItemStackArgument(this.itemStack.getRegistryEntry(), datacomponentpatch$builder.build());

      try {
         this.itemStack = input.createStack(1, false);
      } catch (CommandSyntaxException var12) {
      }

   }

   public boolean isBuy(ItemStack stack) {
      if (!super.isBuy(stack)) {
         return false;
      } else {
         NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
         return customData != null && customData.getNbt().contains("SkullOwner") ? customData.getNbt().get("SkullOwner").toString().contains(this.skin) : false;
      }
   }

   public String getSkin() {
      return this.skin;
   }
}