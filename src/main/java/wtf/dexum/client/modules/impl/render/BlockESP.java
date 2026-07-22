package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.utility.render.level.Render3DUtil;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(
        name = "BlockESP",
        category = Category.RENDER,
        description = "Подсвечивает определенные блоки"
)
public final class BlockESP extends Module {
    public static final BlockESP INSTANCE = new BlockESP();

    public final MultiBooleanSetting blocks = new MultiBooleanSetting("Блоки",
            new MultiBooleanSetting.Value("Сундук", true),
            new MultiBooleanSetting.Value("Сундук ловушка", true),
            new MultiBooleanSetting.Value("Эндер сундук", true),
            new MultiBooleanSetting.Value("Спавнер", true),
            new MultiBooleanSetting.Value("Бочка", true),
            new MultiBooleanSetting.Value("Воронка", true),
            new MultiBooleanSetting.Value("Раздатчик", true),
            new MultiBooleanSetting.Value("Выбрасыватель", true),
            new MultiBooleanSetting.Value("Печка", true),
            new MultiBooleanSetting.Value("Шалкер", true)
    );

    private static final Map<BlockEntityType<?>, Integer> blockColors = new HashMap<>();

    @Override
    public void onEnable() {
        super.onEnable();
        blockColors.clear();
        blockColors.put(BlockEntityType.CHEST, new Color(255, 194, 84).getRGB());
        blockColors.put(BlockEntityType.TRAPPED_CHEST, new Color(143, 109, 62).getRGB());
        blockColors.put(BlockEntityType.ENDER_CHEST, new Color(153, 49, 238).getRGB());
        blockColors.put(BlockEntityType.MOB_SPAWNER, 16777215);
        blockColors.put(BlockEntityType.BARREL, new Color(250, 225, 62).getRGB());
        blockColors.put(BlockEntityType.HOPPER, new Color(62, 137, 250).getRGB());
        blockColors.put(BlockEntityType.DISPENSER, new Color(27, 64, 250).getRGB());
        blockColors.put(BlockEntityType.DROPPER, new Color(0, 23, 255).getRGB());
        blockColors.put(BlockEntityType.FURNACE, new Color(115, 115, 115).getRGB());
        blockColors.put(BlockEntityType.SHULKER_BOX, new Color(246, 123, 123).getRGB());
    }

    @EventTarget
    private void onRender3D(wtf.dexum.base.events.impl.render.EventRender3D event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        ChunkPos playerChunk = mc.player.getChunkPos();
        int viewDistance = mc.options.getViewDistance().getValue();

        for (int x = playerChunk.x - viewDistance; x <= playerChunk.x + viewDistance; x++) {
            for (int z = playerChunk.z - viewDistance; z <= playerChunk.z + viewDistance; z++) {
                WorldChunk chunk = mc.world.getChunk(x, z);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockEntityType<?> type = blockEntity.getType();
                    if (!blockColors.containsKey(type)) continue;
                    if (!isBlockEnabled(blockEntity, type)) continue;

                    BlockPos pos = blockEntity.getPos();
                    Box box = new Box(pos);
                    int color = blockColors.get(type);
                    int colorWithAlpha = (120 << 24) | (color & 0x00FFFFFF);
                    Render3DUtil.drawBox(box, colorWithAlpha, 1.5f, true, true, false);
                }
            }
        }
    }

    private boolean isBlockEnabled(BlockEntity blockEntity, BlockEntityType<?> type) {
        if (blockEntity instanceof ChestBlockEntity && !blocks.isEnable("Сундук")) return false;
        if (blockEntity instanceof TrappedChestBlockEntity && !blocks.isEnable("Сундук ловушка")) return false;
        if (blockEntity instanceof EnderChestBlockEntity && !blocks.isEnable("Эндер сундук")) return false;
        if (blockEntity instanceof MobSpawnerBlockEntity && !blocks.isEnable("Спавнер")) return false;
        if (blockEntity instanceof BarrelBlockEntity && !blocks.isEnable("Бочка")) return false;
        if (blockEntity instanceof HopperBlockEntity && !blocks.isEnable("Воронка")) return false;
        if (blockEntity instanceof DispenserBlockEntity && !blocks.isEnable("Раздатчик")) return false;
        if (blockEntity instanceof DropperBlockEntity && !blocks.isEnable("Выбрасыватель")) return false;
        if (blockEntity instanceof FurnaceBlockEntity && !blocks.isEnable("Печка")) return false;
        if (blockEntity instanceof ShulkerBoxBlockEntity && !blocks.isEnable("Шалкер")) return false;
        return true;
    }
}