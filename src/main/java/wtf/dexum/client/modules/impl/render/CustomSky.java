package wtf.dexum.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.GlProgram;

@ModuleAnnotation(
        name = "CustomSky",
        category = Category.RENDER,
        description = "Красивый кастомный шейдер неба"
)
public class CustomSky extends Module implements IMinecraft {

    public static final CustomSky INSTANCE = new CustomSky();

    public final ModeSetting mode = new ModeSetting("Режим", "Мод 1", "Мод 2", "Плазма");

    public final NumberSetting speed = new NumberSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    public final NumberSetting scale = new NumberSetting("Размер", 5.0f, 1.0f, 20.0f, 0.5f);
    public final NumberSetting intensity = new NumberSetting("Интенсивность", 0.01f, 0.001f, 0.05f, 0.001f);
    public final NumberSetting alpha = new NumberSetting("Прозрачность", 1.0f, 0.3f, 1.0f, 0.05f);

    private static final GlProgram WATER_SHADER = new GlProgram(Dexum.id("skyshader/water"), VertexFormats.POSITION);
    private static final GlProgram CAUSTIC_SHADER = new GlProgram(Dexum.id("skyshader/caustic"), VertexFormats.POSITION);
    private static final GlProgram PLASMA_SHADER = new GlProgram(Dexum.id("skyshader/sky_plasma"), VertexFormats.POSITION);

    private long startMillis = -1;

    private CustomSky() {
    }

    @Override
    public void onEnable() {
        startMillis = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        startMillis = -1;
        super.onDisable();
    }

    public void renderSkyShader() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (startMillis < 0) {
            startMillis = System.currentTimeMillis();
        }

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        ColorRGBA secondColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor();
        float cr = themeColor.getRed() / 255.0f;
        float cg = themeColor.getGreen() / 255.0f;
        float cb = themeColor.getBlue() / 255.0f;
        float cr2 = secondColor.getRed() / 255.0f;
        float cg2 = secondColor.getGreen() / 255.0f;
        float cb2 = secondColor.getBlue() / 255.0f;

        GlProgram activeShader;
        if (mode.is("Плазма")) {
            activeShader = PLASMA_SHADER;
        } else if (mode.is("Мод 2")) {
            activeShader = CAUSTIC_SHADER;
        } else {
            activeShader = WATER_SHADER;
        }

        if (activeShader == null || !activeShader.isLoaded()) {
            return;
        }

        activeShader.use();

        net.minecraft.client.render.Camera cam = mc.gameRenderer.getCamera();
        float yawRad = (float) Math.toRadians(-cam.getYaw());
        float pitchRad = (float) Math.toRadians(cam.getPitch());
        float fovDeg = (float) mc.options.getFov().getValue().intValue();

        if (mode.is("Плазма")) {
            setUniform(activeShader, "u_Color", cr, cg, cb, alpha.getCurrent());
            setUniform(activeShader, "u_Color2", cr2, cg2, cb2, alpha.getCurrent());
            setUniform(activeShader, "u_Resolution", fw, fh);
            setUniform(activeShader, "u_Scale", scale.getCurrent());
            setUniform(activeShader, "u_Time", time * speed.getCurrent());
            setUniform(activeShader, "u_Fov", fovDeg);
            setUniform(activeShader, "u_CameraDir", yawRad, pitchRad);
        } else {
            setUniform(activeShader, "uTime", time);
            setUniform(activeShader, "uResolution", fw, fh);
            setUniform(activeShader, "uColor", cr, cg, cb);
            setUniform(activeShader, "uAlpha", alpha.getCurrent());
            setUniform(activeShader, "uSpeed", speed.getCurrent());
            setUniform(activeShader, "uScale", scale.getCurrent());
            setUniform(activeShader, "uIntensity", intensity.getCurrent());
            setUniform(activeShader, "uCameraDir", yawRad, pitchRad);
            setUniform(activeShader, "uFov", fovDeg);
        }

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(new Matrix4f(), com.mojang.blaze3d.systems.ProjectionType.ORTHOGRAPHIC);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Matrix4f identity = new Matrix4f();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(identity, -1f, -1f, 0f);
        buf.vertex(identity, 1f, -1f, 0f);
        buf.vertex(identity, 1f, 1f, 0f);
        buf.vertex(identity, -1f, 1f, 0f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.setProjectionMatrix(savedProj, com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
    }

    private void setUniform(GlProgram program, String name, float... values) {
        net.minecraft.client.gl.GlUniform uniform = program.findUniform(name);
        if (uniform != null) {
            if (values.length == 1) uniform.set(values[0]);
            else if (values.length == 2) uniform.set(values[0], values[1]);
            else if (values.length == 3) uniform.set(values[0], values[1], values[2]);
            else if (values.length == 4) uniform.set(values[0], values[1], values[2], values[3]);
        }
    }
}