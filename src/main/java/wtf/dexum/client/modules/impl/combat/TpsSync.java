package wtf.dexum.client.modules.impl.combat;

import net.minecraft.util.math.MathHelper;
import wtf.dexum.Dexum;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "TpsSync",
        category = Category.COMBAT,
        description = "Синхронизация с TPS сервера"
)
public final class TpsSync extends Module {

    public static final TpsSync INSTANCE = new TpsSync();

    private TpsSync() {
    }

    public float getCurrentTPS() {
        if (Dexum.getInstance().getServerHandler() == null) {
            return 20.0f;
        }
        float tps = Dexum.getInstance().getServerHandler().getTPS();
        return MathHelper.clamp(tps, 0.1f, 20.0f);
    }

    public long getAdjustedCooldown(long baseCooldown) {
        if (!isEnabled()) {
            return baseCooldown;
        }

        float tps = getCurrentTPS();
        if (tps >= 20.0f) {
            return baseCooldown;
        }

        float multiplier = 20.0f / tps;
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long) (baseCooldown * multiplier * additionalFactor);

        return Math.min(adjusted, 3000);
    }

    public boolean canAttack(long lastAttackTime, long baseCooldown, long currentTime) {
        if (!isEnabled()) {
            return currentTime >= lastAttackTime + baseCooldown;
        }

        long adjustedCooldown = getAdjustedCooldown(baseCooldown);
        return currentTime >= lastAttackTime + adjustedCooldown;
    }
}