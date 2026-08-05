package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.base.waypoint.Waypoint;
import wtf.dexum.utility.game.other.MessageUtil;

import java.util.Map;

public class GPSCommand extends CommandAbstract {
   public GPSCommand() {
      super("gps");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      // .gps add <name> <x> <y> <z>
      builder.then(literal("add")
         .then(arg("name", StringArgumentType.word())
            .then(arg("x", DoubleArgumentType.doubleArg())
               .then(arg("y", DoubleArgumentType.doubleArg())
                  .then(arg("z", DoubleArgumentType.doubleArg())
                     .executes(context -> {
                        String name = context.getArgument("name", String.class);
                        double x = context.getArgument("x", Double.class);
                        double y = context.getArgument("y", Double.class);
                        double z = context.getArgument("z", Double.class);
                        
                        Waypoint waypoint = new Waypoint(name, x, y, z);
                        Dexum.getInstance().getWaypointManager().addWaypoint(name, waypoint);
                        MessageUtil.displayInfo("§aGPS точка '§f%s§a' добавлена: §f%.1f, %.1f, %.1f".formatted(name, x, y, z));
                        return 1;
                     })
                  )
               )
            )
         )
      );

      // .gps remove <name>
      builder.then(literal("remove")
         .then(arg("name", StringArgumentType.word())
            .executes(context -> {
               String name = context.getArgument("name", String.class);
               
               if (Dexum.getInstance().getWaypointManager().removeWaypoint(name)) {
                  MessageUtil.displayInfo("§aGPS точка '§f%s§a' удалена".formatted(name));
               } else {
                  MessageUtil.displayInfo("§cGPS точка '§f%s§c' не найдена".formatted(name));
               }
               return 1;
            })
         )
      );

      // .gps clear
      builder.then(literal("clear")
         .executes(context -> {
            int count = Dexum.getInstance().getWaypointManager().clearAll();
            if (count > 0) {
               MessageUtil.displayInfo("§aУдалено GPS точек: §f%d".formatted(count));
            } else {
               MessageUtil.displayInfo("§cНет GPS точек для удаления");
            }
            return 1;
         })
      );

      // .gps list
      builder.then(literal("list")
         .executes(context -> {
            Map<String, Waypoint> waypoints = Dexum.getInstance().getWaypointManager().getAllWaypoints();
            
            if (waypoints.isEmpty()) {
               MessageUtil.displayInfo("§cСписок GPS точек пуст");
            } else {
               MessageUtil.displayInfo("§aСписок GPS точек §f(%d)§a:".formatted(waypoints.size()));
               for (Map.Entry<String, Waypoint> entry : waypoints.entrySet()) {
                  Waypoint wp = entry.getValue();
                  MessageUtil.displayInfo("  §f%s§7: §f%.1f, %.1f, %.1f".formatted(
                     entry.getKey(), wp.getX(), wp.getY(), wp.getZ()));
               }
            }
            return 1;
         })
      );
   }
}