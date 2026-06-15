package wtf.dexum.base.waypoint;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

public class WaypointManager implements IClient {
   private Waypoint activeWaypoint = null;
   private Waypoint activePlayerWaypoint = null;

   public WaypointManager() {
      EventManager.register(this);
   }

   @Native
   public void set(Waypoint waypoint) {
      this.activeWaypoint = waypoint;
   }

   @Native
   public void remove(Waypoint waypoint) {
      if (this.activeWaypoint != null && this.activeWaypoint.equals(waypoint)) {
         this.activeWaypoint = null;
      }

   }

   @Native
   public void clear() {
      this.activeWaypoint = null;
   }

   public boolean isEmpty() {
      return this.activeWaypoint == null;
   }

   @Native
   public void setPlayerWaypoint(Waypoint waypoint) {
      this.activePlayerWaypoint = waypoint;
   }

   @Native
   public void removePlayerWaypoint(Waypoint waypoint) {
      if (this.activePlayerWaypoint != null && this.activeWaypoint.equals(waypoint)) {
         this.activePlayerWaypoint = null;
      }

   }

   @Native
   public void clearPlayerWaypoint() {
      this.activePlayerWaypoint = null;
   }

   public boolean isEmptyPlayerWaypoint() {
      return this.activePlayerWaypoint == null;
   }

   @EventTarget
   public void onHUD(EventHudRender e) {
      if (mc.player != null) {
         float x;
         float y;
         float size;
         double x2;
         double z2;
         int distance;
         float yaw;
         if (this.activeWaypoint != null) {
            x = (float)mc.getWindow().getScaledWidth() / 2.0F;
            y = (float)mc.getWindow().getScaledHeight() / 4.0F;
            size = 16.0F;
            e.getContext().getMatrices().push();
            x2 = this.activeWaypoint.getX() - mc.player.getX();
            z2 = this.activeWaypoint.getZ() - mc.player.getZ();
            distance = (int)MathHelper.sqrt((float)(x2 * x2 + z2 * z2));
            yaw = (float)(-(Math.atan2(x2, z2) * 57.29577951308232D)) - mc.gameRenderer.getCamera().getYaw();
            e.getContext().drawText(Fonts.REGULAR.getFont(7.0F), distance + "m", x - Fonts.REGULAR.getWidth(distance + "m", 7.0F) / 2.0F, y + 8.0F, ColorRGBA.WHITE);
            e.getContext().getMatrices().translate(x, y, 0.0F);
            e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
            e.getContext().getMatrices().translate(-x, -y, 0.0F);
            e.getContext().drawTexture(Dexum.id("icons/arrow.png"), x - size / 2.0F, y - size / 2.0F, size, size, Dexum.getInstance().getThemeManager().getCurrentTheme().getColor());
            e.getContext().getMatrices().pop();
         }

         if (this.activePlayerWaypoint != null) {
            x = (float)mc.getWindow().getScaledWidth() / 2.0F;
            y = (float)mc.getWindow().getScaledHeight() / 3.35F;
            size = 16.0F;
            e.getContext().getMatrices().push();
            x2 = this.activeWaypoint.getX() - mc.player.getX();
            z2 = this.activeWaypoint.getZ() - mc.player.getZ();
            distance = (int)MathHelper.sqrt((float)(x2 * x2 + z2 * z2));
            yaw = (float)(-(Math.atan2(x2, z2) * 57.29577951308232D)) - mc.gameRenderer.getCamera().getYaw();
            e.getContext().drawText(Fonts.REGULAR.getFont(7.0F), this.activePlayerWaypoint.getName(), x - Fonts.REGULAR.getWidth(this.activePlayerWaypoint.getName(), 7.0F) / 2.0F, y + 7.0F, ColorRGBA.WHITE);
            e.getContext().drawText(Fonts.REGULAR.getFont(7.0F), distance + "m", x - Fonts.REGULAR.getWidth(distance + "m", 7.0F) / 2.0F, y + 14.0F, ColorRGBA.WHITE);
            e.getContext().getMatrices().translate(x, y, 0.0F);
            e.getContext().getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
            e.getContext().getMatrices().translate(-x, -y, 0.0F);
            e.getContext().drawTexture(Dexum.id("icons/arrow.png"), x - size / 2.0F, y - size / 2.0F, size, size, new ColorRGBA(255, 32, 32));
            e.getContext().getMatrices().pop();
         }

      }
   }

   @Generated
   public Waypoint getActiveWaypoint() {
      return this.activeWaypoint;
   }

   @Generated
   public Waypoint getActivePlayerWaypoint() {
      return this.activePlayerWaypoint;
   }
}