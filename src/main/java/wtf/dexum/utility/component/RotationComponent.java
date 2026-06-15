package wtf.dexum.utility.component;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.utility.game.player.MovingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.interfaces.IClient;

public class RotationComponent implements IClient {
   public static RotationComponent instance = new RotationComponent();
   private RotationComponent.RotationTask currentTask;
   private float currentYawSpeed;
   private float currentPitchSpeed;
   private float currentYawReturnSpeed;
   private float currentPitchReturnSpeed;
   private int currentPriority;
   private int currentTimeout;
   private int idleTicks;
   private Rotation targetRotation;

   public RotationComponent() {
      this.currentTask = RotationComponent.RotationTask.IDLE;
      this.targetRotation = new Rotation(0.0F, 0.0F);
      EventManager.register(this);
      new FreeLookComponent();
   }

   public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
      if (moveForward < 0.0F) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0F) {
         forward = -0.5F;
      }

      if (moveForward > 0.0F) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0F) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0F) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians((double)rotationYaw);
   }

   @EventTarget
   public void onInput(EventMoveInput event) {
      if (this.isRotating()) {
         MovingUtil.fixMovementFocus(event, MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw()));
      }

   }

   private void resetRotation() {
      Rotation targetRotation = new Rotation(FreeLookComponent.getFreeYaw(), FreeLookComponent.getFreePitch());
      if (this.updateRotation(targetRotation, this.currentYawReturnSpeed(), this.currentPitchReturnSpeed())) {
         this.stopRotation();
      }

   }

   @EventTarget
   @Native
   public void onEventTick(EventUpdate event) {
      if (this.currentTask().equals(RotationComponent.RotationTask.AIM) && this.idleTicks() > this.currentTimeout()) {
         this.currentTask(RotationComponent.RotationTask.RESET);
      }

      if (this.currentTask().equals(RotationComponent.RotationTask.RESET)) {
         this.resetRotation();
      }

      ++this.idleTicks;
   }

   @Native
   public static Vec2f applySensitivityPatch(Vec2f rotation, Vec2f previousRotation) {
      double sens = (Double)mc.options.getMouseSensitivity().getValue();
      double gcd = Math.pow(sens * 0.6000000238418579D + 0.20000000298023224D, 3.0D) * 8.0D;
      double prevYaw = (double)previousRotation.x;
      double prevPitch = (double)previousRotation.y;
      double currentYaw = (double)rotation.x;
      double currentPitch = (double)rotation.y;
      double yaw = (double) Math.round((currentYaw - prevYaw) / (gcd * 0.15000000596046448D)) * gcd * 0.15000000596046448D;
      double pitch = (double) Math.round((currentPitch - prevPitch) / (gcd * 0.15000000596046448D)) * gcd * 0.15000000596046448D;
      return new Vec2f((float)(prevYaw + yaw), (float)(prevPitch + pitch));
   }

   public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
      RotationComponent instance = RotationComponent.instance;
      if (instance.currentPriority() <= priority) {
         if (instance.currentTask().equals(RotationComponent.RotationTask.IDLE) && !clientRotation) {
            FreeLookComponent.setActive(true);
         }

         instance.currentYawSpeed(yawSpeed);
         instance.currentPitchSpeed(pitchSpeed);
         instance.currentYawReturnSpeed(yawReturnSpeed);
         instance.currentPitchReturnSpeed(pitchReturnSpeed);
         instance.currentTimeout(timeout);
         instance.currentPriority(priority);
         instance.currentTask(RotationComponent.RotationTask.AIM);
         instance.targetRotation(target);
         instance.updateRotation(target, yawSpeed, pitchSpeed);
      }
   }

   public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
      update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
   }

   public static void update(Rotation targetRotation, float yawSpeed, float pitchSpeed, float returnSpeed, int timeout, int priority) {
      update(targetRotation, yawSpeed, pitchSpeed, returnSpeed, returnSpeed, timeout, priority, false);
   }

   @Native
   private boolean updateRotation(Rotation rotation, float turnYawSpeed, float turnPitchSpeed) {
      if (mc.player == null) {
         return false;
      } else {
         Rotation currentRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         float yawDelta = MathHelper.wrapDegrees(rotation.getYaw() - currentRotation.getYaw());
         float pitchDelta = rotation.getPitch() - currentRotation.getPitch();
         float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);
         float yawSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(yawDelta / totalDelta) * turnYawSpeed;
         float pitchSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(pitchDelta / totalDelta) * turnPitchSpeed;
         Vec2f rot = applySensitivityPatch(new Vec2f(mc.player.getYaw() + MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed), MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed), -90.0F, 90.0F)), new Vec2f(mc.player.getYaw(), mc.player.getPitch()));
         mc.player.setYaw(rot.x);
         mc.player.setPitch(rot.y);
         Rotation finalRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         this.idleTicks(0);
         return (double)finalRotation.getDelta(rotation) < (this.currentTask.equals(RotationComponent.RotationTask.RESET) ? Math.hypot((double)this.currentYawReturnSpeed, (double)this.currentPitchReturnSpeed) : Math.hypot((double)this.currentYawSpeed, (double)this.currentPitchSpeed));
      }
   }

   @Native
   public void stopRotation() {
      this.currentTask(RotationComponent.RotationTask.IDLE);
      this.currentPriority(0);
      FreeLookComponent.setActive(false);
   }

   public boolean isRotating() {
      return !this.currentTask.equals(RotationComponent.RotationTask.IDLE);
   }

   @Generated
   public RotationComponent.RotationTask currentTask() {
      return this.currentTask;
   }

   @Generated
   public float currentYawSpeed() {
      return this.currentYawSpeed;
   }

   @Generated
   public float currentPitchSpeed() {
      return this.currentPitchSpeed;
   }

   @Generated
   public float currentYawReturnSpeed() {
      return this.currentYawReturnSpeed;
   }

   @Generated
   public float currentPitchReturnSpeed() {
      return this.currentPitchReturnSpeed;
   }

   @Generated
   public int currentPriority() {
      return this.currentPriority;
   }

   @Generated
   public int currentTimeout() {
      return this.currentTimeout;
   }

   @Generated
   public int idleTicks() {
      return this.idleTicks;
   }

   @Generated
   public Rotation targetRotation() {
      return this.targetRotation;
   }

   @Generated
   public RotationComponent currentTask(RotationComponent.RotationTask currentTask) {
      this.currentTask = currentTask;
      return this;
   }

   @Generated
   public RotationComponent currentYawSpeed(float currentYawSpeed) {
      this.currentYawSpeed = currentYawSpeed;
      return this;
   }

   @Generated
   public RotationComponent currentPitchSpeed(float currentPitchSpeed) {
      this.currentPitchSpeed = currentPitchSpeed;
      return this;
   }

   @Generated
   public RotationComponent currentYawReturnSpeed(float currentYawReturnSpeed) {
      this.currentYawReturnSpeed = currentYawReturnSpeed;
      return this;
   }

   @Generated
   public RotationComponent currentPitchReturnSpeed(float currentPitchReturnSpeed) {
      this.currentPitchReturnSpeed = currentPitchReturnSpeed;
      return this;
   }

   @Generated
   public RotationComponent currentPriority(int currentPriority) {
      this.currentPriority = currentPriority;
      return this;
   }

   @Generated
   public RotationComponent currentTimeout(int currentTimeout) {
      this.currentTimeout = currentTimeout;
      return this;
   }

   @Generated
   public RotationComponent idleTicks(int idleTicks) {
      this.idleTicks = idleTicks;
      return this;
   }

   @Generated
   public RotationComponent targetRotation(Rotation targetRotation) {
      this.targetRotation = targetRotation;
      return this;
   }

   public static enum RotationTask {
      AIM,
      RESET,
      IDLE;

      private static RotationComponent.RotationTask[] $values() {
         return new RotationComponent.RotationTask[]{AIM, RESET, IDLE};
      }
   }
}