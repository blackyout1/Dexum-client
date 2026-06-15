package wtf.dexum.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.api.Module;

public class ModuleArgumentType implements ArgumentType<Module> {
   private static final Collection<String> EXAMPLES = Dexum.getInstance().getModuleManager().getModules().stream().map(Module::getName).limit(5L).toList();

   public static ModuleArgumentType create() {
      return new ModuleArgumentType();
   }

   public Module parse(StringReader reader) throws CommandSyntaxException {
      Module module = Dexum.getInstance().getModuleManager().getModule(reader.readString());
      if (module == null) {
         throw (new DynamicCommandExceptionType((name) -> {
            return Text.literal(name.toString() + " не существует.");
         })).create(reader.readString());
      } else {
         return module;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return CommandSource.suggestMatching(Dexum.getInstance().getModuleManager().getModules().stream().map(Module::getName), builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}