package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import wtf.dexum.base.events.impl.player.EventMoveInput;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "AutoTotem",
        category = Category.COMBAT,
        description = "Автоматически берёт тотем в опасности"
)
public class AutoTotem extends Module {

    public static AutoTotem INSTANCE = new AutoTotem();

    private final MultiBooleanSetting triggers = new MultiBooleanSetting("Брать от",
            new MultiBooleanSetting.Value("Кристалл рядом", true),
            new MultiBooleanSetting.Value("Кристалл в руке", true),
            new MultiBooleanSetting.Value("Обсидиан в руке", true),
            new MultiBooleanSetting.Value("Падения", true));

    private final NumberSetting hp = new NumberSetting("Брать от хп", 6, 1, 20, 0.5f);
    private final NumberSetting hpOnElytra = new NumberSetting("Хп на элитрах", 10, 1, 20, 0.5f);
    private final NumberSetting crystalRadius = new NumberSetting("Радиус кристалла", 6, 1, 12, 0.5f,
            this::isCrystalRadiusVisible);
    private final NumberSetting fallHeight = new NumberSetting("Высота падения", 10, 3, 50, 1f,
            () -> triggers.isEnable("Падения"));
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять зачар", true);
    private final BooleanSetting returnTotem = new BooleanSetting("Возвращать тотем", true);
    private final NumberSetting returnDelay = new NumberSetting("Задержка возврата", 20, 5, 100, 5f,
            () -> returnTotem.isEnabled());
    private final BooleanSetting bypassgrim = new BooleanSetting("Обходить Grim", true);

    private final ModeSetting swapVersion = new ModeSetting("Версия свапа", "1.21.4", "1.21.4", "1.16.5");
    private int bypassTicks;
    private int swapCooldown;
    private int savedTotemSlot = -1;
    private ItemStack originalOffhandItem = ItemStack.EMPTY;
    private boolean totemTakenByUs = false;
    private boolean returnMode = false;
    private boolean needFastSwap = false;
    private int safeTicks = 0;

    public AutoTotem() {
    }

    @EventTarget
    public void onInput(final EventMoveInput e) {
        if (bypassgrim.isEnabled() && bypassTicks > 0) {
            if (mc.player == null) return;
            mc.player.setSprinting(false);
            e.setForward(0);
            e.setStrafe(0);
        }
    }

    @EventTarget
    public void onUpdate(final EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        boolean isCrystalDanger = isCrystalDanger();

        if (isCrystalDanger) {
            needFastSwap = true;
            safeTicks = 0;
        }

        if (swapCooldown > 0) {
            swapCooldown--;
        }

        if (bypassgrim.isEnabled() && bypassTicks > 0) {
            mc.player.setSprinting(false);
            bypassTicks--;

            if (bypassTicks <= 0) {
                if (returnMode) {
                    performReturn();
                } else {
                    performSwap();
                }
            }
            return;
        }

        boolean needTotem = shouldTakeTotem(isCrystalDanger);

        if (needTotem && !hasTotemInOffhand()) {
            int totemSlot = findTotemSlot();
            if (totemSlot == -1) return;

            if (!needFastSwap && swapCooldown > 0) return;

            if (originalOffhandItem.isEmpty() && !totemTakenByUs) {
                originalOffhandItem = mc.player.getOffHandStack().copy();
            }

            savedTotemSlot = totemSlot;
            returnMode = false;
            safeTicks = 0;

            if (bypassgrim.isEnabled()) {
                bypassTicks = needFastSwap ? 1 : 2;
                swapCooldown = needFastSwap ? 0 : 2;
            } else {
                performSwap();
                swapCooldown = needFastSwap ? 0 : 2;
            }
        }

        boolean isSafe = !needTotem;

        if (isSafe) {
            safeTicks++;
        } else {
            safeTicks = 0;
        }

        if (returnTotem.isEnabled() && !needTotem && hasTotemInOffhand() && totemTakenByUs
                && safeTicks >= returnDelay.getCurrent()) {
            if (!needFastSwap && swapCooldown > 0) return;

            returnMode = true;

            if (bypassgrim.isEnabled()) {
                bypassTicks = needFastSwap ? 1 : 2;
                swapCooldown = needFastSwap ? 0 : 2;
            } else {
                performReturn();
                swapCooldown = needFastSwap ? 0 : 2;
            }
        }

        if (!isCrystalDanger) {
            needFastSwap = false;
        }
    }

    private boolean isCrystalDanger() {
        float radius = crystalRadius.getCurrent();
        double radiusSq = radius * radius;

        if (triggers.isEnable("Кристалл рядом")) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    if (mc.player.squaredDistanceTo(entity) <= radiusSq) {
                        return true;
                    }
                }
            }
        }

        if (triggers.isEnable("Кристалл в руке")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (mc.player.squaredDistanceTo(player) <= radiusSq) {
                    if (player.getMainHandStack().isOf(Items.END_CRYSTAL)
                            || player.getOffHandStack().isOf(Items.END_CRYSTAL)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldTakeTotem(boolean isCrystalDanger) {
        float currentHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean isGliding = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && mc.player.isGliding();

        float hpThreshold = isGliding ? hpOnElytra.getCurrent() : hp.getCurrent();

        if (currentHp <= hpThreshold) {
            return true;
        }

        if (isCrystalDanger) {
            return true;
        }

        float radius = crystalRadius.getCurrent();
        double radiusSq = radius * radius;

        if (triggers.isEnable("Обсидиан в руке")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (mc.player.squaredDistanceTo(player) <= radiusSq) {
                    if (player.getMainHandStack().isOf(Items.OBSIDIAN) || player.getOffHandStack().isOf(Items.OBSIDIAN)) {
                        return true;
                    }
                }
            }
        }

        if (triggers.isEnable("Падения")) {
            if (mc.player.fallDistance >= fallHeight.getCurrent() && !isGliding) {
                return true;
            }
        }

        return false;
    }

    private boolean hasTotemInOffhand() {
        return mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private int findTotemSlot() {
        int normalTotem = -1;
        int enchantedTotem = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                boolean isEnchanted = stack.hasEnchantments();

                if (isEnchanted) {
                    if (enchantedTotem == -1) {
                        enchantedTotem = i;
                    }
                } else if (normalTotem == -1) {
                    normalTotem = i;
                }
            }
        }

        if (saveEnchanted.isEnabled()) {
            return normalTotem != -1 ? normalTotem : enchantedTotem;
        } else {
            return enchantedTotem != -1 ? enchantedTotem : normalTotem;
        }
    }

    private void performSwap() {
        int totemSlot = findTotemSlot();

        if (totemSlot == -1) {
            return;
        }

        savedTotemSlot = totemSlot;
        doSwap(totemSlot);
        totemTakenByUs = true;
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
    }

    private void performReturn() {
        if (!hasTotemInOffhand()) {
            totemTakenByUs = false;
            return;
        }

        if (!originalOffhandItem.isEmpty()) {
            int slotToReturn = findSlotForItem(originalOffhandItem);
            if (slotToReturn != -1) {
                doSwap(slotToReturn);
            } else {
                if (savedTotemSlot == -1) {
                    savedTotemSlot = 9;
                }
                doSwap(savedTotemSlot);
            }
        } else {
            if (savedTotemSlot == -1) {
                savedTotemSlot = 9;
            }
            doSwap(savedTotemSlot);
        }

        totemTakenByUs = false;
        savedTotemSlot = -1;
        originalOffhandItem = ItemStack.EMPTY;
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
    }

    private int findSlotForItem(ItemStack item) {
        if (item.isEmpty()) return -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (ItemStack.areItemsEqual(stack, item) && ItemStack.areEqual(stack, item)) {
                return i;
            }
        }
        return -1;
    }

    private void doSwap(int slot) {
        if (swapVersion.is("1.16.5")) {
            doSwap1165(slot);
            return;
        }

        doSwap1214(slot);
    }

    private void doSwap1214(int slot) {
        if (slot >= 36 && slot <= 44) {
            int hotbarSlot = slot - 36;
            mc.interactionManager.clickSlot(0, 45, hotbarSlot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        }
    }

    private void doSwap1165(int slot) {
        mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
    }

    private boolean isCrystalRadiusVisible() {
        return triggers.isEnable("Кристалл рядом")
                || triggers.isEnable("Кристалл в руке")
                || triggers.isEnable("Обсидиан в руке");
    }

    @Override
    public void onDisable() {
        bypassTicks = 0;
        swapCooldown = 0;
        savedTotemSlot = -1;
        originalOffhandItem = ItemStack.EMPTY;
        totemTakenByUs = false;
        returnMode = false;
        needFastSwap = false;
        safeTicks = 0;
        super.onDisable();
    }
}