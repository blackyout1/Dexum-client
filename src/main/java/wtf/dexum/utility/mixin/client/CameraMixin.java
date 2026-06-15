package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.base.events.impl.player.EventRotation;
import wtf.dexum.base.events.impl.render.EventCamera;
import wtf.dexum.base.events.impl.render.EventCameraPosition;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.mixin.accessors.CameraAccessor;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3d field_18712;
   @Shadow
   @Final
   private Mutable field_18713;
   @Shadow
   private float field_18718;
   @Shadow
   private float field_18717;

   @Shadow
   protected abstract void method_19325(float var1, float var2);

   @Shadow
   protected abstract void method_19324(float var1, float var2, float var3);

   @Shadow
   protected abstract float method_19318(float var1);

   @Inject(
      method = {"update"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V",
   shift = Shift.AFTER
)},
      cancellable = true
   )
   private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      EventCamera event = new EventCamera(false, 4.0F, new Rotation(this.field_18718, this.field_18717));
      EventManager.call(event);
      Rotation angle = new Rotation(this.field_18718, this.field_18717);
      if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity) {
         ClientPlayerEntity player = (ClientPlayerEntity)focusedEntity;
         if (!player.isSleeping() && thirdPerson) {
            float pitch = angle.getPitch();
            float yaw = angle.getYaw();
            float distance = event.getDistance();
            this.method_19325(yaw, pitch);
            this.method_19324(event.isCameraClip() ? -distance : -this.method_19318(distance), 0.0F, 0.0F);
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"setPos(Lnet/minecraft/util/math/Vec3d;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void posHook(Vec3d pos, CallbackInfo ci) {
      EventCameraPosition event = new EventCameraPosition(pos);
      EventManager.call(event);
      this.field_18712 = pos = event.getPos();
      this.field_18713.set(pos.x, pos.y, pos.z);
      ci.cancel();
   }

   @Redirect(
      method = {"update"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
)
   )
   private void redirectSetRotation(Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
      EventRotation event = new EventRotation(yaw, pitch, tickDelta);
      EventManager.call(event);
      float newYaw = event.getYaw();
      float newPitch = event.getPitch();
      if (thirdPerson && inverseView) {
         newYaw += 180.0F;
         newPitch = -newPitch;
      }

      ((CameraAccessor)instance).setCustomRotation(newYaw, newPitch);
   }
}