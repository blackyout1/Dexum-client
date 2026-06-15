package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
        name = "LineGlyphes",
        category = Category.RENDER,
        description = "Анимированные линии в 3D пространстве"
)
public class LineGlyphes extends Module {
    public static final LineGlyphes INSTANCE = new LineGlyphes();

    private final NumberSetting count = new NumberSetting("Количество", 50, 10, 200, 1);
    private final NumberSetting speed = new NumberSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final NumberSetting thickness = new NumberSetting("Толщина", 1.5f, 0.5f, 5.0f, 0.1f);

    private final List<Path> paths = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public void onDisable() {
        paths.clear();
        super.onDisable();
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            paths.clear();
            return;
        }

        paths.removeIf(Path::isDead);

        for (Path path : paths) {
            path.update(speed.getCurrent());
        }

        if (paths.size() < count.getCurrent()) {
            paths.add(new Path(getRandomSpawnPos()));
        }
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (paths.isEmpty() || mc.player == null) return;

        MatrixStack matrices = event.getMatrix();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        float partialTicks = event.getPartialTicks();

        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        Matrix4f model = matrices.peek().getPositionMatrix();
        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();

        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        if (glow.isEnabled()) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            renderPaths(matrices, themeColor, cameraPos, partialTicks, true, 8.0f, 0.04f);

            renderPaths(matrices, themeColor, cameraPos, partialTicks, true, 5.0f, 0.08f);

            renderPaths(matrices, themeColor, cameraPos, partialTicks, true, 2.5f, 0.15f);
            renderPaths(matrices, themeColor, cameraPos, partialTicks, true, 1.35f, 0.22f);
            RenderSystem.defaultBlendFunc();
        }

        renderPaths(matrices, themeColor, cameraPos, partialTicks, false, 1.0f, 1.0f);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private void renderPaths(MatrixStack matrices, ColorRGBA color, Vec3d cameraPos, float partialTicks, boolean isGlow, float widthMul, float alphaMul) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        float baseWidth = thickness.getCurrent();
        RenderSystem.lineWidth(isGlow ? baseWidth * widthMul : baseWidth);

        for (Path path : paths) {
            float alpha = path.getAlpha() * alphaMul;
            if (alpha <= 0.005f) continue;

            List<Vec3d> points = path.getPoints(partialTicks);
            if (points.size() < 2) continue;

            if (!mc.gameRenderer.getCamera().isThirdPerson()) {
                Vec3d cameraPosRel = points.get(0).subtract(cameraPos);

                float pitch = mc.gameRenderer.getCamera().getPitch();
                float yaw = mc.gameRenderer.getCamera().getYaw();

                float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
                float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
                float f2 = -MathHelper.cos(-pitch * 0.017453292F);
                float f3 = MathHelper.sin(-pitch * 0.017453292F);
                Vec3d actualLookVec = new Vec3d(f1 * f2, f3, f * f2);

                if (cameraPosRel.dotProduct(actualLookVec) < 0) {
                    continue;
                }
            }

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3d start = points.get(i);
                Vec3d end = points.get(i + 1);

                float segmentAlpha = alpha * ((float) (i + 1) / points.size());
                int argb = color.withAlpha((int) (segmentAlpha * 255)).getRGB();

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int a = (argb >> 24) & 0xFF;

                float dx = (float) (end.x - start.x);
                float dy = (float) (end.y - start.y);
                float dz = (float) (end.z - start.z);
                float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 0.0001f) continue;
                dx /= len; dy /= len; dz /= len;

                float dashLength = 0.58f;
                float gapLength = 0.34f;
                float cycleLength = dashLength + gapLength;

                if (len <= cycleLength) {
                    buffer.vertex(model, (float)(start.x - cameraPos.x), (float)(start.y - cameraPos.y), (float)(start.z - cameraPos.z))
                            .color(r, g, b, a).normal(matrices.peek(), dx, dy, dz);
                    buffer.vertex(model, (float)(end.x - cameraPos.x), (float)(end.y - cameraPos.y), (float)(end.z - cameraPos.z))
                            .color(r, g, b, a).normal(matrices.peek(), dx, dy, dz);
                    continue;
                }

                float t = 0.0f;
                while (t < len) {
                    float dashStart = t;
                    float dashEnd = Math.min(len, t + dashLength);

                    float startFactor = dashStart / len;
                    float endFactor = dashEnd / len;

                    Vec3d dashFrom = new Vec3d(
                            start.x + (end.x - start.x) * startFactor,
                            start.y + (end.y - start.y) * startFactor,
                            start.z + (end.z - start.z) * startFactor
                    );
                    Vec3d dashTo = new Vec3d(
                            start.x + (end.x - start.x) * endFactor,
                            start.y + (end.y - start.y) * endFactor,
                            start.z + (end.z - start.z) * endFactor
                    );

                    buffer.vertex(model, (float)(dashFrom.x - cameraPos.x), (float)(dashFrom.y - cameraPos.y), (float)(dashFrom.z - cameraPos.z))
                            .color(r, g, b, a).normal(matrices.peek(), dx, dy, dz);
                    buffer.vertex(model, (float)(dashTo.x - cameraPos.x), (float)(dashTo.y - cameraPos.y), (float)(dashTo.z - cameraPos.z))
                            .color(r, g, b, a).normal(matrices.peek(), dx, dy, dz);

                    t += cycleLength;
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private Vec3d getRandomSpawnPos() {
        if (mc.player == null) return Vec3d.ZERO;
        double range = 30.0;
        double x = mc.player.getX() + (random.nextDouble() - 0.5) * range * 2;
        double y = mc.player.getY() + (random.nextDouble() - 0.5) * 15;
        double z = mc.player.getZ() + (random.nextDouble() - 0.5) * range * 2;
        return new Vec3d(x, y, z);
    }

    private static class Path {
        private final List<Vec3d> points = new ArrayList<>();
        private final Animation alphaAnim = new Animation(1200, Easing.QUAD_IN_OUT);
        private final int maxPoints;
        private boolean removing = false;
        private Vec3d lastDir = Vec3d.ZERO;
        private float moveProgress = 0;
        private float prevMoveProgress = 0;

        public Path(Vec3d start) {
            points.add(start);
            alphaAnim.setValue(0);
            this.maxPoints = 12 + new Random().nextInt(15);
        }

        public void update(float speedMul) {
            alphaAnim.update(!removing);

            if (!removing) {
                prevMoveProgress = moveProgress;
                moveProgress += 0.025f * speedMul;
                if (moveProgress >= 1.0f) {
                    prevMoveProgress = 0;
                    moveProgress = 0;
                    addPoint();
                }

                if (points.size() >= maxPoints) {
                    removing = true;
                }
            }
        }

        private void addPoint() {
            Vec3d last = points.get(points.size() - 1);
            Vec3d nextDir = getRandomDir();
            while (nextDir.dotProduct(lastDir) < -0.5) {
                nextDir = getRandomDir();
            }
            lastDir = nextDir;
            points.add(last.add(nextDir.multiply(2.5)));
        }

        private Vec3d getRandomDir() {
            int axis = new Random().nextInt(3);
            int dir = new Random().nextBoolean() ? 1 : -1;
            return switch (axis) {
                case 0 -> new Vec3d(dir, 0, 0);
                case 1 -> new Vec3d(0, dir, 0);
                default -> new Vec3d(0, 0, dir);
            };
        }

        private List<Vec3d> cachedPoints = new ArrayList<>();
        private float lastPartialTicks = -1;

        public List<Vec3d> getPoints(float partialTicks) {
            if (points.size() < 2 || removing) return points;

            if (lastPartialTicks == partialTicks && !cachedPoints.isEmpty()) {
                return cachedPoints;
            }

            if (cachedPoints.size() != points.size()) {
                cachedPoints = new ArrayList<>(points);
            } else {
                for (int i = 0; i < points.size(); i++) {
                    cachedPoints.set(i, points.get(i));
                }
            }

            int lastIdx = cachedPoints.size() - 1;
            Vec3d last = points.get(lastIdx);
            Vec3d prev = points.get(lastIdx - 1);

            float lerpProgress = prevMoveProgress + (moveProgress - prevMoveProgress) * partialTicks;

            double x = prev.x + (last.x - prev.x) * lerpProgress;
            double y = prev.y + (last.y - prev.y) * lerpProgress;
            double z = prev.z + (last.z - prev.z) * lerpProgress;

            cachedPoints.set(lastIdx, new Vec3d(x, y, z));
            lastPartialTicks = partialTicks;
            return cachedPoints;
        }

        public float getAlpha() {
            return MathHelper.clamp(alphaAnim.getValue(), 0, 1);
        }

        public boolean isDead() {
            return removing && alphaAnim.getValue() <= 0.01f;
        }
    }
}