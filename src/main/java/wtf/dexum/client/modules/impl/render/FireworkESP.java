package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.ProjectionUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleAnnotation(
        name = "FireworkESP",
        category = Category.RENDER,
        description = "Показывает теги и трейлы фейерверков"
)
public final class FireworkESP extends Module implements IMinecraft {

    public static final FireworkESP INSTANCE = new FireworkESP();

    private final NumberSetting interval = new NumberSetting("Интервал (мс)", 100.0f, 10.0f, 1000.0f, 10.0f);
    private final NumberSetting lifetime = new NumberSetting("Время жизни (мс)", 1000.0f, 100.0f, 5000.0f, 100.0f);

    private final Map<Integer, FireworkData> fireworks = new HashMap<>();
    private float lastTickDelta;

    private FireworkESP() {
    }

    @Override
    public void onDisable() {
        super.onDisable();
        fireworks.clear();
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        lastTickDelta = event.getPartialTicks();

        if (mc.world == null) return;

        long currentTime = System.currentTimeMillis();

        fireworks.entrySet().removeIf(entry -> {
            Entity entity = mc.world.getEntityById(entry.getKey());
            boolean isDead = (entity == null || !entity.isAlive());
            entry.getValue().points.removeIf(p -> currentTime - p.timestamp > lifetime.getCurrent());
            return isDead && entry.getValue().points.isEmpty();
        });

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity && entity.isAlive()) {
                FireworkData data = fireworks.computeIfAbsent(entity.getId(), k -> new FireworkData());

                if (currentTime - data.lastSpawnTime >= interval.getCurrent()) {
                    Vec3d pos = new Vec3d(
                            MathHelper.lerp(lastTickDelta, entity.prevX, entity.getX()),
                            MathHelper.lerp(lastTickDelta, entity.prevY, entity.getY()) + 0.5,
                            MathHelper.lerp(lastTickDelta, entity.prevZ, entity.getZ())
                    );
                    float ageInSeconds = entity.age / 20.0f;
                    data.points.add(new TrailPoint(pos, currentTime, ageInSeconds));
                    data.lastSpawnTime = currentTime;
                }
            }
        }
    }

    @EventTarget
    private void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = event.getContext().getMatrices();
        ItemStack icon = new ItemStack(Items.FIREWORK_ROCKET);
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Integer, FireworkData> entry : fireworks.entrySet()) {
            FireworkData data = entry.getValue();

            for (TrailPoint p : data.points) {
                Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(p.pos);
                if (screen.z < 0.0 || screen.z > 1.0) continue;

                float progress = 1.0f - ((float) (currentTime - p.timestamp) / lifetime.getCurrent());
                progress = MathHelper.clamp(progress, 0.0f, 1.0f);
                String text = String.format("%.1fs", p.ageSec);

                renderIconRect(event, matrices, icon, screen, progress, text);
            }

            Entity entity = mc.world.getEntityById(entry.getKey());
            if (entity instanceof FireworkRocketEntity && entity.isAlive()) {
                Vec3d currentPos = new Vec3d(
                        MathHelper.lerp(lastTickDelta, entity.prevX, entity.getX()),
                        MathHelper.lerp(lastTickDelta, entity.prevY, entity.getY()) + 0.5,
                        MathHelper.lerp(lastTickDelta, entity.prevZ, entity.getZ())
                );
                Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(currentPos);
                if (screen.z >= 0.0 && screen.z <= 1.0) {
                    String text = String.format("%.1fs", entity.age / 20.0f);
                    renderIconRect(event, matrices, icon, screen, 1.0f, text);
                }
            }
        }
    }

    private void renderIconRect(EventRender2D event, MatrixStack matrices, ItemStack icon, Vec3d screen, float progress, String text) {
        float iconScale = 0.6f;
        float rectHeight = 12.0f;
        float padding = 2.5f;
        float gap = 2.0f;
        float textYOffset = 3.5f;

        float animScale = 0.35f + 0.65f * progress;
        int alpha = (int) (255 * progress);
        if (alpha <= 5) return;

        int bgColor = (alpha << 24) | 0x0A0A0A;
        int textColor = (alpha << 24) | 0xFFFFFF;

        float textWidth = Fonts.REGULAR.getWidth(text, 6.0f);
        float iconWidth = 16.0f * iconScale;
        float totalWidth = padding + iconWidth + gap + textWidth + padding;

        matrices.push();
        matrices.translate(screen.x, screen.y, 0);
        matrices.scale(animScale, animScale, 1.0f);

        DrawUtil.drawRoundedRect(matrices, -totalWidth / 2.0f, -rectHeight / 2.0f, totalWidth, rectHeight, BorderRadius.all(2.0f), new ColorRGBA(bgColor));

        float currentX = -totalWidth / 2.0f + padding;

        matrices.push();
        matrices.translate(currentX, -(16.0f * iconScale) / 2.0f, 0);
        matrices.scale(iconScale, iconScale, 1.0f);
        event.getContext().drawItem(icon, 0, 0);
        matrices.pop();

        currentX += iconWidth + gap;

        CustomDrawContext.of(event.getContext()).drawText(Fonts.REGULAR.getFont(6.0F), text, currentX, -rectHeight / 2.0f + textYOffset + 0.5f, new ColorRGBA(textColor));

        matrices.pop();
    }

    private static class FireworkData {
        long lastSpawnTime;
        final List<TrailPoint> points = new ArrayList<>();
    }

    private static class TrailPoint {
        final Vec3d pos;
        final long timestamp;
        final float ageSec;

        TrailPoint(Vec3d pos, long timestamp, float ageSec) {
            this.pos = pos;
            this.timestamp = timestamp;
            this.ageSec = ageSec;
        }
    }
}