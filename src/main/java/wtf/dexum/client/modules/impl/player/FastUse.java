package wtf.dexum.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import net.minecraft.entity.player.PlayerInventory;

import java.util.List;

@ModuleAnnotation(
        name = "FastUse",
        category = Category.PLAYER,
        description = "Позволяет использовать предметы быстрее"
)
public class FastUse extends Module {
    public static final FastUse INSTANCE = new FastUse();

    private final MultiBooleanSetting mode = MultiBooleanSetting.create("Режимы", List.of("Опыт", "Еда", "Зелья"));

    private FastUse() {
    }

    @EventTarget
    private void onUpdate(EventGameUpdate event) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getStackInHand(Hand.MAIN_HAND);

        if (mode.isEnable("Опыт") && stack.getItem() instanceof ExperienceBottleItem && mc.options.useKey.isPressed()) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
        }

        if (mc.player.isUsingItem()) {
            ItemStack usingStack = mc.player.getActiveItem();
            boolean isFood = mode.isEnable("Еда") && usingStack.get(DataComponentTypes.FOOD) != null;
            boolean isPotion = mode.isEnable("Зелья") && usingStack.getItem() instanceof PotionItem;

            if (isFood || isPotion) {
                if (mc.player.getItemUseTime() >= 1) {
                    for (int i = 0; i < 32; i++) {
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
                    }
                    mc.player.stopUsingItem();
                }
            }
        }
    }
}