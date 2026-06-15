package wtf.dexum.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandArgumentType implements ArgumentType<String> {
   public static CommandArgumentType create() {
      return new CommandArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      StringBuilder command = new StringBuilder();

      while(reader.canRead()) {
         command.append(reader.read());
      }

      return command.toString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return new CompletableFuture();
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}