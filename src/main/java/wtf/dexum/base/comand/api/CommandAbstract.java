package wtf.dexum.base.comand.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Generated;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import wtf.dexum.utility.interfaces.IClient;

public abstract class CommandAbstract implements IClient {
   private final String command;

   protected CommandAbstract(String command) {
      this.command = command;
   }

   public void register(CommandDispatcher<CommandSource> dispatcher) {
      LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(this.command);
      this.execute(builder);
      dispatcher.register(builder);
   }

   public abstract void execute(LiteralArgumentBuilder<CommandSource> var1);

   @NotNull
   protected static <T> RequiredArgumentBuilder<CommandSource, T> arg(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   @NotNull
   protected static LiteralArgumentBuilder<CommandSource> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   @Generated
   public String getCommand() {
      return this.command;
   }
}