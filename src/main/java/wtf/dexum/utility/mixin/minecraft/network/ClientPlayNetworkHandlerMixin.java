package wtf.dexum.utility.mixin.minecraft.network;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.input.EventChatSend;
import wtf.dexum.base.events.impl.player.EventPickupItem;

@Mixin({ClientPlayNetworkHandler.class})
public class ClientPlayNetworkHandlerMixin {
   @Unique
   private boolean dexum$chatEventReentry;

   @Inject(
      method = {"sendChatMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {
      if (!this.dexum$chatEventReentry) {
         EventChatSend event = new EventChatSend(message);
         EventManager.call(event);
         if (event.isCancelled()) {
            ci.cancel();
            return;
         }

         if (!event.getMessage().equals(message)) {
            ci.cancel();
            this.dexum$chatEventReentry = true;

            try {
               ((ClientPlayNetworkHandler)(Object)this).sendChatMessage(event.getMessage());
            } finally {
               this.dexum$chatEventReentry = false;
            }

            return;
         }
      }

      if (message.startsWith(Dexum.getInstance().getCommandManager().getPrefix())) {
         try {
            Dexum.getInstance().getCommandManager().getDispatcher().execute(message.substring(Dexum.getInstance().getCommandManager().getPrefix().length()), Dexum.getInstance().getCommandManager().getSource());
         } catch (CommandSyntaxException var4) {
         }

         ci.cancel();
      }

   }

   @Inject(
      method = {"onEntitySpawn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
      if (packet.getEntityId() == 12345) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"onItemPickupAnimation"},
      at = {@At(
   value = "INVOKE_ASSIGN",
   target = "Lnet/minecraft/entity/ItemEntity;getStack()Lnet/minecraft/item/ItemStack;"
)}
   )
   private void onPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
      ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler)(Object)this;
      if (handler.getWorld() != null) {
         Entity var6 = handler.getWorld().getEntityById(packet.getCollectorEntityId());
         if (var6 instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)var6;
            Entity var7 = handler.getWorld().getEntityById(packet.getEntityId());
               if (var7 instanceof ItemEntity) {
                  ItemEntity itemEntity = (ItemEntity)var7;
                  ItemStack stack = itemEntity.getStack();
                  EventManager.call(new EventPickupItem(living, stack));
               }
            }
         }
   }
}