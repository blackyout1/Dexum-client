package wtf.dexum.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CoordinateArgumentType implements ArgumentType<Double> {
   public static CoordinateArgumentType create() {
      return new CoordinateArgumentType();
   }

   public Double parse(StringReader reader) throws CommandSyntaxException {
      try {
         return Double.parseDouble(reader.readString());
      } catch (NumberFormatException var3) {
         throw new CommandSyntaxException((CommandExceptionType)null, () -> {
            return "Не те циферки пишешь родной";
         });
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}