package wtf.dexum.client.modules.impl.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.game.other.ChatUtil;
import wtf.dexum.utility.game.other.ScreenUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ModuleAnnotation(
        name = "AutoVillageTrade",
        category = Category.MISC,
        description = "Автоматически покупает товары у жителей"
)
public final class AutoVillageTrade extends Module {
    public static final AutoVillageTrade INSTANCE = new AutoVillageTrade();

    private static final String[] ITEM_NAMES = {
            "Золотой слиток", "Редстоун", "Лазурит", "Жемчуг Эндера",
            "Бутылочка опыта", "Стекло", "Бирка", "Стрелы",
            "Хлеб", "Золотая морковь", "Кварцевый блок", "Седло"
    };

    private static final long RESCAN_COOLDOWN = 10000L;
    private static final int MAX_EMERALDS = 64;
    private static final long CLICK_DELAY = 120L;
    private static final long SCREEN_WAIT = 2500L;

    public final ModeSetting selectedItem = new ModeSetting(
            "Что покупать",
            "Золотой слиток",
            "Редстоун",
            "Лазурит",
            "Жемчуг Эндера",
            "Бутылочка опыта",
            "Стекло",
            "Бирка",
            "Стрелы",
            "Хлеб",
            "Золотая морковь",
            "Кварцевый блок",
            "Седло"
    );

    private final NumberSetting maxPrice = new NumberSetting("Макс. цена", 64.0F, 1.0F, 64.0F, 1.0F, "Максимальная цена товара");
    private final NumberSetting emeraldStock = new NumberSetting("Запас изумрудов", 64.0F, 0.0F, 2304.0F, 64.0F, "Минимальный запас изумрудов");
    private final NumberSetting villagerRadius = new NumberSetting("Радиус жителя", 4.0F, 2.0F, 8.0F, 0.5F, "Радиус поиска жителя");
    private final NumberSetting clickDelay = new NumberSetting("Задержка (мс)", 120.0F, 50.0F, 1000.0F, 10.0F, "Задержка между действиями");
    private final NumberSetting rescanCooldown = new NumberSetting("КД рескана (сек)", 45.0F, 5.0F, 300.0F, 5.0F, "Время ожидания перед ресканом");
    private final BooleanSetting autoEmeralds = new BooleanSetting("Авто-изумруды", "Автоматически покупать изумруды", true);

    private final KeySetting routePoint = new KeySetting("Точка", -1);
    private final KeySetting storageChest = new KeySetting("Сундук", -1);

    private static BlockPos routePoint1;
    private static BlockPos routePoint2;
    private static BlockPos storagePos;

    private final List<BlockPos> routePoints = new ArrayList<>();
    private final Map<UUID, TradeInfo> tradeInfoMap = new HashMap<>();

    private enum State {
        IDLE, SCAN_ROUTE, OPEN_SCAN, WAIT_SCAN_SCREEN, READ_SCAN_SCREEN, CLOSE_SCAN_SCREEN,
        MOVE_TO_TRADE, OPEN_TRADE, WAIT_TRADE_SCREEN, BUY_TRADE, CLOSE_TRADE_SCREEN,
        MOVE_TO_STORAGE, OPEN_STORAGE, WAIT_STORAGE_SCREEN, PUT_STORAGE,
        BUY_EMERALDS_OPEN_SHOP, BUY_EMERALDS_WAIT_SHOP, BUY_EMERALDS_FIND_GOLD,
        BUY_EMERALDS_WAIT_MENU, BUY_EMERALDS_FIND_EMERALD, BUY_EMERALDS_WAIT_CONFIRM,
        BUY_EMERALDS_CONFIRM, BUY_EMERALDS_CLOSE, WAIT_RESTOCK
    }

    private State currentState = State.IDLE;
    private UUID selectedVillager;
    private int selectedTradeIndex = -1;
    private int scanIndex = 0;
    private long lastActionTime = 0;
    private Boolean oldAllowBreak = null;
    private Boolean oldAllowPlace = null;
    private BlockPos lastPathPos = null;
    private int lastPathRange = -1;
    private long autoEmeraldCooldown = 0;

    private static class TradeInfo {
        final UUID uuid;
        BlockPos villagerPos;
        int villagerId;
        int tradeIndex = -1;
        int price = -1;
        int maxPrice = Integer.MAX_VALUE;
        int itemCount = 1;
        int uses = 0;
        int maxUses = 1;
        boolean scanned = false;
        boolean unavailable = true;
        long lastScanTime;
        long cooldownTime;
        int boughtCount;
        int previousUses;

        TradeInfo(UUID uuid) {
            this.uuid = uuid;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        selectedItem.set("Золотой слиток");
        updateRoutePoints();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
        resetBaritoneSettings();
    }

    private void updateRoutePoints() {
        routePoints.clear();
        if (routePoint1 != null && routePoint2 != null) {
            int dx = routePoint2.getX() - routePoint1.getX();
            int dz = routePoint2.getZ() - routePoint1.getZ();
            int distance = Math.max(Math.abs(dx), Math.abs(dz));
            if (distance == 0) {
                routePoints.add(routePoint1);
            } else {
                for (int i = 0; i <= distance; i++) {
                    int x = routePoint1.getX() + Math.round((float) dx * i / distance);
                    int z = routePoint1.getZ() + Math.round((float) dz * i / distance);
                    BlockPos pos = new BlockPos(x, routePoint1.getY(), z);
                    if (routePoints.isEmpty() || !routePoints.get(routePoints.size() - 1).equals(pos)) {
                        routePoints.add(pos);
                    }
                }
            }
        }
    }

    private void resetState() {
        currentState = State.IDLE;
        selectedVillager = null;
        selectedTradeIndex = -1;
        scanIndex = 0;
        lastActionTime = 0;
        tradeInfoMap.clear();
        resetBaritone();
    }

    private void resetBaritone() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        } catch (Throwable ignored) {}
    }

    private void setGoal(BlockPos pos, int range) {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (!pos.equals(lastPathPos) || range != lastPathRange || !baritone.getCustomGoalProcess().isActive()) {
            lastPathPos = pos;
            lastPathRange = range;
            if (range > 0) {
                baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, range));
            } else {
                baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(pos));
            }
        }
    }

    private void resetBaritoneSettings() {
        try {
            BaritoneAPI.getSettings().allowBreak.value = oldAllowBreak;
            BaritoneAPI.getSettings().allowPlace.value = oldAllowPlace;
        } catch (Throwable ignored) {}
        oldAllowBreak = null;
        oldAllowPlace = null;
    }

    private boolean isChestOrBarrel(BlockPos pos) {
        return mc.world != null && pos != null && (
                mc.world.getBlockEntity(pos) instanceof ChestBlockEntity ||
                        mc.world.getBlockEntity(pos) instanceof BarrelBlockEntity);
    }

    private void clickScreenSlot(int syncId, int slot, int button, SlotActionType type) {
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            mc.interactionManager.clickSlot(syncId, slot, button, type, mc.player);
        }
    }

    private void interactWithEntity(Entity entity, Hand hand) {
        if (mc.player != null && mc.interactionManager != null) {
            mc.interactionManager.interactEntity(mc.player, entity, hand);
            mc.player.swingHand(hand);
        }
    }

    private void interactWithBlock(BlockPos pos, Hand hand) {
        if (mc.player != null && mc.interactionManager != null) {
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d blockCenter = Vec3d.ofCenter(pos);
            Vec3d dir = eyePos.subtract(blockCenter);
            net.minecraft.util.math.Direction direction = net.minecraft.util.math.Direction.getFacing(dir.x, dir.y, dir.z);
            Vec3d hitPos = new Vec3d(
                    pos.getX() + 0.5 + direction.getOffsetX() * 0.5,
                    pos.getY() + 0.5 + direction.getOffsetY() * 0.5,
                    pos.getZ() + 0.5 + direction.getOffsetZ() * 0.5
            );
            BlockHitResult hitResult = new BlockHitResult(hitPos, direction, pos, false);
            mc.player.swingHand(hand);
            mc.interactionManager.interactBlock(mc.player, hand, hitResult);
        }
    }

    private void onGameUpdate() {
        if (mc.player == null || mc.world == null) return;

        if (routePoints.isEmpty()) {
            ChatUtil.sendMessage("Установи две точки маршрута через бинд «Точка».");
            toggle();
            return;
        }

        if (currentState == State.IDLE) {
            tryScanRoute();
        } else {
            switch (currentState) {
                case SCAN_ROUTE -> performScanRoute();
                case OPEN_SCAN -> openScan();
                case WAIT_SCAN_SCREEN -> waitScanScreen();
                case READ_SCAN_SCREEN -> readScanScreen();
                case CLOSE_SCAN_SCREEN -> closeScanScreen();
                case MOVE_TO_TRADE -> moveToTrade();
                case OPEN_TRADE -> openTrade();
                case WAIT_TRADE_SCREEN -> waitTradeScreen();
                case BUY_TRADE -> buyTrade();
                case CLOSE_TRADE_SCREEN -> closeTradeScreen();
                case MOVE_TO_STORAGE -> moveToStorage();
                case OPEN_STORAGE -> openStorage();
                case WAIT_STORAGE_SCREEN -> waitStorageScreen();
                case PUT_STORAGE -> putStorage();
                case BUY_EMERALDS_OPEN_SHOP -> buyEmeraldsOpenShop();
                case BUY_EMERALDS_WAIT_SHOP -> buyEmeraldsWaitShop();
                case BUY_EMERALDS_FIND_GOLD -> buyEmeraldsFindGold();
                case BUY_EMERALDS_WAIT_MENU -> buyEmeraldsWaitMenu();
                case BUY_EMERALDS_FIND_EMERALD -> buyEmeraldsFindEmerald();
                case BUY_EMERALDS_WAIT_CONFIRM -> buyEmeraldsWaitConfirm();
                case BUY_EMERALDS_CONFIRM -> buyEmeraldsConfirm();
                case BUY_EMERALDS_CLOSE -> buyEmeraldsClose();
                case WAIT_RESTOCK -> waitRestock();
            }
        }
    }

    private void tryScanRoute() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;

        TradeInfo bestTrade = findBestTrade();
        if (bestTrade != null) {
            selectedVillager = bestTrade.uuid;
            selectedTradeIndex = bestTrade.tradeIndex;
            currentState = State.MOVE_TO_TRADE;
            lastActionTime = System.currentTimeMillis();
            ChatUtil.sendMessage("Иду к жителю для покупки " + selectedItem.get());
        } else if (canBuyEmeralds()) {
            currentState = State.BUY_EMERALDS_OPEN_SHOP;
            lastActionTime = System.currentTimeMillis();
            ChatUtil.sendMessage("Начинаю покупку изумрудов...");
        } else if (needsRestock()) {
            currentState = State.WAIT_RESTOCK;
            lastActionTime = System.currentTimeMillis();
        } else {
            currentState = State.SCAN_ROUTE;
            scanIndex = 0;
            lastActionTime = System.currentTimeMillis();
            ChatUtil.sendMessage("Сканирую жителей...");
        }
    }

    private void performScanRoute() {
        if (scanIndex >= routePoints.size()) {
            scanIndex = 0;
            currentState = State.IDLE;
            ChatUtil.sendMessage("Скан завершен");
            return;
        }

        BlockPos point = routePoints.get(scanIndex);
        setGoal(point, 0);
        if (mc.player.squaredDistanceTo(point.getX() + 0.5, point.getY(), point.getZ() + 0.5) < 1.44) {
            VillagerEntity villager = findVillagerAt(point);
            if (villager != null) {
                selectedVillager = villager.getUuid();
                currentState = State.OPEN_SCAN;
                lastActionTime = System.currentTimeMillis();
            } else {
                scanIndex++;
            }
        }
    }

    private VillagerEntity findVillagerAt(BlockPos pos) {
        double radius = villagerRadius.getCurrent() * 2;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof VillagerEntity villager && villager.isAlive() &&
                    mc.player.squaredDistanceTo(villager.getX(), villager.getY(), villager.getZ()) < radius * radius) {
                return villager;
            }
        }
        return null;
    }

    private void openScan() {
        VillagerEntity villager = getSelectedVillager();
        if (villager == null) {
            currentState = State.IDLE;
            return;
        }

        if (mc.player.squaredDistanceTo(villager.getPos()) > (villagerRadius.getCurrent() + 1) * (villagerRadius.getCurrent() + 1)) {
            currentState = State.MOVE_TO_TRADE;
            return;
        }

        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;

        interactWithEntity(villager, Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        currentState = State.WAIT_SCAN_SCREEN;
    }

    private void waitScanScreen() {
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            currentState = State.READ_SCAN_SCREEN;
            lastActionTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastActionTime > SCREEN_WAIT) {
            currentState = State.IDLE;
        }
    }

    private void readScanScreen() {
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
            TradeOfferList trades = handler.getRecipes();
            updateTradeInfo(selectedVillager, trades);
            currentState = State.CLOSE_SCAN_SCREEN;
            lastActionTime = System.currentTimeMillis();
        } else {
            currentState = State.IDLE;
        }
    }

    private void closeScanScreen() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        currentState = State.SCAN_ROUTE;
        lastActionTime = System.currentTimeMillis();
    }

    private void moveToTrade() {
        VillagerEntity villager = getSelectedVillager();
        if (villager == null) {
            currentState = State.IDLE;
            return;
        }

        setGoal(villager.getBlockPos(), (int) villagerRadius.getCurrent() + 1);
        if (mc.player.squaredDistanceTo(villager.getX(), villager.getY(), villager.getZ()) < (villagerRadius.getCurrent() + 0.5) * (villagerRadius.getCurrent() + 0.5)) {
            currentState = State.OPEN_TRADE;
            lastActionTime = System.currentTimeMillis();
        }
    }

    private void openTrade() {
        VillagerEntity villager = getSelectedVillager();
        if (villager == null || !villager.isAlive()) {
            currentState = State.IDLE;
            return;
        }

        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;

        interactWithEntity(villager, Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        currentState = State.WAIT_TRADE_SCREEN;
    }

    private void waitTradeScreen() {
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            currentState = State.BUY_TRADE;
            lastActionTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastActionTime > SCREEN_WAIT) {
            currentState = State.IDLE;
        }
    }

    private void buyTrade() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            currentState = State.IDLE;
            return;
        }

        TradeInfo info = tradeInfoMap.get(selectedVillager);
        if (info == null || info.tradeIndex < 0) {
            currentState = State.IDLE;
            return;
        }

        TradeOffer trade = handler.getRecipes().get(info.tradeIndex);
        if (trade == null) {
            currentState = State.CLOSE_TRADE_SCREEN;
            return;
        }

        if (!trade.isDisabled() && info.price <= maxPrice.getCurrent()) {
            handler.setRecipeIndex(info.tradeIndex);
            handler.switchTo(info.tradeIndex);
            mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(info.tradeIndex));

            Slot slot = handler.getSlot(2);
            if (slot.hasStack() && slot.getStack().isOf(getItemToBuy())) {
                int count = Math.max(1, slot.getStack().getCount());
                clickScreenSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE);
                info.boughtCount += count;
                info.previousUses = info.uses;
                info.uses++;
                lastActionTime = System.currentTimeMillis();
                currentState = State.CLOSE_TRADE_SCREEN;
            }
        }
    }

    private void closeTradeScreen() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        currentState = State.IDLE;
        lastActionTime = System.currentTimeMillis();
    }

    private void moveToStorage() {
        if (storagePos == null) {
            ChatUtil.sendMessage("Сундук для складирования не установлен.");
            currentState = State.IDLE;
            return;
        }

        setGoal(storagePos, 2);
        if (mc.player.squaredDistanceTo(storagePos.getX() + 0.5, storagePos.getY(), storagePos.getZ() + 0.5) < 12.25) {
            currentState = State.OPEN_STORAGE;
            lastActionTime = System.currentTimeMillis();
        }
    }

    private void openStorage() {
        if (storagePos == null || !isChestOrBarrel(storagePos)) {
            currentState = State.IDLE;
            return;
        }

        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;

        interactWithBlock(storagePos, Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        currentState = State.WAIT_STORAGE_SCREEN;
    }

    private void waitStorageScreen() {
        if (ScreenUtil.hasOpenScreen(mc)) {
            currentState = State.PUT_STORAGE;
            lastActionTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastActionTime > SCREEN_WAIT) {
            currentState = State.OPEN_STORAGE;
        }
    }

    private void putStorage() {
        GenericContainerScreen screen = ScreenUtil.get(mc, null, GenericContainerScreen.class);
        if (screen == null) {
            currentState = State.IDLE;
            return;
        }

        int itemSlot = findItemInInventory(getItemToBuy());
        if (itemSlot < 0) {
            currentState = State.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
        int storageSlot = findEmptySlot(handler);
        if (storageSlot < 0) {
            ChatUtil.sendMessage("Сундук заполнен.");
            currentState = State.IDLE;
            return;
        }

        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;

        clickScreenSlot(handler.syncId, itemSlot, 0, SlotActionType.QUICK_MOVE);
        lastActionTime = System.currentTimeMillis();
        currentState = State.PUT_STORAGE;
    }

    private void buyEmeraldsOpenShop() {
        if (mc.player != null) {
            mc.getNetworkHandler().sendChatCommand("shop");
            lastActionTime = System.currentTimeMillis();
            currentState = State.BUY_EMERALDS_WAIT_SHOP;
        }
    }

    private void buyEmeraldsWaitShop() {
        if (ScreenUtil.hasOpenScreen(mc)) {
            currentState = State.BUY_EMERALDS_FIND_GOLD;
            lastActionTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastActionTime > SCREEN_WAIT) {
            currentState = State.IDLE;
        }
    }

    private void buyEmeraldsFindGold() {
        GenericContainerScreen screen = ScreenUtil.get(mc, null, GenericContainerScreen.class);
        if (screen == null) {
            currentState = State.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
        int goldSlot = findSlotByItem(handler, Items.GOLD_INGOT);
        if (goldSlot >= 0) {
            clickScreenSlot(handler.syncId, goldSlot, 0, SlotActionType.PICKUP);
            lastActionTime = System.currentTimeMillis();
            currentState = State.BUY_EMERALDS_WAIT_MENU;
        }
    }

    private void buyEmeraldsWaitMenu() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;
        currentState = State.BUY_EMERALDS_FIND_EMERALD;
        lastActionTime = System.currentTimeMillis();
    }

    private void buyEmeraldsFindEmerald() {
        GenericContainerScreen screen = ScreenUtil.get(mc, null, GenericContainerScreen.class);
        if (screen == null) {
            currentState = State.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
        int emeraldSlot = findSlotByItem(handler, Items.EMERALD);
        if (emeraldSlot >= 0) {
            clickScreenSlot(handler.syncId, emeraldSlot, 1, SlotActionType.PICKUP);
            lastActionTime = System.currentTimeMillis();
            currentState = State.BUY_EMERALDS_WAIT_CONFIRM;
        }
    }

    private void buyEmeraldsWaitConfirm() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;
        currentState = State.BUY_EMERALDS_CONFIRM;
        lastActionTime = System.currentTimeMillis();
    }

    private void buyEmeraldsConfirm() {
        GenericContainerScreen screen = ScreenUtil.get(mc, null, GenericContainerScreen.class);
        if (screen == null) {
            currentState = State.IDLE;
            return;
        }

        GenericContainerScreenHandler handler = (GenericContainerScreenHandler) screen.getScreenHandler();
        int confirmSlot = findSlotByText(handler, "Купить");
        if (confirmSlot >= 0) {
            clickScreenSlot(handler.syncId, confirmSlot, 0, SlotActionType.PICKUP);
            lastActionTime = System.currentTimeMillis();
            currentState = State.BUY_EMERALDS_CLOSE;
        }
    }

    private void buyEmeraldsClose() {
        if (System.currentTimeMillis() - lastActionTime < getClickDelay()) return;
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        currentState = State.IDLE;
        lastActionTime = System.currentTimeMillis();
    }

    private void waitRestock() {
        if (System.currentTimeMillis() - lastActionTime > getRescanCooldown()) {
            currentState = State.IDLE;
            ChatUtil.sendMessage("Рескан жителей...");
        }
    }

    private TradeInfo findBestTrade() {
        return tradeInfoMap.values().stream()
                .filter(info -> info.scanned && !info.unavailable && info.tradeIndex >= 0 && info.price <= maxPrice.getCurrent())
                .min(Comparator.comparingDouble(info -> (double) info.price / info.itemCount))
                .orElse(null);
    }

    private void updateTradeInfo(UUID uuid, TradeOfferList trades) {
        if (trades == null) return;
        TradeInfo info = tradeInfoMap.computeIfAbsent(uuid, TradeInfo::new);

        Item item = getItemToBuy();
        for (int i = 0; i < trades.size(); i++) {
            TradeOffer trade = trades.get(i);
            if (!trade.isDisabled() && trade.getSellItem().isOf(item)) {
                int price = getTradePrice(trade);
                if (price < info.maxPrice) {
                    info.tradeIndex = i;
                    info.price = price;
                    info.itemCount = trade.getSellItem().getCount();
                    info.maxPrice = price;
                }
            }
        }
        info.scanned = true;
        info.unavailable = false;
    }

    private int getTradePrice(TradeOffer trade) {
        int price = 0;
        if (!trade.getDisplayedFirstBuyItem().isEmpty() && trade.getDisplayedFirstBuyItem().isOf(Items.EMERALD)) {
            price += trade.getDisplayedFirstBuyItem().getCount();
        }
        if (!trade.getDisplayedSecondBuyItem().isEmpty() && trade.getDisplayedSecondBuyItem().isOf(Items.EMERALD)) {
            price += trade.getDisplayedSecondBuyItem().getCount();
        }
        return price <= 0 ? Integer.MAX_VALUE : price;
    }

    private boolean canBuyEmeralds() {
        return autoEmeralds.isEnabled() && getEmeraldCount() < emeraldStock.getCurrent() && !needsEmeraldCooldown();
    }

    private boolean needsEmeraldCooldown() {
        return System.currentTimeMillis() - autoEmeraldCooldown < 10000;
    }

    private boolean needsRestock() {
        return !tradeInfoMap.isEmpty() && tradeInfoMap.values().stream().anyMatch(info -> info.scanned && !info.unavailable);
    }

    private int getEmeraldCount() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int findItemInInventory(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptySlot(GenericContainerScreenHandler handler) {
        for (int i = handler.getRows() * 9; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).hasStack()) {
                return i;
            }
        }
        return -1;
    }

    private int findSlotByItem(ScreenHandler handler, Item item) {
        int start = Math.min(handler.slots.size(), Math.max(0, handler.slots.size() - 36));
        for (int i = start; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).hasStack() && handler.getSlot(i).getStack().isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findSlotByText(ScreenHandler handler, String text) {
        int start = Math.min(handler.slots.size(), Math.max(0, handler.slots.size() - 36));
        for (int i = start; i < handler.slots.size(); i++) {
            String name = handler.getSlot(i).getStack().getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(text.toLowerCase(Locale.ROOT))) {
                return i;
            }
        }
        return -1;
    }

    private VillagerEntity getSelectedVillager() {
        if (selectedVillager == null) return null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof VillagerEntity villager && selectedVillager.equals(villager.getUuid())) {
                return villager;
            }
        }
        return null;
    }

    private Item getItemToBuy() {
        String name = selectedItem.get();
        return switch (name) {
            case "Редстоун" -> Items.REDSTONE;
            case "Лазурит" -> Items.LAPIS_LAZULI;
            case "Жемчуг Эндера" -> Items.ENDER_PEARL;
            case "Бутылочка опыта" -> Items.EXPERIENCE_BOTTLE;
            case "Стекло" -> Items.GLASS;
            case "Бирка" -> Items.NAME_TAG;
            case "Стрелы" -> Items.ARROW;
            case "Хлеб" -> Items.BREAD;
            case "Золотая морковь" -> Items.GOLDEN_CARROT;
            case "Кварцевый блок" -> Items.QUARTZ_BLOCK;
            case "Седло" -> Items.SADDLE;
            default -> Items.GOLD_INGOT;
        };
    }

    private long getClickDelay() {
        return Math.max(50L, (long) clickDelay.getCurrent());
    }

    private long getRescanCooldown() {
        return Math.max(1000L, (long) (rescanCooldown.getCurrent() * 1000));
    }

    @EventTarget
    private void onUpdate(EventGameUpdate event) {
        onGameUpdate();
    }

    public void handleKeyPress(String settingName) {
        if (mc.player == null || mc.world == null) return;
        
        if (settingName.equals("Точка")) {
            if (routePoint1 == null) {
                routePoint1 = mc.player.getBlockPos();
                ChatUtil.sendMessage("Поставлена точка 1");
            } else if (routePoint2 == null) {
                routePoint2 = mc.player.getBlockPos();
                ChatUtil.sendMessage("Поставлена точка 2");
                updateRoutePoints();
            }
        } else if (settingName.equals("Сундук")) {
            storagePos = mc.player.getBlockPos();
            ChatUtil.sendMessage("Сундук для складирования установлен.");
        }
    }
    
    @EventTarget
    private void onKey(wtf.dexum.base.events.impl.input.EventKey event) {
        if (event.getAction() != 1 || mc.currentScreen != null) return;

        // Сохраняем последний нажатый код для проверки в onUpdate
        lastKeyPressed = event.getKeyCode();
        
        if (routePoint.getKeyCode() != -1 && routePoint.getKeyCode() == event.getKeyCode()) {
            handleKeyPress("Точка");
        }

        if (storageChest.getKeyCode() != -1 && storageChest.getKeyCode() == event.getKeyCode()) {
            handleKeyPress("Сундук");
        }
    }
    
    private int lastKeyPressed = -1;
}