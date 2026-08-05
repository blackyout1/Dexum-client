package wtf.dexum.base.waypoint;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.utility.interfaces.IClient;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.math.ProjectionUtil;

import java.util.*;

public class WaypointManager implements IClient {
   private Waypoint activeWaypoint = null;
   private Waypoint activePlayerWaypoint = null;
   
   // Хранилище всех GPS точек по именам
   private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();

   public WaypointManager() {
      EventManager.register(this);
   }

   // === Новые методы для работы с несколькими точками ===
   
   @Native
   public void addWaypoint(String name, Waypoint waypoint) {
      this.waypoints.put(name, waypoint);
   }
   
   @Native
   public boolean removeWaypoint(String name) {
      return this.waypoints.remove(name) != null;
   }
   
   @Native
   public int clearAll() {
      int size = this.waypoints.size();
      this.waypoints.clear();
      return size;
   }
   
   @Generated
   public Map<String, Waypoint> getAllWaypoints() {
      return new LinkedHashMap<>(this.waypoints);
   }

   // === Старые методы (оставляем для совместимости) ===
   
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

   // === Рендер плашек на координатах (как NameTags) ===
   
   @EventTarget
   public void onRender2D(EventRender2D e) {
      if (mc.player == null || mc.world == null) return;
      
      float tickDelta = e.getTickDelta();
      ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
      ColorRGBA bgColor = themeColor.darker(0.92F).withAlpha(200);
      
      // Рисуем все GPS точки
      for (Map.Entry<String, Waypoint> entry : waypoints.entrySet()) {
         Waypoint wp = entry.getValue();
         String name = entry.getKey();
         
         // Координаты точки
         double x = wp.getX();
         double y = wp.getY();
         double z = wp.getZ();
         
         // Проверяем видимость
         Vec3d waypointPos = new Vec3d(x, y, z);
         if (!ProjectionUtil.canSee(waypointPos)) {
            continue;
         }
         
         // Проверяем что точка в поле зрения камеры
         if (!mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
            Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
            Vec3d entityPosRel = waypointPos.subtract(cameraPos);

            float pitch = mc.getEntityRenderDispatcher().camera.getPitch();
            float yaw = mc.getEntityRenderDispatcher().camera.getYaw();
            float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
            float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
            float f2 = -MathHelper.cos(-pitch * 0.017453292F);
            float f3 = MathHelper.sin(-pitch * 0.017453292F);
            Vec3d actualLookVec = new Vec3d(f1 * f2, f3, f * f2);

            if (entityPosRel.dotProduct(actualLookVec) < 0) {
               continue;
            }
         }
         
         // Преобразуем в экранные координаты
         Vec3d screenPos = ProjectionUtil.worldSpaceToScreenSpace(waypointPos);
         if (screenPos.z <= 0.0D || screenPos.z >= 1.0D) {
            continue;
         }
         
         // Расстояние до точки
         double distance = mc.player.getPos().distanceTo(waypointPos);
         String distText = String.format("%.0fm", distance);
         
         // Размеры плашки
         float scale = 1.0f;
         float nameWidth = Fonts.MEDIUM.getWidth(name, 7.0f * scale);
         float distWidth = Fonts.REGULAR.getWidth(distText, 6.5f * scale);
         float plateWidth = Math.max(nameWidth, distWidth) + 12.0f * scale;
         float plateHeight = 22.0f * scale;
         
         // Используем screenPos напрямую
         float plateX = (float)screenPos.x - plateWidth / 2.0f;
         float plateY = (float)screenPos.y - plateHeight / 2.0f;
         
         // Тень
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(), 
            plateX - 0.5F, plateY - 0.5F + 1.0F, 
            plateWidth + 1.0F, plateHeight + 1.0F, 
            BorderRadius.all(2.0F), 
            new ColorRGBA(0, 0, 0, 66));
         
         // Фон плашки
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(), 
            plateX, plateY, 
            plateWidth, plateHeight, 
            BorderRadius.all(2.0F), 
            bgColor);
         
         // Цветная полоска сверху
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(),
            plateX, plateY, 
            plateWidth, 2.5f * scale,
            new BorderRadius(2.0F, 2.0F, 0.0F, 0.0F),
            themeColor);
         
         // Название точки (белый текст)
         e.getContext().drawText(
            Fonts.MEDIUM.getFont(7.0f * scale), 
            name, 
            plateX + (plateWidth - nameWidth) / 2.0f, 
            plateY + 5.0f * scale, 
            ColorRGBA.WHITE
         );
         
         // Расстояние (серый текст)
         e.getContext().drawText(
            Fonts.REGULAR.getFont(6.5f * scale), 
            distText, 
            plateX + (plateWidth - distWidth) / 2.0f, 
            plateY + 13.0f * scale, 
            new ColorRGBA(180, 180, 180)
         );
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