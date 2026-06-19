package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.utility.render.level.Render3DUtil;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.other.EventTickMovement;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.base.events.impl.player.EventRotation;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.player.AttackUtil;
import wtf.dexum.base.request.ScriptManager;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.combat.rotation.*;
import wtf.dexum.client.modules.impl.movement.AirStuck;
import wtf.dexum.client.modules.impl.movement.AutoSprint;
import wtf.dexum.utility.component.FreeLookComponent;
import wtf.dexum.utility.game.player.MovingUtil;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;
import wtf.dexum.utility.game.player.RaytracingUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.math.MultipointUtils;
import wtf.dexum.utility.math.Timer;
import wtf.dexum.utility.predict.PredictUtils;
import wtf.dexum.utility.component.RotationComponent;

@ModuleAnnotation(
        name = "AttackAura",
        category = Category.COMBAT,
        description = "ёбашит головы тупым ансофтером"
)
public final class Aura extends Module {
    public static final Aura INSTANCE = new Aura();
    private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать", List.of("Игроков", "Мобов", "Животных"));
    public final ModeSetting rotationMode = new ModeSetting("Ротация", new String[0]);
    private final MultiBooleanSetting targetPriority = MultiBooleanSetting.create("Приоритет", List.of("Здоровье", "Дистанция", "Зрение"));
    private final BooleanSetting predictOnElytra = new BooleanSetting("Перегонять противника", true);
    private final ModeSetting predictionType = new ModeSetting("Перегон по", () -> this.predictOnElytra.isEnabled(), "По тикам", "По смещению хитбокса");

    private final ModeSetting.Value modeVanilla;
    private final ModeSetting.Value modeFunTime;
    private final ModeSetting.Value modeUniversal;
    private final ModeSetting.Value modeSloth1;
    private final ModeSetting.Value modeSloth2;
    private final ModeSetting.Value modeReallyWorld;
    private final ModeSetting.Value modeHVH;
    private final ModeSetting.Value modeNeural;
    private final ModeSetting.Value modeFunTimeNeural; // [FunTimeNeural]

    private final VanillaRotation rotVanilla = new VanillaRotation();
    private final SpookytimeRotation rotFunTime = new SpookytimeRotation();
    private final UniversalRotation rotUniversal = new UniversalRotation();
    private final Sloth1Rotation rotSloth1 = new Sloth1Rotation();
    private final Sloth2Rotation rotSloth2 = new Sloth2Rotation();
    private final ReallyWorldRotation rotReallyWorld = new ReallyWorldRotation();
    private final HVHRotation rotHVH = new HVHRotation();
    private final NeuralRotation rotNeural = new NeuralRotation();
    private final FunTimeNeuralRotation rotFunTimeNeural = new FunTimeNeuralRotation(); // [FunTimeNeural]

    private final ModeSetting correction;
    private final ModeSetting.Value correctionFocus;
    private final ModeSetting.Value correctionGood;
    private final ModeSetting.Value correctionNone;
    private final NumberSetting distance;
    private final NumberSetting distanceRotation;
    private final BooleanSetting shieldBreak;
    private final BooleanSetting legitSwap;
    private final BooleanSetting raycastCheck;
    private final BooleanSetting doubleAttack;
    public final NumberSetting predict;
    public final BooleanSetting critsOnlyWithSpace;
    private final BooleanSetting visualElytraRotation;
    private final BooleanSetting visualBackTurn = new BooleanSetting("Визуальный разворот", true, () -> this.predictOnElytra.isEnabled() && this.predictionType.is("По смещению хитбокса"));
    private final BooleanSetting visualizePrediction = new BooleanSetting("Визуализация предсказания", true);
    private final BooleanSetting keepTarget;
    private final BooleanSetting sprintReset;

    private LivingEntity target;
    private Vec3d lastPredictedPoint;
    private final Timer hurtTimer;
    private final ScriptManager.ScriptTask script;
    private int lastSlot;
    public float lastYaw;
    public float lastPitch;
    private int postAttackTicks = 0;
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;
    private int targetLostTicks = 0;
    private Vec3d smoothedAimPoint;
    private Vec3d smoothedTargetVelocity = Vec3d.ZERO;
    private float visualLookYaw;
    private float visualLookPitch;
    private boolean visualLookInitialized;
    private boolean visualBackTurnEngaged;

    private final NumberSetting fov = new NumberSetting("FOV", 180.0F, 5.0F, 360.0F, 1.0F);

    private Aura() {
        this.modeVanilla = new ModeSetting.Value(this.rotationMode, "Vanilla");
        this.modeFunTime = new ModeSetting.Value(this.rotationMode, "Spookytime");
        this.modeUniversal = new ModeSetting.Value(this.rotationMode, "Universal (OLD)");
        this.modeSloth1 = new ModeSetting.Value(this.rotationMode, "Sloth1");
        this.modeSloth2 = new ModeSetting.Value(this.rotationMode, "Sloth2");
        this.modeReallyWorld = new ModeSetting.Value(this.rotationMode, "ReallyWorld");
        this.modeHVH = new ModeSetting.Value(this.rotationMode, "HVH");
        this.modeNeural = new ModeSetting.Value(this.rotationMode, "Neural");
        this.modeFunTimeNeural = new ModeSetting.Value(this.rotationMode, "FunTimeNeural"); // [FunTimeNeural]

        this.correction = new ModeSetting("Коррекция", new String[0]);
        this.correctionFocus = new ModeSetting.Value(this.correction, "Фокус");
        this.correctionGood = (new ModeSetting.Value(this.correction, "Свободная")).select();
        this.correctionNone = new ModeSetting.Value(this.correction, "Нет");
        this.distance = new NumberSetting("Дистанция", 3.0F, 0.5F, 6.0F, 0.1F, "Дистанция атаки");
        this.distanceRotation = new NumberSetting("Дистанция аима", 0.1F, 0.0F, 6.0F, 0.1F);
        this.shieldBreak = new BooleanSetting("Ломать щит", true);
        BooleanSetting var10005 = this.shieldBreak;
        Objects.requireNonNull(var10005);
        this.legitSwap = new BooleanSetting("Легитно ломать", true, var10005::isEnabled);
        this.raycastCheck = new BooleanSetting("Проверка на наведение", false);
        this.doubleAttack = new BooleanSetting("Двойной удар", true);
        this.predict = new NumberSetting("Насколько перегонять", 2.0F, 1.0F, 8.0F, 0.1F, () -> this.predictOnElytra.isEnabled());
        this.critsOnlyWithSpace = new BooleanSetting("Только с пробелом", true);
        this.visualElytraRotation = new BooleanSetting("Визуальная ротация элитр", true);
        this.keepTarget = new BooleanSetting("Удерживать одну цель", true);
        this.sprintReset = new BooleanSetting("Сброс спринта", true);
        this.target = null;
        this.hurtTimer = new Timer();
        this.script = new ScriptManager.ScriptTask();
        this.lastSlot = -1;
    }

    @Native
    private void breakShieldAndAttack() {
        boolean wasSwapped = false;
        boolean wasSwappedInventory = false;
        int slotHotbar = PlayerInventoryUtil.find((List)List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 0, 8);
        int slotInventory = PlayerInventoryUtil.find((List)List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 8, 35);

        if (this.shouldPrepareSprintReset()) return;

        if (slotHotbar != -1 && this.shieldBreak.isEnabled() && this.target.isBlocking()) {
            if (this.legitSwap.isEnabled()) {
                this.lastSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = slotHotbar;
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
            }
            wasSwapped = true;
        }

        if (slotHotbar == -1 && slotInventory != -1 && this.shieldBreak.isEnabled() && this.target.isBlocking()) {
            if (this.legitSwap.isEnabled()) {
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                this.lastSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = 8;
            } else {
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
            }
            wasSwappedInventory = true;
        }

        mc.interactionManager.attackEntity(mc.player, this.target);
        if (this.doubleAttack.isEnabled()) mc.interactionManager.attackEntity(mc.player, this.target);
        mc.player.swingHand(Hand.MAIN_HAND);
        this.postAttackTicks = 7;
        this.resetSprintResetState();

        if (wasSwapped) {
            if (this.legitSwap.isEnabled()) {
                Dexum.getInstance().getScriptManager().addTask(this.script);
                this.script.schedule(EventUpdate.class, (eventUpdate) -> {
                    mc.player.getInventory().selectedSlot = this.lastSlot;
                    this.lastSlot = -1;
                    return true;
                });
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
        }

        if (wasSwappedInventory) {
            if (this.legitSwap.isEnabled()) {
                Dexum.getInstance().getScriptManager().addTask(this.script);
                this.script.schedule(EventUpdate.class, (eventUpdate) -> {
                    mc.player.getInventory().selectedSlot = this.lastSlot;
                    mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                    this.lastSlot = -1;
                    return true;
                });
            } else {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        }
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (this.sprintResetDone) this.sprintResetTicks++;

        LivingEntity newTarget = this.updateTarget();
        if (this.keepTarget.isEnabled() && this.target != null && this.isValid(this.target)) {
            // держим цель
        } else if (newTarget != null) {
            this.target = newTarget;
            this.targetLostTicks = 0;
            // rotNeural.onTargetChange(); // удалён
            rotFunTimeNeural.onTargetChange(); // [FunTimeNeural]
        } else if (this.target != null && !this.isValid(this.target)) {
            this.targetLostTicks++;
            if (this.targetLostTicks > 5) this.target = null;
        }

        if (this.target != null) {
            if (this.isCanAttack() && this.hurtTimer.finished(458L) && !this.target.isBlocking()) {
                if (this.shouldPrepareSprintReset()) return;

                mc.interactionManager.attackEntity(mc.player, this.target);
                mc.player.swingHand(Hand.MAIN_HAND);
                this.postAttackTicks = 7;
                this.resetSprintResetState();
                this.hurtTimer.reset();

                // rotNeural.onAttack(); // удалён
                rotFunTimeNeural.onAttack(); // [FunTimeNeural]
            }
        }

        if (!this.isElytraPredictActive()) this.resetElytraPredictState();
    }

    @EventTarget
    @Native
    public void onTickMovement(EventTickMovement e) {
        if (this.target != null && this.target.isBlocking() && this.hurtTimer.finished(200L)) {
            this.breakShieldAndAttack();
            this.hurtTimer.reset();
        }
    }

    @EventTarget
    @Native
    public void eventRotate(EventGameUpdate e) {
        if (this.target != null) {
            Dexum.getInstance().getModuleManager().setAcceleration(0.0F);
            Vec3d eyes = mc.player.getEyePos();
            boolean elytraPredictActive = this.isElytraPredictActive();
            boolean hitboxOffsetMode = this.predictionType.is("По смещению хитбокса");
            boolean useBackVisual = elytraPredictActive && hitboxOffsetMode && this.visualBackTurn.isEnabled();
            boolean elytraVisual = elytraPredictActive && this.visualElytraRotation.isEnabled() && !useBackVisual;

            Vec3d point = MultipointUtils.getMultipoint(this.target, (double)this.distance.getCurrent());

            if (elytraPredictActive) {
                double distToTarget = mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
                float pingFactor = this.getPlayerPing() / 1000.0F;
                float basePrediction = distToTarget > 8.0D ? 8.0F : this.predict.getCurrent();
                float adjustedPrediction = basePrediction + pingFactor * 2.0F;

                Vec3d targetVel = this.target.getVelocity();
                Vec3d playerVel = mc.player.getVelocity();
                double relativeSpeed = playerVel.subtract(targetVel).horizontalLength();
                if (relativeSpeed > 1.5) adjustedPrediction += (float)(relativeSpeed * 0.3);

                if (this.predictionType.is("По тикам")) {
                    point = PredictUtils.predict(this.target, this.target.getPos(), adjustedPrediction);
                    point = point.add(targetVel.x * pingFactor * 3, targetVel.y * pingFactor, targetVel.z * pingFactor * 3);
                    this.smoothedAimPoint = point;
                } else {
                    this.resetElytraPredictState();
                    this.lastYaw = mc.player.getYaw();
                    this.lastPitch = mc.player.getPitch();
                }
            }

            this.lastPredictedPoint = point;
            Rotation angle = RotationUtil.fromVec3d(point.subtract(eyes));

            if (useBackVisual) this.updateVisualLook(point);
            else this.resetVisualBackTurn();

            RotationBase currentRot = null;
            if (this.modeVanilla.isSelected()) currentRot = rotVanilla;
            else if (this.modeReallyWorld.isSelected()) currentRot = rotReallyWorld;
            else if (this.modeFunTime.isSelected()) currentRot = rotFunTime;
            else if (this.modeUniversal.isSelected()) currentRot = rotUniversal;
            else if (this.modeSloth1.isSelected()) currentRot = rotSloth1;
            else if (this.modeSloth2.isSelected()) currentRot = rotSloth2;
            else if (this.modeHVH.isSelected()) currentRot = rotHVH;
            else if (this.modeNeural.isSelected()) currentRot = rotNeural;
            else if (this.modeFunTimeNeural.isSelected()) currentRot = rotFunTimeNeural; // [FunTimeNeural]

            if (currentRot != null) {
                currentRot.setYaw(this.lastYaw);
                currentRot.setPitch(this.lastPitch);

                if (currentRot instanceof SpookytimeRotation) {
                    ((SpookytimeRotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof ReallyWorldRotation) {
                    ((ReallyWorldRotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof UniversalRotation) {
                    ((UniversalRotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof Sloth1Rotation) {
                    ((Sloth1Rotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof Sloth2Rotation) {
                    ((Sloth2Rotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof NeuralRotation) {
                    ((NeuralRotation) currentRot).update(this.target, angle, elytraVisual);
                } else if (currentRot instanceof FunTimeNeuralRotation) { // [FunTimeNeural]
                    ((FunTimeNeuralRotation) currentRot).update(this.target, angle, elytraVisual);
                } else {
                    currentRot.update(angle, elytraVisual);
                }

                this.lastYaw = currentRot.getYaw();
                this.lastPitch = currentRot.getPitch();
            }
        } else {
            this.resetElytraPredictState();
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
            RotationComponent.update(new Rotation(this.lastYaw, this.lastPitch), 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, false);
        }
    }

    private boolean isElytraPredictContext() {
        return mc.player != null && mc.player.isGliding() && this.target != null && this.target instanceof PlayerEntity && this.target.isGliding();
    }

    private boolean isElytraPredictActive() {
        return this.isEnabled() && this.predictOnElytra.isEnabled() && this.isElytraPredictContext();
    }

    private void resetVisualBackTurn() {
        this.visualLookInitialized = false;
        if (this.visualBackTurnEngaged) {
            FreeLookComponent.setActive(false);
            this.visualBackTurnEngaged = false;
        }
    }

    private void resetElytraPredictState() {
        this.lastPredictedPoint = null;
        this.smoothedAimPoint = null;
        this.smoothedTargetVelocity = Vec3d.ZERO;
        this.resetVisualBackTurn();
    }

    private Vec3d calculateHitboxOffsetPoint(Vec3d eyes, float pingFactor, float adjustedPrediction, double distToTarget) {
        Vec3d velocity = this.target.getVelocity();
        Vec3d playerVel = mc.player.getVelocity();
        Vec3d relativeVel = velocity.subtract(playerVel);
        this.smoothedTargetVelocity = this.smoothedTargetVelocity.multiply(0.55).add(velocity.multiply(0.45));

        double speed = this.smoothedTargetVelocity.horizontalLength();
        double relativeSpeed = relativeVel.horizontalLength();
        Vec3d targetDir;
        if (speed >= 0.05) {
            targetDir = new Vec3d(this.smoothedTargetVelocity.x, this.smoothedTargetVelocity.y * 0.42, this.smoothedTargetVelocity.z).normalize();
        } else {
            targetDir = Vec3d.fromPolar(this.target.getPitch() * 0.35F, this.target.getYaw()).normalize();
        }

        double interceptFactor = MathHelper.clamp(distToTarget / Math.max(relativeSpeed + speed, 0.75), 0.35, 2.5);
        double pingStrength = pingFactor * Math.min(speed * 4.5 + relativeSpeed * 1.15, 5.5);
        double baseStrength = this.predict.getCurrent() * (0.92 + Math.min(distToTarget / 14.0, 0.65));
        double totalStrength = MathHelper.clamp(baseStrength + pingStrength + relativeSpeed * 0.42 + adjustedPrediction * 0.18, 1.25, 10.0);

        Vec3d offset = targetDir.multiply(totalStrength).add(relativeVel.multiply(interceptFactor * (0.85 + pingFactor * 1.35)));
        double maxOffset = Math.max(distToTarget * 0.58, 2.0);
        if (offset.length() > maxOffset) offset = offset.normalize().multiply(maxOffset);

        Vec3d rawPoint = this.target.getBoundingBox().getCenter().add(offset);
        if (this.smoothedAimPoint == null) {
            this.smoothedAimPoint = rawPoint;
        } else {
            double smooth = MathHelper.clamp(0.18 + speed * 0.14 + relativeSpeed * 0.06, 0.18, 0.55);
            this.smoothedAimPoint = new Vec3d(
                    MathHelper.lerp(smooth, this.smoothedAimPoint.x, rawPoint.x),
                    MathHelper.lerp(smooth, this.smoothedAimPoint.y, rawPoint.y),
                    MathHelper.lerp(smooth, this.smoothedAimPoint.z, rawPoint.z)
            );
        }
        return this.smoothedAimPoint;
    }

    private void updateVisualLook(Vec3d aimPoint) {
        if (mc.player == null) return;
        Rotation look = RotationUtil.fromVec3d(aimPoint.subtract(mc.player.getEyePos()));
        if (!this.visualLookInitialized) {
            this.visualLookYaw = mc.gameRenderer.getCamera().getYaw();
            this.visualLookPitch = mc.gameRenderer.getCamera().getPitch();
            this.visualLookInitialized = true;
        }
        float deltaYaw = MathHelper.wrapDegrees(look.getYaw() - this.visualLookYaw);
        float deltaPitch = look.getPitch() - this.visualLookPitch;
        float maxStep = 28.0F;
        deltaYaw = MathHelper.clamp(deltaYaw, -maxStep, maxStep);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxStep * 0.65F, maxStep * 0.65F);
        this.visualLookYaw = MathHelper.wrapDegrees(this.visualLookYaw + deltaYaw);
        this.visualLookPitch = MathHelper.clamp(this.visualLookPitch + deltaPitch, -90.0F, 90.0F);

        FreeLookComponent.setActive(true);
        FreeLookComponent.setFreeYaw(this.visualLookYaw);
        FreeLookComponent.setFreePitch(this.visualLookPitch);
        this.visualBackTurnEngaged = true;

        float backYaw = MathHelper.wrapDegrees(this.visualLookYaw + 180.0F);
        mc.player.bodyYaw = backYaw;
        mc.player.headYaw = backYaw;
        mc.player.prevBodyYaw = backYaw;
        mc.player.prevHeadYaw = backYaw;
    }

    @EventTarget
    private void onCameraRotation(EventRotation event) {
        if (!this.isEnabled() || !this.isElytraPredictActive()) return;
        if (!this.predictionType.is("По смещению хитбокса") || !this.visualBackTurn.isEnabled()) return;
        FreeLookComponent.setActive(true);
        event.setYaw(this.visualLookYaw);
        event.setPitch(this.visualLookPitch);
    }

    private int getPlayerPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return 50;
    }

    private boolean isCanAttack() {
        if (mc.player.getAttackCooldownProgress(0.5F) < 0.9F) return false;
        if (!AttackUtil.canAttack()) return false;

        if (this.critsOnlyWithSpace.isEnabled() && mc.player.isOnGround()) {
            mc.player.jump();
            return false;
        }

        if (AirStuck.INSTANCE.isEnabled() && AirStuck.INSTANCE.frozen) {
            double distToActual = mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
            if (distToActual > 6.0) return false;
        } else if (this.target instanceof PlayerEntity && this.predictOnElytra.isEnabled() && mc.player.isGliding() && this.target.isGliding()) {
            float pingFactor = this.getPlayerPing() / 1000.0F;
            double extraReach = Math.min(pingFactor * mc.player.getVelocity().length() * 2.5, 2.0);
            double maxDist = (double)this.distance.getCurrent() + extraReach;
            double distToPredicted = mc.player.getEyePos().distanceTo(this.lastPredictedPoint != null ? this.lastPredictedPoint : this.target.getBoundingBox().getCenter());
            double distToActual = mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
            if (distToPredicted > maxDist && distToActual > maxDist) return false;
        } else if ((!mc.player.isGliding() || !this.target.isGliding()) && mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(this.target, (double)this.distance.getCurrent())) > (double)this.distance.getCurrent()) {
            return false;
        }

        if (this.raycastCheck.isEnabled()) {
            return RaytracingUtil.rayTrace(mc.player.getRotationVector(), (double)this.distance.getCurrent(), this.target.getBoundingBox()) || mc.targetedEntity != null;
        }

        if (this.target != null) {
            Rotation rot = Rotation.getRotations(this.target.getBoundingBox().getCenter());
            float yawDiff = Math.abs(MathHelper.wrapDegrees(rot.getYaw() - mc.player.getYaw()));
            float pitchDiff = Math.abs(rot.getPitch() - mc.player.getPitch());
            float maxAngle = fov.getCurrent() / 2.0f;
            if (yawDiff > maxAngle || pitchDiff > maxAngle) {
                return false;
            }
        }

        return true;
    }

    private LivingEntity updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player instanceof LivingEntity living && this.isValid(living)) targets.add(living);
        }
        try {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof LivingEntity living && !(entity instanceof PlayerEntity)) {
                    if (this.isValid(living)) targets.add(living);
                }
            }
        } catch (Exception ignored) {}

        if (!targets.isEmpty() && this.isEnabled()) {
            targets.sort(Comparator.comparingDouble((entityx) -> {
                double score = 0;
                if (targetPriority.isEnable("Здоровье")) score += ((LivingEntity)entityx).getHealth();
                if (targetPriority.isEnable("Дистанция")) score += mc.player.squaredDistanceTo(entityx) * 0.1;
                if (targetPriority.isEnable("Зрение")) {
                    Rotation vec = Rotation.getRotations(entityx.getBoundingBox().getCenter());
                    double dy = Math.abs(MathHelper.wrapDegrees(vec.getYaw() - mc.player.getYaw()));
                    double dp = Math.abs(vec.getPitch() - mc.player.getPitch());
                    score += (dy + dp) * 0.5;
                }
                return score;
            }));
            return (LivingEntity)targets.get(0);
        }
        return null;
    }

    public boolean isValid(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0.0F) return false;
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0.0F) return false;

        if (entity instanceof PlayerEntity) {
            if (!this.targetTypeSetting.isEnable("Игроков")) return false;
            if (Dexum.getInstance().getFriendManager().isFriend(entity.getName().getString())) return false;
            if (AntiBot.INSTANCE.isBot((PlayerEntity)entity)) return false;
        }

        if (!(entity instanceof PassiveEntity) && !(entity instanceof FishEntity) || this.targetTypeSetting.isEnable("Животных") && !Dexum.getInstance().getServerHandler().isPvp()) {
            if (!(entity instanceof HostileEntity) && !(entity instanceof AmbientEntity) || this.targetTypeSetting.isEnable("Мобов") && !Dexum.getInstance().getServerHandler().isPvp()) {
                double maxDist = mc.player.isGliding() ? 20.0F : this.distance.getCurrent() + this.distanceRotation.getCurrent();
                if (mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(entity, maxDist)) > maxDist) return false;
                return !(entity instanceof ArmorStandEntity);
            }
        }
        return false;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null || target == null) return;
        int color = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB();
        Render3DUtil.drawBox(target.getBoundingBox(), color, 1.0F);
        if (this.isElytraPredictActive() && this.visualizePrediction.isEnabled() && this.lastPredictedPoint != null) {
            Box box = new Box(lastPredictedPoint.x - 0.3, lastPredictedPoint.y - 0.3, lastPredictedPoint.z - 0.3, lastPredictedPoint.x + 0.3, lastPredictedPoint.y + 0.3, lastPredictedPoint.z + 0.3);
            Render3DUtil.drawBox(box, color, 1.0F);
        }
    }

    @EventTarget
    private void onMoveInput(EventMoveInput eventMoveInput) {
        if (this.needSprintReset) {
            eventMoveInput.setForward(0.0F);
            eventMoveInput.setStrafe(0.0F);
            this.needSprintReset = false;
            this.sprintResetDone = true;
            this.sprintResetTicks = 0;
            mc.player.setSprinting(false);
            return;
        }
        if (!this.correctionNone.isSelected() && this.target != null) {
            if (this.correctionFocus.isSelected()) MovingUtil.fixMovementFocus(eventMoveInput, mc.player.getYaw());
            else MovingUtil.fixMovementFree(eventMoveInput);
        }
    }

    private boolean shouldPrepareSprintReset() {
        if (!this.sprintReset.isEnabled() || !mc.player.isSprinting() || this.shouldSkipSprintResetInWater()) return false;
        if (this.sprintResetDone) return this.sprintResetTicks < 1;
        this.needSprintReset = true;
        AutoSprint.pauseForSprintReset(2);
        return true;
    }

    private boolean shouldSkipSprintResetInWater() {
        return mc.player != null && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) && AutoSprint.INSTANCE.shouldKeepSprintInWater();
    }

    private void resetSprintResetState() {
        this.needSprintReset = false;
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
    }

    public LivingEntity getTarget() {
        return this.isEnabled() ? this.target : null;
    }

    public void onEnable() {
        this.target = null;
        this.lastPredictedPoint = null;
        this.smoothedAimPoint = null;
        this.smoothedTargetVelocity = Vec3d.ZERO;
        this.visualLookInitialized = false;
        this.resetSprintResetState();
        super.onEnable();
    }

    public void onDisable() {
        Dexum.getInstance().getModuleManager().setAcceleration(0.0F);
        this.resetElytraPredictState();
        this.resetSprintResetState();
        super.onDisable();
    }
}