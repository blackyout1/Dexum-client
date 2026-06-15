package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.BowItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "ClientBow",
        category = Category.RENDER,
        description = "Визуальная траектория полета стрелы"
)
public class ClientBow extends Module {
    private final BooleanSetting markers = new BooleanSetting("Маркеры", true);
    private final BooleanSetting landingBox = new BooleanSetting("Бокс приземления", true);
    private final NumberSetting thickness = new NumberSetting("Толщина", 2.0f, 0.5f, 5.0f, 0.1f);

    public ClientBow() {
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        boolean isUsingBow = mc.player.getMainHandStack().getItem() instanceof BowItem || mc.player.getOffHandStack().getItem() instanceof BowItem;
        if (!isUsingBow) return;

        float pullProgress = BowItem.getPullProgress(mc.player.getItemUseTime());
        if (pullProgress == 0 && !mc.player.isUsingItem()) return;
        if (pullProgress == 0) pullProgress = 1.0f;

        double velocity = pullProgress * 3.0;

        Rotation rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        if (RotationComponent.instance.isRotating()) {
            rotation = RotationComponent.instance.targetRotation();
        }

        double motionX = -Math.sin(Math.toRadians(rotation.getYaw())) * Math.cos(Math.toRadians(rotation.getPitch()));
        double motionY = -Math.sin(Math.toRadians(rotation.getPitch()));
        double motionZ = Math.cos(Math.toRadians(rotation.getYaw())) * Math.cos(Math.toRadians(rotation.getPitch()));

        double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = (motionX / length) * velocity;
        motionY = (motionY / length) * velocity;
        motionZ = (motionZ / length) * velocity;

        Vec3d pos = mc.player.getEyePos();
        List<Vec3d> trajectory = new ArrayList<>();
        trajectory.add(pos);

        HitResult hit = null;
        for (int i = 0; i < 100; i++) {
            Vec3d nextPos = pos.add(motionX, motionY, motionZ);

            RaycastContext context = new RaycastContext(pos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            hit = mc.world.raycast(context);

            if (hit.getType() != HitResult.Type.MISS) {
                nextPos = hit.getPos();
                trajectory.add(nextPos);
                break;
            }

            trajectory.add(nextPos);
            pos = nextPos;

            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
            motionY -= 0.05;
        }

        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        int color = theme.getColor().getRGB();

        for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3d start = trajectory.get(i);
            Vec3d end = trajectory.get(i + 1);
            Render3DUtil.drawLine(start, end, color, thickness.getCurrent(), false);

            if (markers.isEnabled() && i % 3 == 0) {
                Render3DUtil.drawBox(new Box(end.subtract(0.05, 0.05, 0.05), end.add(0.05, 0.05, 0.05)), color, 1.0f, true, true, false);
            }
        }

        if (landingBox.isEnabled() && hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            Box landing = new Box(blockHit.getPos().subtract(0.2, 0.2, 0.2), blockHit.getPos().add(0.2, 0.2, 0.2));
            Render3DUtil.drawBox(landing, color, 1.5f, true, true, false);
        }
    }
}