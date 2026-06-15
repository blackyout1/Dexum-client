package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.level.Render3DUtil;

import java.util.Comparator;
import java.util.List;

@ModuleAnnotation(
        name = "CrystalOptimizer",
        category = Category.MISC,
        description = "Спамит кристаллами и взрывает их при зажатии ПКМ"
)
public class CrystalOptimizer extends Module {
    public static final CrystalOptimizer INSTANCE = new CrystalOptimizer();

    private final NumberSetting breakDelay = new NumberSetting("Задержка ломания", 0.0f, 0.0f, 10.0f, 1.0f);
    private final NumberSetting placeDelay = new NumberSetting("Задержка установки", 0.0f, 0.0f, 10.0f, 1.0f);
    private final NumberSetting maxBreakPerTick = new NumberSetting("Ломать за тик", 2.0f, 1.0f, 5.0f, 1.0f);
    private final BooleanSetting wallBypass = new BooleanSetting("Через стены", true);
    private final BooleanSetting obsidianESP = new BooleanSetting("Obsidian ESP", true);
    private final NumberSetting espRange = new NumberSetting("ESP Радиус", 10.0f, 5.0f, 30.0f, 1.0f, () -> obsidianESP.isEnabled());

    private int breakTimer = 0;
    private int placeTimer = 0;

    private CrystalOptimizer() {
    }

    @Override
    public void onEnable() {
        breakTimer = 0;
        placeTimer = 0;
        super.onEnable();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (!mc.options.useKey.isPressed()) {
            return;
        }

        boolean isMainHand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
        boolean isOffHand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

        if (!isMainHand && !isOffHand) return;

        BlockHitResult hitResult = null;

        if (wallBypass.isEnabled()) {

            hitResult = customRayTrace(5.0);
        }

        if (hitResult == null && mc.crosshairTarget instanceof BlockHitResult standardHit) {
            if (standardHit.getType() == HitResult.Type.BLOCK) {
                hitResult = standardHit;
            }
        }

        if (hitResult == null) return;

        BlockPos blockPos = hitResult.getBlockPos();

        if (mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN &&
                mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK) {
            return;
        }

        int breakDelayTicks = (int) breakDelay.getCurrent();
        int placeDelayTicks = (int) placeDelay.getCurrent();
        int maxBreaks = (int) maxBreakPerTick.getCurrent();

        BlockPos crystalPos = blockPos.up();
        Box crystalBox = new Box(crystalPos).expand(0.5);

        if (breakTimer >= breakDelayTicks) {
            List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
                    EndCrystalEntity.class, crystalBox, Entity::isAlive
            );

            crystals.sort(Comparator.comparingDouble(crystal -> crystal.squaredDistanceTo(mc.player)));

            Hand breakHand = isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            int broken = 0;

            for (EndCrystalEntity crystal : crystals) {
                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(breakHand);
                broken++;
                if (broken >= maxBreaks) {
                    break;
                }
            }

            if (broken > 0) {
                breakTimer = 0;
            }
        } else {
            breakTimer++;
        }

        if (placeTimer >= placeDelayTicks) {
            if (mc.world.isAir(crystalPos) &&
                    mc.world.isAir(crystalPos.up()) &&
                    mc.world.getEntitiesByClass(EndCrystalEntity.class, crystalBox, Entity::isAlive).isEmpty()) {
                Hand hand = isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                mc.interactionManager.interactBlock(mc.player, hand, hitResult);
                mc.player.swingHand(hand);
                placeTimer = 0;
            }
        } else {
            placeTimer++;
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;
        if (!obsidianESP.isEnabled()) return;

        int range = (int) espRange.getCurrent();
        BlockPos playerPos = mc.player.getBlockPos();
        int color = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB();
        int boxColor = (color & 0x00FFFFFF) | 0x50000000;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN ||
                            mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                        Box box = new Box(pos);
                        Render3DUtil.drawBox(box, boxColor, 1.0f, true, true, false);
                    }
                }
            }
        }
    }

    private BlockHitResult customRayTrace(double range) {
        Vec3d start = mc.player.getEyePos();
        Vec3d dir = mc.player.getRotationVector();

        for (double d = 0; d < range; d += 0.08) {
            Vec3d posVec = start.add(dir.multiply(d));
            BlockPos pos = BlockPos.ofFloored(posVec);

            if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN ||
                    mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                return new BlockHitResult(posVec, Direction.UP, pos, false);
            }
        }

        return null;
    }
}