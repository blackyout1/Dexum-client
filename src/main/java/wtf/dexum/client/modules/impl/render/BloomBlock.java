package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "BlockOverlay",
        category = Category.RENDER,
        description = "3D обводка блока в фокусе цветом темы"
)
public class BloomBlock extends Module {

    public static final BloomBlock INSTANCE = new BloomBlock();

    public final ModeSetting mode = new ModeSetting("Режим", "Обычный", "Обычный", "Шейдер", "Шейдер 2");

    public final NumberSetting waveSpeed = new NumberSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting waveScale = new NumberSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting outline = new NumberSetting("Ширина обводки", 1.2f, 0.1f, 5.0f, 0.1f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting glow = new NumberSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting fill = new NumberSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting alpha = new NumberSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.05f, () -> mode.is("Шейдер") || mode.is("Шейдер 2"));
    public final NumberSetting intensity = new NumberSetting("Интенсивность", 0.015f, 0.001f, 0.05f, 0.001f, () -> mode.is("Шейдер 2"));

    public BloomBlock() {
        super();
    }

    @Override
    public void onDisable() {
        wtf.dexum.utility.render.shader.ShaderBlockRenderer.getInstance().invalidateState();
        super.onDisable();
    }

    @EventTarget
    private void onRender3D(EventRender3D e) {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();

        if (mode.is("Шейдер") || mode.is("Шейдер 2")) {
            renderShaderBlock(e, pos);
        } else {
            renderNormalBlock(e, pos);
        }
    }

    private void renderNormalBlock(EventRender3D e, BlockPos pos) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        int alpha = 180;
        int outlineColor = theme.getColor().withAlpha(alpha).getRGB();
        int fillColor = theme.getColor().withAlpha(40).getRGB();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.5f);

        Tessellator tessellator = Tessellator.getInstance();

        matrices.push();
        matrices.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        matrices.scale(1.002f, 1.002f, 1.002f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        BufferBuilder fillBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        fillBuffer.vertex(matrix, 0, 0, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 0, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 0, 1).color(fillColor);
        fillBuffer.vertex(matrix, 0, 0, 1).color(fillColor);

        fillBuffer.vertex(matrix, 0, 1, 0).color(fillColor);
        fillBuffer.vertex(matrix, 0, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 0).color(fillColor);

        fillBuffer.vertex(matrix, 0, 0, 0).color(fillColor);
        fillBuffer.vertex(matrix, 0, 1, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 0, 0).color(fillColor);

        fillBuffer.vertex(matrix, 1, 0, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 0).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 1, 0, 1).color(fillColor);

        fillBuffer.vertex(matrix, 1, 0, 1).color(fillColor);
        fillBuffer.vertex(matrix, 1, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 0, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 0, 0, 1).color(fillColor);

        fillBuffer.vertex(matrix, 0, 0, 1).color(fillColor);
        fillBuffer.vertex(matrix, 0, 1, 1).color(fillColor);
        fillBuffer.vertex(matrix, 0, 1, 0).color(fillColor);
        fillBuffer.vertex(matrix, 0, 0, 0).color(fillColor);

        BufferRenderer.drawWithGlobalProgram(fillBuffer.end());

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, 0, 0, 0).color(outlineColor);
        buffer.vertex(matrix, 1, 0, 0).color(outlineColor);

        buffer.vertex(matrix, 1, 0, 0).color(outlineColor);
        buffer.vertex(matrix, 1, 0, 1).color(outlineColor);

        buffer.vertex(matrix, 1, 0, 1).color(outlineColor);
        buffer.vertex(matrix, 0, 0, 1).color(outlineColor);

        buffer.vertex(matrix, 0, 0, 1).color(outlineColor);
        buffer.vertex(matrix, 0, 0, 0).color(outlineColor);

        buffer.vertex(matrix, 0, 1, 0).color(outlineColor);
        buffer.vertex(matrix, 1, 1, 0).color(outlineColor);

        buffer.vertex(matrix, 1, 1, 0).color(outlineColor);
        buffer.vertex(matrix, 1, 1, 1).color(outlineColor);

        buffer.vertex(matrix, 1, 1, 1).color(outlineColor);
        buffer.vertex(matrix, 0, 1, 1).color(outlineColor);

        buffer.vertex(matrix, 0, 1, 1).color(outlineColor);
        buffer.vertex(matrix, 0, 1, 0).color(outlineColor);

        buffer.vertex(matrix, 0, 0, 0).color(outlineColor);
        buffer.vertex(matrix, 0, 1, 0).color(outlineColor);

        buffer.vertex(matrix, 1, 0, 0).color(outlineColor);
        buffer.vertex(matrix, 1, 1, 0).color(outlineColor);

        buffer.vertex(matrix, 1, 0, 1).color(outlineColor);
        buffer.vertex(matrix, 1, 1, 1).color(outlineColor);

        buffer.vertex(matrix, 0, 0, 1).color(outlineColor);
        buffer.vertex(matrix, 0, 1, 1).color(outlineColor);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderShaderBlock(EventRender3D e, BlockPos pos) {

        wtf.dexum.utility.render.shader.ShaderBlockRenderer.getInstance().renderBlock(e, pos, this);
    }
}