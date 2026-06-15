package wtf.dexum.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.comand.api.CommandAbstract;
import wtf.dexum.base.comand.impl.args.CoordinateArgumentType;
import wtf.dexum.utility.game.other.MessageUtil;

public class ClipCommand extends CommandAbstract {
   public ClipCommand() {
      super("vclip");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("up").executes((context) -> {
         double yOffset = this.findOffset(mc.player.getBlockPos(), true);
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();

         for(int i = 0; i < 3; ++i) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y + yOffset, z, false, mc.player.horizontalCollision));
         mc.player.setPosition(x, y + yOffset, z);
         MessageUtil.displayInfo("Телепортировано на " + (int)yOffset + " блоков вверх");
         return 1;
      }));
      builder.then(literal("down").executes((context) -> {
         double yOffset = this.findOffset(mc.player.getBlockPos(), false);
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();

         for(int i = 0; i < 3; ++i) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y + yOffset, z, false, mc.player.horizontalCollision));
         mc.player.setPosition(x, y + yOffset, z);
         MessageUtil.displayInfo("Телепортировано на " + (int)yOffset + " блоков вниз");
         return 1;
      }));
      builder.then(arg("distance", CoordinateArgumentType.create()).executes((context) -> {
         Double distance = (Double)context.getArgument("distance", Double.class);
         double x = mc.player.getX();
         double y = mc.player.getY();
         double z = mc.player.getZ();

         for(int i = 0; i < 3; ++i) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y + distance, z, false, mc.player.horizontalCollision));
         mc.player.setPosition(x, y + distance, z);
         if (distance > 0.0D) {
            MessageUtil.displayInfo("Телепортировано на " + distance + " блоков вверх");
         } else {
            MessageUtil.displayInfo("Телепортировано на " + distance + " блоков вниз");
         }

         return 1;
      }));
   }

   private double findOffset(BlockPos pos, boolean toUp) {
      int i;
      BlockPos base;
      BlockPos head;
      if (toUp) {
         for(i = 3; i < 255; ++i) {
            base = pos.add(0, i, 0);
            head = base.up();
            if (mc.world.getBlockState(base).isAir() && mc.world.getBlockState(head).isAir()) {
               return (double)base.getY() - mc.player.getY();
            }
         }
      } else {
         for(i = -1; i > -255; --i) {
            base = pos.add(0, i, 0);
            head = base.down();
            BlockPos air2 = head.down();
            boolean isSolid = !mc.world.getBlockState(base).isAir();
            boolean isAirBelow1 = mc.world.getBlockState(head).isAir();
            boolean isAirBelow2 = mc.world.getBlockState(air2).isAir();
            if (isSolid && isAirBelow1 && isAirBelow2) {
               return (double)air2.getY() - mc.player.getY();
            }
         }
      }

      return 0.0D;
   }
}