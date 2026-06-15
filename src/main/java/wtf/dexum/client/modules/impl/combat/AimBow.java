package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;

import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleAnnotation(
        name = "AimBow",
        category = Category.COMBAT,
        description = "Аим на лук с ротациями как в Aura и предсказанием"
)
public class AimBow extends Module {
    public static final AimBow INSTANCE = new AimBow();
    private final NumberSetting range = new NumberSetting("Дистанция", 60.0f, 10.0f, 100.0f, 1.0f);
    private final NumberSetting fov = new NumberSetting("FOV", 90.0f, 1.0f, 180.0f, 1.0f);
    private final NumberSetting smooth = new NumberSetting("Плавность", 0.5f, 0.05f, 1.0f, 0.05f);
    private final BooleanSetting prediction = new BooleanSetting("Предсказание", true);
    private final NumberSetting predictAmount = new NumberSetting("Предикт", 1.0f, 0.1f, 5.0f, 0.1f);
    private final BooleanSetting visualizeFov = new BooleanSetting("Визуал FOV", true);

    private Entity target;
    private float lastYaw;
    private float lastPitch;
    private float acceleration;
    private boolean isBack;

    public AimBow() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        target = null;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventGameUpdate event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack activeStack = mc.player.getActiveItem();
        boolean holdingBow = mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getOffHandStack().getItem() instanceof BowItem;
        boolean usingBow = (mc.player.isUsingItem() && activeStack.getItem() instanceof BowItem) || (holdingBow && mc.options.useKey.isPressed());

        if (!usingBow) {
            target = null;
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            acceleration = 0;
            return;
        }

        Entity newTarget = findTarget();
        if (newTarget != target) {
            target = newTarget;
            acceleration = 0;
        }

        if (target == null) return;

        double velocity = getBowVelocity();
        Vec3d bestPos = getBestPoint(target, velocity);

        if (bestPos == null) return;

        Rotation angle = RotationUtil.fromVec3d(bestPos.subtract(mc.player.getEyePos()));

        float correctedPitch = solveBallistics(target, bestPos, (float)velocity);
        if (!Float.isNaN(correctedPitch)) {
            angle = new Rotation(angle.getYaw(), correctedPitch);
        }

        applyRotation(angle);
    }

    private double getBowVelocity() {
        int useTicks = mc.player.getItemUseTime();
        float pullProgress = BowItem.getPullProgress(useTicks);
        if (pullProgress < 0.1f) return 3.0;
        return pullProgress * 3.0f;
    }

    private Vec3d getBestPoint(Entity target, double velocity) {
        double dist = mc.player.distanceTo(target);
        float ping = 0;
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() / 1000.0f * 20.0f;
        }

        double ticks = (dist / velocity) + ping;

        double vX = (target.getX() - target.prevX);
        double vY = (target.getY() - target.prevY);
        double vZ = (target.getZ() - target.prevZ);

        Vec3d targetVel = new Vec3d(vX, vY, vZ);
        if (targetVel.lengthSquared() < 0.0001) {
            targetVel = target.getVelocity();
        }

        Vec3d pos = target.getPos().add(0, target.getHeight() * 0.45, 0);

        if (prediction.isEnabled()) {
            double amount = predictAmount.getCurrent();

            for (int i = 0; i < 3; i++) {
                double travelTime = simulateTicks(pos, velocity);
                ticks = (travelTime + ping) * amount;

                double predX = targetVel.x * ticks;
                double predY = targetVel.y * ticks;
                double predZ = targetVel.z * ticks;

                if (!target.isOnGround() && Math.abs(targetVel.y) > 0.01) {
                    double gravY = predY;
                    for (int t = 0; t < (int)ticks; t++) {
                        gravY -= 0.08;
                        gravY *= 0.98;
                    }
                    predY = gravY;
                }

                pos = target.getPos().add(predX, predY, predZ).add(0, target.getHeight() * 0.45, 0);
            }
        }

        return pos;
    }

    private double simulateTicks(Vec3d targetPos, double velocity) {
        double x = Math.sqrt(Math.pow(targetPos.x - mc.player.getX(), 2) + Math.pow(targetPos.z - mc.player.getZ(), 2));
        double motX = velocity * 0.99;
        int ticks = 0;
        double currentX = 0;
        while (currentX < x && ticks < 100) {
            currentX += motX;
            motX *= 0.99;
            ticks++;
        }
        return ticks;
    }

    private float solveBallistics(Entity target, Vec3d targetPos, float velocity) {
        double x = Math.sqrt(Math.pow(targetPos.x - mc.player.getX(), 2) + Math.pow(targetPos.z - mc.player.getZ(), 2));
        double y = targetPos.y - mc.player.getEyePos().y;

        float bestPitch = (float) -Math.toDegrees(Math.atan2(y, x));

        Vec3d playerVel = mc.player.getVelocity();

        for (int i = 0; i < 20; i++) {
            double simX = 0;
            double simY = 0;

            float radPitch = (float) Math.toRadians(bestPitch);
            float radYaw = (float) Math.toRadians(mc.player.getYaw());

            double motX = velocity * Math.cos(radPitch);
            double motY = -velocity * Math.sin(radPitch);

            double playerXVel = Math.sqrt(playerVel.x * playerVel.x + playerVel.z * playerVel.z);

            double relativePlayerVel = playerVel.x * -Math.sin(radYaw) + playerVel.z * Math.cos(radYaw);

            motX += relativePlayerVel;
            if (!mc.player.isOnGround()) motY += playerVel.y;

            boolean hit = false;
            for (int t = 0; t < 150; t++) {
                simX += motX;
                simY += motY;

                motX *= 0.99;
                motY *= 0.99;
                motY -= 0.05;

                if (simX >= x) {
                    double error = y - simY;
                    bestPitch -= (float) (error * 1.2);
                    hit = true;
                    break;
                }

                if (simY < y - 10) break;
            }
            if (!hit) break;
        }

        return bestPitch;
    }

    @EventTarget
    public void onRender(EventHudRender event) {
        if (mc.player == null || !visualizeFov.isEnabled()) return;

        boolean holdingBow = mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getOffHandStack().getItem() instanceof BowItem;
        if (!holdingBow) return;

        float centerX = (float) mc.getWindow().getScaledWidth() / 2;
        float centerY = (float) mc.getWindow().getScaledHeight() / 2;

        float gameFov = mc.options.getFov().getValue().floatValue();
        float radius = (fov.getCurrent() / gameFov) * (mc.getWindow().getScaledWidth() / 2f);

        DrawUtil.drawRoundedBorder(event.getContext().getMatrices(), centerX - radius, centerY - radius, radius * 2, radius * 2, 1.0f, BorderRadius.all(radius), new ColorRGBA(255, 255, 255, 100));
    }

    private void applyRotation(Rotation angle) {
        SecureRandom rng = new SecureRandom();
        float yawDiff = MathHelper.wrapDegrees(angle.getYaw() - lastYaw);
        float pitchDiff = angle.getPitch() - lastPitch;

        float smoothVal = smooth.getCurrent();

        if (acceleration < 1.0f) {
            acceleration += 0.15f * smoothVal;
        }

        float currentAcc = MathHelper.clamp(acceleration, 0.0F, 1.0F);
        float smoothFactor = MathHelper.clamp(currentAcc * smoothVal, 0.01f, 1.0f);

        float microYawNoise = (rng.nextFloat() - 0.5F) * 0.01F;
        float microPitchNoise = (rng.nextFloat() - 0.5F) * 0.005F;

        float newYaw = lastYaw + yawDiff * smoothFactor + microYawNoise;
        float newPitch = lastPitch + pitchDiff * smoothFactor + microPitchNoise;

        float gcd = Rotation.gcd();
        newYaw -= (newYaw - lastYaw) % gcd;
        newPitch -= (newPitch - lastPitch) % gcd;

        Rotation smoothRot = new Rotation(newYaw, newPitch);
        RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);

        lastYaw = smoothRot.getYaw();
        lastPitch = smoothRot.getPitch();
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        List<Entity> entities = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isRemoved() || !player.isAlive()) continue;
            if (player.distanceTo(mc.player) > range.getCurrent()) continue;
            if (Dexum.getInstance().getFriendManager().isFriend(player.getName().getString())) continue;

            Rotation rots = RotationUtil.fromVec3d(player.getBoundingBox().getCenter().subtract(mc.player.getEyePos()));
            float yawDiff = Math.abs(MathHelper.wrapDegrees(rots.getYaw() - mc.player.getYaw()));
            float pitchDiff = Math.abs(MathHelper.wrapDegrees(rots.getPitch() - mc.player.getPitch()));

            if (yawDiff <= fov.getCurrent() && pitchDiff <= fov.getCurrent()) {
                entities.add(player);
            }
        }

        if (entities.isEmpty()) return null;

        entities.sort(Comparator.comparingDouble(e -> e.distanceTo(mc.player)));
        return entities.get(0);
    }
}