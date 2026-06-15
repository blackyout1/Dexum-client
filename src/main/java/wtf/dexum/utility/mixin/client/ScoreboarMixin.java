package wtf.dexum.utility.mixin.client;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Scoreboard.class})
public abstract class ScoreboarMixin {
   @Shadow
   @Nullable
   public abstract Team method_1164(String var1);

   @Inject(
      method = {"removeScoreHolderFromTeam"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void remove(String scoreHolderName, Team team, CallbackInfo ci) {
      if (this.method_1164(scoreHolderName) != team) {
         ci.cancel();
      }

   }
}