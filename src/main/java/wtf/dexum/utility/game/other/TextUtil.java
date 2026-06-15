package wtf.dexum.utility.game.other;

import java.util.Locale;
import lombok.Generated;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class TextUtil implements IMinecraft {
   public static String formatNumber(double number) {
      return String.format(Locale.US, "%.1f", number);
   }

   @Native
   public static Text truncateAfterSecondSpace(Text input, boolean addEllipsis) {
      OrderedText ordered = input.asOrderedText();
      MutableText out = Text.empty();
      StringBuilder run = new StringBuilder();
      Style[] current = new Style[]{Style.EMPTY};
      int[] spaceCount = new int[]{0};
      boolean[] truncated = new boolean[]{false};
      ordered.accept((index, style, codePoint) -> {
         if (spaceCount[0] > 2) {
            truncated[0] = true;
            return false;
         } else {
            if (!style.equals(current[0])) {
               if (!run.isEmpty()) {
                  out.append(Text.literal(run.toString()).setStyle(current[0]));
                  run.setLength(0);
               }

               current[0] = style;
            }

            run.appendCodePoint(codePoint);
            if (Character.isWhitespace(codePoint)) {
               int var10002 = spaceCount[0]++;
            }

            return true;
         }
      });
      if (run.length() > 0) {
         out.append(Text.literal(run.toString()).setStyle(current[0]));
      }

      if (addEllipsis && truncated[0]) {
         Style tailStyle = current[0] != null ? current[0] : input.getStyle();
         out.append(Text.literal("…").setStyle(tailStyle));
      }

      return out;
   }

   public static MutableText replaceLastChar(Text source, String replacement) {
      OrderedText ordered = source.asOrderedText();
      int[] length = new int[]{0};
      ordered.accept((index, style, codePoint) -> {
         int var10002 = length[0]++;
         return true;
      });
      if (length[0] == 0) {
         return source.copy();
      } else {
         int lastIdx = length[0] - 1;
         MutableText out = Text.empty();
         StringBuilder run = new StringBuilder();
         Style[] currentStyle = new Style[]{null};
         int[] pos = new int[]{0};
         ordered.accept((index, style, codePoint) -> {
            boolean isLast = pos[0] == lastIdx;
            if (currentStyle[0] == null || !currentStyle[0].equals(style)) {
               flushRun(out, run, currentStyle[0]);
               currentStyle[0] = style;
            }

            if (isLast) {
            }

            run.appendCodePoint(codePoint);
            int var10002 = pos[0]++;
            return true;
         });
         flushRun(out, run, currentStyle[0]);
         return out;
      }
   }

   @Native
   private static void flushRun(MutableText out, StringBuilder run, Style style) {
      if (run.length() != 0) {
         MutableText chunk = Text.literal(run.toString());
         if (style != null) {
            chunk.setStyle(style);
         }

         out.append(chunk);
         run.setLength(0);
      }
   }

   @Native
   public static Text truncateAfterSubstring(Text input, String argStr, boolean addEllipsis) {
      if (argStr != null && !argStr.isEmpty()) {
         OrderedText ordered = input.asOrderedText();
         MutableText out = Text.empty();
         StringBuilder buffer = new StringBuilder();
         Style[] currentStyle = new Style[]{Style.EMPTY};
         StringBuilder collected = new StringBuilder();
         boolean[] truncated = new boolean[]{false};
         ordered.accept((index, style, codePoint) -> {
            String cpStr = new String(Character.toChars(codePoint));
            if (collected.toString().contains(argStr)) {
               truncated[0] = true;
               return false;
            } else {
               collected.append(cpStr);
               if (!style.equals(currentStyle[0])) {
                  if (buffer.length() > 0) {
                     out.append(Text.literal(buffer.toString()).setStyle(currentStyle[0]));
                     buffer.setLength(0);
                  }

                  currentStyle[0] = style;
               }

               buffer.append(cpStr);
               return true;
            }
         });
         if (buffer.length() > 0) {
            out.append(Text.literal(buffer.toString()).setStyle(currentStyle[0]));
         }

         if (addEllipsis && truncated[0]) {
            Style tailStyle = currentStyle[0] != null ? currentStyle[0] : input.getStyle();
            out.append(Text.literal("…").setStyle(tailStyle));
         }

         return out;
      } else {
         return input;
      }
   }

   @Generated
   private TextUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}