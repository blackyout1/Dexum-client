package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.mixin.minecraft.entity.LimbAnimatorMixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleAnnotation(
        name = "TotemPop",
        category = Category.RENDER,
        description = "Показывает призрачную копию игрока при срабатывании тотема"
)
public class TotemPop extends Module {
    public static final TotemPop INSTANCE = new TotemPop();

    public static float ghostAlpha = -1.0f;
    public static float ghostProgress = -1.0f;
    public static int ghostRgb = 0xFFFFFF;

    private final NumberSetting duration = new NumberSetting("Длительность", 700.0f, 250.0f, 2000.0f, 50.0f);
    private final NumberSetting rise = new NumberSetting("Подъем", 2.4f, 0.0f, 6.0f, 0.1f);
    private final BooleanSetting self = new BooleanSetting("На себя", true);

    private final List<PopEntry> entries = new ArrayList<>();

    private TotemPop() {
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (!event.isReceive() || mc.world == null || mc.player == null) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35) return;
        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player)) return;
        if (!self.isEnabled() && player == mc.player) return;

        entries.add(new PopEntry(
                player,
                player.getX(), player.getY(), player.getZ(),
                System.currentTimeMillis()
        ));
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (mc.world == null || entries.isEmpty()) return;

        long now = System.currentTimeMillis();
        long life = (long) duration.getCurrent();
        float tickDelta = event.getPartialTicks();
        int baseRgb = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() & 0x00FFFFFF;

        MatrixStack matrices = event.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        Iterator<PopEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            PopEntry entry = iterator.next();
            float progress = (float) (now - entry.startTime) / life;
            if (progress >= 1.0f) {
                iterator.remove();
                continue;
            }

            float alpha = 0.7f * (1.0f - progress);

            ghostAlpha = alpha;
            ghostProgress = progress;
            ghostRgb = baseRgb;
            try {

                float origBodyYaw = entry.entity.bodyYaw;
                float origHeadYaw = entry.entity.headYaw;
                float origPitch = entry.entity.getPitch();
                float origPrevBodyYaw = entry.entity.prevBodyYaw;
                float origPrevHeadYaw = entry.entity.prevHeadYaw;
                float origPrevPitch = entry.entity.prevPitch;
                float origHandSwingProgress = entry.entity.handSwingProgress;
                float origLastHandSwingProgress = entry.entity.lastHandSwingProgress;
                boolean origHandSwinging = entry.entity.handSwinging;

                entry.entity.bodyYaw = entry.bodyYaw;
                entry.entity.headYaw = entry.headYaw;
                entry.entity.prevBodyYaw = entry.bodyYaw;
                entry.entity.prevHeadYaw = entry.headYaw;
                entry.entity.prevPitch = entry.pitch;
                entry.entity.setPitch(entry.pitch);

                LimbAnimatorMixin limbAccessor = (LimbAnimatorMixin) entry.entity.limbAnimator;
                limbAccessor.setPos(entry.limbAngle);
                limbAccessor.setSpeedField(entry.limbSpeed);
                entry.entity.handSwingProgress = 0;
                entry.entity.lastHandSwingProgress = 0;
                entry.entity.handSwinging = false;

                dispatcher.render(
                        entry.entity,
                        entry.x - camPos.x,
                        (entry.y + rise.getCurrent() * progress) - camPos.y,
                        entry.z - camPos.z,
                        tickDelta,
                        matrices,
                        immediate,
                        0xF000F0
                );
                immediate.draw();
                entry.entity.bodyYaw = origBodyYaw;
                entry.entity.headYaw = origHeadYaw;
                entry.entity.prevBodyYaw = origPrevBodyYaw;
                entry.entity.prevHeadYaw = origPrevHeadYaw;
                entry.entity.prevPitch = origPrevPitch;
                entry.entity.setPitch(origPitch);
                entry.entity.handSwingProgress = origHandSwingProgress;
                entry.entity.lastHandSwingProgress = origLastHandSwingProgress;
                entry.entity.handSwinging = origHandSwinging;
            } finally {
                ghostAlpha = -1.0f;
                ghostProgress = -1.0f;
            }
        }
    }

    private static final class PopEntry {
        private final PlayerEntity entity;
        private final double x, y, z;
        private final long startTime;
        private final float bodyYaw;
        private final float headYaw;
        private final float pitch;
        private final float limbAngle;
        private final float limbSpeed;

        private PopEntry(PlayerEntity entity, double x, double y, double z,
                         long startTime) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
            this.startTime = startTime;
            this.bodyYaw = entity.bodyYaw;
            this.headYaw = entity.headYaw;
            this.pitch = entity.getPitch();
            this.limbAngle = entity.limbAnimator.getPos();
            this.limbSpeed = entity.limbAnimator.getSpeed();
        }
    }
}