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

public class FriendArgumentType implements ArgumentType<String> {
   public static FriendArgumentType create() {
      return new FriendArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String friend = reader.readString();
      if (!Dexum.getInstance().getFriendManager().isFriend(friend)) {
         throw (new DynamicCommandExceptionType((name) -> {
            return Text.literal("У тебя нет друга с ником " + friend);
         })).create(friend);
      } else {
         return friend;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Dexum.getInstance().getFriendManager().getItems(), builder);
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}