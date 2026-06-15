package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ModuleAnnotation(name = "Velocity", category = Category.COMBAT, description = "Убирает или уменьшает отдачу")
public class Velocity extends Module implements IMinecraft {
    public static final Velocity INSTANCE = new Velocity();

    private final ModeSetting mode = new ModeSetting("Mode", "GrimAC", "GrimAC", "Freeze", "Cancel");
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0f, 0f, 100f, 1f);
    private final NumberSetting vertical = new NumberSetting("Vertical", 100f, 0f, 100f, 1f);
    private final NumberSetting freezeTime = new NumberSetting("Freeze Time", 500f, 100f, 2000f, 50f);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on Ground", false);

    private final Timer freezeTimer = new Timer();
    private boolean isFreezing = false;
    private final List<Packet<?>> queuedPackets = new ArrayList<>();
    private int velocityTicks = 0;

    public Velocity() {
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isFreezing = false;
        sendQueuedPackets();
        velocityTicks = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (isFreezing) {
            if (freezeTimer.finished((long) freezeTime.getCurrent())) {
                isFreezing = false;
                sendQueuedPackets();
            } else {
                mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            }
        }

        if (velocityTicks > 0) {
            if (mode.is("Cancel") || (mode.is("GrimAC") && horizontal.getCurrent() == 0)) {

                mc.player.setVelocity(0, mc.player.getVelocity().y, 0);

                if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
                    mc.player.setSprinting(true);
                }
            }
            velocityTicks--;
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null) return;

        if (event.isReceive()) {
            Packet<?> p = event.getPacket();
            if (p instanceof EntityVelocityUpdateS2CPacket packet) {
                if (packet.getEntityId() == mc.player.getId()) {
                    handleVelocity(event);
                }
            } else if (p instanceof ExplosionS2CPacket) {
                handleVelocity(event);
            }
        } else if (event.isSent()) {
            if (mode.is("Freeze") && isFreezing) {
                Packet<?> p = event.getPacket();
                if (p instanceof PlayerMoveC2SPacket) {
                    queuedPackets.add(p);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleVelocity(EventPacket event) {
        Packet<?> p = event.getPacket();

        if (onlyOnGround.isEnabled() && !mc.player.isOnGround() && mc.player.fallDistance > 0.1) return;

        double hMult = horizontal.getCurrent() / 100.0;
        double vMult = vertical.getCurrent() / 100.0;

        switch (mode.get()) {
            case "Cancel" -> {

                if (p instanceof EntityVelocityUpdateS2CPacket packet) {
                    event.setPacket(new EntityVelocityUpdateS2CPacket(packet.getEntityId(), Vec3d.ZERO));
                } else if (p instanceof ExplosionS2CPacket explosion) {
                    event.setPacket(new ExplosionS2CPacket(
                        explosion.center(),
                        Optional.of(Vec3d.ZERO),
                        explosion.explosionParticle(),
                        explosion.explosionSound()
                    ));
                }
                velocityTicks = 5;
            }
            case "GrimAC" -> {
                if (p instanceof EntityVelocityUpdateS2CPacket packet) {
                    double xVel = (packet.getVelocityX() / 8000.0D) * hMult;
                    double yVel = (packet.getVelocityY() / 8000.0D) * vMult;
                    double zVel = (packet.getVelocityZ() / 8000.0D) * hMult;

                    if (hMult == 0) {
                        xVel = (packet.getVelocityX() / 8000.0D) * 0.001;
                        zVel = (packet.getVelocityZ() / 8000.0D) * 0.001;
                    }

                    event.setPacket(new EntityVelocityUpdateS2CPacket(packet.getEntityId(), new Vec3d(xVel, yVel, zVel)));
                    velocityTicks = 1;
                } else if (p instanceof ExplosionS2CPacket explosion) {
                    Optional<Vec3d> knockback = explosion.playerKnockback();
                    if (knockback.isPresent()) {
                        Vec3d oldKb = knockback.get();
                        double xVel = oldKb.x * hMult;
                        double yVel = oldKb.y * vMult;
                        double zVel = oldKb.z * hMult;

                        if (hMult == 0) {
                            xVel = oldKb.x * 0.001;
                            zVel = oldKb.z * 0.001;
                        }

                        event.setPacket(new ExplosionS2CPacket(
                            explosion.center(),
                            Optional.of(new Vec3d(xVel, yVel, zVel)),
                            explosion.explosionParticle(),
                            explosion.explosionSound()
                        ));
                        velocityTicks = 1;
                    }
                }
            }
            case "Freeze" -> {
                isFreezing = true;
                freezeTimer.reset();
                event.setCancelled(true);
            }
        }
    }

    private void sendQueuedPackets() {
        if (queuedPackets.isEmpty() || mc.getNetworkHandler() == null) {
            queuedPackets.clear();
            return;
        }
        for (Packet<?> packet : queuedPackets) {
            mc.getNetworkHandler().sendPacket(packet);
        }
        queuedPackets.clear();
    }
}