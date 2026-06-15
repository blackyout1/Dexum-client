package wtf.dexum.base.events.impl.render;

import lombok.Generated;
import wtf.dexum.base.events.callables.EventCancellable;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class EventCamera extends EventCancellable {
   private boolean cameraClip;
   private float distance;
   private Rotation angle;

   @Generated
   public boolean isCameraClip() {
      return this.cameraClip;
   }

   @Generated
   public float getDistance() {
      return this.distance;
   }

   @Generated
   public Rotation getAngle() {
      return this.angle;
   }

   @Generated
   public void setCameraClip(boolean cameraClip) {
      this.cameraClip = cameraClip;
   }

   @Generated
   public void setDistance(float distance) {
      this.distance = distance;
   }

   @Generated
   public void setAngle(Rotation angle) {
      this.angle = angle;
   }

   @Generated
   public EventCamera(boolean cameraClip, float distance, Rotation angle) {
      this.cameraClip = cameraClip;
      this.distance = distance;
      this.angle = angle;
   }
}