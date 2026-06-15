package wtf.dexum.utility.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.HeldItemRenderer.HandRenderType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.impl.render.ShaderHands;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.CustomRenderTarget;
import wtf.dexum.utility.render.display.shader.GlProgram;

@Getter
public class ShaderHandsRenderer implements IMinecraft {

    private static final ShaderHandsRenderer INSTANCE = new ShaderHandsRenderer();

    public static volatile boolean renderingHands = false;

    private CustomRenderTarget handsRenderTarget;
    private CustomRenderTarget blurRenderTarget1;
    private CustomRenderTarget blurRenderTarget2;

    private GlProgram kawaseDownProgram;
    private GlProgram kawaseUpProgram;
    private GlProgram handsGlowProgram;
    private GlProgram handsOverlayProgram;
    private GlProgram handWaveProgram;

    private GlProgram handGlow3DProgram;
    private GlProgram handWave3DProgram;

    private ShaderHandsRenderer() {
    }

    public static ShaderHandsRenderer getInstance() {
        return INSTANCE;
    }

    public static void initializeShaders() {
        INSTANCE.kawaseDownProgram = new GlProgram(Dexum.id("hands/hands_kawase_down"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.kawaseUpProgram = new GlProgram(Dexum.id("hands/hands_kawase_up"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.handsGlowProgram = new GlProgram(Dexum.id("hands/hands_glow"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.handsOverlayProgram = new GlProgram(Dexum.id("hands/hands_overlay"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.handWaveProgram = new GlProgram(Dexum.id("hand_wave/hand_wave"), VertexFormats.POSITION_TEXTURE_COLOR);

        INSTANCE.handGlow3DProgram = new GlProgram(Dexum.id("hand_glow/hand_glow"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        INSTANCE.handWave3DProgram = new GlProgram(Dexum.id("hand_wave/hand_wave"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
    }

    @FunctionalInterface
    public interface HandRendererCallback {
        void render(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
    }

    public void captureHands(
            float tickDelta, MatrixStack matrices, AbstractClientPlayerEntity player, int light,
            HandRenderType handRenderType, float f, Hand hand, float g,
            HandRendererCallback callback,
            ItemStack mainHandItem, ItemStack offHandItem,
            float mainHandEquipProgress, float mainHandPrevEquipProgress,
            float offHandEquipProgress, float offHandPrevEquipProgress
    ) {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        if (handsRenderTarget == null) {
            handsRenderTarget = new CustomRenderTarget(width, height, true);
            blurRenderTarget1 = new CustomRenderTarget(width, height, false);
            blurRenderTarget2 = new CustomRenderTarget(width, height, false);
        } else if (handsRenderTarget.textureWidth != width || handsRenderTarget.textureHeight != height) {
            handsRenderTarget.resize(width, height);
            blurRenderTarget1.resize(width, height);
            blurRenderTarget2.resize(width, height);
        }

        handsRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        handsRenderTarget.setup();

        VertexConsumerProvider.Immediate customConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

        ShaderHands module = ShaderHands.INSTANCE;
        boolean beautiful = true;

        float j;
        float k;
        if (handRenderType.renderMainHand) {
            j = hand == Hand.MAIN_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, mainHandPrevEquipProgress, mainHandEquipProgress);
            callback.render(player, tickDelta, g, Hand.MAIN_HAND, j, mainHandItem, k, matrices, customConsumers, light);
        }

        if (handRenderType.renderOffHand) {
            j = hand == Hand.OFF_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, offHandPrevEquipProgress, offHandEquipProgress);
            callback.render(player, tickDelta, g, Hand.OFF_HAND, j, offHandItem, k, matrices, customConsumers, light);
        }

        customConsumers.draw();
        handsRenderTarget.stop();

        org.joml.Matrix4f originalProj = RenderSystem.getProjectionMatrix();

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        RenderSystem.setProjectionMatrix(new org.joml.Matrix4f(), com.mojang.blaze3d.systems.ProjectionType.ORTHOGRAPHIC);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        blurRenderTarget1.setup(true);
        RenderSystem.setShaderTexture(0, handsRenderTarget.getColorAttachment());
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

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        float red = themeColor.getRed() / 255.0f;
        float green = themeColor.getGreen() / 255.0f;
        float blue = themeColor.getBlue() / 255.0f;

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

        if (handsGlowProgram != null) {
            handsGlowProgram.use();
            RenderSystem.setShaderTexture(0, blurRenderTarget2.getColorAttachment());
            RenderSystem.setShaderTexture(1, handsRenderTarget.getColorAttachment());
            setUniform(handsGlowProgram, "color", red, green, blue);
            ColorRGBA themeColor2 = Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor();
            float red2 = themeColor2.getRed() / 255.0f;
            float green2 = themeColor2.getGreen() / 255.0f;
            float blue2 = themeColor2.getBlue() / 255.0f;
            setUniform(handsGlowProgram, "color2", red2, green2, blue2);
            setUniform(handsGlowProgram, "exposure", module.glow.getCurrent() * 2.0f);
            drawFullScreenQuad();
        }

        if (beautiful && handWaveProgram != null && handWaveProgram.isLoaded()) {
            handWaveProgram.use();
            RenderSystem.setShaderTexture(0, handsRenderTarget.getColorAttachment());
            setUniform(handWaveProgram, "Time", (System.currentTimeMillis() % 1000000) / 1000.0f);
            setUniform(handWaveProgram, "Resolution", (float)width, (float)height);
            setUniform(handWaveProgram, "ThemeColor", red, green, blue, themeColor.getAlpha() / 255.0f);
            setUniform(handWaveProgram, "OutlineWidth", module.outline.getCurrent());
            setUniform(handWaveProgram, "GlowStrength", module.glow.getCurrent());
            setUniform(handWaveProgram, "FillAmount", module.fill.getCurrent());
            setUniform(handWaveProgram, "Alpha", module.alpha.getCurrent());
            setUniform(handWaveProgram, "WaveSpeed", module.waveSpeed.getCurrent());
            setUniform(handWaveProgram, "WaveScale", module.waveScale.getCurrent());
            drawFullScreenQuad();
        } else if (handsOverlayProgram != null && handsOverlayProgram.isLoaded()) {
            handsOverlayProgram.use();
            RenderSystem.setShaderTexture(0, handsRenderTarget.getColorAttachment());
            setUniform(handsOverlayProgram, "color", red, green, blue);
            setUniform(handsOverlayProgram, "fill", module.fill.getCurrent());
            setUniform(handsOverlayProgram, "alpha", module.alpha.getCurrent());
            drawFullScreenQuad();
        }

        RenderSystem.setProjectionMatrix(originalProj, com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
        RenderSystem.getModelViewStack().popMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    public net.minecraft.client.gl.ShaderProgramKey getCustomShaderKey(net.minecraft.client.gl.ShaderProgramKey originalKey) {
        if (!ShaderHands.INSTANCE.isEnabled()) return null;

        if (originalKey.vertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
            return null;
        }

        String path = originalKey.configId().getPath();
        if (!path.contains("entity") && !path.contains("item")) {
            return null;
        }

        if (handWave3DProgram != null && handWave3DProgram.isLoaded()) {
            updateWaveUniforms();
            return handWave3DProgram.programKey;
        }

        return null;
    }

    private void updateWaveUniforms() {
        ShaderHands module = ShaderHands.INSTANCE;
        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        float red = themeColor.getRed() / 255.0f;
        float green = themeColor.getGreen() / 255.0f;
        float blue = themeColor.getBlue() / 255.0f;
        float alpha = module.alpha.getCurrent();

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        setUniform(handWave3DProgram, "Time", (System.currentTimeMillis() % 1000000) / 1000.0f);
        setUniform(handWave3DProgram, "Resolution", (float)width, (float)height);
        setUniform(handWave3DProgram, "ThemeColor", red, green, blue, themeColor.getAlpha() / 255.0f);
        setUniform(handWave3DProgram, "OutlineWidth", module.outline.getCurrent());
        setUniform(handWave3DProgram, "GlowStrength", module.glow.getCurrent());
        setUniform(handWave3DProgram, "FillAmount", module.fill.getCurrent());
        setUniform(handWave3DProgram, "Alpha", alpha);
        setUniform(handWave3DProgram, "WaveSpeed", module.waveSpeed.getCurrent());
        setUniform(handWave3DProgram, "WaveScale", module.waveScale.getCurrent());
    }

    private void updateGlowUniforms() {
        ShaderHands module = ShaderHands.INSTANCE;
        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        float red = themeColor.getRed() / 255.0f;
        float green = themeColor.getGreen() / 255.0f;
        float blue = themeColor.getBlue() / 255.0f;
        float alpha = module.alpha.getCurrent();

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        setUniform(handGlow3DProgram, "Time", (System.currentTimeMillis() % 1000000) / 1000.0f);
        setUniform(handGlow3DProgram, "Resolution", (float)width, (float)height);
        setUniform(handGlow3DProgram, "ThemeColor", red, green, blue, themeColor.getAlpha() / 255.0f);
        setUniform(handGlow3DProgram, "OutlineWidth", module.outline.getCurrent());
        setUniform(handGlow3DProgram, "GlowStrength", module.glow.getCurrent());
        setUniform(handGlow3DProgram, "FillAmount", module.fill.getCurrent());
        setUniform(handGlow3DProgram, "Alpha", alpha);
    }

    public void renderOverlayIfPending() {

    }

    public void invalidateState() {
        if (handsRenderTarget != null) {
            handsRenderTarget.delete();
            handsRenderTarget = null;
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