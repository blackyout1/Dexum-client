package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;

import java.util.Comparator;
import java.util.stream.StreamSupport;

@ModuleAnnotation(
    name = "PearlTarget",
    category = Category.COMBAT,
    description = "Throws an ender pearl at the same target as the nearest entity"
)
public final class PearlTarget extends Module {
    public static final PearlTarget INSTANCE = new PearlTarget();

    private PearlTarget() {
    }

    @EventTarget
    private void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null || !this.isEnabled()) return;

        if (e.isReceive() && e.getPacket() instanceof EntitySpawnS2CPacket packet) {
            if (packet.getEntityType() == EntityType.ENDER_PEARL) {
                Vec3d pearlPos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

                LivingEntity thrower = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                        .filter(entity -> entity instanceof LivingEntity && entity != mc.player)
                        .map(entity -> (LivingEntity) entity)
                        .min(Comparator.comparingDouble(entity -> entity.getPos().distanceTo(pearlPos)))
                        .orElse(null);

                if (thrower != null && thrower.getPos().distanceTo(pearlPos) < 5.0) {

                    Vec3d velocity = new Vec3d(packet.getVelocityX(), packet.getVelocityY(), packet.getVelocityZ());

                    Vec3d targetPoint = pearlPos.add(velocity.normalize().multiply(100.0));

                    double diffX = targetPoint.x - mc.player.getX();
                    double diffY = targetPoint.y - (mc.player.getY() + mc.player.getStandingEyeHeight());
                    double diffZ = targetPoint.z - mc.player.getZ();
                    double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                    float yaw = (float) net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
                    float pitch = (float) net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(diffY, diffXZ)));

                    Vec3d start = mc.player.getEyePos();
                    Vec3d dirVec = Vec3d.fromPolar(pitch, yaw);
                    Vec3d end = start.add(dirVec.multiply(1.5));

                    boolean blocked = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                            .anyMatch(entity -> entity instanceof LivingEntity && entity != mc.player && entity != thrower &&
                                      entity.getBoundingBox().expand(0.1).raycast(start, end).isPresent());

                    if (blocked) return;

                    int pearlSlot = PlayerInventoryUtil.find(Items.ENDER_PEARL, 0, 8);
                    if (pearlSlot != -1) {
                        int oldSlot = mc.player.getInventory().selectedSlot;

                        mc.player.getInventory().selectedSlot = pearlSlot;
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));

                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), true));
                        RotationComponent.update(new Rotation(yaw, pitch), 360F, 360F, 360F, 360F, 0, 100, true);

                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

                        mc.player.getInventory().selectedSlot = oldSlot;
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                    }
                }
            }
        }
    }
}