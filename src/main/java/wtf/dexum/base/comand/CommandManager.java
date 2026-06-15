package wtf.dexum.base.comand;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.base.comand.impl.ClipCommand;
import wtf.dexum.base.comand.impl.ConfigCommand;
import wtf.dexum.base.comand.impl.FriendCommand;
import wtf.dexum.base.comand.impl.GPSCommand;
import wtf.dexum.base.comand.impl.MacroCommand;
import wtf.dexum.base.comand.impl.RCTCommand;

public class CommandManager {
   private String prefix = ".";
   private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher();
   private final CommandSource source = new ClientCommandSource((ClientPlayNetworkHandler)null, MinecraftClient.getInstance());
   private final List<CommandAbstract> commands = new ArrayList();

   public CommandManager() {
      this.register();
   }

   @Native
   private void register() {
      this.registerCommand(new FriendCommand());
      this.registerCommand(new MacroCommand());
      this.registerCommand(new ClipCommand());
      this.registerCommand(new ConfigCommand());
      this.registerCommand(new RCTCommand());
      this.registerCommand(new GPSCommand());
   }

   @Native
   public void registerCommand(CommandAbstract command) {
      if (command != null) {
         command.register(this.dispatcher);
         this.commands.add(command);
      }
   }

   @Generated
   public String getPrefix() {
      return this.prefix;
   }

   @Generated
   public CommandDispatcher<CommandSource> getDispatcher() {
      return this.dispatcher;
   }

   @Generated
   public CommandSource getSource() {
      return this.source;
   }

   @Generated
   public List<CommandAbstract> getCommands() {
      return this.commands;
   }
}