package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.client.modules.impl.misc.PartyModuleCloud;
import wtf.dexum.utility.game.other.MessageUtil;

public class PartyCloudCommand extends CommandAbstract {
    public PartyCloudCommand() {
        super("partycloud");
    }

    @Native
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        // .pc create <code>
        builder.then(literal("create")
            .then(arg("code", StringArgumentType.word())
                .executes(context -> {
                    String code = context.getArgument("code", String.class);
                    PartyModuleCloud.INSTANCE.createParty(code);
                    return 1;
                })
            )
        );

        // .pc join <code>
        builder.then(literal("join")
            .then(arg("code", StringArgumentType.word())
                .executes(context -> {
                    String code = context.getArgument("code", String.class);
                    PartyModuleCloud.INSTANCE.joinParty(code);
                    return 1;
                })
            )
        );

        // .pc leave
        builder.then(literal("leave")
            .executes(context -> {
                if (!PartyModuleCloud.INSTANCE.isInParty()) {
                    MessageUtil.displayInfo("§a[Party] §fВы не в пати");
                } else {
                    PartyModuleCloud.INSTANCE.leaveParty();
                }
                return 1;
            })
        );

        // .pc members
        builder.then(literal("members")
            .executes(context -> {
                PartyModuleCloud.INSTANCE.showMembers();
                return 1;
            })
        );
    }
}
