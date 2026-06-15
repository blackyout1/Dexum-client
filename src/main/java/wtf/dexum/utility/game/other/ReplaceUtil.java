package wtf.dexum.utility.game.other;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.PlainTextContent.Literal;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;

public class ReplaceUtil {
   @Native
   public static Text replace(Text input, String target, String replacement) {
      if (input != null && target != null && replacement != null) {
         MutableText result = Text.empty().setStyle(input.getStyle());
         appendReplaced(result, input, target, replacement);
         return result;
      } else {
         return input;
      }
   }

   @Native
   private static void appendReplaced(MutableText result, Text current, String target, String replacement) {
      TextContent content = current.getContent();
      Style style = current.getStyle();
      if (content instanceof Literal) {
         Literal literal = (Literal)content;
         Pattern pattern = Pattern.compile(Pattern.quote(target), 2);
         String replaced = pattern.matcher(literal.string()).replaceAll(replacement);
         result.append(Text.literal(replaced).setStyle(style));
      }

      Iterator var9 = current.getSiblings().iterator();

      while(var9.hasNext()) {
         Text sibling = (Text)var9.next();
         appendReplaced(result, sibling, target, replacement);
      }

   }

   @Native
   public static Text replaceLiteral(Text input, String target, String replacement) {
      if (input == null) {
         return null;
      } else {
         String full = input.getString();
         if (!full.toLowerCase().contains(target.toLowerCase())) {
            return input;
         } else {
            full = full.replaceAll("(?i)" + Pattern.quote(target), replacement);
            MutableText out = Text.empty();
            List<ReplaceUtil.StyledChar> styledChars = flatten(input);
            int index = 0;

            for(int i = 0; i < full.length(); ++i) {
               Style style = index < styledChars.size() ? ((ReplaceUtil.StyledChar)styledChars.get(index)).style : Style.EMPTY;
               out.append(Text.literal(String.valueOf(full.charAt(i))).setStyle(style));
               ++index;
            }

            return out;
         }
      }
   }

   @Native
   private static List<ReplaceUtil.StyledChar> flatten(Text text) {
      List<ReplaceUtil.StyledChar> list = new ArrayList();
      collect(text, list);
      return list;
   }

   private static void collect(Text text, List<ReplaceUtil.StyledChar> list) {
      Style style = text.getStyle();
      TextContent var4 = text.getContent();
      if (var4 instanceof Literal) {
         Literal literal = (Literal)var4;
         String s = literal.string();

         for(int i = 0; i < s.length(); ++i) {
            list.add(new ReplaceUtil.StyledChar(s.charAt(i), style));
         }
      }

      Iterator var6 = text.getSiblings().iterator();

      while(var6.hasNext()) {
         Text sibling = (Text)var6.next();
         collect(sibling, list);
      }

   }

   @Native
   public static String replaceSymbols(String string) {
      String var10000 = string.replaceAll("ꔗ", String.valueOf(Formatting.BLUE) + "MODER").replaceAll("ꔥ", String.valueOf(Formatting.BLUE) + "ST.MODER").replaceAll("ꔡ", String.valueOf(Formatting.LIGHT_PURPLE) + "MODER+").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER").replaceAll("ꔉ", String.valueOf(Formatting.YELLOW) + "HELPER").replaceAll("◆", "@").replaceAll("┃", "|").replaceAll("ꔳ", String.valueOf(Formatting.AQUA) + "ML.ADMIN");
      String var10002 = String.valueOf(Formatting.RED);
      return var10000.replaceAll("ꔅ", var10002 + "Y" + String.valueOf(Formatting.WHITE) + "T").replaceAll("ꔂ", String.valueOf(Formatting.BLUE) + "D.MODER").replaceAll("ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER").replaceAll("ꕄ", String.valueOf(Formatting.RED) + "DRACULA").replaceAll("ꔖ", String.valueOf(Formatting.AQUA) + "OVERLORD").replaceAll("ꕈ", String.valueOf(Formatting.GREEN) + "COBRA").replaceAll("ꔨ", String.valueOf(Formatting.LIGHT_PURPLE) + "DRAGON").replaceAll("ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR").replaceAll("ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER").replaceAll("ꔄ", String.valueOf(Formatting.BLUE) + "HERO").replaceAll("ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER").replaceAll("ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA").replaceAll("ꔶ", String.valueOf(Formatting.GOLD) + "TIGER").replaceAll("ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL").replaceAll("ꕖ", String.valueOf(Formatting.BLACK) + "BUNNY").replaceAll("ꕗꕘ", String.valueOf(Formatting.YELLOW) + "SPONSOR").replaceAll("\ud83d\udd25", "@").replaceAll("ᴀ", "A").replaceAll("ʙ", "B").replaceAll("ᴄ", "C").replaceAll("ᴅ", "D").replaceAll("ᴇ", "E").replaceAll("ғ", "F").replaceAll("ɢ", "G").replaceAll("ʜ", "H").replaceAll("ɪ", "I").replaceAll("ᴊ", "J").replaceAll("ᴋ", "K").replaceAll("ʟ", "L").replaceAll("ᴍ", "M").replaceAll("ɴ", "N").replaceAll("ꜱ", "S").replaceAll("ᴏ", "O").replaceAll("ᴘ", "P").replaceAll("ǫ", "Q").replaceAll("ʀ", "R").replaceAll("ᴛ", "T").replaceAll("ᴜ", "U").replaceAll("ᴠ", "V").replaceAll("ᴡ", "W").replaceAll("ꜰ", "F").replaceAll("ʏ", "Y").replaceAll("ᴢ", "Z");
   }

   public static Text replaceSymbols(Text text) {
      if (text.getString().contains("ꔗ")) {
         text = replace(text, "ꔗ", String.valueOf(Formatting.BLUE) + "MODER");
      }

      if (text.getString().contains("ꔥ")) {
         text = replace(text, "ꔥ", String.valueOf(Formatting.BLUE) + "ST.MODER");
      }

      if (text.getString().contains("ꔡ")) {
         text = replace(text, "ꔡ", String.valueOf(Formatting.LIGHT_PURPLE) + "MODER+");
      }

      if (text.getString().contains("ꔀ")) {
         text = replace(text, "ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER");
      }

      if (text.getString().contains("ꔉ")) {
         text = replace(text, "ꔉ", String.valueOf(Formatting.YELLOW) + "HELPER");
      }

      if (text.getString().contains("◆")) {
         text = replace(text, "◆", "@");
      }

      if (text.getString().contains("┃")) {
         text = replace(text, "┃", "|");
      }

      if (text.getString().contains("ꔳ")) {
         text = replace(text, "ꔳ", String.valueOf(Formatting.AQUA) + "ML.ADMIN");
      }

      if (text.getString().contains("ꔅ")) {
         String var10002 = String.valueOf(Formatting.RED);
         text = replace(text, "ꔅ", var10002 + "Y" + String.valueOf(Formatting.WHITE) + "T");
      }

      if (text.getString().contains("ꔂ")) {
         text = replace(text, "ꔂ", String.valueOf(Formatting.BLUE) + "D.MODER");
      }

      if (text.getString().contains("ꕠ")) {
         text = replace(text, "ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER");
      }

      if (text.getString().contains("ꕄ")) {
         text = replace(text, "ꕄ", String.valueOf(Formatting.RED) + "DRACULA");
      }

      if (text.getString().contains("ꔖ")) {
         text = replace(text, "ꔖ", String.valueOf(Formatting.AQUA) + "OVERLORD");
      }

      if (text.getString().contains("ꕈ")) {
         text = replace(text, "ꕈ", String.valueOf(Formatting.GREEN) + "COBRA");
      }

      if (text.getString().contains("ꔨ")) {
         text = replace(text, "ꔨ", String.valueOf(Formatting.LIGHT_PURPLE) + "DRAGON");
      }

      if (text.getString().contains("ꔤ")) {
         text = replace(text, "ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR");
      }

      if (text.getString().contains("ꔠ")) {
         text = replace(text, "ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER");
      }

      if (text.getString().contains("ꔄ")) {
         text = replace(text, "ꔄ", String.valueOf(Formatting.BLUE) + "HERO");
      }

      if (text.getString().contains("ꔒ")) {
         text = replace(text, "ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER");
      }

      if (text.getString().contains("ꕒ")) {
         text = replace(text, "ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT");
      }

      if (text.getString().contains("ꔈ")) {
         text = replace(text, "ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN");
      }

      if (text.getString().contains("ꕀ")) {
         text = replace(text, "ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA");
      }

      if (text.getString().contains("ꔶ")) {
         text = replace(text, "ꔶ", String.valueOf(Formatting.GOLD) + "TIGER");
      }

      if (text.getString().contains("ꔲ")) {
         text = replace(text, "ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL");
      }

      if (text.getString().contains("ꕖ")) {
         text = replace(text, "ꕖ", String.valueOf(Formatting.BLACK) + "BUNNY");
      }

      if (text.getString().contains("ꕗꕘ")) {
         text = replace(text, "ꕗꕘ", String.valueOf(Formatting.YELLOW) + "SPONSOR");
      }

      if (text.getString().contains("\ud83d\udd25")) {
         text = replace(text, "\ud83d\udd25", "@");
      }

      if (text.getString().contains("ᴀ")) {
         text = replace(text, "ᴀ", "A");
      }

      if (text.getString().contains("ʙ")) {
         text = replace(text, "ʙ", "B");
      }

      if (text.getString().contains("ᴄ")) {
         text = replace(text, "ᴄ", "C");
      }

      if (text.getString().contains("ᴅ")) {
         text = replace(text, "ᴅ", "D");
      }

      if (text.getString().contains("ᴇ")) {
         text = replace(text, "ᴇ", "E");
      }

      if (text.getString().contains("ғ")) {
         text = replace(text, "ғ", "F");
      }

      if (text.getString().contains("ɢ")) {
         text = replace(text, "ɢ", "G");
      }

      if (text.getString().contains("ʜ")) {
         text = replace(text, "ʜ", "H");
      }

      if (text.getString().contains("ɪ")) {
         text = replace(text, "ɪ", "I");
      }

      if (text.getString().contains("ᴊ")) {
         text = replace(text, "ᴊ", "J");
      }

      if (text.getString().contains("ᴋ")) {
         text = replace(text, "ᴋ", "K");
      }

      if (text.getString().contains("ʟ")) {
         text = replace(text, "ʟ", "L");
      }

      if (text.getString().contains("ᴍ")) {
         text = replace(text, "ᴍ", "M");
      }

      if (text.getString().contains("ɴ")) {
         text = replace(text, "ɴ", "N");
      }

      if (text.getString().contains("ꜱ")) {
         text = replace(text, "ꜱ", "S");
      }

      if (text.getString().contains("ᴏ")) {
         text = replace(text, "ᴏ", "O");
      }

      if (text.getString().contains("ᴘ")) {
         text = replace(text, "ᴘ", "P");
      }

      if (text.getString().contains("ǫ")) {
         text = replace(text, "ǫ", "Q");
      }

      if (text.getString().contains("ʀ")) {
         text = replace(text, "ʀ", "R");
      }

      if (text.getString().contains("ᴛ")) {
         text = replace(text, "ᴛ", "T");
      }

      if (text.getString().contains("ᴜ")) {
         text = replace(text, "ᴜ", "U");
      }

      if (text.getString().contains("ᴠ")) {
         text = replace(text, "ᴠ", "V");
      }

      if (text.getString().contains("ᴡ")) {
         text = replace(text, "ᴡ", "W");
      }

      if (text.getString().contains("ꜰ")) {
         text = replace(text, "ꜰ", "F");
      }

      if (text.getString().contains("ʏ")) {
         text = replace(text, "ʏ", "Y");
      }

      if (text.getString().contains("ᴢ")) {
         text = replace(text, "ᴢ", "Z");
      }

      return text;
   }

   public static String toQwerty(String text) {
      return text.replace("й", "q").replace("ц", "w").replace("у", "e").replace("к", "r").replace("е", "t").replace("н", "y").replace("г", "u").replace("ш", "i").replace("щ", "o").replace("з", "p").replace("х", "[").replace("ъ", "]").replace("ф", "a").replace("ы", "s").replace("в", "d").replace("а", "f").replace("п", "g").replace("р", "h").replace("о", "j").replace("л", "k").replace("д", "l").replace("ж", ";").replace("э", "'").replace("я", "z").replace("ч", "x").replace("с", "c").replace("м", "v").replace("и", "b").replace("т", "n").replace("ь", "m").replace("б", ",").replace("ю", ".").replace("ё", "`").replace("Й", "Q").replace("Ц", "W").replace("У", "E").replace("К", "R").replace("Е", "T").replace("Н", "Y").replace("Г", "U").replace("Ш", "I").replace("Щ", "O").replace("З", "P").replace("Х", "{").replace("Ъ", "}").replace("Ф", "A").replace("Ы", "S").replace("В", "D").replace("А", "F").replace("П", "G").replace("Р", "H").replace("О", "J").replace("Л", "K").replace("Д", "L").replace("Ж", ":").replace("Э", "\"").replace("Я", "Z").replace("Ч", "X").replace("С", "C").replace("М", "V").replace("И", "B").replace("Т", "N").replace("Ь", "M").replace("Б", "<").replace("Ю", ">").replace("Ё", "~");
   }

   private static record StyledChar(char c, Style style) {
      private StyledChar(char c, Style style) {
         this.c = c;
         this.style = style;
      }

      public char c() {
         return this.c;
      }

      public Style style() {
         return this.style;
      }
   }
}