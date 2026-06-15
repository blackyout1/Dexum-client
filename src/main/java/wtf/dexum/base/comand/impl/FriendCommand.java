package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.base.comand.impl.args.FriendArgumentType;
import wtf.dexum.base.comand.impl.args.PlayerArgumentType;
import wtf.dexum.utility.game.other.MessageUtil;

public class FriendCommand extends CommandAbstract {
   public FriendCommand() {
      super("friend");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes((context) -> {
         String name = (String)context.getArgument("player", String.class);
         String var10000;
         if (Dexum.getInstance().getFriendManager().getItems().contains(name)) {
            var10000 = String.valueOf(Formatting.GRAY);
            MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + name + String.valueOf(Formatting.GRAY) + " уже добавлен в список друзей");
            return 1;
         } else {
            Dexum.getInstance().getFriendManager().add(name);
            var10000 = String.valueOf(Formatting.GRAY);
            MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + name + String.valueOf(Formatting.GRAY) + " добавлен в список друзей");
            return 1;
         }
      })));
      builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes((context) -> {
         String nickname = (String)context.getArgument("player", String.class);
         Dexum.getInstance().getFriendManager().removeFriend(nickname);
         String var10000 = String.valueOf(Formatting.GRAY);
         MessageUtil.displayInfo(var10000 + "Игрок с ником " + String.valueOf(Formatting.WHITE) + nickname + String.valueOf(Formatting.GRAY) + " удален из списка друзей");
         return 1;
      })));
      builder.then(literal("list").executes((commandContext) -> {
         MessageUtil.displayInfo(Dexum.getInstance().getFriendManager().getItems().toString());
         return 1;
      }));
   }
}