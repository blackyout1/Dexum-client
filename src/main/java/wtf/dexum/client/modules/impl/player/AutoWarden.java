package wtf.dexum.client.modules.impl.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalNear;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.StringSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.Timer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleAnnotation(name = "AutoWarden", description = "Авто-варден фарм (без бочек, обход скалкрикунов, жёсткая камера, таймер на расстоянии)", category = Category.PLAYER)
public final class AutoWarden extends Module implements IMinecraft {

    public static final AutoWarden INSTANCE = new AutoWarden();

    // ── Настройки ─────────────────────────────────────────────────────────
    private final StringSetting wardenHome    = new StringSetting("Хом вардена",          "warden");
    private final StringSetting supplyHome    = new StringSetting("Хом базы (инвиз)",     "st");
    private final StringSetting stashHome     = new StringSetting("Хом стежа",            "home");
    private final StringSetting supplySign    = new StringSetting("Слово на табличке",    "инвиз");
    private final StringSetting leaveSeconds  = new StringSetting("Макс. ожидание (сек)", "60");
    private final BooleanSetting waitForBossBar = new BooleanSetting("Ждать боссбар", "Ждать окончания таймера на боссбаре перед депозитом", true);

    // ── Паттерны таймера ──────────────────────────────────────────────────
    private static final Pattern PAT_TIME   = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?!\\d)");
    private static final Pattern PAT_MINSEC = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*(?:мин[.]?|min[.]?)\\s*(?:(\\d{1,2})\\s*(?:сек[.]?|sec[.]?))?");
    private static final Pattern PAT_SEC    = Pattern.compile("(?<![:\\d])(\\d{1,3})\\s*(?:секунд[аы]?|сек[.]?|sec(?:ond)?s?|с[.]|s[.])");

    private static final String[] LOOT_IDS = {
            "totem_of_undying","netherite_helmet","netherite_chestplate","netherite_leggings",
            "netherite_boots","netherite_sword","netherite_pickaxe","enchanted_golden_apple",
            "shulker_box","netherite_ingot","dragon_head","elytra","beacon","ender_eye",
            "diamond","golden_apple","tnt","iron_nugget","amethyst_shard","goat_horn",
            "name_tag","netherite_scrap","villager_spawn_egg","firework_rocket",
            "phantom_membrane","paper","splash_potion","emerald"
    };

    // Тайминги (с рандомизацией)
    private static final long CLAN_HOME_DELAY     = 7_000L;
    private static final long DARENA_DELAY        = 0L;
    private static final long TP_WAIT_MS          = 7_000L;
    private static final long TP_FAST_MS          = 1_500L;
    private static final long CLAN_ST_WAIT_MS     = 2_000L;
    private static final long TICK_MS             = 100L;
    private static final long STUCK_MS            = 3_000L;
    private static final long CANDLE_MS           = 120L;
    private static final long OPEN_TIMEOUT_MS     = 25_000L;
    private static final long OPEN_DELAY_MS       = 500L;
    private static final long ARENA_THRESHOLD_MS  = 15_000L;
    private static final long NO_TIMER_IGNORE_MS  = 600_000L;
    private static final long POST_DEATH_WAIT_MS  = 10_000L;
    private static final int  SEARCH_RADIUS       = 80; // немного уменьшил, чтобы не лагать
    private static final int  SEARCH_Y_RADIUS     = 6;
    private static final double RANGE_SQ          = 9.0;
    private static final double RETREAT_DIST_SQ   = 16.0;
    private static final double MAX_HOLOGRAM_READ_DIST_SQ = 100.0; // не используется
    // Максимальное расстояние для сундуков без таймера (20 блоков)
    private static final double UNTIMED_CHEST_RANGE_SQ = 400.0;
    private static final String CLAN_PFX          = "clan home ";

    // Константы для арены
    private static final long ARENA_SETHOME_DELAY = 1500L;
    private static final long ARENA_HOME_DELAY    = 7000L;
    private static final long MOVE_TO_CHEST_LEAD_MS = 10_000L;

    // Целевые углы (для жёсткой фиксации)
    private float targetYaw, targetPitch;

    private enum S {
        IDLE,
        DEAD_WAIT,
        SUPPLY_TP, SUPPLY_FIND, SUPPLY_PATH, SUPPLY_OPEN, SUPPLY_TAKE, SUPPLY_DRINK,
        WARDEN_TP, WARDEN_FIND, WARDEN_PATH, WARDEN_WAIT, WARDEN_OPEN_DELAY, WARDEN_OPEN, WARDEN_LOOT,
        WARDEN_RETREAT, WARDEN_WAIT_BOSS,
        DEPOSIT_TP, DEPOSITING,
        STASH_TP, STASH_FIND, STASH_PATH, STASH_DEPOSIT, STASH_WITHDRAW_TP, STASH_WITHDRAW,
        ARENA_SETHOME, ARENA_DARENA, ARENA_CLICK, ARENA_WAIT, ARENA_RETURN, ARENA_RETURN_WAIT
    }

    private S state = S.IDLE;
    private final Timer tick      = new Timer();
    private final Timer candle    = new Timer();
    private final Timer drink     = new Timer();
    private final Random random   = new Random();

    private BlockPos targetChest  = null;
    private BlockPos stashChest   = null;
    private BlockPos wardenSpawn  = null;
    private BlockPos homePos      = null;
    private BlockPos retreatFrom  = null;
    private long     chestOpenAt  = -1L;
    private boolean  cmdSent      = false;
    private boolean  tpSeen       = false;
    private boolean  lockedChest  = false;

    private boolean drinking      = false;
    private int     drinkDelay    = 0;
    private int     prevSlot      = 0;

    private long    bossTimerFound = -1L;
    private boolean bossWasActive  = false;

    private int     depositIdx      = 0;
    private int     depositAttempts = 0;
    private int     clanStorageRetries = 0;

    private long    stuckSince  = -1L;
    private BlockPos candlePos  = null;

    private boolean tpBarPaused = false;
    private long    barEndedAt  = -1L;

    private final Map<BlockPos, Long> ignoredChests = new HashMap<>();

    private boolean takeOnePending   = false;
    private int     takeOneChestSlot = -1;

    private long    lastInvisCheck = 0L;
    private static final long INVIS_CHECK_INTERVAL = 5_000L;

    private long lastIgnoredCleanup = 0L;
    private static final long IGNORED_CLEANUP_INTERVAL = 600_000L;

    private boolean lootedCurrentChest = false;

    private AutoWarden() {}

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;
        reset();
        configureBaritone(true);
        state = S.IDLE;
        tick.reset();
        lastIgnoredCleanup = System.currentTimeMillis();
        ignoredChests.clear();
        lootedCurrentChest = false;
        targetYaw = mc.player.getYaw();
        targetPitch = mc.player.getPitch();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cancelBaritone();
        closeScreen();
        stopDrinking();
        configureBaritone(false);
        mc.options.backKey.setPressed(false);
        reset();
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (e.getPacket() instanceof GameMessageS2CPacket pkt) {
            String msg = pkt.content().getString().toLowerCase(Locale.ROOT);
            if (msg.contains("вы погибли") || msg.contains("you died")) {
                cancelBaritone(); closeScreen(); stopDrinking();
                mc.options.backKey.setPressed(false);
                reset();
                state = S.DEAD_WAIT;
                tick.reset();
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        try { mainTick(); } catch (Exception ex) { ex.printStackTrace(); reset(); state = S.IDLE; }
    }

    private void mainTick() {
        long now = System.currentTimeMillis();
        if (now - lastIgnoredCleanup > IGNORED_CLEANUP_INTERVAL) {
            ignoredChests.clear();
            lastIgnoredCleanup = now;
        }

        if (state == S.DEAD_WAIT) {
            if (tick.finished(POST_DEATH_WAIT_MS)) {
                go(S.SUPPLY_TP);
            }
            return;
        }

        if (isTpBar()) {
            if (!tpBarPaused) { tpBarPaused = true; cancelBaritone(); closeScreen(); tick.reset(); }
            return;
        } else if (tpBarPaused) {
            tpBarPaused = false; barEndedAt = System.currentTimeMillis(); tpSeen = true; tick.reset();
        }

        if (now - lastInvisCheck > INVIS_CHECK_INTERVAL) {
            lastInvisCheck = now;
            if (isWardenState() && !hasInvis()) {
                cancelBaritone(); closeScreen(); stopDrinking();
                cmdSent = false;
                go(S.SUPPLY_TP);
                return;
            }
        }

        if (state == S.WARDEN_WAIT_BOSS && waitForBossBar.isEnabled()) {
            checkBossBarTimer();
        }

        if (drinking) { tickDrink(); return; }

        if (state == S.WARDEN_PATH || state == S.SUPPLY_PATH || state == S.STASH_PATH) breakCandles();
        checkStuck();

        // Жёсткая фиксация камеры
        updateRotation();

        switch (state) {
            case IDLE              -> tickIdle();
            case SUPPLY_TP         -> tickSupplyTp();
            case SUPPLY_FIND       -> tickSupplyFind();
            case SUPPLY_PATH       -> tickSupplyPath();
            case SUPPLY_OPEN       -> tickSupplyOpen();
            case SUPPLY_TAKE       -> tickSupplyTake();
            case SUPPLY_DRINK      -> tickSupplyDrink();
            case WARDEN_TP         -> tickWardenTp();
            case WARDEN_FIND       -> tickWardenFind();
            case WARDEN_PATH       -> tickWardenPath();
            case WARDEN_WAIT       -> tickWardenWait();
            case WARDEN_OPEN_DELAY -> tickWardenOpenDelay();
            case WARDEN_OPEN       -> tickWardenOpen();
            case WARDEN_LOOT       -> tickWardenLoot();
            case WARDEN_RETREAT    -> tickWardenRetreat();
            case WARDEN_WAIT_BOSS  -> tickWardenWaitBoss();
            case DEPOSIT_TP        -> tickDepositTp();
            case DEPOSITING        -> tickDepositing();
            case STASH_TP          -> tickStashTp();
            case STASH_FIND        -> tickStashFind();
            case STASH_PATH        -> tickStashPath();
            case STASH_DEPOSIT     -> tickStashDeposit();
            case STASH_WITHDRAW_TP -> tickStashWithdrawTp();
            case STASH_WITHDRAW    -> tickStashWithdraw();
            case ARENA_SETHOME     -> tickArenaSetHome();
            case ARENA_DARENA      -> tickArenaDarena();
            case ARENA_CLICK       -> tickArenaClick();
            case ARENA_WAIT        -> tickArenaWait();
            case ARENA_RETURN      -> tickArenaReturn();
            case ARENA_RETURN_WAIT -> tickArenaReturnWait();
        }
    }

    // === ЖЁСТКОЕ УПРАВЛЕНИЕ КАМЕРОЙ ===
    private void updateRotation() {
        if (mc.player == null) return;

        BlockPos lookTarget = null;
        boolean hasTarget = false;

        switch (state) {
            case WARDEN_PATH:
            case SUPPLY_PATH:
            case STASH_PATH:
                if (targetChest != null) lookTarget = targetChest;
                else if (stashChest != null) lookTarget = stashChest;
                hasTarget = true;
                break;
            case WARDEN_WAIT:
            case WARDEN_OPEN_DELAY:
            case WARDEN_OPEN:
            case SUPPLY_OPEN:
            case STASH_DEPOSIT:
            case ARENA_RETURN_WAIT:
            case WARDEN_LOOT:
            case SUPPLY_TAKE:
            case SUPPLY_DRINK:
                if (targetChest != null) lookTarget = targetChest;
                else if (stashChest != null) lookTarget = stashChest;
                hasTarget = true;
                break;
            default:
                if (mc.player.getVelocity().lengthSquared() > 0.01) {
                    Vec3d vel = mc.player.getVelocity();
                    targetYaw = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
                    targetPitch = 0;
                    hasTarget = true;
                }
                break;
        }

        if (lookTarget != null) {
            Vec3d c = Vec3d.ofCenter(lookTarget);
            double dx = c.x - mc.player.getX();
            double dy = c.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dz = c.z - mc.player.getZ();
            targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
            hasTarget = true;
        }

        if (drinking) {
            targetPitch = 90.0f;
            hasTarget = true;
        }

        if (!hasTarget) {
            targetYaw = mc.player.getYaw();
            targetPitch = mc.player.getPitch();
        }

        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
    }

    private boolean isWardenState() {
        return state == S.WARDEN_TP || state == S.WARDEN_FIND || state == S.WARDEN_PATH ||
                state == S.WARDEN_WAIT || state == S.WARDEN_OPEN_DELAY || state == S.WARDEN_OPEN ||
                state == S.WARDEN_LOOT || state == S.WARDEN_RETREAT || state == S.WARDEN_WAIT_BOSS;
    }

    private void checkBossBarTimer() {
        if (mc.inGameHud == null) return;
        long now = System.currentTimeMillis();
        long bestSecs = -1;
        for (ClientBossBar bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String text = bar.getName().getString();
            long secs = parseTimer(text);
            if (secs > 0 && (bestSecs == -1 || secs < bestSecs)) bestSecs = secs;
        }
        if (bestSecs > 0) {
            bossTimerFound = now;
            bossWasActive = true;
        } else if (bossWasActive) {
            bossWasActive = false;
            bossTimerFound = -1L;
            go(S.DEPOSIT_TP);
        }
    }

    private void tickIdle() {
        if (hasInvis()) go(S.WARDEN_TP);
        else            go(S.SUPPLY_TP);
    }

    // ======================== SUPPLY ========================
    private void tickSupplyTp() {
        if (!cmdSent) { closeScreen(); cmd(CLAN_PFX + supplyHome.getValue()); cmdSent = true; tick.reset(); }
        else if (tpDone()) { cmdSent = false; tpSeen = false; targetChest = null; go(S.SUPPLY_FIND); }
    }

    private void tickSupplyFind() {
        BlockPos chest = findSupplyChest();
        if (chest != null) {
            targetChest = chest;
            if (inRange(chest)) { go(S.SUPPLY_OPEN); }
            else { setGoal(chest); go(S.SUPPLY_PATH); }
        }
    }

    private void tickSupplyPath() {
        if (targetChest == null) { go(S.SUPPLY_FIND); return; }
        if (inRange(targetChest)) { cancelBaritone(); go(S.SUPPLY_OPEN); return; }
        if (isStuck()) { go(S.SUPPLY_FIND); return; }
        if (!isPathing() && tick.finished(2000L + random.nextInt(500))) {
            setGoal(targetChest);
            tick.reset();
        }
    }

    private void tickSupplyOpen() {
        if (targetChest == null) { go(S.SUPPLY_FIND); return; }
        if (isContainerOpen()) { go(S.SUPPLY_TAKE); tick.reset(); takeOnePending = false; return; }
        if (!tick.finished(100L + random.nextInt(200))) return;
        aimAt(targetChest);
        interact(targetChest);
        tick.reset();
    }

    private void tickSupplyTake() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            takeOnePending = false;
            if (findInvisAny() != -1) {
                closeScreen();
                go(S.SUPPLY_DRINK);
            } else {
                closeScreen();
                go(S.SUPPLY_FIND);
            }
            return;
        }
        if (!tick.finished(200L + random.nextInt(100))) return;

        if (!takeOnePending) {
            for (int i = 0; i < c.getRows() * 9; i++) {
                if (isInvisPotion(c.getSlot(i).getStack())) {
                    mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                    takeOnePending = true;
                    takeOneChestSlot = c.getSlot(i).id;
                    tick.reset();
                    return;
                }
            }
            mc.player.closeHandledScreen();
            if (findInvisAny() != -1) {
                go(S.SUPPLY_DRINK);
            } else {
                go(S.SUPPLY_FIND);
            }
        } else {
            int invSlot = findInvisAny();
            if (invSlot != -1) {
                int hotbar = findEmptyHotbar();
                if (hotbar == -1) hotbar = mc.player.getInventory().selectedSlot;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, invSlot, hotbar, SlotActionType.SWAP, mc.player);
            }
            takeOnePending = false;
            mc.player.closeHandledScreen();
            go(S.SUPPLY_DRINK);
        }
    }

    // ======================== ПИТЬЁ ========================
    private void tickSupplyDrink() {
        if (hasInvis()) { go(S.WARDEN_TP); return; }
        int slot = findInvisHotbar();
        if (slot == -1) {
            int inv = findInvisAny();
            if (inv == -1) { go(S.SUPPLY_FIND); return; }
            int hotbar = findEmptyHotbar();
            if (hotbar == -1) hotbar = mc.player.getInventory().selectedSlot;
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, inv, hotbar, SlotActionType.SWAP, mc.player);
            tick.reset(); return;
        }
        if (!drinking) {
            prevSlot = mc.player.getInventory().selectedSlot;
            switchSlot(slot);
            drinking = true;
            drinkDelay = 2;
            drink.reset();
        }
    }

    private void tickDrink() {
        if (drinkDelay > 0) { drinkDelay--; return; }
        mc.player.setPitch(90.0f);
        mc.options.useKey.setPressed(true);
        if (hasInvis() || drink.finished(8000L + random.nextInt(1000))) {
            stopDrinking();
            if (hasInvis()) {
                go(S.WARDEN_TP);
            } else {
                go(S.SUPPLY_TP);
            }
        }
    }

    private void stopDrinking() {
        mc.options.useKey.setPressed(false);
        if (mc.player != null && prevSlot >= 0) switchSlot(prevSlot);
        drinking = false;
        drinkDelay = 0;
    }

    // ======================== WARDEN ========================
    private void tickWardenTp() {
        if (!cmdSent) {
            closeScreen();
            cmd(CLAN_PFX + wardenHome.getValue());
            cmdSent = true;
            tick.reset();
        } else if (tpDone()) {
            cmdSent = false;
            tpSeen = false;
            wardenSpawn = mc.player.getBlockPos();
            targetChest = null;
            chestOpenAt = -1L;
            ignoredChests.clear();
            lastIgnoredCleanup = System.currentTimeMillis();
            lootedCurrentChest = false;
            go(S.WARDEN_FIND);
        }
    }

    // ИСПРАВЛЕННЫЙ МЕТОД findBestChest с ограничением для сундуков без таймера
    private BlockPos findBestChest(long thresholdMs) {
        if (mc.world == null) return null;
        long now = System.currentTimeMillis();
        ignoredChests.entrySet().removeIf(e -> e.getValue() < now);

        BlockPos pp = mc.player.getBlockPos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_Y_RADIUS; y <= SEARCH_Y_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (!isChest(p) || ignoredChests.containsKey(p)) continue;

                    long secs = readHologram(p);
                    double dist = p.getSquaredDistance(pp);

                    // Если таймер есть и он слишком большой – игнорируем
                    if (secs >= 0 && secs * 1000L > thresholdMs) {
                        continue;
                    }

                    // Если таймера нет – игнорируем сундук, если он дальше UNTIMED_CHEST_RANGE_SQ
                    if (secs < 0 && dist > UNTIMED_CHEST_RANGE_SQ) {
                        continue;
                    }

                    long effectiveTime = (secs >= 0) ? secs : 0;
                    chestOpenAt = (secs >= 0) ? now + secs * 1000L : now;
                    double score = dist + effectiveTime * 0.5;
                    if (score < bestScore) {
                        bestScore = score;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    private void tickWardenFind() {
        BlockPos best = findBestChest(parseLeaveSeconds() * 1000L);
        if (best != null) {
            targetChest = best;
            lockedChest = true;
            long secs = readHologram(targetChest);
            if (secs >= 0) chestOpenAt = System.currentTimeMillis() + secs * 1000L;
            else chestOpenAt = System.currentTimeMillis();
            go(S.WARDEN_WAIT);
            return;
        }
        if (tick.finished(5000L + random.nextInt(1000))) {
            tick.reset();
        }
    }

    private void tickWardenPath() {
        if (targetChest == null) { lockedChest = false; go(S.WARDEN_FIND); return; }
        if (inRange(targetChest)) {
            cancelBaritone();
            lockedChest = false;
            go(S.WARDEN_OPEN_DELAY);
            return;
        }
        if (isStuck()) {
            ignoredChests.put(targetChest, System.currentTimeMillis() + NO_TIMER_IGNORE_MS);
            targetChest = null;
            lockedChest = false;
            go(S.WARDEN_FIND);
            return;
        }
        if (!isPathing() && tick.finished(2000L + random.nextInt(500))) {
            setGoal(targetChest);
            tick.reset();
        }
    }

    private void tickWardenWait() {
        if (targetChest == null) { go(S.WARDEN_FIND); return; }

        long secs = readHologram(targetChest);
        if (secs >= 0) chestOpenAt = System.currentTimeMillis() + secs * 1000L;

        long left = chestOpenAt - System.currentTimeMillis();

        if (chestOpenAt == -1) {
            // Сундук без таймера – открываем, только если он рядом
            if (inRange(targetChest)) {
                go(S.WARDEN_OPEN_DELAY);
            } else {
                setGoal(targetChest);
                go(S.WARDEN_PATH);
            }
            return;
        }

        if (left <= OPEN_DELAY_MS) {
            if (inRange(targetChest)) {
                go(S.WARDEN_OPEN_DELAY);
            } else {
                setGoal(targetChest);
                go(S.WARDEN_PATH);
            }
            return;
        }

        if (!inRange(targetChest) && left <= MOVE_TO_CHEST_LEAD_MS) {
            setGoal(targetChest);
            go(S.WARDEN_PATH);
            return;
        }

        long maxWait = parseLeaveSeconds() * 1000L;
        if (secs >= 0 && left > maxWait) {
            ignoredChests.put(targetChest, chestOpenAt);
            targetChest = null;
            chestOpenAt = -1L;
            lockedChest = false;
            go(S.WARDEN_FIND);
            return;
        }

        if (secs >= 0 && left > ARENA_THRESHOLD_MS) {
            homePos = targetChest;
            if (!inRange(targetChest)) {
                setGoal(targetChest);
                go(S.WARDEN_PATH);
                return;
            }
            go(S.ARENA_SETHOME);
        }
    }

    private void tickWardenOpenDelay() {
        if (chestOpenAt != -1 && System.currentTimeMillis() < chestOpenAt - OPEN_DELAY_MS) {
            go(S.WARDEN_WAIT);
            return;
        }
        if (tick.finished(OPEN_DELAY_MS + random.nextInt(200))) {
            go(S.WARDEN_OPEN);
        }
    }

    private void tickWardenOpen() {
        if (targetChest == null) { go(S.WARDEN_FIND); return; }
        if (!inRange(targetChest)) { setGoal(targetChest); go(S.WARDEN_PATH); return; }
        if (isContainerOpen()) {
            lootedCurrentChest = false;
            go(S.WARDEN_LOOT);
            tick.reset();
            return;
        }
        if (tick.finished(OPEN_TIMEOUT_MS + random.nextInt(1000))) {
            if (targetChest != null) {
                ignoredChests.put(targetChest, System.currentTimeMillis() + NO_TIMER_IGNORE_MS);
            }
            targetChest = null;
            go(S.WARDEN_FIND);
            tick.reset();
            return;
        }
        aimAt(targetChest);
        if (!tick.finished(150L + random.nextInt(150))) return;
        interact(targetChest);
        tick.reset();
    }

    private void tickWardenLoot() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            if (lootedCurrentChest) {
                go(S.WARDEN_RETREAT);
            } else {
                if (targetChest != null) {
                    ignoredChests.put(targetChest, System.currentTimeMillis() + NO_TIMER_IGNORE_MS);
                }
                targetChest = null;
                chestOpenAt = -1L;
                go(S.WARDEN_FIND);
            }
            return;
        }
        if (!tick.finished(TICK_MS + random.nextInt(50))) return;

        boolean took = false;
        for (int i = 0; i < c.getRows() * 9; i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                lootedCurrentChest = true;
                took = true;
                break;
            }
        }

        if (!took) {
            mc.player.closeHandledScreen();
            if (targetChest != null) {
                ignoredChests.put(targetChest, System.currentTimeMillis() + NO_TIMER_IGNORE_MS);
            }
            targetChest = null;
            chestOpenAt = -1L;

            if (lootedCurrentChest) {
                go(S.WARDEN_RETREAT);
            } else {
                go(S.WARDEN_FIND);
            }
        }
        tick.reset();
    }

    private void tickWardenRetreat() {
        if (waitForBossBar.isEnabled()) {
            bossWasActive = false; bossTimerFound = -1L;
            go(S.WARDEN_WAIT_BOSS);
        } else {
            go(S.DEPOSIT_TP);
        }
    }

    private void tickWardenWaitBoss() {}

    // ======================== DEPOSIT ========================
    private void tickDepositTp() {
        if (!cmdSent) {
            closeScreen();
            cmd("clan storage");
            cmdSent = true;
            clanStorageRetries = 0;
            tick.reset();
        } else if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            cmdSent = false;
            depositIdx = 0;
            depositAttempts = 0;
            go(S.DEPOSITING);
        } else if (tick.finished(CLAN_ST_WAIT_MS + random.nextInt(500))) {
            if (clanStorageRetries < 2) {
                cmd("clan storage");
                clanStorageRetries++;
                tick.reset();
            } else {
                cmdSent = false;
                go(S.STASH_TP);
            }
        }
    }

    private void tickDepositing() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            if (tick.finished(3000L + random.nextInt(500))) { go(S.STASH_TP); }
            return;
        }
        if (!tick.finished(TICK_MS + random.nextInt(30))) return;
        int rows = c.getRows() * 9;
        boolean hasSpace = false;
        for (int i = 0; i < rows; i++) { if (c.getSlot(i).getStack().isEmpty()) { hasSpace = true; break; } }
        if (!hasSpace) { mc.player.closeHandledScreen(); go(S.STASH_TP); return; }
        boolean moved = false;
        for (int i = rows; i < c.slots.size(); i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                depositIdx = i;
                moved = true;
                break;
            }
        }
        if (moved) {
            depositAttempts = 0;
        } else {
            depositAttempts++;
            if (depositAttempts > 20) {
                mc.player.closeHandledScreen();
                cmdSent = false;
                go(S.WARDEN_TP);
                return;
            }
        }
        tick.reset();
    }

    // ======================== STASH ========================
    private void tickStashTp() {
        if (!cmdSent) { closeScreen(); cmd(CLAN_PFX + stashHome.getValue()); cmdSent = true; tick.reset(); }
        else if (tpDone()) { cmdSent = false; tpSeen = false; stashChest = null; depositIdx = 0; go(S.STASH_FIND); }
    }

    private void tickStashFind() {
        BlockPos chest = findNotFullChest();
        if (chest != null) {
            stashChest = chest;
            if (inRange(chest)) { go(S.STASH_DEPOSIT); }
            else { setGoal(chest); go(S.STASH_PATH); }
        }
    }

    private void tickStashPath() {
        if (stashChest == null) { go(S.STASH_FIND); return; }
        if (inRange(stashChest)) { cancelBaritone(); go(S.STASH_DEPOSIT); return; }
        if (isStuck()) { go(S.STASH_FIND); return; }
        if (!isPathing() && tick.finished(2000L + random.nextInt(500))) {
            setGoal(stashChest);
            tick.reset();
        }
    }

    private void tickStashDeposit() {
        if (stashChest == null) { go(S.STASH_FIND); return; }
        if (!inRange(stashChest)) { setGoal(stashChest); go(S.STASH_PATH); return; }
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            aimAt(stashChest);
            if (!tick.finished(150L + random.nextInt(150))) return;
            interact(stashChest);
            tick.reset();
            return;
        }
        if (!tick.finished(TICK_MS + random.nextInt(30))) return;
        int rows = c.getRows() * 9;
        boolean hasSpace = false;
        for (int i = 0; i < rows; i++) { if (c.getSlot(i).getStack().isEmpty()) { hasSpace = true; break; } }
        if (!hasSpace) { mc.player.closeHandledScreen(); stashChest = null; go(S.STASH_FIND); return; }
        for (int i = rows; i < c.slots.size(); i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                tick.reset(); return;
            }
        }
        mc.player.closeHandledScreen();
        cmdSent = false; depositIdx = 0;
        go(S.STASH_WITHDRAW_TP);
    }

    private void tickStashWithdrawTp() {
        if (!cmdSent) { closeScreen(); cmd("clan storage"); cmdSent = true; tick.reset(); }
        else if (tick.finished(CLAN_ST_WAIT_MS + random.nextInt(500))) { cmdSent = false; depositIdx = 0; go(S.STASH_WITHDRAW); }
    }

    private void tickStashWithdraw() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            if (tick.finished(3000L + random.nextInt(500))) { cmdSent = false; go(S.WARDEN_TP); }
            return;
        }
        if (!tick.finished(TICK_MS + random.nextInt(30))) return;
        int rows = c.getRows() * 9;
        for (int i = 0; i < rows; i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                tick.reset(); return;
            }
        }
        mc.player.closeHandledScreen();
        go(S.STASH_DEPOSIT);
    }

    // ======================== АРЕНА ========================
    private void tickArenaSetHome() {
        if (targetChest != null && !inRange(targetChest)) {
            setGoal(targetChest);
            go(S.WARDEN_PATH);
            return;
        }
        if (!cmdSent) {
            cmd("sethome");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(ARENA_SETHOME_DELAY + random.nextInt(300))) {
            cmdSent = false;
            go(S.ARENA_DARENA);
        }
    }

    private void tickArenaDarena() {
        if (!cmdSent) {
            cmd("darena");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(DARENA_DELAY + random.nextInt(200))) {
            cmdSent = false;
            go(S.ARENA_CLICK);
        }
    }

    private void tickArenaClick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (!tick.finished(500L + random.nextInt(300))) return;
        GenericContainerScreenHandler c = (GenericContainerScreenHandler) mc.player.currentScreenHandler;
        for (int i = 0; i < c.getRows() * 9; i++) {
            ItemStack stack = c.getSlot(i).getStack();
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("смотров") || name.contains("0_0")) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.PICKUP, mc.player);
                break;
            }
        }
        mc.player.closeHandledScreen();
        go(S.ARENA_WAIT);
    }

    private void tickArenaWait() {
        if (targetChest == null) { go(S.WARDEN_FIND); return; }
        long left = chestOpenAt - System.currentTimeMillis();
        if (left > MOVE_TO_CHEST_LEAD_MS + random.nextInt(1000)) return;
        go(S.ARENA_RETURN);
    }

    private void tickArenaReturn() {
        if (!cmdSent) {
            cmd("home");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(ARENA_HOME_DELAY + random.nextInt(500))) {
            cmdSent = false;
            go(S.ARENA_RETURN_WAIT);
        }
    }

    private void tickArenaReturnWait() {
        if (!inRange(targetChest)) {
            setGoal(targetChest);
            return;
        }
        go(S.WARDEN_OPEN_DELAY);
    }

    // ======================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ========================

    // Бочки исключены
    private boolean isChest(BlockPos p) {
        if (mc.world == null) return false;
        var b = mc.world.getBlockState(p).getBlock();
        return b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST;
    }

    private BlockPos findSupplyChest() {
        if (mc.world == null) return null;
        BlockPos pp = mc.player.getBlockPos();
        BlockPos best = null; double bestD = Double.MAX_VALUE;
        for (int x = -48; x <= 48; x++) for (int y = -4; y <= 4; y++) for (int z = -48; z <= 48; z++) {
            BlockPos p = pp.add(x, y, z);
            if (!isChest(p) || !hasSupplySign(p)) continue;
            double d = p.getSquaredDistance(pp);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private BlockPos findNotFullChest() { return findNearestChest(); }

    private BlockPos findNearestChest() {
        if (mc.world == null) return null;
        BlockPos pp = mc.player.getBlockPos();
        BlockPos best = null; double bestD = Double.MAX_VALUE;
        for (int x = -32; x <= 32; x++) for (int y = -4; y <= 4; y++) for (int z = -32; z <= 32; z++) {
            BlockPos p = pp.add(x, y, z);
            if (!isChest(p)) continue;
            double d = p.getSquaredDistance(pp);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private boolean hasSupplySign(BlockPos pos) {
        if (mc.world == null) return false;
        String[] kws = supplySign.getValue().split("[,;\\s]+");
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++) {
            BlockEntity be = mc.world.getBlockEntity(pos.add(dx, dy, dz));
            if (!(be instanceof SignBlockEntity sign)) continue;
            StringBuilder sb = new StringBuilder();
            for (int line = 0; line < 4; line++) {
                sb.append(sign.getFrontText().getMessage(line, false).getString()).append(' ');
                sb.append(sign.getBackText().getMessage(line, false).getString()).append(' ');
            }
            String txt = sb.toString().toLowerCase(Locale.ROOT);
            for (String kw : kws) if (!kw.isBlank() && txt.contains(kw.toLowerCase(Locale.ROOT).trim())) return true;
        }
        return false;
    }

    private long readHologram(BlockPos chest) {
        if (mc.world == null) return -1L;
        Vec3d center = Vec3d.ofCenter(chest);
        Box box = new Box(center.x - 0.5, chest.getY() - 0.5, center.z - 0.5,
                center.x + 0.5, chest.getY() + 3.0, center.z + 0.5);
        long best = -1L;
        for (Entity e : mc.world.getEntitiesByClass(Entity.class, box, en -> true)) {
            String txt = entityText(e);
            if (txt == null || txt.isBlank()) continue;
            long s = parseTimer(txt);
            if (s >= 0 && (best < 0 || s < best)) best = s;
        }
        return best;
    }

    private void aimAt(BlockPos p) {
        if (p == null || mc.player == null) return;
        Vec3d c = Vec3d.ofCenter(p);
        double dx = c.x - mc.player.getX();
        double dy = c.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = c.z - mc.player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
        targetYaw = yaw;
        targetPitch = pitch;
    }

    private String entityText(Entity e) {
        if (e.hasCustomName() && e.getCustomName() != null) return fmt(e.getCustomName().getString());
        return null;
    }

    private String fmt(String s) { return s == null ? "" : s.replaceAll("§.", "").toLowerCase(Locale.ROOT).trim(); }

    private long parseTimer(String t) {
        if (t == null || t.isBlank()) return -1L;
        Matcher m = PAT_TIME.matcher(t);
        if (m.find()) {
            long a = Long.parseLong(m.group(1)), b = Long.parseLong(m.group(2));
            return m.group(3) != null ? a * 3600 + b * 60 + Long.parseLong(m.group(3)) : a * 60 + b;
        }
        m = PAT_MINSEC.matcher(t);
        if (m.find()) {
            long r = Long.parseLong(m.group(1)) * 60;
            if (m.group(2) != null) r += Long.parseLong(m.group(2));
            return r;
        }
        m = PAT_SEC.matcher(t);
        if (m.find()) return Long.parseLong(m.group(1));
        return -1L;
    }

    private long parseLeaveSeconds() {
        try { return Math.max(1, Long.parseLong(leaveSeconds.getValue().trim())); }
        catch (Exception e) { return 60L; }
    }

    private boolean isLoot(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        for (String k : LOOT_IDS) if (id.equals(k) || id.contains(k)) return true;
        return false;
    }

    private boolean hasInvis() {
        return mc.player != null && mc.player.getStatusEffect(StatusEffects.INVISIBILITY) != null;
    }

    private boolean isInvisPotion(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        if (!s.isOf(Items.POTION) && !s.isOf(Items.SPLASH_POTION) && !s.isOf(Items.LINGERING_POTION)) return false;
        var cont = s.get(DataComponentTypes.POTION_CONTENTS);
        if (cont == null) return false;
        for (StatusEffectInstance ef : cont.getEffects())
            if (ef.getEffectType().equals(StatusEffects.INVISIBILITY)) return true;
        return false;
    }

    private int findInvisHotbar() {
        for (int i = 0; i < 9; i++) if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    private int findInvisAny() {
        for (int i = 0; i < mc.player.getInventory().size(); i++)
            if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    private int findEmptyHotbar() {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }

    private void breakCandles() {
        if (mc.world == null || mc.interactionManager == null) return;
        BlockPos pp = mc.player.getBlockPos();
        for (int x = -2; x <= 2; x++) for (int y = -1; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            BlockPos p = pp.add(x, y, z);
            String name = mc.world.getBlockState(p).getBlock().getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (name.contains("candle")) {
                if (!p.equals(candlePos) || candle.finished(CANDLE_MS + random.nextInt(50))) {
                    mc.interactionManager.attackBlock(p, Direction.UP);
                    candlePos = p; candle.reset();
                }
                return;
            }
        }
        candlePos = null;
    }

    private void setGoal(BlockPos p) {
        if (p == null) return;
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(p, 1));
            stuckSince = -1L;
        } catch (Throwable ignored) {}
    }

    private void cancelBaritone() {
        try { BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything(); }
        catch (Throwable ignored) {}
    }

    private boolean isPathing() {
        try { return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(); }
        catch (Throwable ignored) { return false; }
    }

    private boolean isStuck() {
        if (!isPathing()) {
            stuckSince = -1L;
            return false;
        }
        try {
            var pb = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
            if (!pb.hasPath() && !pb.getInProgress().isPresent()) {
                if (stuckSince < 0) stuckSince = System.currentTimeMillis();
                else if (System.currentTimeMillis() - stuckSince > STUCK_MS) {
                    cancelBaritone();
                    stuckSince = -1L;
                    return true;
                }
            } else {
                stuckSince = -1L;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private void checkStuck() {
        isStuck();
    }

    private void configureBaritone(boolean on) {
        try {
            BaritoneAPI.getSettings().freeLook.value = false;
            BaritoneAPI.getSettings().rightClickContainerOnArrival.value = false;
            // Обход скалкрикунов (если доступно)
            try {
                BaritoneAPI.getSettings().avoidSculkShriekers.value = true;
                BaritoneAPI.getSettings().avoidSculkShriekersDistance.value = 12;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private boolean inRange(BlockPos p) {
        return p != null && mc.player.squaredDistanceTo(p.getX() + .5, p.getY(), p.getZ() + .5) <= RANGE_SQ;
    }

    private void interact(BlockPos p) {
        if (mc.interactionManager == null) return;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(p), Direction.UP, p, false));
    }

    private boolean isContainerOpen() { return mc.currentScreen instanceof GenericContainerScreen; }
    private void closeScreen() { if (mc.currentScreen != null && mc.player != null) mc.player.closeHandledScreen(); }
    private void cmd(String c) { if (mc.player != null && c != null && !c.isBlank()) mc.player.networkHandler.sendCommand(c.trim()); }

    private void switchSlot(int s) {
        if (s < 0 || s > 8 || mc.player == null) return;
        mc.player.getInventory().selectedSlot = s;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(s));
    }

    private void go(S next) { state = next; tick.reset(); }

    private boolean tpDone() { return tick.finished(tpSeen ? TP_FAST_MS + random.nextInt(300) : TP_WAIT_MS + random.nextInt(500)); }

    private boolean isTpBar() {
        if (mc.inGameHud == null) return false;
        for (ClientBossBar b : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String n = b.getName().getString().toLowerCase(Locale.ROOT).trim();
            if (n.contains("телепортац") || n.contains("teleport")) return true;
        }
        return false;
    }

    private void reset() {
        targetChest = null; stashChest = null; chestOpenAt = -1L; wardenSpawn = null; homePos = null; retreatFrom = null;
        cmdSent = false; tpSeen = false; depositIdx = 0; depositAttempts = 0; clanStorageRetries = 0;
        stuckSince = -1L; candlePos = null;
        drinking = false; drinkDelay = 0; prevSlot = 0;
        bossTimerFound = -1L; bossWasActive = false;
        takeOnePending = false; takeOneChestSlot = -1;
        lockedChest = false;
        lootedCurrentChest = false;
        ignoredChests.clear();
        lastInvisCheck = 0L;
        mc.options.backKey.setPressed(false);
        if (mc.player != null) mc.options.useKey.setPressed(false);
    }
}