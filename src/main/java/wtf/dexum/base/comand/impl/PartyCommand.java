package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.client.modules.impl.misc.PartyModule;
import wtf.dexum.utility.game.other.MessageUtil;

public class PartyCommand extends CommandAbstract {
    public PartyCommand() {
        super("party");
    }

    @Native
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        // .party create <code>
        builder.then(literal("create")
            .then(arg("code", StringArgumentType.word())
                .executes(context -> {
                    String code = context.getArgument("code", String.class);
                    PartyModule.INSTANCE.createParty(code);
                    return 1;
                })
            )
        );

        // .party join <code>
        builder.then(literal("join")
            .then(arg("code", StringArgumentType.word())
                .executes(context -> {
                    String code = context.getArgument("code", String.class);
                    PartyModule.INSTANCE.joinParty(code);
                    return 1;
                })
            )
        );

        // .party leave
        builder.then(literal("leave")
            .executes(context -> {
                if (!PartyModule.INSTANCE.isInParty()) {
                    MessageUtil.displayInfo("§a[Party] §fВы не в пати.");
                } else {
                    PartyModule.INSTANCE.leaveParty();
                    MessageUtil.displayInfo("§a[Party] §fВы покинули пати.");
                }
                return 1;
            })
        );

        // .party disband
        builder.then(literal("disband")
            .executes(context -> {
                if (!PartyModule.INSTANCE.isInParty()) {
                    MessageUtil.displayInfo("§a[Party] §fВы не в пати.");
                } else if (!PartyModule.INSTANCE.isLeader()) {
                    MessageUtil.displayInfo("§a[Party] §fТолько создатель может распустить пати.");
                } else {
                    PartyModule.INSTANCE.disbandParty();
                }
                return 1;
            })
        );

        // .party members
        builder.then(literal("members")
            .executes(context -> {
                PartyModule.INSTANCE.showMembers();
                return 1;
            })
        );
    }
}
