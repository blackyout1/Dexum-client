package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleAnnotation(
        name = "JumpCircle",
        category = Category.RENDER,
        description = "Рисует круг при прыжке"
)
public final class JumpCircle extends Module implements IMinecraft {

    public static final JumpCircle INSTANCE = new JumpCircle();

    private static final float MAX_LIFETIME_MS = 1850f;
    private static final float ROTATION_SPEED = 120f;
    private static final float PULSE_SPEED = 7f;
    private static final float PULSE_SCALE = 0.06f;
    private static final float PULSE_ALPHA = 0.12f;
    private static final int MAX_CIRCLES = 8;

    private final NumberSetting radius = new NumberSetting("Радиус", 1.85f, 0.5f, 4.0f, 0.1f);
    private final NumberSetting speed = new NumberSetting("Скорость", 1.2f, 1.0f, 5.0f, 0.1f);
    private final NumberSetting fadeSpeed = new NumberSetting("Скорость исчезновения", 1.5f, 1.0f, 5.0f, 0.5f);

    private final List<CircleData> circles = new ArrayList<>();
    private final Identifier circleTexture = Dexum.id("icons/circle.png");

    private boolean wasOnGround = true;

    private JumpCircle() {
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            wasOnGround = mc.player.isOnGround();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        circles.clear();
        super.onDisable();
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        boolean isOnGround = mc.player.isOnGround();
        if (wasOnGround && !isOnGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    Math.floor(mc.player.getY()) + 0.01,
                    mc.player.getZ()
            );
            circles.add(new CircleData(pos, System.currentTimeMillis()));
            while (circles.size() > MAX_CIRCLES) {
                circles.remove(0);
            }
        }
        wasOnGround = isOnGround;

        long now = System.currentTimeMillis();
        float lifeTimeMs = getLifeTimeMs();
        Iterator<CircleData> iterator = circles.iterator();
        while (iterator.hasNext()) {
            CircleData circle = iterator.next();
            if (now - circle.startTimeMs > (long) lifeTimeMs) {
                iterator.remove();
            }
        }
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (circles.isEmpty()) return;

        long now = System.currentTimeMillis();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        MatrixStack matrices = event.getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, circleTexture);

        for (CircleData circle : circles) {
            float progress = getProgress(now, circle);
            if (progress >= 1f) continue;

            float alpha = getAlpha(progress);
            if (alpha <= 0.01f) continue;
            renderGlowCircle(matrices, cameraPos, circle, progress, alpha, now);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private float getLifeTimeMs() {
        return MAX_LIFETIME_MS / Math.max(0.25f, speed.getCurrent());
    }

    private float getProgress(long now, CircleData circle) {
        return (float) (now - circle.startTimeMs) / getLifeTimeMs();
    }

    private float getAlpha(float progress) {
        float fade = MathHelper.clamp(progress * fadeSpeed.getCurrent(), 0f, 1f);
        return 1f - fade;
    }

    private void renderGlowCircle(MatrixStack matrices, Vec3d cameraPos, CircleData circle, float progress, float alpha, long now) {
        float lifeTimeSec = (now - circle.startTimeMs) / 1000f;
        float easedProgress = easeOutCubic(progress);
        float scale = Math.min(easedProgress * radius.getCurrent(), radius.getCurrent());

        float rotation = lifeTimeSec * ROTATION_SPEED * speed.getCurrent();
        rotation += (float) Math.sin(progress * Math.PI * 2.0) * 30f;

        float pulse = (float) Math.sin(lifeTimeSec * PULSE_SPEED * speed.getCurrent());
        float pulseScale = 1f + pulse * PULSE_SCALE;
        float pulseAlpha = MathHelper.clamp(alpha * (1f + pulse * PULSE_ALPHA), 0f, 1f);
        float alphaBoost = MathHelper.clamp(pulseAlpha * 1.25f, 0f, 1f);
        float finalScale = scale * pulseScale;

        ColorRGBA baseTheme = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        int colorA = baseTheme.mulAlpha(alphaBoost).getRGB();
        int colorB = baseTheme.mulAlpha(alphaBoost).getRGB();

        int darkA = baseTheme.darker(0.65f).mulAlpha(MathHelper.clamp(alphaBoost * 0.9f, 0f, 1f)).getRGB();
        int darkB = baseTheme.darker(0.65f).mulAlpha(MathHelper.clamp(alphaBoost * 0.9f, 0f, 1f)).getRGB();

        matrices.push();
        matrices.translate(circle.pos.x - cameraPos.x, circle.pos.y - cameraPos.y, circle.pos.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = finalScale * 0.5f;
        float thickScale = finalScale * 1.08f;
        float thickHalf = thickScale * 0.5f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        addTexturedQuad(buffer, matrix, -half, -half, half, half, colorA, colorB);
        addTexturedQuad(buffer, matrix, -thickHalf, -thickHalf, thickHalf, thickHalf, darkA, darkB);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private void addTexturedQuad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float x2, float y2, int colorA, int colorB) {
        buffer.vertex(matrix, x1, y1, 0).texture(0, 1).color(colorA);
        buffer.vertex(matrix, x1, y2, 0).texture(0, 0).color(colorB);
        buffer.vertex(matrix, x2, y2, 0).texture(1, 0).color(colorB);
        buffer.vertex(matrix, x2, y1, 0).texture(1, 1).color(colorA);
    }

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private record CircleData(Vec3d pos, long startTimeMs) {
    }
}