package wtf.dexum.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import wtf.dexum.utility.render.display.Keyboard;

public class MacroArgumentType implements ArgumentType<String> {
   public static MacroArgumentType create() {
      return new MacroArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      List<String> keys = new ArrayList();
      Iterator var4 = Keyboard.BY_NAME.keySet().iterator();

      while(var4.hasNext()) {
         String key = (String)var4.next();
         keys.add(key.toUpperCase());
      }

      return CommandSource.suggestMatching(keys, builder);
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}