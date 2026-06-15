package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.render.level.Render3DUtil;

@ModuleAnnotation(
        name = "CrystallAura",
        category = Category.COMBAT,
        description = "CrystallAura"
) public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private final BooleanSetting autoPlace = new BooleanSetting("Размещение", true);
    private final BooleanSetting autoBreak = new BooleanSetting("Взрыв", true);
    private final BooleanSetting autoObsidian = new BooleanSetting("Ставить обсидиан", true);
    private final BooleanSetting undermine = new BooleanSetting("Подкоп", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", true);
    private final BooleanSetting antiFriend = new BooleanSetting("Не бахать друзей", true);
    private final BooleanSetting noPlayer = new BooleanSetting("Не взрывать себя", true);
    private final BooleanSetting onlyAirborne = new BooleanSetting("Только в воздухе", false);
    private final BooleanSetting highlight = new BooleanSetting("Подсветка", true);
    private final NumberSetting range = new NumberSetting("Range", 4.5f, 1.0f, 6.0f, 0.1f);
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", 0f, 0f, 20f, 1f);
    private final NumberSetting breakDelay = new NumberSetting("Break Delay", 0f, 0f, 20f, 1f);
    private final NumberSetting minDamage = new NumberSetting("Min Damage", 4.0f, 0.0f, 36.0f, 0.5f);
    private final NumberSetting maxSelfDamage = new NumberSetting("Max Self Damage", 10.0f, 0.0f, 36.0f, 0.5f);

    private float rotationYaw;
    private float rotationPitch;
    private boolean rotating = false;
    private EndCrystalEntity targetCrystal = null;
    private BlockPos targetPos = null;
    private int breakTimer = 0;
    private int placeTimer = 0;

    private BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;
    private int oldSlot = -1;
    private boolean crystalSlotSwapped = false;
    private PlayerEntity currentTarget = null;
    private BlockPos renderPlacePos = null;
    private EndCrystalEntity renderCrystal = null;
    private BlockPos underminePos = null;
    private BlockPos renderUnderminePos = null;

    private CrystalAura() {}

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            rotationYaw = mc.player.getYaw();
            rotationPitch = mc.player.getPitch();
            oldSlot = mc.player.getInventory().selectedSlot;
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = findTarget();
        if (target == null) {
            rotating = false;
            currentTarget = null;
            renderPlacePos = null;
            renderCrystal = null;
            renderUnderminePos = null;
            return;
        }

        currentTarget = target;
        boolean brokeThisTick = false;

        if (autoBreak.isEnabled()) {
            targetCrystal = findBestCrystal(target);
            renderCrystal = targetCrystal;

            if (targetCrystal != null) {
                boolean canBreak = !onlyAirborne.isEnabled() || !target.isOnGround();

                if (canBreak) {
                    calcGrimRotations(targetCrystal.getBoundingBox().getCenter());

                    if (rotating) {
                        RotationComponent.update(new Rotation(rotationYaw, rotationPitch), 360F, 360F, 360F, 360F, 0, 10, false);
                    }

                    if (breakTimer >= breakDelay.getCurrent()) {
                        attackCrystal(targetCrystal);
                        breakTimer = 0;
                        brokeThisTick = true;
                    } else {
                        breakTimer++;
                    }
                }
            }
        }

        if (autoPlace.isEnabled()) {
            targetPos = findBestPlacePos(target);
            renderPlacePos = targetPos;

            if (undermine.isEnabled() && targetPos == null && targetCrystal == null) {
                BlockPos minePos = findUnderminePos(target);
                renderUnderminePos = minePos;
                if (minePos != null) {
                    mineBlock(minePos);
                }
            } else {
                renderUnderminePos = null;
            }

            if (targetPos == null && autoObsidian.isEnabled() && targetCrystal == null) {
                BlockPos obsidianPos = findBestObsidianPos(target);
                if (obsidianPos != null) {
                    int obsidianSlot = PlayerInventoryUtil.find(Items.OBSIDIAN, 0, 8);
                    if (obsidianSlot != -1) {
                        calcGrimRotations(obsidianPos.toCenterPos().add(0, 0.5, 0));
                        if (rotating) {
                            RotationComponent.update(new Rotation(rotationYaw, rotationPitch), 360F, 360F, 360F, 360F, 0, 10, false);
                        }

                        if (placeTimer >= placeDelay.getCurrent()) {
                            placeBlock(obsidianPos, obsidianSlot);
                            placeTimer = 0;
                        } else {
                            placeTimer++;
                        }
                        return;
                    }
                }
            }

            if (targetPos != null && (targetCrystal == null || brokeThisTick)) {
                if (lastPlacedPos != null && lastPlacedPos.equals(targetPos) &&
                        System.currentTimeMillis() - lastPlaceTime < 50) {
                    return;
                }

                if (!hasCrystalInHand()) {
                    switchToCrystal();
                }

                if (hasCrystalInHand()) {
                    Vec3d placeVec = targetPos.toCenterPos().add(0, 1, 0);
                    calcGrimRotations(placeVec);

                    if (rotating) {
                        RotationComponent.update(new Rotation(rotationYaw, rotationPitch), 360F, 360F, 360F, 360F, 0, 10, false);
                    }

                    if (placeTimer >= placeDelay.getCurrent()) {
                        placeCrystal(targetPos);
                        restoreSlot();
                        lastPlacedPos = targetPos;
                        lastPlaceTime = System.currentTimeMillis();
                        placeTimer = 0;
                    } else {
                        placeTimer++;
                    }
                }
            } else {

                restoreSlot();
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null || !highlight.isEnabled()) return;

        if (renderPlacePos != null) {
            Box placeBox = new Box(renderPlacePos);
            Render3DUtil.drawBox(placeBox, 0x6000FF00, 1.5f);
        }

        if (renderCrystal != null && renderCrystal.isAlive()) {
            float tickDelta = event.getPartialTicks();
            double x = MathHelper.lerp(tickDelta, renderCrystal.lastRenderX, renderCrystal.getX());
            double y = MathHelper.lerp(tickDelta, renderCrystal.lastRenderY, renderCrystal.getY());
            double z = MathHelper.lerp(tickDelta, renderCrystal.lastRenderZ, renderCrystal.getZ());
            Box crystalBox = new Box(x - 0.5, y, z - 0.5, x + 0.5, y + 1.0, z + 0.5);
            Render3DUtil.drawBox(crystalBox, 0x60FF0000, 1.5f);
        }

        if (renderUnderminePos != null) {
            Box undermineBox = new Box(renderUnderminePos);
            Render3DUtil.drawBox(undermineBox, 0x60FFA500, 1.5f);
        }
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isDead() || player.getHealth() <= 0) continue;

            if (Dexum.INSTANCE.getFriendManager().isFriend(player.getName().getString())) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > range.getCurrent() + 2) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    private EndCrystalEntity findBestCrystal(PlayerEntity target) {
        if (mc.world == null || mc.player == null) return null;

        EndCrystalEntity bestCrystal = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            double distance = mc.player.distanceTo(crystal);
            if (distance > range.getCurrent()) continue;

            float targetDamage = calculateDamage(crystal.getPos(), target);
            if (targetDamage < minDamage.getCurrent()) continue;

            if (antiFriend.isEnabled() && wouldHurtFriend(crystal.getPos())) continue;

            if (noPlayer.isEnabled()) {
                float selfDamage = calculateDamage(crystal.getPos(), mc.player);
                float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

                if (selfDamage >= health - 2.0f) continue;
                if (selfDamage > maxSelfDamage.getCurrent()) continue;

                float score = targetDamage - (selfDamage * 0.5f);
                if (score > bestScore) {
                    bestScore = score;
                    bestCrystal = crystal;
                }
            } else {
                if (targetDamage > bestScore) {
                    bestScore = targetDamage;
                    bestCrystal = crystal;
                }
            }
        }

        return bestCrystal;
    }

    private BlockPos findBestPlacePos(PlayerEntity target) {
        if (mc.world == null || mc.player == null || target == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestPos = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        int r = (int) Math.ceil(range.getCurrent());

        for (int x = -r; x <= r; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (!canPlaceCrystal(pos)) continue;

                    double distToPlayer = mc.player.getPos().distanceTo(pos.toCenterPos());
                    if (distToPlayer > range.getCurrent()) continue;

                    Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

                    float targetDamage = calculateDamage(crystalPos, target);
                    if (targetDamage < minDamage.getCurrent()) continue;

                    if (antiFriend.isEnabled() && wouldHurtFriend(crystalPos)) continue;

                    if (noPlayer.isEnabled()) {
                        float selfDamage = calculateDamage(crystalPos, mc.player);
                        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

                        if (selfDamage >= health - 2.0f) continue;
                        if (selfDamage > maxSelfDamage.getCurrent()) continue;

                        float score = targetDamage - (selfDamage * 0.5f);

                        if (score > bestScore) {
                            bestScore = score;
                            bestPos = pos;
                        }
                    } else {
                        if (targetDamage > bestScore) {
                            bestScore = targetDamage;
                            bestPos = pos;
                        }
                    }
                }
            }
        }

        return bestPos;
    }

    private BlockPos findBestObsidianPos(PlayerEntity target) {
        if (mc.world == null || mc.player == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestPos = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        int r = (int) Math.ceil(range.getCurrent());

        for (int x = -r; x <= r; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (mc.player.getPos().distanceTo(pos.toCenterPos()) > range.getCurrent()) continue;

                    if (!mc.world.getBlockState(pos).isReplaceable()) continue;

                    if (!mc.world.isAir(pos.up())) continue;
                    if (!mc.world.isAir(pos.up(2))) continue;

                    boolean hasNeighbor = false;
                    for (Direction d : Direction.values()) {
                        if (!mc.world.getBlockState(pos.offset(d)).isReplaceable()) {
                            hasNeighbor = true;
                            break;
                        }
                    }
                    if (!hasNeighbor) continue;

                    Box box = new Box(pos.up()).expand(0, 1, 0);
                    boolean blocked = false;
                    for (Entity entity : mc.world.getOtherEntities(null, box)) {
                        if (entity instanceof EndCrystalEntity) continue;
                        blocked = true;
                        break;
                    }
                    if (blocked) continue;

                    Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    float targetDamage = calculateDamage(crystalPos, target);
                    if (targetDamage < minDamage.getCurrent()) continue;

                    if (antiFriend.isEnabled() && wouldHurtFriend(crystalPos)) continue;

                    if (noPlayer.isEnabled()) {
                        float selfDamage = calculateDamage(crystalPos, mc.player);
                        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

                        if (selfDamage >= health - 2.0f) continue;
                        if (selfDamage > maxSelfDamage.getCurrent()) continue;

                        float score = targetDamage - (selfDamage * 0.5f);
                        if (score > bestScore) {
                            bestScore = score;
                            bestPos = pos;
                        }
                    } else {
                        if (targetDamage > bestScore) {
                            bestScore = targetDamage;
                            bestPos = pos;
                        }
                    }
                }
            }
        }

        return bestPos;
    }

    private BlockPos findUnderminePos(PlayerEntity target) {
        if (mc.world == null || mc.player == null) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestPos = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        int r = (int) Math.ceil(range.getCurrent());

        for (int x = -r; x <= r; x++) {
            for (int y = -3; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (mc.player.getPos().distanceTo(pos.toCenterPos()) > range.getCurrent()) continue;

                    BlockState state = mc.world.getBlockState(pos);

                    if (state.isAir()) continue;
                    if (state.isOf(Blocks.OBSIDIAN)) continue;
                    if (state.isOf(Blocks.BEDROCK)) continue;
                    if (state.getHardness(mc.world, pos) < 0) continue;

                    BlockPos below = pos.down();
                    boolean useful = false;

                    if (mc.world.getBlockState(below).isOf(Blocks.OBSIDIAN) ||
                            mc.world.getBlockState(below).isOf(Blocks.BEDROCK)) {

                        BlockPos above = pos.up();
                        if (mc.world.isAir(above) || pos.equals(below.up(2))) {
                            useful = true;
                        }
                    }

                    if (!useful && mc.world.getBlockState(below).isReplaceable()) {

                        useful = mc.world.isAir(pos.up());
                    }

                    BlockPos twoBelow = pos.down(2);
                    if (!useful && (mc.world.getBlockState(twoBelow).isOf(Blocks.OBSIDIAN) ||
                            mc.world.getBlockState(twoBelow).isOf(Blocks.BEDROCK))) {
                        if (mc.world.isAir(pos.down())) {
                            useful = true;
                        }
                    }

                    if (!useful) continue;

                    BlockPos obsBase = mc.world.getBlockState(below).isOf(Blocks.OBSIDIAN) ||
                            mc.world.getBlockState(below).isOf(Blocks.BEDROCK) ? below : pos;
                    Vec3d crystalPos = new Vec3d(obsBase.getX() + 0.5, obsBase.getY() + 1.0, obsBase.getZ() + 0.5);
                    float targetDamage = calculateDamage(crystalPos, target);
                    if (targetDamage < minDamage.getCurrent()) continue;

                    if (antiFriend.isEnabled() && wouldHurtFriend(crystalPos)) continue;

                    if (noPlayer.isEnabled()) {
                        float selfDamage = calculateDamage(crystalPos, mc.player);
                        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

                        if (selfDamage >= health - 2.0f) continue;
                        if (selfDamage > maxSelfDamage.getCurrent()) continue;

                        float score = targetDamage - (selfDamage * 0.5f);
                        if (score > bestScore) {
                            bestScore = score;
                            bestPos = pos;
                        }
                    } else {
                        if (targetDamage > bestScore) {
                            bestScore = targetDamage;
                            bestPos = pos;
                        }
                    }
                }
            }
        }

        return bestPos;
    }

    private void mineBlock(BlockPos pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        calcGrimRotations(pos.toCenterPos());
        if (rotating) {
            RotationComponent.update(new Rotation(rotationYaw, rotationPitch), 360F, 360F, 360F, 360F, 0, 10, false);
        }

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private float calculateDamage(Vec3d crystalPos, PlayerEntity player) {
        if (player == null || mc.world == null) return 0;

        double distance = crystalPos.distanceTo(player.getPos());
        if (distance > 12.0) return 0;

        double impact = 1.0 - distance / 12.0;
        double rawDamage = (impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0;

        float exposure = 1.0f;
        Vec3d eyePos = player.getEyePos();
        if (mc.world.raycast(new net.minecraft.world.RaycastContext(crystalPos, eyePos, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, player)).getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
            exposure = 0.5f;
        }

        float damage = (float) (rawDamage * exposure);

        if (player == mc.player) {
            float armor = (float) player.getArmor();
            float toughness = (float) player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);

            float f = 2.0F + toughness / 4.0F;
            float f1 = MathHelper.clamp(armor - damage / f, armor * 0.2F, 20.0F);
            damage = damage * (1.0F - f1 / 25.0F);

            damage *= 0.6f;
        } else {

            damage *= 0.4f;
        }

        return damage;
    }

    private boolean wouldHurtFriend(Vec3d crystalPos) {
        if (mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!Dexum.INSTANCE.getFriendManager().isFriend(player.getName().getString())) continue;

            float friendDamage = calculateDamage(crystalPos, player);
            if (friendDamage > 4.0f) return true;
        }
        return false;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        if (mc.world == null) return false;

        if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) &&
                !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
            return false;
        }

        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;

        Box box = new Box(pos.up()).expand(0, 1, 0);
        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof EndCrystalEntity) continue;
            return false;
        }

        return true;
    }

    private void placeCrystal(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null || mc.world == null) return;

        boolean offhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        boolean mainhand = mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);

        if (!offhand && !mainhand) return;

        Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                Direction.UP,
                pos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        mc.player.swingHand(hand);
    }

    private void placeBlock(BlockPos pos, int slot) {
        BlockPos neighbor = null;
        Direction side = null;

        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            if (!mc.world.getBlockState(p).isReplaceable()) {
                neighbor = p;
                side = d.getOpposite();
                break;
            }
        }

        if (neighbor == null) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );

        calcGrimRotations(hitVec);
        if (rotating) {
            RotationComponent.update(new Rotation(rotationYaw, rotationPitch), 360F, 360F, 360F, 360F, 0, 10, false);
        }

        BlockHitResult hit = new BlockHitResult(hitVec, side, neighbor, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.getInventory().selectedSlot = prevSlot;
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (throughWalls.isEnabled()) {

            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        } else {
            mc.interactionManager.attackEntity(mc.player, crystal);
        }
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean hasCrystalInHand() {
        if (mc.player == null) return false;
        return mc.player.getOffHandStack().isOf(Items.END_CRYSTAL) ||
                mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);
    }

    private void switchToCrystal() {
        if (mc.player == null) return;

        if (mc.player.getOffHandStack().isOf(Items.END_CRYSTAL)) return;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
                if (!crystalSlotSwapped) {
                    oldSlot = mc.player.getInventory().selectedSlot;
                    crystalSlotSwapped = true;
                }
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }

    private void restoreSlot() {
        if (crystalSlotSwapped && mc.player != null && oldSlot != -1) {
            mc.player.getInventory().selectedSlot = oldSlot;
            crystalSlotSwapped = false;
        }
    }

    private void calcGrimRotations(Vec3d vec) {
        if (mc.player == null) return;

        float yawDelta = MathHelper.wrapDegrees(
                (float) MathHelper.wrapDegrees(
                        Math.toDegrees(Math.atan2(
                                vec.z - mc.player.getZ(),
                                vec.x - mc.player.getX()
                        )) - 90
                ) - rotationYaw
        );

        float pitchDelta = (float) (
                -Math.toDegrees(Math.atan2(
                        vec.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())),
                        Math.sqrt(
                                Math.pow(vec.x - mc.player.getX(), 2) +
                                        Math.pow(vec.z - mc.player.getZ(), 2)
                        )
                ))
        ) - rotationPitch;

        float angleToRad = (float) Math.toRadians(27 * (mc.player.age % 30));
        yawDelta = (float) (yawDelta + Math.sin(angleToRad) * 3) + randomFloat(-1f, 1f);
        pitchDelta = pitchDelta + randomFloat(-0.6f, 0.6f);

        if (yawDelta > 180) yawDelta = yawDelta - 180;

        float clampedYawDelta = MathHelper.clamp(Math.abs(yawDelta), -180f, 180f);
        float clampedPitchDelta = MathHelper.clamp(pitchDelta, -45, 45);

        float newYaw = rotationYaw + (yawDelta > 0 ? clampedYawDelta : -clampedYawDelta);
        float newPitch = MathHelper.clamp(rotationPitch + clampedPitchDelta, -90.0F, 90.0F);

        double gcdFix = Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
        rotationYaw = (float) (newYaw - (newYaw - rotationYaw) % gcdFix);
        rotationPitch = (float) (newPitch - (newPitch - rotationPitch) % gcdFix);

        rotating = true;
    }

    private float randomFloat(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        rotating = false;
        targetCrystal = null;
        targetPos = null;
        lastPlacedPos = null;
        currentTarget = null;
        renderPlacePos = null;
        renderCrystal = null;
        renderUnderminePos = null;
        breakTimer = 0;
        placeTimer = 0;

        restoreSlot();
        oldSlot = -1;
        crystalSlotSwapped = false;
    }
}