package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.utility.game.player.rotation.Rotation;

public class HitChanceUtil {
    public static float get(LivingEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (target == null || mc.player == null) return 0f;
        Vec3d center = target.getBoundingBox().getCenter();
        Rotation rot = Rotation.getRotations(center);
        float diffYaw = Math.abs(MathHelper.wrapDegrees(rot.getYaw() - mc.player.getYaw()));
        float diffPitch = Math.abs(rot.getPitch() - mc.player.getPitch());
        float ang = (float) Math.hypot(diffYaw, diffPitch);
        float dist = mc.player.distanceTo(target);
        float angScore = MathHelper.clamp(1f - ang / 70f, 0f, 1f);
        float distScore = MathHelper.clamp(1f - (dist - 3f) / 4f, 0f, 1f);
        return angScore * 0.75f + distScore * 0.25f;
    }
}