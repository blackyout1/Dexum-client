package wtf.dexum.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.other.EventTick;
import wtf.dexum.base.events.impl.player.EventAttack;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

import java.util.List;
import java.util.Objects;

import static wtf.dexum.utility.interfaces.IMinecraft.mc;

@ModuleAnnotation(
        name = "AirStuck",
        category = Category.MOVEMENT,
        description = "Зависает в воздухе"
)
public class AirStuck extends Module {
    public static AirStuck INSTANCE = new AirStuck();

    public final BooleanSetting onlyFalling = new BooleanSetting("Только при падении", false);
    private final BooleanSetting swapToChestplate = new BooleanSetting("Свап на нагрудник", false);
    private final BooleanSetting swapBack = new BooleanSetting("Свапать обратно", false);

    private boolean wearingElytra;
    public boolean frozen;
    private Vec3d frozenPos;
    private Packet<?> lastPacket;
    private int tickCounter;

    private boolean sending = false;

    @Override
    public void onEnable() {
        super.onEnable();
        frozen = false;
        frozenPos = null;
        lastPacket = null;
        wearingElytra = false;
        tickCounter = 0;
        sending = false;
    }

    @Override
    public void onDisable() {
        if (swapBack.isEnabled() && wearingElytra) {
            trySwapToElytra();
        }

        super.onDisable();

        if (mc.player != null && frozenPos != null) {
            mc.player.setVelocity(Vec3d.ZERO);
        }

        frozenPos = null;
        frozen = false;
        lastPacket = null;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        tickCounter++;

        if (frozen && frozenPos != null) {
            mc.player.setPosition(frozenPos.x, frozenPos.y, frozenPos.z);
            mc.player.setVelocity(0, 0, 0);
            mc.player.fallDistance = 0.0F;
        }
    }

    @EventTarget
    public void onGameUpdate(EventGameUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (frozen) return;

        if (onlyFalling.isEnabled()) {
            if (mc.player.isOnGround() && !mc.world.isAir(mc.player.getBlockPos().down())) {
                return;
            }
        }

        wearingElytra = mc.player.isGliding()
                || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;

        if (swapToChestplate.isEnabled() && wearingElytra) {
            trySwapToChestplate();
        }

        frozenPos = mc.player.getPos();
        frozen = true;
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!frozen || mc.player == null || frozenPos == null) return;
        if (!e.isSent()) return;
        if (sending) return;

        if (e.getPacket() instanceof PlayerMoveC2SPacket movePacket) {
            if (movePacket instanceof PlayerMoveC2SPacket.Full
                    || movePacket instanceof PlayerMoveC2SPacket.PositionAndOnGround) {
                lastPacket = movePacket;
            }
            e.cancel();
        }
    }

    @EventTarget
    public void onAttack(EventAttack e) {
        if (!frozen || mc.player == null || frozenPos == null) return;
        if (mc.getNetworkHandler() == null) return;

        sending = true;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                frozenPos.x, frozenPos.y, frozenPos.z,
                mc.player.getYaw(), mc.player.getPitch(),
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
        sending = false;
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || !frozen) return;
        mc.player.setVelocity(Vec3d.ZERO);
    }

    private void trySwapToChestplate() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

        int slot = PlayerInventoryUtil.find(
                List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
                        Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                        Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE), 0, 8);
        if (slot != -1) {
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    private void trySwapToElytra() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return;

        int slot = PlayerInventoryUtil.find(Items.ELYTRA, 0, 8);
        if (slot != -1) {
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }
}