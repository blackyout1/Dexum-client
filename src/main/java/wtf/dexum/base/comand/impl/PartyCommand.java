package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.client.modules.impl.misc.PartyModule;
import wtf.dexum.utility.game.other.MessageUtil;

public class PartyCommand extends CommandAbstract {
    public PartyCommand() {
        super("party");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("create").then(arg("code", StringArgumentType.word()).executes((context) -> {
            String code = context.getArgument("code", String.class);
            PartyModule.INSTANCE.processCommand(".party create " + code);
            return 1;
        })));

        builder.then(literal("join").then(arg("code", StringArgumentType.word()).executes((context) -> {
            String code = context.getArgument("code", String.class);
            PartyModule.INSTANCE.processCommand(".party join " + code);
            return 1;
        })));

        builder.then(literal("leave").executes((context) -> {
            PartyModule.INSTANCE.processCommand(".party leave");
            return 1;
        }));

        builder.then(literal("disband").executes((context) -> {
            PartyModule.INSTANCE.processCommand(".party disband");
            return 1;
        }));

        builder.then(literal("members").executes((context) -> {
            PartyModule.INSTANCE.processCommand(".party members");
            return 1;
        }));
    }
}
