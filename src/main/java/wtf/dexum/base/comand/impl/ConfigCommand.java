package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.File;
import java.io.IOException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.utility.game.other.MessageUtil;

public class ConfigCommand extends CommandAbstract {
   public ConfigCommand() {
      super("cfg");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("save").then(arg("name", StringArgumentType.word()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         boolean success = Dexum.getInstance().getConfigManager().saveConfig(name);
         if (success) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигурация сохранена");
         } else {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ошибка при сохранении конфигурации");
         }

         return 1;
      })));
      builder.then(literal("load").then(arg("name", StringArgumentType.word()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         boolean success = Dexum.getInstance().getConfigManager().loadConfig(name);
         if (success) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигурация загружена");
         } else {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ошибка при загрузке конфигурации");
         }

         return 1;
      })));
      builder.then(literal("dir").executes((context) -> {
         try {
            File dir = Dexum.DIRECTORY;
            if (!dir.exists()) {
               MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ты нахуя папку удалил фрик");
               dir.mkdirs();
            } else {
               MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Открываю папку dexum...");
            }

            Runtime.getRuntime().exec("explorer " + dir.getAbsolutePath());
         } catch (IOException var2) {
            String var10000 = String.valueOf(Formatting.GRAY);
            MessageUtil.displayInfo(var10000 + "Ошибка при открытии папки: " + String.valueOf(Formatting.WHITE) + var2.getMessage());
         }

         return 1;
      }));
   }
}