package wtf.dexum.utility.game.other;

import java.awt.Color;
import lombok.Generated;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;

public final class MessageUtil implements IMinecraft {
   @Native
   public static void displayMessage(MessageUtil.LogLevel level, Object message) {
      Text icon = createGradientText("dexum", Dexum.getInstance().getThemeManager().getCurrentTheme().getColor(), Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor());
      Text styledMessage = Text.of(String.valueOf(message)).copy().getWithStyle(Style.EMPTY).isEmpty() ? Text.of("?") : (Text)Text.of(String.valueOf(message)).copy().getWithStyle(Style.EMPTY).getFirst();
      mc.player.sendMessage(icon.copy().append(styledMessage), false);
   }

   @Native
   private static Text createGradientText(Object text, ColorRGBA color1, ColorRGBA color2) {
      Text result = Text.empty();
      int length = String.valueOf(text).length();

      for(int i = 0; i < length; ++i) {
         float progress = length > 1 ? (float)i / (float)(length - 1) : 0.0F;
         ColorRGBA color = ColorUtil.interpolate(color1, color2, progress);
         Text charText = (Text)Text.of(String.valueOf(String.valueOf(text).charAt(i))).copy().getWithStyle(Style.EMPTY.withColor(color.getRGB()).withBold(true)).getFirst();
         result = result.copy().append(charText);
      }

      return result.copy().append((Text)Text.of(" -> ").getWithStyle(Style.EMPTY.withColor(Formatting.GRAY).withBold(false)).getFirst());
   }

   public static void displayWarning(Object message) {
      displayMessage(MessageUtil.LogLevel.WARN, message);
   }

   public static void displayError(Object message) {
      displayMessage(MessageUtil.LogLevel.ERROR, message);
   }

   public static void displayInfo(Object message) {
      displayMessage(MessageUtil.LogLevel.INFO, message);
   }

   private static Style getLevelStyle(MessageUtil.LogLevel level) {
      return Style.EMPTY.withColor(level.getColor().getRGB());
   }

   @Generated
   private MessageUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static enum LogLevel {
      WARN("Warning", new Color(247, 206, 59)),
      ERROR("Error", new Color(242, 79, 68)),
      INFO("Info", new Color(87, 126, 255));

      private final String level;
      private final Color color;

      @Generated
      public String getLevel() {
         return this.level;
      }

      @Generated
      public Color getColor() {
         return this.color;
      }

      @Generated
      private LogLevel(final String level, final Color color) {
         this.level = level;
         this.color = color;
      }

      private static MessageUtil.LogLevel[] $values() {
         return new MessageUtil.LogLevel[]{WARN, ERROR, INFO};
      }
   }
}