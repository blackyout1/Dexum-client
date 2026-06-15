package wtf.dexum.client.modules.api.setting.impl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import wtf.dexum.client.modules.api.setting.Setting;

public class ItemSelectSetting extends Setting {
   private List<String> itemsById;
   private static final Gson gson = new Gson();

   public void setItemsById(List<String> itemsById) {
      this.itemsById = itemsById;
   }

   public ItemSelectSetting(String name, List<String> itemsById) {
      this(name, itemsById, () -> {
         return true;
      });
   }

   public ItemSelectSetting(String name, List<String> itemsById, Supplier<Boolean> visible) {
      super(name);
      this.itemsById = itemsById;
   }

   public List<String> getItemsById() {
      return this.itemsById;
   }

   public void add(String s) {
      this.itemsById.add(s);
   }

   public void remove(String s) {
      this.itemsById.remove(s);
   }

   public boolean contains(String s) {
      return this.itemsById.contains(s);
   }

   public void add(Block b) {
      this.add(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public void add(Item i) {
      this.add(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public void remove(Block b) {
      this.remove(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public void remove(Item i) {
      this.remove(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public boolean contains(Block b) {
      return this.contains(b.getTranslationKey().replace("block.minecraft.", ""));
   }

   public boolean contains(Item i) {
      return this.contains(i.getTranslationKey().replace("item.minecraft.", ""));
   }

   public void clear() {
      this.itemsById.clear();
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.add(String.valueOf(this.name), gson.toJsonTree(this.getItemsById()));
   }

   public void load(JsonObject propertiesObject) {
      Type listType = (new TypeToken<List<String>>() {
      }).getType();
      JsonElement jsonElement = propertiesObject.get(String.valueOf(this.name));
      if (jsonElement != null && jsonElement.isJsonArray()) {
         List<String> list = (List)gson.fromJson(jsonElement, listType);
         this.setItemsById(list);
      }

   }
}