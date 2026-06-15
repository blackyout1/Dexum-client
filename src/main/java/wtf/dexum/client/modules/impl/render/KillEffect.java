package wtf.dexum.client.modules.impl.render;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.player.EventAttack;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
        name = "KillEffect",
        category = Category.RENDER,
        description = "Визуальные эффекты при убийстве"
)
public class KillEffect extends Module {
    public static final KillEffect INSTANCE = new KillEffect();

    private static final long TRACK_TIMEOUT_MS = 4000L;
    private final List<TrackedKill> tracked = new ArrayList<>();
    private final List<VisualEffect> effects = new ArrayList<>();
    private final Random random = new Random();

    private KillEffect() {
    }

    @Override
    public void onDisable() {
        tracked.clear();
        effects.clear();
        super.onDisable();
    }

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.world == null || mc.player == null) return;

        if (event.getTarget() instanceof LivingEntity living) {
            if (!living.isAlive()) return;
            for (TrackedKill existing : tracked) {
                if (existing.entityId == living.getId()) {
                    existing.startTime = System.currentTimeMillis();
                    return;
                }
            }
            tracked.add(new TrackedKill(living, System.currentTimeMillis()));
        }
    }

    @EventTarget
    private void onTick(EventTick event) {
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        Iterator<TrackedKill> iterator = tracked.iterator();
        while (iterator.hasNext()) {
            TrackedKill trackedKill = iterator.next();
            LivingEntity entity = trackedKill.entity;
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (!entity.isAlive()) {
                spawnEffects(entity.getPos().add(0, entity.getHeight() / 2f, 0));
                iterator.remove();
                continue;
            }

            if (now - trackedKill.startTime > TRACK_TIMEOUT_MS) {
                iterator.remove();
            }
        }

        effects.removeIf(VisualEffect::isDead);
        for (VisualEffect effect : effects) {
            effect.update();
        }
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (effects.isEmpty() || mc.player == null) return;

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

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getClientColor(0);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        for (VisualEffect effect : effects) {
            effect.render(matrices, cameraPos, themeColor, partialTicks);
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private void spawnEffects(Vec3d pos) {
        if (mc.world != null) {
            mc.world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0f, 1.0f, false);
        }
        effects.add(new LightningEffect(pos));
    }

    private abstract static class VisualEffect {
        protected final long startTime = System.currentTimeMillis();
        protected final long duration;

        protected VisualEffect(long duration) {
            this.duration = duration;
        }

        public abstract void update();
        public abstract void render(MatrixStack matrices, Vec3d cameraPos, ColorRGBA color, float partialTicks);

        public boolean isDead() {
            return System.currentTimeMillis() - startTime > duration;
        }

        protected float getProgress() {
            return MathHelper.clamp((float)(System.currentTimeMillis() - startTime) / duration, 0, 1);
        }
    }

    private static class LightningEffect extends VisualEffect {
        private final List<Vec3d> segments = new ArrayList<>();
        private final Random random = new Random();

        public LightningEffect(Vec3d target) {
            super(800);
            Vec3d current = target.add(0, 200, 0);
            segments.add(current);

            while (current.y > target.y) {
                double dx = (random.nextDouble() - 0.5) * 0.8;
                double dy = -(6.0 + random.nextDouble() * 10.0);
                double dz = (random.nextDouble() - 0.5) * 0.8;
                current = current.add(dx, dy, dz);
                if (current.y < target.y) {
                    current = new Vec3d(target.x, target.y, target.z);
                }
                segments.add(current);
            }
        }

        @Override
        public void update() {}

        @Override
        public void render(MatrixStack matrices, Vec3d cameraPos, ColorRGBA color, float partialTicks) {
            float progress = getProgress();
            float alpha = progress < 0.15f ? progress / 0.15f : 1.0f - (progress - 0.15f) / 0.85f;
            if (alpha <= 0) return;

            Matrix4f model = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();

            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            renderLines(tessellator, model, cameraPos, color.withAlpha((int) (alpha * 40)), 40.0f);
            renderLines(tessellator, model, cameraPos, color.withAlpha((int) (alpha * 80)), 24.0f);
            renderLines(tessellator, model, cameraPos, color.withAlpha((int) (alpha * 130)), 12.0f);
            renderLines(tessellator, model, cameraPos, color.withAlpha((int) (alpha * 190)), 6.0f);
            RenderSystem.defaultBlendFunc();
            renderLines(tessellator, model, cameraPos, color.withAlpha((int) (alpha * 255)), 4.4f);
        }

        private void renderLines(Tessellator tessellator, Matrix4f model, Vec3d cameraPos, ColorRGBA color, float width) {
            RenderSystem.lineWidth(width);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

            for (int i = 0; i < segments.size() - 1; i++) {
                Vec3d start = segments.get(i);
                Vec3d end = segments.get(i + 1);
                buffer.vertex(model, (float)(start.x - cameraPos.x), (float)(start.y - cameraPos.y), (float)(start.z - cameraPos.z)).color(r, g, b, a).normal(0, 1, 0);
                buffer.vertex(model, (float)(end.x - cameraPos.x), (float)(end.y - cameraPos.y), (float)(end.z - cameraPos.z)).color(r, g, b, a).normal(0, 1, 0);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private static final class TrackedKill {
        private final LivingEntity entity;
        private final int entityId;
        private long startTime;

        private TrackedKill(LivingEntity entity, long startTime) {
            this.entity = entity;
            this.entityId = entity.getId();
            this.startTime = startTime;
        }
    }
}