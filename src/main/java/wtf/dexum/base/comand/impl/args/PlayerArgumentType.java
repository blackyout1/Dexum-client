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
import net.minecraft.command.CommandSource;
import wtf.dexum.utility.interfaces.IMinecraft;

public class PlayerArgumentType implements ArgumentType<String>, IMinecraft {
   public static PlayerArgumentType create() {
      return new PlayerArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readUnquotedString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(mc.getNetworkHandler().getPlayerList().stream().map((p) -> {
         return p.getProfile().getName();
      }), builder);
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}