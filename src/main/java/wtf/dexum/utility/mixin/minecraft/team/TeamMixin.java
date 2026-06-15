package wtf.dexum.utility.mixin.minecraft.team;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wtf.dexum.client.modules.impl.misc.NameProtect;
import wtf.dexum.utility.game.other.ReplaceUtil;

@Mixin({Team.class})
public class TeamMixin {
   @ModifyArg(
      method = {"decorateName(Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/text/MutableText;append(Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;",
   ordinal = 0
),
      index = 0
   )
   private Text modify(Text original) {
      if (MinecraftClient.getInstance().player == null) {
         return original;
      } else {
         String me = MinecraftClient.getInstance().player.getNameForScoreboard();
         String replaced = NameProtect.getCustomName(me);
         return me.equals(replaced) ? original : ReplaceUtil.replaceLiteral(original, me, replaced);
      }
   }
}