package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Objects;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.player.EventMove;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.game.player.MovingUtil;
import wtf.dexum.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "FreeCam",
   description = "Обзор местности за фейк игрока",
   category = Category.MISC
)
public final class FreeCam extends Module {
   public static final FreeCam INSTANCE = new FreeCam();
   public Vec3d pos;
   public Vec3d prevPos;

   private FreeCam() {
   }

   public void onEnable() {
      super.onEnable();
      if (mc.player != null) {
         this.pos = mc.player.getPos();
      }
   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.player.setPosition(this.pos);
      }
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate e) {
      if (mc.player != null) {
         ;
      }
   }

   @EventTarget
   @Native
   public void onPacket(EventPacket e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof PlayerMoveC2SPacket) {
         e.cancel();
      } else if (packet instanceof PlayerRespawnS2CPacket) {
         this.toggle();
      } else if (packet instanceof GameJoinS2CPacket) {
         this.toggle();
      }
   }

   @EventTarget
   public void onWorldRender(EventRender3D e) {
      if (this.pos != null) {
         Render3DUtil.drawBox(new Box(this.pos.x - (double)(mc.player.getWidth() / 2.0F), this.pos.y, this.pos.z - (double)(mc.player.getWidth() / 2.0F), this.pos.x + (double)(mc.player.getWidth() / 2.0F), this.pos.y + (double)mc.player.getHeight(), this.pos.z + (double)(mc.player.getWidth() / 2.0F)), dexum.getThemeManager().getClientColor(90).getRGB(), 1.0F);
      }
   }

   @EventTarget
   public void onMove(EventMove e) {
      mc.player.noClip = true;
      double[] motion = MovingUtil.calculateDirection(1.0119999647140503D);
      e.setMovePos(new Vec3d(motion[0], mc.options.sneakKey.isPressed() ? -1.0D : (mc.options.jumpKey.isPressed() ? 1.0D : 0.0D), motion[1]));
   }

   @EventTarget
   private void onMoveInput(EventMoveInput e) {
      if (mc.player != null) {
         if (mc.player.getPose() == EntityPose.CROUCHING || mc.player.getPose() == EntityPose.SWIMMING) {
            e.setStrafe(e.getStrafe() * 5.0F);
         }

      }
   }
}