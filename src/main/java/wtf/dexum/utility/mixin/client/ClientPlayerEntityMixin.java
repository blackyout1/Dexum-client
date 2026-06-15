package wtf.dexum.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventCloseScreen;
import wtf.dexum.base.events.impl.player.EventMotion;
import wtf.dexum.base.events.impl.player.EventMove;
import wtf.dexum.base.events.impl.player.EventSlowWalking;
import wtf.dexum.base.events.impl.player.EventSprintUpdate;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.impl.player.NoPush;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Shadow
   private float field_3941;
   @Shadow
   @Final
   protected MinecraftClient field_3937;
   @Final
   @Shadow
   public ClientPlayNetworkHandler field_3944;
   @Shadow
   private double field_3926;
   @Shadow
   private double field_3940;
   @Shadow
   private double field_3924;
   @Shadow
   private float field_3925;
   @Shadow
   private boolean field_3920;
   @Shadow
   private boolean field_53040;
   @Shadow
   private boolean field_3927;
   @Shadow
   private int field_3923;
   @Unique
   private final EventMotion event = new EventMotion(0.0F, 0.0F);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Shadow
   protected abstract void method_46742();

   @Shadow
   protected boolean method_3134() {
      return false;
   }

   @Shadow
   protected abstract void method_3148(float var1, float var2);

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventManager.call(new EventUpdate());
      Dexum.getInstance().getScriptManager().updateTick(null);
   }

   @Redirect(
      method = {"sendMovementPackets"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendSprintingPacket()V"
)
   )
   public void invokeSprintUpdate(ClientPlayerEntity instance) {
      EventSprintUpdate eventSprintUpdate = new EventSprintUpdate();
      EventManager.call(eventSprintUpdate);
      if (!eventSprintUpdate.isCancelled()) {
         this.method_46742();
      }

   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
      if (NoPush.INSTANCE.isEnabled() && NoPush.INSTANCE.getObjects().isEnable("Блоки")) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"tickMovement"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
),
      require = 0
   )
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      if (player.isUsingItem()) {
         EventSlowWalking slowDownEvent = new EventSlowWalking();
         EventManager.call(slowDownEvent);
         return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
      } else {
         return player.isUsingItem() && player.getVehicle() == null;
      }
   }

   @Inject(
      method = {"closeHandledScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo info) {
      EventCloseScreen event = new EventCloseScreen(this.field_3937.currentScreen);
      EventManager.call(event);
      if (event.isCancelled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"move"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
)},
      cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      EventMove event = new EventMove(movement);
      EventManager.call(event);
      double d = this.getX();
      double e = this.getZ();
      super.move(movementType, event.getMovePos());
      this.method_3148((float)(this.getX() - d), (float)(this.getZ() - e));
      ci.cancel();
   }

   @Overwrite
   public void method_3136() {
      this.method_46742();
      if (this.method_3134()) {
         this.event.setYaw(this.getYaw());
         this.event.setPitch(this.getPitch());
         EventManager.call(this.event);
         if (this.event.isCancelled()) {
            this.event.setCancelled(false);
            return;
         }

         double d = this.getX() - this.field_3926;
         double e = this.getY() - this.field_3940;
         double f = this.getZ() - this.field_3924;
         double g = (double)(this.event.getYaw() - this.field_3941);
         double h = (double)(this.event.getPitch() - this.field_3925);
         ++this.field_3923;
         boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4D) || this.field_3923 >= 20;
         boolean bl2 = g != 0.0D || h != 0.0D;
         if (bl && bl2) {
            this.field_3944.sendPacket(new Full(this.getX(), this.getY(), this.getZ(), this.event.getYaw(), this.event.getPitch(), this.isOnGround(), this.horizontalCollision));
         } else if (bl) {
            this.field_3944.sendPacket(new PositionAndOnGround(this.getX(), this.getY(), this.getZ(), this.isOnGround(), this.horizontalCollision));
         } else if (bl2) {
            this.field_3944.sendPacket(new LookAndOnGround(this.event.getYaw(), this.event.getPitch(), this.isOnGround(), this.horizontalCollision));
         } else if (this.field_3920 != this.isOnGround() || this.field_53040 != this.horizontalCollision) {
            this.field_3944.sendPacket(new OnGroundOnly(this.isOnGround(), this.horizontalCollision));
         }

         if (bl) {
            this.field_3926 = this.getX();
            this.field_3940 = this.getY();
            this.field_3924 = this.getZ();
            this.field_3923 = 0;
         }

         if (bl2) {
            this.field_3941 = this.event.getYaw();
            this.field_3925 = this.event.getPitch();
         }

         this.field_3920 = this.isOnGround();
         this.field_53040 = this.horizontalCollision;
         this.field_3927 = (Boolean)this.field_3937.options.getAutoJump().getValue();
      }

   }
}