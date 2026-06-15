package wtf.dexum.utility.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.impl.render.BloomBlock;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.CustomRenderTarget;
import wtf.dexum.utility.render.display.shader.GlProgram;

@Getter
public class ShaderBlockRenderer implements IMinecraft {

    private static final ShaderBlockRenderer INSTANCE = new ShaderBlockRenderer();

    private CustomRenderTarget blockRenderTarget;
    private CustomRenderTarget blurRenderTarget1;
    private CustomRenderTarget blurRenderTarget2;

    private GlProgram kawaseDownProgram;
    private GlProgram kawaseUpProgram;
    private GlProgram blockGlowProgram;
    private GlProgram blockWaveProgram;

    private ShaderBlockRenderer() {
    }

    public static ShaderBlockRenderer getInstance() {
        return INSTANCE;
    }

    public static void initializeShaders() {
        INSTANCE.kawaseDownProgram = new GlProgram(Dexum.id("hands/hands_kawase_down"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.kawaseUpProgram = new GlProgram(Dexum.id("hands/hands_kawase_up"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.blockGlowProgram = new GlProgram(Dexum.id("hands/hands_glow"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.blockWaveProgram = new GlProgram(Dexum.id("hand_wave/hand_wave"), VertexFormats.POSITION_TEXTURE_COLOR);
    }

    public void renderBlock(EventRender3D e, BlockPos pos, BloomBlock module) {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        if (blockRenderTarget == null) {
            blockRenderTarget = new CustomRenderTarget(width, height, true);
            blurRenderTarget1 = new CustomRenderTarget(width, height, false);
            blurRenderTarget2 = new CustomRenderTarget(width, height, false);
        } else if (blockRenderTarget.textureWidth != width || blockRenderTarget.textureHeight != height) {
            blockRenderTarget.resize(width, height);
            blurRenderTarget1.resize(width, height);
            blurRenderTarget2.resize(width, height);
        }

        blockRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        blockRenderTarget.setup();

        renderBlockToTarget(e, pos);

        blockRenderTarget.stop();

        org.joml.Matrix4f originalProj = RenderSystem.getProjectionMatrix();

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        RenderSystem.setProjectionMatrix(new org.joml.Matrix4f(), com.mojang.blaze3d.systems.ProjectionType.ORTHOGRAPHIC);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        float red = themeColor.getRed() / 255.0f;
        float green = themeColor.getGreen() / 255.0f;
        float blue = themeColor.getBlue() / 255.0f;

        blurRenderTarget1.setup(true);
        RenderSystem.setShaderTexture(0, blockRenderTarget.getColorAttachment());
        if (kawaseDownProgram != null) {
            kawaseDownProgram.use();
            setUniform(kawaseDownProgram, "uOffset", 1.0f, 1.0f);
            setUniform(kawaseDownProgram, "uHalfPixel", 0.5f / width, 0.5f / height);
            setUniform(kawaseDownProgram, "uSize", (float)width, (float)height);
        }
        drawFullScreenQuad();
        blurRenderTarget1.stop();

        blurRenderTarget2.setup(true);
        RenderSystem.setShaderTexture(0, blurRenderTarget1.getColorAttachment());
        if (kawaseDownProgram != null) {
            kawaseDownProgram.use();
            setUniform(kawaseDownProgram, "uOffset", 2.0f, 2.0f);
            setUniform(kawaseDownProgram, "uHalfPixel", 0.5f / width, 0.5f / height);
            setUniform(kawaseDownProgram, "uSize", (float)width, (float)height);
        }
        drawFullScreenQuad();
        blurRenderTarget2.stop();

        blurRenderTarget1.setup(true);
        RenderSystem.setShaderTexture(0, blurRenderTarget2.getColorAttachment());
        if (kawaseUpProgram != null) {
            kawaseUpProgram.use();
            setUniform(kawaseUpProgram, "uOffset", 2.0f, 2.0f);
            setUniform(kawaseUpProgram, "uHalfPixel", 0.5f / width, 0.5f / height);
            setUniform(kawaseUpProgram, "uSize", (float)width, (float)height);
            setUniform(kawaseUpProgram, "color", red, green, blue);
        }
        drawFullScreenQuad();
        blurRenderTarget1.stop();

        blurRenderTarget2.setup(true);
        RenderSystem.setShaderTexture(0, blurRenderTarget1.getColorAttachment());
        if (kawaseUpProgram != null) {
            kawaseUpProgram.use();
            setUniform(kawaseUpProgram, "uOffset", 1.0f, 1.0f);
            setUniform(kawaseUpProgram, "uHalfPixel", 0.5f / width, 0.5f / height);
            setUniform(kawaseUpProgram, "uSize", (float)width, (float)height);
            setUniform(kawaseUpProgram, "color", red, green, blue);
        }
        drawFullScreenQuad();
        blurRenderTarget2.stop();

        if (blockGlowProgram != null) {
            blockGlowProgram.use();
            RenderSystem.setShaderTexture(0, blurRenderTarget2.getColorAttachment());
            RenderSystem.setShaderTexture(1, blockRenderTarget.getColorAttachment());
            setUniform(blockGlowProgram, "color", red, green, blue);
            ColorRGBA themeColor2 = Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor();
            float red2 = themeColor2.getRed() / 255.0f;
            float green2 = themeColor2.getGreen() / 255.0f;
            float blue2 = themeColor2.getBlue() / 255.0f;
            setUniform(blockGlowProgram, "color2", red2, green2, blue2);
            setUniform(blockGlowProgram, "exposure", module.glow.getCurrent() * 2.0f);
            drawFullScreenQuad();
        }

        if (blockWaveProgram != null && blockWaveProgram.isLoaded()) {
            blockWaveProgram.use();
            RenderSystem.setShaderTexture(0, blockRenderTarget.getColorAttachment());
            setUniform(blockWaveProgram, "Time", (System.currentTimeMillis() % 1000000) / 1000.0f);
            setUniform(blockWaveProgram, "Resolution", (float)width, (float)height);
            setUniform(blockWaveProgram, "ThemeColor", red, green, blue, themeColor.getAlpha() / 255.0f);
            setUniform(blockWaveProgram, "OutlineWidth", module.outline.getCurrent());
            setUniform(blockWaveProgram, "GlowStrength", module.glow.getCurrent());
            setUniform(blockWaveProgram, "FillAmount", module.fill.getCurrent());
            setUniform(blockWaveProgram, "Alpha", module.alpha.getCurrent());
            setUniform(blockWaveProgram, "WaveSpeed", module.waveSpeed.getCurrent());
            setUniform(blockWaveProgram, "WaveScale", module.waveScale.getCurrent());
            drawFullScreenQuad();
        }

        RenderSystem.setProjectionMatrix(originalProj, com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
        RenderSystem.getModelViewStack().popMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderBlockToTarget(EventRender3D e, BlockPos pos) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        int outlineColor = themeColor.withAlpha(255).getRGB();
        int fillColor = themeColor.withAlpha(100).getRGB();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(3.0f);

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
    }

    private void drawFullScreenQuad() {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        org.joml.Matrix4f matrix = new org.joml.Matrix4f();
        builder.vertex(matrix, -1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(-1);
        builder.vertex(matrix, -1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(-1);
        builder.vertex(matrix, 1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(-1);
        builder.vertex(matrix, 1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(-1);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void setUniform(GlProgram program, String name, float... values) {
        GlUniform uniform = program.findUniform(name);
        if (uniform != null) {
            if (values.length == 1) uniform.set(values[0]);
            else if (values.length == 2) uniform.set(values[0], values[1]);
            else if (values.length == 3) uniform.set(values[0], values[1], values[2]);
            else if (values.length == 4) uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    public void invalidateState() {
        if (blockRenderTarget != null) {
            blockRenderTarget.delete();
            blockRenderTarget = null;
        }
        if (blurRenderTarget1 != null) {
            blurRenderTarget1.delete();
            blurRenderTarget1 = null;
        }
        if (blurRenderTarget2 != null) {
            blurRenderTarget2.delete();
            blurRenderTarget2 = null;
        }
    }
}