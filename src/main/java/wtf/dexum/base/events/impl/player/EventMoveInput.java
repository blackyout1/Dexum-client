package wtf.dexum.base.events.impl.player;

import lombok.Generated;
import net.minecraft.util.PlayerInput;
import wtf.dexum.base.events.callables.EventCancellable;

public class EventMoveInput extends EventCancellable {
   private PlayerInput input;
   private float forward;
   private float strafe;

   @Generated
   public PlayerInput getInput() {
      return this.input;
   }

   @Generated
   public float getForward() {
      return this.forward;
   }

   @Generated
   public float getStrafe() {
      return this.strafe;
   }

   @Generated
   public void setInput(PlayerInput input) {
      this.input = input;
   }

   @Generated
   public void setForward(float forward) {
      this.forward = forward;
   }

   @Generated
   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   @Generated
   public EventMoveInput(PlayerInput input, float forward, float strafe) {
      this.input = input;
      this.forward = forward;
      this.strafe = strafe;
   }
}