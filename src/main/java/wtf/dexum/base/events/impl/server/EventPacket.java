package wtf.dexum.base.events.impl.server;

import lombok.Generated;
import net.minecraft.network.packet.Packet;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventPacket extends EventCancellable {
   private final EventPacket.Action action;
   private Packet<?> packet;

   public boolean isSent() {
      return this.getAction() == EventPacket.Action.SENT;
   }

   public boolean isReceive() {
      return this.getAction() == EventPacket.Action.RECEIVE;
   }

   @Generated
   public EventPacket.Action getAction() {
      return this.action;
   }

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   @Generated
   public EventPacket(EventPacket.Action action, Packet<?> packet) {
      this.action = action;
      this.packet = packet;
   }

   public static enum Action {
      SENT,
      RECEIVE;

      private static EventPacket.Action[] $values() {
         return new EventPacket.Action[]{SENT, RECEIVE};
      }
   }
}