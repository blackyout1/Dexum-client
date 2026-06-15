package wtf.dexum.base.events.impl.player;

import lombok.Generated;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventMove extends EventCancellable {
   private Vec3d movePos;

   @Generated
   public EventMove(Vec3d movePos) {
      this.movePos = movePos;
   }

   @Generated
   public Vec3d getMovePos() {
      return this.movePos;
   }

   @Generated
   public void setMovePos(Vec3d movePos) {
      this.movePos = movePos;
   }
}