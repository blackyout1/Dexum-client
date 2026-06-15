package wtf.dexum.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class EventPickupItem implements Event {
   private final LivingEntity entity;
   private final ItemStack itemStack;

   @Generated
   public EventPickupItem(LivingEntity entity, ItemStack itemStack) {
      this.entity = entity;
      this.itemStack = itemStack;
   }

   @Generated
   public LivingEntity getEntity() {
      return this.entity;
   }

   @Generated
   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventPickupItem)) {
         return false;
      } else {
         EventPickupItem other = (EventPickupItem)o;
         if (!other.canEqual(this)) {
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

            Object this$itemStack = this.getItemStack();
            Object other$itemStack = other.getItemStack();
            if (this$itemStack == null) {
               if (other$itemStack != null) {
                  return false;
               }
            } else if (!this$itemStack.equals(other$itemStack)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof EventPickupItem;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1;
      Object $entity = this.getEntity();
      result = result * 59 + ($entity == null ? 43 : $entity.hashCode());
      Object $itemStack = this.getItemStack();
      result = result * 59 + ($itemStack == null ? 43 : $itemStack.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getEntity());
      return "EventPickupItem(entity=" + var10000 + ", itemStack=" + String.valueOf(this.getItemStack()) + ")";
   }
}