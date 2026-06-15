package wtf.dexum.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import wtf.dexum.Dexum;

public class CfgArgumentType implements ArgumentType<String> {
   private static final Collection<String> EXAMPLES = List.of("current_config");

   public static CfgArgumentType create() {
      return new CfgArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String config = reader.readString();
      if (Dexum.getInstance().getConfigManager().findConfig(config) == null) {
         throw (new DynamicCommandExceptionType((name) -> {
            return Text.literal("Конфига " + name.toString() + " не существует.");
         })).create(config);
      } else {
         return config;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Dexum.getInstance().getConfigManager().configNames(), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}