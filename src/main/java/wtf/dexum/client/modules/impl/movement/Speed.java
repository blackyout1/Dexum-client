package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.utility.game.player.MovingUtil;
import wtf.dexum.utility.predict.PredictUtils;

@ModuleAnnotation(
   name = "Speed",
   category = Category.MOVEMENT,
   description = "Ускоряет вас возле цели ауры"
)
public class Speed extends Module {
   public static final Speed INSTANCE = new Speed();
   private final ModeSetting mode = new ModeSetting("Mode", "Grim", "Vanilla");
   private final NumberSetting speedValue = new NumberSetting("Speed", 0.42f, 0.1f, 1.0f, 0.01f);

   @EventTarget
   private void onUpdate(EventUpdate ignored) {
      if (mc.player == null || mc.world == null) return;

      if (mode.is("Grim")) {
          if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
              if (mc.player.isOnGround()) {
                  mc.player.jump();
              } else {
                  MovingUtil.strafe(MovingUtil.getSpeed() * 1.006f);
              }
          }
      } else {
          this.collisionSpeed();
      }
   }

   @Native
   private void collisionSpeed() {
      Aura aura = Aura.INSTANCE;
      LivingEntity target = aura.getTarget();
      if (target != null && target != mc.player) {
         Box aABB = mc.player.getBoundingBox().expand(1.2000000476837158D);
         if ((mc.player.isGliding() || target.getBoundingBox().intersects(aABB)) && (!mc.player.isGliding() || !(mc.player.getEyePos().distanceTo(PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent() - 0.3F)) > 2.5D) && !(mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter()) > 2.5D))) {
            Vec3d newVelocity = getVec3d(target);
            mc.player.setVelocity(newVelocity);
         }
      }
   }

   @NotNull
   private static Vec3d getVec3d(LivingEntity target) {
      double deltaX = target.getX() - mc.player.getX();
      double deltaZ = target.getZ() - mc.player.getZ();
      if (mc.player.isGliding() && target.isGliding()) {
         deltaX = PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent()).x - mc.player.getX();
         deltaZ = PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent()).z - mc.player.getZ();
      }

      float targetYaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
      double radYaw = Math.toRadians((double)targetYaw);
      double force = 0.07200000107288361D;
      Vec3d velocity = mc.player.getVelocity();
      return new Vec3d(velocity.x + -Math.sin(radYaw) * 0.07200000107288361D, velocity.y, velocity.z + Math.cos(radYaw) * 0.07200000107288361D);
   }
}