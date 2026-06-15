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
import wtf.dexum.Dexum;
import wtf.dexum.base.macro.Macro;
import wtf.dexum.utility.render.display.Keyboard;

public class MacroRemoveArgumentType implements ArgumentType<String> {
   public static MacroRemoveArgumentType create() {
      return new MacroRemoveArgumentType();
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      return reader.readString();
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      List<String> binds = new ArrayList();
      Iterator var4 = Dexum.getInstance().getMacroManager().getItems().iterator();

      while(var4.hasNext()) {
         Macro macro = (Macro)var4.next();
         binds.add(Keyboard.getKeyName(macro.getBind()));
      }

      return CommandSource.suggestMatching(binds, builder);
   }

   public Collection<String> getExamples() {
      return List.of();
   }
}