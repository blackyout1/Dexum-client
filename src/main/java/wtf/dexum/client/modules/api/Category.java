package wtf.dexum.client.modules.api;

import lombok.Generated;

public enum Category {
   COMBAT("Combat", "b"),
   MOVEMENT("Movement", "c"),
   RENDER("Render", "m"),
   PLAYER("Player", "e"),
   MISC("Misc", "q"),
   THEMES("Themes", "G");

   private final String name;
   private final String icon;

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   private static Category[] $values() {
      return new Category[]{COMBAT, MOVEMENT, RENDER, PLAYER, MISC, THEMES};
   }
}