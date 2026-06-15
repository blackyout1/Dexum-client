package wtf.dexum.client.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

import java.util.UUID;

@ModuleAnnotation(
        name = "FakePlayer",
        category = Category.MISC,
        description = "Создает локального фейкового игрока для тестов"
)
public class FakePlayer extends Module {
    public static final FakePlayer INSTANCE = new FakePlayer();
    private OtherClientPlayerEntity fakePlayer;

    private FakePlayer() {}

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            this.setEnabled(false);
            return;
        }

        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "trfxcvii_GHOST"));
        fakePlayer.copyFrom(mc.player);
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            fakePlayer.getInventory().setStack(i, mc.player.getInventory().getStack(i).copy());
        }
        fakePlayer.refreshPositionAndAngles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch());

        mc.world.addEntity(fakePlayer);
    }

    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
    }
}