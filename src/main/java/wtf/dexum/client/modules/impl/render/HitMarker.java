package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.player.EventAttack;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.display.base.color.ColorUtil;

import java.util.ArrayList;
import java.util.Optional;

@ModuleAnnotation(
        name = "HitMarker",
        category = Category.RENDER,
        description = "Показывает маркер при ударе"
)
public class HitMarker extends Module {

    public static HitMarker INSTANCE = new HitMarker();

    private final NumberSetting size = new NumberSetting("Размер", 0.5f, 0.1f, 2.0f, 0.05f);
    private final NumberSetting fadeInTime = new NumberSetting("Время появления", 100f, 50f, 500f, 10f);
    private final NumberSetting displayTime = new NumberSetting("Время показа", 300f, 100f, 1000f, 50f);
    private final NumberSetting fadeOutTime = new NumberSetting("Время исчезновения", 200f, 50f, 500f, 10f);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final BooleanSetting scale = new BooleanSetting("Анимация масштаба", true);

    private final ArrayList<HitMarkerData> hitMarkers = new ArrayList<>();

    private HitMarker() {
    }

    @Override
    public void onDisable() {
        synchronized (hitMarkers) {
            hitMarkers.clear();
        }
        super.onDisable();
    }

    private Identifier getTexture() {
        return Identifier.of("dexum", "icons/cross.png");
    }

    @EventTarget
    public void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = event.getTarget();
        if (target != null) {
            synchronized (hitMarkers) {
                hitMarkers.add(new HitMarkerData(
                        resolveHitPosition(mc.player, target),
                        System.currentTimeMillis(),
                        (long) fadeInTime.getCurrent(),
                        (long) displayTime.getCurrent(),
                        (long) fadeOutTime.getCurrent()
                ));
            }
        }
    }

    private Vec3d resolveHitPosition(Entity attacker, Entity target) {
        Vec3d fallback = new Vec3d(
                target.getX(),
                target.getY() + target.getHeight() / 2.0,
                target.getZ()
        );
        if (attacker == null) return fallback;

        Vec3d eyePos = attacker.getCameraPosVec(1.0F);
        Vec3d lookVec = attacker.getRotationVec(1.0F);
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        double distance = Math.max(eyePos.distanceTo(targetCenter) + 1.0, 6.0);
        Vec3d reachPos = eyePos.add(lookVec.multiply(distance));

        Optional<Vec3d> hitPos = target.getBoundingBox().raycast(eyePos, reachPos);
        if (hitPos.isPresent()) {
            return hitPos.get();
        }

        return eyePos.add(lookVec.multiply(eyePos.distanceTo(targetCenter)));
    }

    @EventTarget
    public void onRender3D(EventRender3D e) {
        if (mc.player == null || mc.world == null) return;

        synchronized (hitMarkers) {
            hitMarkers.removeIf(HitMarkerData::isDead);
        }

        if (hitMarkers.isEmpty()) return;

        MatrixStack matrices = e.getMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        Identifier texture = getTexture();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (glow.isEnabled()) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        ArrayList<HitMarkerData> renderList;
        synchronized (hitMarkers) {
            renderList = new ArrayList<>(hitMarkers);
        }

        int color = Dexum.getInstance().getThemeManager().getClientColor(0).getRGB();
        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);

        for (HitMarkerData marker : renderList) {
            float alpha = marker.getAlpha();
            if (alpha <= 0) continue;

            double x = marker.position.x - camera.x;
            double y = marker.position.y - camera.y;
            double z = marker.position.z - camera.z;

            matrices.push();
            matrices.translate((float) x, (float) y, (float) z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float currentSize = size.getCurrent();
            if (scale.isEnabled()) {
                float scaleMultiplier = marker.getScaleMultiplier();
                currentSize *= scaleMultiplier;
            }

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            float half = currentSize / 2f;
            int alphaInt = (int) (alpha * 255);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(r, g, b, alphaInt);
            buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(r, g, b, alphaInt);
            buffer.vertex(matrix, half, half, 0).texture(1, 0).color(r, g, b, alphaInt);
            buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(r, g, b, alphaInt);

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static class HitMarkerData {
        Vec3d position;
        long birthTime;
        long fadeInTime;
        long displayTime;
        long fadeOutTime;

        HitMarkerData(Vec3d position, long birthTime, long fadeInTime, long displayTime, long fadeOutTime) {
            this.position = position;
            this.birthTime = birthTime;
            this.fadeInTime = fadeInTime;
            this.displayTime = displayTime;
            this.fadeOutTime = fadeOutTime;
        }

        boolean isDead() {
            return System.currentTimeMillis() - birthTime >= fadeInTime + displayTime + fadeOutTime;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - birthTime;

            if (elapsed < fadeInTime) {
                float progress = (float) elapsed / fadeInTime;
                return easeOutCubic(progress);
            } else if (elapsed < fadeInTime + displayTime) {
                return 1.0f;
            } else {
                long fadeOutElapsed = elapsed - fadeInTime - displayTime;
                float progress = Math.min(1.0f, (float) fadeOutElapsed / fadeOutTime);
                return 1.0f - easeInCubic(progress);
            }
        }

        float getScaleMultiplier() {
            long elapsed = System.currentTimeMillis() - birthTime;

            if (elapsed < fadeInTime) {
                float progress = (float) elapsed / fadeInTime;
                return 0.5f + 0.5f * easeOutBack(progress);
            } else if (elapsed < fadeInTime + displayTime) {
                return 1.0f;
            } else {
                long fadeOutElapsed = elapsed - fadeInTime - displayTime;
                float progress = Math.min(1.0f, (float) fadeOutElapsed / fadeOutTime);
                return 1.0f - 0.3f * easeInCubic(progress);
            }
        }

        private float easeOutCubic(float x) {
            return 1.0f - (float) Math.pow(1.0 - x, 3);
        }

        private float easeInCubic(float x) {
            return x * x * x;
        }

        private float easeOutBack(float x) {
            float c1 = 1.70158f;
            float c3 = c1 + 1.0f;
            return 1.0f + c3 * (float) Math.pow(x - 1.0, 3) + c1 * (float) Math.pow(x - 1.0, 2);
        }
    }
}