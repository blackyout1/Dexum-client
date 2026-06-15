package wtf.dexum.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.entity.Entity;

public class EventEntityHitBox implements Event {
   private Entity entity;
   private float size;

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public float getSize() {
      return this.size;
   }

   @Generated
   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   @Generated
   public void setSize(float size) {
      this.size = size;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventEntityHitBox)) {
         return false;
      } else {
         EventEntityHitBox other = (EventEntityHitBox)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Float.compare(this.getSize(), other.getSize()) != 0) {
            return false;
         } else {
            Object this$entity = this.getEntity();
            Object other$entity = other.getEntity();
            if (this$entity == null) {
               if (other$entity != null) {
                  return false;
               }
            } else if (!this$entity.equals(other$entity)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof EventEntityHitBox;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + Float.floatToIntBits(this.getSize());
      Object $entity = this.getEntity();
      result = result * 59 + ($entity == null ? 43 : $entity.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getEntity());
      return "EventEntityHitBox(entity=" + var10000 + ", size=" + this.getSize() + ")";
   }

   @Generated
   public EventEntityHitBox(Entity entity, float size) {
      this.entity = entity;
      this.size = size;
   }
}