package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import wtf.dexum.Dexum;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.game.player.rotation.RotationUtil;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.Timer;

import java.util.*;
import java.util.stream.StreamSupport;

@ModuleAnnotation(
        name = "AutoExplosion",
        category = Category.COMBAT,
        description = "Automatically places and explodes crystals on recently placed obsidian"
)
public class AutoExplosion extends Module implements IMinecraft {
    public static final AutoExplosion INSTANCE = new AutoExplosion();

    private final NumberSetting range = new NumberSetting("Радиус", 4.5f, 1.0f, 6.0f, 0.1f);
    private final NumberSetting delay = new NumberSetting("Задержка (мс)", 10.0f, 0.0f, 500.0f, 5.0f);
    private final BooleanSetting rotate = new BooleanSetting("Ротация", true);
    private final BooleanSetting raycast = new BooleanSetting("Raycast", true);
    private final BooleanSetting antiSelf = new BooleanSetting("Не бахать Себя", true);
    private final BooleanSetting antiFriend = new BooleanSetting("Не бахать Друзей", true);
    private final BooleanSetting antiItems = new BooleanSetting("Не бахать Ресурсы", true);

    private final Timer breakTimer = new Timer();
    private final Timer placeTimer = new Timer();
    private final Map<BlockPos, Long> activeObsidians = new HashMap<>();
    private final Set<BlockPos> placedOn = new HashSet<>();

    private AutoExplosion() {}

    @Override
    public void onDisable() {
        activeObsidians.clear();
        placedOn.clear();
        super.onDisable();
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (mc.player == null) return;

        if (event.isSent() && event.getPacket() instanceof PlayerInteractBlockC2SPacket interactPacket) {
            if (mc.player.getStackInHand(interactPacket.getHand()).getItem() == Items.OBSIDIAN) {
                BlockPos pos = interactPacket.getBlockHitResult().getBlockPos();
                Direction side = interactPacket.getBlockHitResult().getSide();
                BlockPos placedPos = pos.offset(side);
                activeObsidians.put(placedPos, System.currentTimeMillis());
            }
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        activeObsidians.entrySet().removeIf(entry -> now - entry.getValue() > 5000);

        EndCrystalEntity targetCrystal = findTargetCrystal();
        if (targetCrystal != null) {
            handleCrystalExplosion(targetCrystal);
        }

        for (BlockPos pos : new ArrayList<>(activeObsidians.keySet())) {
            if (placedOn.contains(pos)) continue;
            if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
                if (canPlaceCrystal(pos)) {
                    int crystalSlot = PlayerInventoryUtil.find(Items.END_CRYSTAL, 0, 8);
                    if (crystalSlot != -1) {
                        handleCrystalPlacement(pos, crystalSlot);
                        placedOn.add(pos);
                        break;
                    }
                }
            }
        }
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockPos up = pos.up();
        if (!mc.world.isAir(up)) return false;

        return mc.world.getOtherEntities(null, new Box(up)).stream()
                .noneMatch(e -> !(e instanceof EndCrystalEntity));
    }

    private void handleCrystalExplosion(EndCrystalEntity crystal) {
        Vec3d targetPos = crystal.getBoundingBox().getCenter();
        Rotation rot = RotationUtil.calculateAngle(targetPos);

        if (rotate.isEnabled()) {
            RotationComponent.update(rot, 360F, 360F, 360F, 360F, 0, 100, false);
        }

        mc.getNetworkHandler().sendPacket(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);

        BlockPos obsPos = crystal.getBlockPos().down();
        activeObsidians.remove(obsPos);
        placedOn.remove(obsPos);
    }

    private void handleCrystalPlacement(BlockPos pos, int crystalSlot) {
        Vec3d center = pos.toCenterPos().add(0, 0.5, 0);
        Rotation rot = RotationUtil.calculateAngle(center);

        if (rotate.isEnabled()) {
            RotationComponent.update(rot, 360F, 360F, 360F, 360F, 0, 100, false);
        }

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        placeTimer.reset();
    }

    private EndCrystalEntity findTargetCrystal() {
        return StreamSupport.stream(mc.world.getEntitiesByClass(EndCrystalEntity.class,
                        mc.player.getBoundingBox().expand(range.getCurrent()),
                        c -> activeObsidians.containsKey(c.getBlockPos().down())).spliterator(), false)
                .filter(c -> {
                    Vec3d crystalPos = c.getBoundingBox().getCenter();

                    if (antiSelf.isEnabled() && mc.player.distanceTo(c) < 2.5) return false;

                    if (antiFriend.isEnabled()) {
                        boolean friendNearby = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                                .anyMatch(e -> e instanceof PlayerEntity && e != mc.player &&
                                        Dexum.getInstance().getFriendManager().isFriend(e.getName().getString()) &&
                                        e.distanceTo(c) < 3.5);
                        if (friendNearby) return false;
                    }

                    if (antiItems.isEnabled()) {
                        boolean itemsNearby = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                                .anyMatch(e -> e instanceof ItemEntity && e.distanceTo(c) < 2.5);
                        if (itemsNearby) return false;
                    }

                    return true;
                })
                .min(Comparator.comparingDouble(c -> mc.player.getEyePos().distanceTo(c.getBoundingBox().getCenter())))
                .orElse(null);
    }
}