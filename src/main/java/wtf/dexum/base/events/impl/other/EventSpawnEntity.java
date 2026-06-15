package wtf.dexum.base.events.impl.other;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.entity.Entity;

public class EventSpawnEntity implements Event {
   private Entity entity;

   @Generated
   public Entity getEntity() {
      return this.entity;
   }

   @Generated
   public EventSpawnEntity(Entity entity) {
      this.entity = entity;
   }
}