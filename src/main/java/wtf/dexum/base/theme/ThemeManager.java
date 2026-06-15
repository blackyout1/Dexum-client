package wtf.dexum.base.theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;

public class ThemeManager {
   private Theme currentTheme;
   private final List<Theme> themes = new ArrayList();
   private final Theme defaultTheme = new Theme("Cyber Blue", (new Color(0, 200, 255, 255)).getRGB(), (new Color(10, 10, 20, 255)).getRGB());

   public ThemeManager() {
      this.initThemes();
   }

   @Native
   private void initThemes() {
      if (this.currentTheme == null) {
         this.currentTheme = this.defaultTheme;
      }

      this.themes.addAll(List.of(new Theme[]{
         this.defaultTheme,
         new Theme("Olive", (new Color(156, 151, 85, 255)).getRGB(), (new Color(40, 40, 30, 255)).getRGB()),
         new Theme("Violet Void", (new Color(180, 0, 255, 255)).getRGB(), (new Color(30, 0, 40, 255)).getRGB()),
         new Theme("Sunset", (new Color(255, 94, 0, 255)).getRGB(), (new Color(40, 20, 0, 255)).getRGB()),
         new Theme("Neon Green", (new Color(57, 255, 20, 255)).getRGB(), (new Color(10, 20, 10, 255)).getRGB()),
         new Theme("Abyss Blue", (new Color(0, 102, 204, 255)).getRGB(), (new Color(10, 10, 30, 255)).getRGB()),
         new Theme("Sakura", (new Color(255, 183, 197, 255)).getRGB(), (new Color(255, 255, 255, 255)).getRGB()),
         new Theme("Cyber Dragon", (new Color(220, 20, 60, 255)).getRGB(), (new Color(255, 215, 0, 255)).getRGB()),
         new Theme("Aurora", (new Color(0, 255, 255, 255)).getRGB(), (new Color(147, 112, 219, 255)).getRGB()),
         new Theme("Matrix", (new Color(0, 255, 0, 255)).getRGB(), (new Color(0, 0, 0, 255)).getRGB()),
         new Theme("Hellfire", (new Color(255, 0, 0, 255)).getRGB(), (new Color(255, 140, 0, 255)).getRGB()),
         new Theme("Frozen Soul", (new Color(173, 216, 230, 255)).getRGB(), (new Color(0, 0, 139, 255)).getRGB()),
         new Theme("Gold Dust", (new Color(255, 215, 0, 255)).getRGB(), (new Color(0, 0, 0, 255)).getRGB()),
         new Theme("Neon Wave", (new Color(255, 20, 147, 255)).getRGB(), (new Color(138, 43, 226, 255)).getRGB()),
         new Theme("Desert Rose", (new Color(237, 201, 175, 255)).getRGB(), (new Color(255, 105, 180, 255)).getRGB()),
         new Theme("Ghost", (new Color(169, 169, 169, 255)).getRGB(), (new Color(245, 245, 245, 255)).getRGB()),
         new Theme("Ruby Red", (new Color(224, 17, 95, 255)).getRGB(), (new Color(30, 0, 10, 255)).getRGB()),
         new Theme("Oceanic", (new Color(0, 119, 190, 255)).getRGB(), (new Color(0, 20, 40, 255)).getRGB()),
         new Theme("Forest", (new Color(34, 139, 34, 255)).getRGB(), (new Color(0, 20, 0, 255)).getRGB()),
         new Theme("Cyberpunk", (new Color(255, 0, 255, 255)).getRGB(), (new Color(0, 255, 255, 255)).getRGB()),
         new Theme("Solar", (new Color(255, 140, 0, 255)).getRGB(), (new Color(255, 69, 0, 255)).getRGB()),
         new Theme("Deep Sea", (new Color(0, 0, 128, 255)).getRGB(), (new Color(0, 128, 255, 255)).getRGB()),
         new Theme("Autumn", (new Color(139, 69, 19, 255)).getRGB(), (new Color(255, 165, 0, 255)).getRGB()),
         new Theme("Polar", (new Color(240, 248, 255, 255)).getRGB(), (new Color(176, 196, 222, 255)).getRGB()),
         new Theme("Volcanic", (new Color(255, 69, 0, 255)).getRGB(), (new Color(0, 0, 0, 255)).getRGB()),
         new Theme("Royal", (new Color(123, 104, 238, 255)).getRGB(), (new Color(255, 215, 0, 255)).getRGB()),
         new Theme("Pastel Pink", (new Color(255, 209, 220, 255)).getRGB(), (new Color(255, 255, 255, 255)).getRGB()),
         new Theme("Grape", (new Color(111, 45, 189, 255)).getRGB(), (new Color(157, 78, 221, 255)).getRGB()),
         new Theme("Steel", (new Color(112, 128, 144, 255)).getRGB(), (new Color(192, 192, 192, 255)).getRGB()),
         new Theme("Ocean", (new Color(2, 62, 138, 255)).getRGB(), (new Color(0, 119, 182, 255)).getRGB()),
         new Theme("Fire", (new Color(230, 57, 70, 255)).getRGB(), (new Color(241, 250, 238, 255)).getRGB()),
         new Theme("Night", (new Color(20, 33, 61, 255)).getRGB(), (new Color(252, 163, 17, 255)).getRGB()),
         new Theme("Rose Gold", (new Color(183, 110, 121, 255)).getRGB(), (new Color(255, 228, 225, 255)).getRGB()),
         new Theme("Deep Purple", (new Color(48, 25, 52, 255)).getRGB(), (new Color(138, 43, 226, 255)).getRGB()),
         new Theme("Toxic", (new Color(173, 255, 47, 255)).getRGB(), (new Color(0, 100, 0, 255)).getRGB()),
         new Theme("Ice", (new Color(240, 255, 255, 255)).getRGB(), (new Color(0, 255, 255, 255)).getRGB()),
         new Theme("Electric", (new Color(125, 249, 255, 255)).getRGB(), (new Color(0, 0, 255, 255)).getRGB()),
         new Theme("Candy", (new Color(255, 105, 180, 255)).getRGB(), (new Color(255, 192, 203, 255)).getRGB()),
         new Theme("Starlight", (new Color(25, 25, 112, 255)).getRGB(), (new Color(192, 192, 192, 255)).getRGB()),
         new Theme("Emerald Dream", (new Color(0, 100, 0, 255)).getRGB(), (new Color(50, 205, 50, 255)).getRGB()),
         new Theme("Synthwave", (new Color(255, 0, 127, 255)).getRGB(), (new Color(0, 255, 255, 255)).getRGB()),
         new Theme("Bloodlust", (new Color(139, 0, 0, 255)).getRGB(), (new Color(255, 0, 0, 255)).getRGB()),
         new Theme("Thunder", (new Color(47, 79, 79, 255)).getRGB(), (new Color(255, 215, 0, 255)).getRGB()),
         new Theme("Mocha", (new Color(62, 39, 35, 255)).getRGB(), (new Color(215, 204, 200, 255)).getRGB()),
         new Theme("Amethyst", (new Color(75, 0, 130, 255)).getRGB(), (new Color(153, 102, 204, 255)).getRGB()),
         new Theme("Arctic", (new Color(0, 255, 255, 255)).getRGB(), (new Color(240, 255, 255, 255)).getRGB()),
         new Theme("Shadow", (new Color(20, 20, 20, 255)).getRGB(), (new Color(105, 105, 105, 255)).getRGB()),
         new Theme("Vampire", (new Color(40, 0, 0, 255)).getRGB(), (new Color(128, 0, 128, 255)).getRGB())
      }));
   }

   public ColorRGBA getClientColor(int index) {
      return this.currentTheme == null ? new ColorRGBA(255, 255, 255, 255) : ColorUtil.gradient(3, index, this.currentTheme.getColor(), this.currentTheme.getSecondColor());
   }

   @Generated
   public Theme getCurrentTheme() {
      return this.currentTheme;
   }

   @Generated
   public List<Theme> getThemes() {
      return this.themes;
   }

   @Generated
   public Theme getDefaultTheme() {
      return this.defaultTheme;
   }

   @Generated
   public void setCurrentTheme(Theme currentTheme) {
      this.currentTheme = currentTheme;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ThemeManager)) {
         return false;
      } else {
         ThemeManager other = (ThemeManager)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label47: {
               Object this$currentTheme = this.getCurrentTheme();
               Object other$currentTheme = other.getCurrentTheme();
               if (this$currentTheme == null) {
                  if (other$currentTheme == null) {
                     break label47;
                  }
               } else if (this$currentTheme.equals(other$currentTheme)) {
                  break label47;
               }

               return false;
            }

            Object this$themes = this.getThemes();
            Object other$themes = other.getThemes();
            if (this$themes == null) {
               if (other$themes != null) {
                  return false;
               }
            } else if (!this$themes.equals(other$themes)) {
               return false;
            }

            Object this$defaultTheme = this.getDefaultTheme();
            Object other$defaultTheme = other.getDefaultTheme();
            if (this$defaultTheme == null) {
               if (other$defaultTheme != null) {
                  return false;
               }
            } else if (!this$defaultTheme.equals(other$defaultTheme)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof ThemeManager;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1;
      Object $currentTheme = this.getCurrentTheme();
      result = result * 59 + ($currentTheme == null ? 43 : $currentTheme.hashCode());
      Object $themes = this.getThemes();
      result = result * 59 + ($themes == null ? 43 : $themes.hashCode());
      Object $defaultTheme = this.getDefaultTheme();
      result = result * 59 + ($defaultTheme == null ? 43 : $defaultTheme.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getCurrentTheme());
      return "ThemeManager(currentTheme=" + var10000 + ", themes=" + String.valueOf(this.getThemes()) + ", defaultTheme=" + String.valueOf(this.getDefaultTheme()) + ")";
   }
}