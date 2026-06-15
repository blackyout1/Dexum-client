package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.util.math.Vec3d;

public class EventCameraPosition implements Event {
   private Vec3d pos;

   @Generated
   public Vec3d getPos() {
      return this.pos;
   }

   @Generated
   public void setPos(Vec3d pos) {
      this.pos = pos;
   }

   @Generated
   public EventCameraPosition(Vec3d pos) {
      this.pos = pos;
   }
}