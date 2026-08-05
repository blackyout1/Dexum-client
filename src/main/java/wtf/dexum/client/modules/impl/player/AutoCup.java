package wtf.dexum.client.modules.impl.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalNear;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import wtf.dexum.client.modules.api.setting.impl.StringSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleAnnotation(name = "AutoCup", description = "Авто-фарм медного данжа (бочки + вазы)", category = Category.PLAYER)
public final class AutoCup extends Module implements IMinecraft {

    public static final AutoCup INSTANCE = new AutoCup();

    // === Настройки ===
    private final StringSetting cupHome      = new StringSetting("Хом медного данжа", "cup");
    private final StringSetting supplyHome   = new StringSetting("Хом базы (инвиз + стеш)", "st");
    private final StringSetting supplySign   = new StringSetting("Слово на табличке", "инвиз");
    private final StringSetting invisKeyword = new StringSetting("Слово в названии инвиза", "невидим");

    // === Константы ===
    private static final long CLAN_HOME_DELAY     = 7_000L;
    private static final long TP_WAIT_MS          = 10_000L;
    private static final long CLAN_ST_WAIT_MS     = 2_000L;
    private static final long TICK_MS             = 100L;
    private static final long OPEN_DELAY_MS       = 400L;
    private static final long ARENA_THRESHOLD_MS  = 15_000L;
    private static final long MOVE_TO_BARREL_LEAD_MS = 10_000L;
    private static final long POST_DEATH_WAIT_MS  = 10_000L;
    private static final long BARREL_THRESHOLD_MS = 60_000L;

    private static final int  SEARCH_RADIUS       = 80;
    private static final int  SEARCH_Y_RADIUS     = 6;
    private static final double RANGE_SQ          = 9.0;

    private static final String CLAN_PFX          = "clan home ";
    private static final long ARENA_SETHOME_DELAY = 1500L;
    private static final long ARENA_HOME_DELAY    = 7000L;
    private static final long DARENA_DELAY        = 0L;

    // === Лут из данжа ===
    private static final String[] LOOT_IDS = {
            "copper_ingot","copper_block","raw_copper","raw_copper_block",
            "iron_ingot","iron_block","gold_ingot","gold_block",
            "diamond","emerald","coal","redstone","lapis_lazuli",
            "amethyst_shard","totem_of_undying","golden_apple","enchanted_golden_apple"
    };

    private static final String[] EXPENSIVE_LOOT = {
            "diamond","emerald","totem_of_undying","enchanted_golden_apple"
    };

    private float targetYaw, targetPitch;

    private enum S {
        IDLE, DEAD_WAIT,
        SUPPLY_TP, SUPPLY_FIND, SUPPLY_PATH, SUPPLY_OPEN, SUPPLY_TAKE, SUPPLY_DRINK,
        CUP_TP, CUP_FIND, CUP_PATH, CUP_WAIT, CUP_OPEN_DELAY, CUP_OPEN, CUP_LOOT,
        BREAK_POT,
        WAIT_CT,
        DEPOSIT_TP, DEPOSITING,
        STASH_TP, STASH_FIND, STASH_PATH, STASH_DEPOSIT, STASH_WITHDRAW_TP, STASH_WITHDRAW,
        ARENA_SETHOME, ARENA_DARENA, ARENA_CLICK, ARENA_WAIT, ARENA_RETURN, ARENA_RETURN_WAIT,
        WAIT_FOR_TIMER
    }

    private S state = S.IDLE;
    private final Timer tick      = new Timer();
    private final Timer drink     = new Timer();
    private final Timer pot       = new Timer();
    private final Random random   = new Random();

    private BlockPos targetBarrel = null;
    private BlockPos stashChest   = null;
    private BlockPos cupSpawn     = null;
    private BlockPos homePos      = null;
    private BlockPos potPos       = null;

    private BlockPos patrolTarget = null;
    private int      patrolPhase  = 0;
    private BlockPos lastPos      = null;
    private long     lastMoveTime = 0L;
    private final Set<BlockPos> visitedPatrolPoints = new HashSet<>();
    private BlockPos lastPatrolPoint = null;
    private static final int  MAX_PATROL_ATTEMPTS = 20;
    private static final int  PATROL_RADIUS = 30;
    private static final long STUCK_TIMEOUT = 5000L;
    private static final double MIN_PATROL_DISTANCE = 10.0;

    private long     lastScanTime = 0L;
    private static final long SCAN_INTERVAL = 2000L; // сканируем раз в 2 секунды
    private static final long POT_MS = 120L;

    private long     barrelOpenAt = -1L;
    private boolean  cmdSent      = false;
    private boolean  tpSeen       = false;

    private boolean drinking      = false;
    private int     drinkDelay    = 0;
    private int     prevSlot      = 0;

    private int     depositIdx      = 0;
    private int     clanStorageRetries = 0;

    private boolean tpBarPaused = false;
    private long    barEndedAt  = -1L;

    private boolean tpSuccessDetected = false;

    // Главное хранилище таймеров: BlockPos -> время открытия (мс)
    private final Map<BlockPos, Long> barrelTimers = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> ignoredBarrels = new ConcurrentHashMap<>();
    private final Set<BlockPos> lootedBarrels = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private int     lastBarrelCount = 0;
    private boolean hasLootToDeposit = false;

    private long    stuckSince = -1L;
    private static final long STUCK_MS = 3000L;
    private static final long IGNORE_DURATION = 600_000L;

    private boolean takeOnePending   = false;
    private int     takeOneChestSlot = -1;

    private long lastInvisCheck = 0L;
    private static final long INVIS_CHECK_INTERVAL = 5_000L;

    private static final long PVP_SAFE_BUFFER_MS = 15_000L;

    // ========== ПАТТЕРНЫ ДЛЯ ПАРСИНГА ВРЕМЕНИ ==========
    private static final Pattern TIME_PATTERN    = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(с|s|сек|sec)");
    private static final Pattern MIN_SEC_PATTERN = Pattern.compile("(\\d+)\\s*(м|m|мин|min(?:\\.|ute)?)\\s*(?:(\\d+)\\s*(с|s|сек|sec(?:\\.|ond)?))?");

    private AutoCup() {}

    private void log(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("[AutoCup] " + message), false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;
        reset();
        configureBaritone(true);

        state = S.IDLE;
        tick.reset();

        barrelTimers.clear();
        ignoredBarrels.clear();
        lootedBarrels.clear();
        visitedPatrolPoints.clear();
        lastPatrolPoint = null;
        targetYaw = mc.player.getYaw();
        targetPitch = mc.player.getPitch();

        log("Модуль включен!");
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

    private void reset() {
        targetBarrel = null;
        stashChest = null;
        cupSpawn = null;
        homePos = null;
        barrelOpenAt = -1L;
        cmdSent = false;
        tpSeen = false;
        drinking = false;
        drinkDelay = 0;
        depositIdx = 0;
        clanStorageRetries = 0;
        tpBarPaused = false;
        barEndedAt = -1L;
        takeOnePending = false;
        takeOneChestSlot = -1;
        lastInvisCheck = 0L;
        patrolTarget = null;
        patrolPhase = 0;
        lastPos = null;
        lastMoveTime = 0L;
        visitedPatrolPoints.clear();
        lastPatrolPoint = null;
        stuckSince = -1L;
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (e.getPacket() instanceof GameMessageS2CPacket pkt) {
            String msg = pkt.content().getString().toLowerCase(Locale.ROOT);

            if (msg.contains("вы погибли") || msg.contains("you died")) {
                log("Я умер! Перезапуск через 10 сек...");
                cancelBaritone(); closeScreen(); stopDrinking();
                mc.options.backKey.setPressed(false);
                reset();
                state = S.DEAD_WAIT;
                tick.reset();
            }

            if (msg.contains("успешная телепортация") ||
                    msg.contains("телепортация завершена") ||
                    msg.contains("вы были телепортированы") ||
                    msg.contains("you have been teleported")) {
                log("Детект успешной телепортации через чат!");
                tpSuccessDetected = true;
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
            if (isCupState() && !hasInvis()) {
                log("ВНИМАНИЕ: Инвиз закончился в данже! Возвращаюсь на базу");
                cancelBaritone(); closeScreen(); stopDrinking();
                cmdSent = false;
                go(S.SUPPLY_TP);
                return;
            }
        }

        if (drinking) { tickDrink(); return; }

        if (state == S.CUP_PATH || state == S.SUPPLY_PATH || state == S.STASH_PATH || state == S.CUP_WAIT) {
            breakPots();
        }

        switch (state) {
            case IDLE              -> tickIdle();
            case SUPPLY_TP         -> tickSupplyTp();
            case SUPPLY_FIND       -> tickSupplyFind();
            case SUPPLY_PATH       -> tickSupplyPath();
            case SUPPLY_OPEN       -> tickSupplyOpen();
            case SUPPLY_TAKE       -> tickSupplyTake();
            case SUPPLY_DRINK      -> tickSupplyDrink();
            case CUP_TP            -> tickCupTp();
            case CUP_FIND          -> tickCupFind();
            case CUP_PATH          -> tickCupPath();
            case CUP_WAIT          -> tickCupWait();
            case CUP_OPEN_DELAY    -> tickCupOpenDelay();
            case CUP_OPEN          -> tickCupOpen();
            case CUP_LOOT          -> tickCupLoot();
            case BREAK_POT         -> tickBreakPot();
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
            case WAIT_FOR_TIMER    -> tickWaitForTimer();
            case WAIT_CT           -> tickWaitCT();
        }

        updateRotation();
    }

    private boolean isCupState() {
        return state == S.CUP_TP || state == S.CUP_FIND || state == S.CUP_PATH ||
                state == S.CUP_WAIT || state == S.CUP_OPEN_DELAY || state == S.CUP_OPEN ||
                state == S.CUP_LOOT || state == S.BREAK_POT || state == S.WAIT_CT;
    }

    // ========== УПРАВЛЕНИЕ КАМЕРОЙ ==========
    private void updateRotation() {
        if (mc.player == null) return;

        BlockPos lookTarget = null;
        boolean hasTarget = false;

        if (targetBarrel != null && (state == S.CUP_PATH || state == S.CUP_WAIT ||
                state == S.CUP_OPEN_DELAY || state == S.CUP_OPEN || state == S.CUP_LOOT ||
                state == S.BREAK_POT)) {
            lookTarget = targetBarrel;
            hasTarget = true;
        } else if (stashChest != null && (state == S.STASH_PATH || state == S.STASH_DEPOSIT)) {
            lookTarget = stashChest;
            hasTarget = true;
        }

        if (drinking) {
            targetPitch = 90.0f;
            hasTarget = true;
        }

        if (lookTarget != null) {
            Vec3d c = Vec3d.ofCenter(lookTarget);
            double dx = c.x - mc.player.getX();
            double dy = c.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dz = c.z - mc.player.getZ();
            targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
        }

        if (!hasTarget) {
            if (mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
                Vec3d vel = mc.player.getVelocity();
                targetYaw = (float) Math.toDegrees(Math.atan2(-vel.x, vel.z));
                targetPitch = 0;
            } else {
                targetYaw = mc.player.getYaw();
                targetPitch = mc.player.getPitch();
            }
        }

        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
        mc.player.prevYaw = targetYaw;
        mc.player.prevPitch = targetPitch;
    }

    // ========== ОСНОВНЫЕ МЕТОДЫ ПОИСКА ==========

    /**
     * Комбинированный поиск голограмм и обновление barrelTimers.
     * Сканирует сущности в радиусе 150 блоков, определяет сундуки под ними,
     * парсит время и сохраняет в barrelTimers.
     */
    private void scanAllBarrels() {
        if (mc.world == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 80; // можно сделать константу

        for (int x = -radius; x <= radius; x++) {
            for (int y = -SEARCH_Y_RADIUS; y <= SEARCH_Y_RADIUS; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (!isBarrel(pos)) continue;
                    if (ignoredBarrels.containsKey(pos) || lootedBarrels.contains(pos)) continue;
                    long secs = readHologramAboveBarrel(pos); // читаем голограмму над конкретной бочкой
                    if (secs > 0) {
                        barrelTimers.put(pos, now + secs * 1000L);
                    } else {
                        // если нет таймера, возможно, бочка без таймера (не фармовая), игнорируем
                        // или добавляем с большим временем, чтобы не мешала
                    }
                }
            }
        }
        // удаляем устаревшие
        barrelTimers.entrySet().removeIf(e -> e.getValue() < now - 600_000L);
    }

    /**
     * Чтение времени с голограммы непосредственно над бочкой (радиус 3 блока от игрока).
     * Используется для уточнения, когда подходим ближе.
     */
    private long readHologramAboveBarrel(BlockPos barrel) {
        if (mc.world == null || mc.player == null) return -1L;
        Box nearBox = mc.player.getBoundingBox().expand(3.0);
        long best = -1L;
        for (Entity entity : mc.world.getEntitiesByClass(Entity.class, nearBox,
                e -> e instanceof ArmorStandEntity || e instanceof DisplayEntity.TextDisplayEntity)) {
            String text = getEntityText(entity);
            if (text == null || text.isEmpty()) continue;
            // Проверяем, что сущность находится над сундуком (по Y)
            double distY = Math.abs(entity.getY() - (barrel.getY() + 1.5));
            if (distY > 4.0) continue; // слишком далеко по вертикали
            long secs = parseTimeToSeconds(text);
            if (secs > 0 && (best < 0 || secs < best)) {
                best = secs;
            }
        }
        return best;
    }

    /**
     * Получить текст с сущности (ArmorStand или TextDisplay)
     */
    private String getEntityText(Entity entity) {
        String name = "";
        if (entity instanceof ArmorStandEntity as && as.getCustomName() != null) {
            name = as.getCustomName().getString();
        } else if (entity instanceof DisplayEntity.TextDisplayEntity td) {
            name = td.getText().getString();
        }
        return name.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Универсальный парсер времени из текста голограммы.
     * Поддерживает форматы:
     *   - MM:SS
     *   - HH:MM:SS
     *   - X мин Y сек
     *   - X сек
     *   - Xм / Xmin
     *   - просто число (если > 60, то минуты, иначе секунды)
     */
    private long parseTimeToSeconds(String text) {
        if (text == null || text.isEmpty()) return -1L;
        String clean = text.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);

        // 1. MM:SS или HH:MM:SS
        Matcher m = TIME_PATTERN.matcher(clean);
        if (m.find()) {
            long hours = 0, minutes, seconds;
            if (m.group(3) == null) {
                minutes = Long.parseLong(m.group(1));
                seconds = Long.parseLong(m.group(2));
            } else {
                hours = Long.parseLong(m.group(1));
                minutes = Long.parseLong(m.group(2));
                seconds = Long.parseLong(m.group(3));
            }
            return hours * 3600 + minutes * 60 + seconds;
        }

        // 2. X мин Y сек
        m = MIN_SEC_PATTERN.matcher(clean);
        if (m.find()) {
            long minutes = Long.parseLong(m.group(1));
            long seconds = 0;
            if (m.group(3) != null) seconds = Long.parseLong(m.group(3));
            return minutes * 60 + seconds;
        }

        // 3. X сек
        m = SECONDS_PATTERN.matcher(clean);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }

        // 4. Просто Xм или X min
        Pattern simpleMin = Pattern.compile("(\\d+)\\s*[мm]\\s*(?:ин|ут)?");
        m = simpleMin.matcher(clean);
        if (m.find()) {
            return Long.parseLong(m.group(1)) * 60L;
        }

        // 5. Если осталось просто число
        Pattern onlyNumber = Pattern.compile("(\\d+)");
        m = onlyNumber.matcher(clean);
        if (m.find()) {
            long val = Long.parseLong(m.group(1));
            // Если число больше 60, считаем минутами
            return val > 60 ? val * 60 : val;
        }

        return -1L;
    }

    /**
     * Найти лучшую бочку (с минимальным временем до открытия) из barrelTimers.
     */
    private BlockPos findBestBarrelFromTimers() {
        if (barrelTimers.isEmpty()) return null;
        long now = System.currentTimeMillis();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos playerPos = mc.player.getBlockPos();
        for (Map.Entry<BlockPos, Long> entry : barrelTimers.entrySet()) {
            BlockPos pos = entry.getKey();
            long openTime = entry.getValue();
            if (ignoredBarrels.containsKey(pos) || lootedBarrels.contains(pos)) continue;
            long left = openTime - now;
            if (left < 0) continue;
            // Игнорируем бочки с таймером > 60 сек (или задаём большой штраф)
            if (left > 60_000L) continue;
            double dist = playerPos.getSquaredDistance(pos);
            // Если расстояние слишком большое (> 40 блоков), не рассматриваем
            if (dist > 1600) continue; // 40^2
            // Комбинированный счёт: время + расстояние*0.5 (приоритет времени, но расстояние тоже)
            double score = left + dist * 0.5;
            if (score < bestScore) {
                bestScore = score;
                best = pos;
            }
        }
        return best;
    }

    // ========== ОСТАЛЬНЫЕ МЕТОДЫ (без изменений, но с использованием новых) ==========

    private void tickIdle() {
        boolean hasInvisEffect = hasInvis();
        if (hasInvisEffect) {
            log("Есть инвиз, иду на медный данж");
            go(S.CUP_TP);
        } else {
            log("Нет инвиза, иду на базу");
            go(S.SUPPLY_TP);
        }
    }

    private boolean canSee(BlockPos target) {
        if (mc.world == null || mc.player == null) return false;
        Vec3d start = mc.player.getEyePos();
        Vec3d end = Vec3d.ofCenter(target);
        var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                start, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit == null || hit.getBlockPos().equals(target);
    }

    private BlockPos findNearestPot() {
        if (mc.world == null) return null;
        BlockPos pp = mc.player.getBlockPos();
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        int potRadius = 10;
        for (int x = -potRadius; x <= potRadius; x++) {
            for (int y = -SEARCH_Y_RADIUS; y <= SEARCH_Y_RADIUS; y++) {
                for (int z = -potRadius; z <= potRadius; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (!isPot(p)) continue;
                    double dist = p.getSquaredDistance(pp);
                    if (dist < minDist && canSee(p)) {
                        minDist = dist;
                        nearest = p;
                    }
                }
            }
        }
        return nearest;
    }

    // ========== СОСТОЯНИЯ ==========

    // === SUPPLY (инвиз) ===
    private void tickSupplyTp() {
        if (!cmdSent) {
            log("SUPPLY_TP: Телепортируюсь на базу (/clan home " + supplyHome.getValue() + ")");
            closeScreen();
            cmd(CLAN_PFX + supplyHome.getValue());
            cmdSent = true;
            tpSuccessDetected = false;
            tick.reset();
        } else if (tpDone()) {
            log("SUPPLY_TP: Телепорт завершен, ищу сундук с инвизом");
            cmdSent = false;
            tpSeen = false;
            go(S.SUPPLY_FIND);
        }
    }

    private void tickSupplyFind() {
        BlockPos chest = findSupplyChest();
        if (chest != null) {
            log("SUPPLY_FIND: Нашел сундук с инвизом на " + chest);
            targetBarrel = chest;
            if (inRange(chest)) { go(S.SUPPLY_OPEN); }
            else { setGoal(chest); go(S.SUPPLY_PATH); }
        } else {
            log("SUPPLY_FIND: Сундук с инвизом не найден!");
        }
    }

    private void tickSupplyPath() {
        if (targetBarrel == null) { go(S.SUPPLY_FIND); return; }
        if (inRange(targetBarrel)) { cancelBaritone(); go(S.SUPPLY_OPEN); return; }
        if (!isPathing() && tick.finished(2000L)) { setGoal(targetBarrel); tick.reset(); }
    }

    private void tickSupplyOpen() {
        if (targetBarrel == null) {
            log("SUPPLY_OPEN: ОШИБКА! Целевой сундук потерян");
            go(S.SUPPLY_FIND);
            return;
        }
        if (isContainerOpen()) {
            log("SUPPLY_OPEN: Сундук открыт, начинаю забирать инвиз");
            go(S.SUPPLY_TAKE);
            tick.reset();
            takeOnePending = false;
            return;
        }
        if (!tick.finished(100L)) return;
        log("SUPPLY_OPEN: Открываю сундук на " + targetBarrel);
        aimAt(targetBarrel);
        interact(targetBarrel);
        tick.reset();
    }

    private void tickSupplyTake() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            takeOnePending = false;
            if (findInvisAny() != -1) {
                log("SUPPLY_TAKE: Инвиз взят, закрываю сундук");
                closeScreen();
                go(S.SUPPLY_DRINK);
            } else {
                log("SUPPLY_TAKE: Инвиз не найден в инвентаре, ищу снова");
                closeScreen();
                go(S.SUPPLY_FIND);
            }
            return;
        }
        if (!tick.finished(200L)) return;

        if (!takeOnePending) {
            for (int i = 0; i < c.getRows() * 9; i++) {
                ItemStack stack = c.getSlot(i).getStack();
                if (isInvisPotion(stack)) {
                    mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                    takeOnePending = true;
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

    private void tickSupplyDrink() {
        if (hasInvis()) {
            log("SUPPLY_DRINK: Инвиз выпит! Иду на медный данж");
            go(S.CUP_TP);
            return;
        }
        int slot = findInvisHotbar();
        if (slot == -1) {
            int inv = findInvisAny();
            if (inv == -1) {
                log("SUPPLY_DRINK: Инвиз не найден в инвентаре!");
                go(S.SUPPLY_FIND);
                return;
            }
            int hotbar = findEmptyHotbar();
            if (hotbar == -1) hotbar = mc.player.getInventory().selectedSlot;
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, inv, hotbar, SlotActionType.SWAP, mc.player);
            tick.reset(); return;
        }
        if (!drinking) {
            log("SUPPLY_DRINK: Начинаю пить инвиз");
            prevSlot = mc.player.getInventory().selectedSlot;
            switchSlot(slot);
            mc.player.setPitch(90.0f);
            drinking = true;
            drinkDelay = 5;
            drink.reset();
        }
    }

    private void tickDrink() {
        if (drinkDelay > 0) {
            mc.player.setPitch(90.0f);
            drinkDelay--;
            return;
        }
        mc.player.setPitch(90.0f);
        mc.options.useKey.setPressed(true);
        if (hasInvis() || drink.finished(8000L)) {
            stopDrinking();
            if (hasInvis()) {
                log("SUPPLY_DRINK: Инвиз выпит, телепортируюсь на медный");
                go(S.CUP_TP);
            } else {
                log("SUPPLY_DRINK: Не удалось выпить, пробую снова");
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

    // === CUP (медный данж) ===
    private void tickCupTp() {
        if (!canUseComandsOrTeleport()) {
            log("CUP_TP: Жду окончания PvP режима");
            return;
        }
        if (!cmdSent) {
            log("CUP_TP: Телепортируюсь на медный данж (/clan home " + cupHome.getValue() + ")");
            closeScreen();
            cmd(CLAN_PFX + cupHome.getValue());
            cmdSent = true;
            tpSuccessDetected = false;
            tick.reset();
        } else if (tpDone()) {
            log("CUP_TP: Телепорт завершен, начинаю патрулирование");
            cmdSent = false;
            tpSeen = false;
            if (mc.player != null) {
                cupSpawn = mc.player.getBlockPos();
            }
            targetBarrel = null;
            barrelOpenAt = -1L;
            go(S.CUP_FIND);
        }
    }

    private void tickCupFind() {
        long now = System.currentTimeMillis();

        // Очистка устаревших записей
        ignoredBarrels.entrySet().removeIf(entry -> entry.getValue() < now);
        barrelTimers.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            return lootedBarrels.contains(pos) || ignoredBarrels.containsKey(pos);
        });

        // Сканирование бочек (голограммы) каждые 2 сек
        if (now - lastScanTime >= SCAN_INTERVAL) {
            scanAllBarrels();
            lastScanTime = now;
        }

        if (isStuck() && targetBarrel != null) {
            log("CUP_FIND: Застрял при попытке дойти до бочки " + targetBarrel + ", игнорирую");
            ignoredBarrels.put(targetBarrel, now + IGNORE_DURATION);
            targetBarrel = null;
            cancelBaritone();
            stuckSince = -1L;
        }

        // Выбор лучшей бочки из barrelTimers
        BlockPos bestBarrel = findBestBarrelFromTimers();
        if (bestBarrel != null) {
            targetBarrel = bestBarrel;
            long openTime = barrelTimers.get(bestBarrel);
            barrelOpenAt = openTime;
            long secs = (openTime - now) / 1000;
            if (secs <= 0) {
                // Если время уже вышло, пробуем открыть
                if (inRange(targetBarrel)) {
                    go(S.CUP_OPEN_DELAY);
                } else {
                    setGoal(targetBarrel);
                    go(S.CUP_PATH);
                }
            } else if (secs <= 15) {
                log("CUP_FIND: Нашел бочку с таймером " + secs + " сек (<= 15 сек), иду к ней");
                cancelBaritone();
                go(S.CUP_WAIT);
            } else {
                log("CUP_FIND: Нашел бочку с таймером " + secs + " сек (> 15 сек), еду на арену");
                homePos = targetBarrel;
                cancelBaritone();
                if (!inRange(targetBarrel)) {
                    setGoal(targetBarrel);
                    go(S.CUP_PATH);
                } else {
                    go(S.ARENA_SETHOME);
                }
            }
            return;
        }

        // Если нет хороших бочек, проверяем вазы
        BlockPos pot = findNearestPot();
        if (pot != null && inRange(pot)) {
            targetBarrel = pot;
            log("CUP_FIND: Нашел вазу рядом, ломаю");
            cancelBaritone();
            go(S.BREAK_POT);
            return;
        }

        // Патруль: идём к бочке с минимальным временем (даже если >60 сек)
        BlockPos patrolBarrel = getPatrolTargetFromBarrels();
        if (patrolBarrel != null) {
            patrolTarget = patrolBarrel;
            log("CUP_FIND: Патруль к бочке " + patrolTarget);
            setGoal(patrolTarget);
            lastMoveTime = now;
        } else if (cupSpawn != null) {
            // Случайный патруль
            if (patrolTarget == null || inRange(patrolTarget)) {
                patrolTarget = getRandomPatrolPoint();
                if (patrolTarget != null) {
                    patrolPhase++;
                    log("CUP_FIND: Патрулирую, попытка " + patrolPhase);
                    setGoal(patrolTarget);
                    lastMoveTime = now;
                }
                if (patrolPhase >= MAX_PATROL_ATTEMPTS) {
                    if (hasLootToDeposit) {
                        log("CUP_FIND: Есть лут, иду депозить");
                        cancelBaritone();
                        go(S.WAIT_CT);
                    } else {
                        log("CUP_FIND: Нет лута, начинаю патруль заново");
                        patrolPhase = 0;
                    }
                    return;
                }
            }
        }

        if (!isPathing() && patrolTarget != null && tick.finished(2000L)) {
            setGoal(patrolTarget);
            tick.reset();
        }
    }

    private BlockPos getPatrolTargetFromBarrels() {
        if (barrelTimers.isEmpty()) return null;
        long now = System.currentTimeMillis();
        BlockPos best = null;
        long bestTime = Long.MAX_VALUE;
        for (Map.Entry<BlockPos, Long> entry : barrelTimers.entrySet()) {
            BlockPos pos = entry.getKey();
            if (ignoredBarrels.containsKey(pos) || lootedBarrels.contains(pos)) continue;
            long left = entry.getValue() - now;
            if (left < bestTime) {
                bestTime = left;
                best = pos;
            }
        }
        if (best != null) {
            // Возвращаем точку рядом, но не саму бочку (чтобы не пытаться открыть)
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            BlockPos near = best.add(dx, 0, dz);
            if (isPassable(near) && isSolid(near.down())) {
                return near;
            }
            return best;
        }
        return null;
    }

    private BlockPos getRandomPatrolPoint() {
        if (mc.world == null || mc.player == null) return null;
        BlockPos currentPos = mc.player.getBlockPos();
        for (int attempt = 0; attempt < 30; attempt++) {
            int offsetX = random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
            int offsetZ = random.nextInt(PATROL_RADIUS * 2 + 1) - PATROL_RADIUS;
            BlockPos point = currentPos.add(offsetX, 0, offsetZ);
            if (visitedPatrolPoints.contains(point)) continue;
            if (lastPatrolPoint != null) {
                double dist = Math.sqrt(point.getSquaredDistance(lastPatrolPoint));
                if (dist < MIN_PATROL_DISTANCE) continue;
            }
            if (isPassable(point) && isSolid(point.down())) {
                visitedPatrolPoints.add(point);
                if (visitedPatrolPoints.size() > 50) visitedPatrolPoints.clear();
                lastPatrolPoint = point;
                return point;
            }
        }
        if (cupSpawn != null) {
            for (int attempt = 0; attempt < 10; attempt++) {
                int direction = random.nextInt(4);
                int offset = PATROL_RADIUS;
                BlockPos farPoint = switch (direction) {
                    case 0 -> cupSpawn.add(0, 0, -offset);
                    case 1 -> cupSpawn.add(offset, 0, 0);
                    case 2 -> cupSpawn.add(0, 0, offset);
                    default -> cupSpawn.add(-offset, 0, 0);
                };
                if (isPassable(farPoint) && isSolid(farPoint.down())) {
                    lastPatrolPoint = farPoint;
                    return farPoint;
                }
            }
        }
        return currentPos;
    }

    private boolean isPassable(BlockPos pos) {
        if (mc.world == null) return false;
        var state = mc.world.getBlockState(pos);
        return state.isAir() || !state.isFullCube(mc.world, pos);
    }

    private boolean isSolid(BlockPos pos) {
        if (mc.world == null) return false;
        var state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }

    private void tickCupPath() {
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        if (isStuck()) {
            log("CUP_PATH: Застрял при попытке дойти до бочки " + targetBarrel + ", игнорирую");
            ignoredBarrels.put(targetBarrel, System.currentTimeMillis() + IGNORE_DURATION);
            targetBarrel = null;
            cancelBaritone();
            stuckSince = -1L;
            go(S.CUP_FIND);
            return;
        }
        if (homePos != null && homePos.equals(targetBarrel)) {
            if (inRange(targetBarrel)) { cancelBaritone(); go(S.ARENA_SETHOME); return; }
        } else {
            if (inRange(targetBarrel)) { cancelBaritone(); go(S.CUP_OPEN_DELAY); return; }
        }
        if (!isPathing() && tick.finished(2000L)) {
            setGoal(targetBarrel);
            tick.reset();
        }
    }

    private void tickCupWait() {
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        // Уточняем время чтением вблизи
        long secs = readHologramAboveBarrel(targetBarrel);
        if (secs > 0) {
            barrelOpenAt = System.currentTimeMillis() + secs * 1000L;
        } else {
            // Если не прочиталось, используем сохраненное
            if (barrelOpenAt == -1) {
                // пробуем открыть, если PvP нет
                if (getPvpTimeLeft() <= 0) {
                    go(S.CUP_OPEN_DELAY);
                }
                return;
            }
        }

        long left = barrelOpenAt - System.currentTimeMillis();
        if (left <= OPEN_DELAY_MS) {
            if (inRange(targetBarrel)) { go(S.CUP_OPEN_DELAY); }
            else { setGoal(targetBarrel); go(S.CUP_PATH); }
            return;
        }
        if (!inRange(targetBarrel) && left <= MOVE_TO_BARREL_LEAD_MS) {
            setGoal(targetBarrel);
            go(S.CUP_PATH);
            return;
        }
        if (left > ARENA_THRESHOLD_MS) {
            long pvpLeft = getPvpTimeLeft();
            if (pvpLeft > 0 && pvpLeft < PVP_SAFE_BUFFER_MS) return;
            long timeNeeded = ARENA_HOME_DELAY + PVP_SAFE_BUFFER_MS + 3000L;
            if (left > timeNeeded) {
                homePos = targetBarrel;
                if (!inRange(targetBarrel)) {
                    setGoal(targetBarrel);
                    go(S.CUP_PATH);
                } else {
                    go(S.ARENA_SETHOME);
                }
            }
        }
    }

    private void tickCupOpenDelay() {
        if (barrelOpenAt != -1 && System.currentTimeMillis() < barrelOpenAt - OPEN_DELAY_MS) {
            go(S.CUP_WAIT);
            return;
        }
        if (tick.finished(OPEN_DELAY_MS)) {
            go(S.CUP_OPEN);
        }
    }

    private void tickCupOpen() {
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        if (!inRange(targetBarrel)) { setGoal(targetBarrel); go(S.CUP_PATH); return; }
        if (getPvpTimeLeft() > 0) {
            log("CUP_OPEN: Есть PvP (" + (getPvpTimeLeft() / 1000) + " сек), жду");
            return;
        }
        if (isContainerOpen()) {
            go(S.CUP_LOOT);
            tick.reset();
            return;
        }
        if (tick.finished(10000L)) {
            log("CUP_OPEN: Бочка не открывается > 10 сек, игнорирую");
            ignoredBarrels.put(targetBarrel, System.currentTimeMillis() + IGNORE_DURATION);
            targetBarrel = null;
            go(S.CUP_FIND);
            return;
        }
        if (!tick.finished(150L)) return;
        aimAt(targetBarrel);
        interact(targetBarrel);
        tick.reset();
    }

    private void tickCupLoot() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            if (targetBarrel != null) {
                lootedBarrels.add(targetBarrel);
                barrelTimers.remove(targetBarrel);
            }
            targetBarrel = null;
            barrelOpenAt = -1L;
            if (hasLootToDeposit) {
                log("CUP_LOOT: Залутал бочку! Отхожу и жду окончания КТ");
                if (mc.player != null && cupSpawn != null) {
                    setGoal(cupSpawn.add(5, 0, 5));
                }
                go(S.WAIT_CT);
            } else {
                log("CUP_LOOT: Бочка была пустая, продолжаю искать");
                go(S.CUP_FIND);
            }
            return;
        }
        if (!tick.finished(TICK_MS)) return;
        boolean took = false;
        for (int i = 0; i < c.getRows() * 9; i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                hasLootToDeposit = true;
                took = true;
                break;
            }
        }
        if (!took) {
            mc.player.closeHandledScreen();
            if (targetBarrel != null) {
                lootedBarrels.add(targetBarrel);
                barrelTimers.remove(targetBarrel);
            }
            targetBarrel = null;
            barrelOpenAt = -1L;
            if (hasLootToDeposit) {
                log("CUP_LOOT: Залутал бочку! Отхожу и жду окончания КТ");
                if (mc.player != null && cupSpawn != null) {
                    setGoal(cupSpawn.add(5, 0, 5));
                }
                go(S.WAIT_CT);
            } else {
                log("CUP_LOOT: Бочка была пустая, продолжаю искать");
                go(S.CUP_FIND);
            }
        }
        tick.reset();
    }

    private void tickBreakPot() {
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        if (isPot(targetBarrel)) {
            cancelBaritone();
            aimAt(targetBarrel);
            mc.options.attackKey.setPressed(true);
            tick.reset();
            return;
        }
        mc.options.attackKey.setPressed(false);
        BlockPos potPos = targetBarrel;
        if (!inRange(potPos)) {
            setGoal(potPos);
            return;
        }
        if (!tick.finished(2000L)) return;
        if (hasAnyLoot()) {
            hasLootToDeposit = true;
            log("BREAK_POT: Подобрал ресурсы с вазы!");
        }
        targetBarrel = null;
        if (hasLootToDeposit) {
            log("BREAK_POT: Есть лут, отхожу и жду окончания КТ");
            if (mc.player != null && cupSpawn != null) {
                setGoal(cupSpawn.add(5, 0, 5));
            }
            go(S.WAIT_CT);
        } else {
            go(S.CUP_FIND);
        }
        tick.reset();
    }

    private boolean hasAnyLoot() {
        if (mc.player == null) return false;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (isLoot(mc.player.getInventory().main.get(i))) return true;
        }
        return false;
    }

    private void tickWaitForTimer() {
        long now = System.currentTimeMillis();
        barrelTimers.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            long endTime = entry.getValue();
            if (lootedBarrels.contains(pos)) return true;
            if (endTime < now - BARREL_THRESHOLD_MS) {
                log("WAIT_TIMER: Бочка " + pos + " устарела, удаляю");
                return true;
            }
            return false;
        });
        BlockPos bestBarrel = null;
        long bestTime = Long.MAX_VALUE;
        for (Map.Entry<BlockPos, Long> entry : barrelTimers.entrySet()) {
            long left = entry.getValue() - now;
            if (left <= BARREL_THRESHOLD_MS && left < bestTime) {
                bestTime = left;
                bestBarrel = entry.getKey();
            }
        }
        if (bestBarrel != null) {
            targetBarrel = bestBarrel;
            barrelOpenAt = barrelTimers.get(bestBarrel);
            log("WAIT_TIMER: Бочка " + targetBarrel + " готова через " + (bestTime / 1000) + " сек, иду к ней");
            go(S.CUP_WAIT);
            return;
        }
        if (hasLootToDeposit) {
            log("WAIT_TIMER: Есть лут в инвентаре, иду депозить вместо ожидания");
            go(S.WAIT_CT);
            return;
        }
        if (!barrelTimers.isEmpty()) {
            long minTimeLeft = Long.MAX_VALUE;
            for (long endTime : barrelTimers.values()) {
                long left = endTime - now;
                if (left < minTimeLeft) minTimeLeft = left;
            }
            log("WAIT_TIMER: Жду, ближайшая бочка через " + (minTimeLeft / 1000) + " сек (" + barrelTimers.size() + " бочек в памяти)");
            if (minTimeLeft > 300_000L) {
                log("WAIT_TIMER: Слишком долго ждать (> 5 мин), возвращаюсь к патрулю");
                barrelTimers.clear();
                patrolPhase = 0;
                patrolTarget = null;
                go(S.CUP_FIND);
                return;
            }
            return;
        }
        if (tick.finished(2000L)) {
            log("WAIT_TIMER: Нет бочек в памяти, возвращаюсь к патрулю");
            patrolPhase = 0;
            patrolTarget = null;
            go(S.CUP_FIND);
        }
    }

    private void tickWaitCT() {
        long pvpLeft = getPvpTimeLeft();
        cancelBaritone();
        if (pvpLeft <= 0) {
            log("WAIT_CT: КТ закончился, иду депозить");
            go(S.DEPOSIT_TP);
            return;
        }
        if (tick.finished(1000L)) {
            log("WAIT_CT: Жду окончания КТ, осталось " + (pvpLeft / 1000) + " сек");
            tick.reset();
        }
    }

    // === DEPOSIT ===
    private void tickDepositTp() {
        if (!canUseComandsOrTeleport()) return;
        if (!cmdSent) {
            closeScreen();
            cmd("clan storage");
            cmdSent = true;
            clanStorageRetries = 0;
            tick.reset();
        } else if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            cmdSent = false;
            depositIdx = 0;
            go(S.DEPOSITING);
        } else if (tick.finished(CLAN_ST_WAIT_MS)) {
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
            if (tick.finished(3000L)) {
                hasLootToDeposit = false;
                log("DEPOSITING: Лут депозитнул, возвращаюсь в данж");
                go(S.CUP_TP);
            }
            return;
        }
        if (!tick.finished(TICK_MS)) return;
        int rows = c.getRows() * 9;
        boolean hasSpace = false;
        for (int i = 0; i < rows; i++) {
            if (c.getSlot(i).getStack().isEmpty()) { hasSpace = true; break; }
        }
        if (!hasSpace) {
            mc.player.closeHandledScreen();
            log("DEPOSITING: Клансторейдж заполнен! Иду на базу");
            go(S.STASH_TP);
            return;
        }
        for (int i = rows; i < c.slots.size(); i++) {
            if (isLoot(c.getSlot(i).getStack())) {
                mc.interactionManager.clickSlot(c.syncId, c.getSlot(i).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                tick.reset();
                return;
            }
        }
        mc.player.closeHandledScreen();
        hasLootToDeposit = false;
        log("DEPOSITING: Лут депозитнул, возвращаюсь в данж");
        go(S.CUP_TP);
    }

    // === STASH ===
    private void tickStashTp() {
        if (!canUseComandsOrTeleport()) return;
        if (!cmdSent) { cmd(CLAN_PFX + supplyHome.getValue()); cmdSent = true; tick.reset(); }
        else if (tpDone()) { cmdSent = false; tpSeen = false; stashChest = null; go(S.STASH_FIND); }
    }

    private void tickStashFind() {
        BlockPos chest = findNearestChest();
        if (chest != null) {
            stashChest = chest;
            if (inRange(chest)) { go(S.STASH_DEPOSIT); }
            else { setGoal(chest); go(S.STASH_PATH); }
        }
    }

    private void tickStashPath() {
        if (stashChest == null) { go(S.STASH_FIND); return; }
        if (inRange(stashChest)) { cancelBaritone(); go(S.STASH_DEPOSIT); return; }
        if (!isPathing() && tick.finished(2000L)) { setGoal(stashChest); tick.reset(); }
    }

    private void tickStashDeposit() {
        if (stashChest == null) { go(S.STASH_FIND); return; }
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler c)) {
            if (!tick.finished(100L)) return;
            aimAt(stashChest);
            interact(stashChest);
            tick.reset();
            return;
        }
        if (!tick.finished(TICK_MS)) return;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack stack = mc.player.getInventory().main.get(i);
            if (!stack.isEmpty() && isLoot(stack)) {
                int slotId = i < 9 ? 36 + i : i;
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                tick.reset();
                return;
            }
        }
        mc.player.closeHandledScreen();
        go(S.SUPPLY_FIND);
    }

    private void tickStashWithdrawTp() { go(S.SUPPLY_FIND); }
    private void tickStashWithdraw() { go(S.SUPPLY_FIND); }

    // === ARENA ===
    private void tickArenaSetHome() {
        if (targetBarrel != null && !inRange(targetBarrel)) {
            setGoal(targetBarrel);
            go(S.CUP_PATH);
            return;
        }
        if (!cmdSent) {
            cmd("sethome");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(ARENA_SETHOME_DELAY)) {
            cmdSent = false;
            go(S.ARENA_DARENA);
        }
    }

    private void tickArenaDarena() {
        if (!canUseComandsOrTeleport()) return;
        if (!cmdSent) {
            cmd("darena");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(DARENA_DELAY)) {
            cmdSent = false;
            go(S.ARENA_CLICK);
        }
    }

    private void tickArenaClick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (!tick.finished(500L)) return;
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
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        long left = barrelOpenAt - System.currentTimeMillis();
        if (left > MOVE_TO_BARREL_LEAD_MS) return;
        go(S.ARENA_RETURN);
    }

    private void tickArenaReturn() {
        if (!cmdSent) {
            cmd("home");
            cmdSent = true;
            tick.reset();
        } else if (tick.finished(ARENA_HOME_DELAY)) {
            cmdSent = false;
            go(S.ARENA_RETURN_WAIT);
        }
    }

    private void tickArenaReturnWait() {
        if (targetBarrel == null) { go(S.CUP_FIND); return; }
        if (!inRange(targetBarrel)) {
            setGoal(targetBarrel);
            return;
        }
        go(S.CUP_OPEN_DELAY);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void go(S newState) { state = newState; tick.reset(); }

    private boolean isBarrel(BlockPos p) {
        return mc.world != null && mc.world.getBlockState(p).getBlock() == Blocks.BARREL;
    }

    private boolean isPot(BlockPos p) {
        return mc.world != null && mc.world.getBlockState(p).getBlock() == Blocks.DECORATED_POT;
    }

    private void breakPots() {
        if (mc.world == null || mc.interactionManager == null) return;
        BlockPos pp = mc.player.getBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (isPot(p)) {
                        if (!p.equals(potPos) || pot.finished(POT_MS)) {
                            mc.interactionManager.attackBlock(p, Direction.UP);
                            potPos = p;
                            pot.reset();
                        }
                        return;
                    }
                }
            }
        }
        potPos = null;
    }

    private boolean isChest(BlockPos p) {
        if (mc.world == null) return false;
        var b = mc.world.getBlockState(p).getBlock();
        return b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST;
    }

    private BlockPos findSupplyChest() {
        if (mc.world == null) return null;
        BlockPos pp = mc.player.getBlockPos();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_Y_RADIUS; y <= SEARCH_Y_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (isChest(p) && hasSignWith(p, supplySign.getValue())) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findNearestChest() {
        if (mc.world == null) return null;
        BlockPos pp = mc.player.getBlockPos();
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_Y_RADIUS; y <= SEARCH_Y_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (!isChest(p)) continue;
                    double dist = p.getSquaredDistance(pp);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = p;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean hasSignWith(BlockPos p, String keyword) {
        if (mc.world == null) return false;
        for (Direction d : Direction.values()) {
            BlockPos signPos = p.offset(d);
            BlockEntity be = mc.world.getBlockEntity(signPos);
            if (be instanceof SignBlockEntity sign) {
                for (int i = 0; i < 4; i++) {
                    if (sign.getFrontText().getMessage(i, false).getString().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void aimAt(BlockPos p) {
        if (mc.player == null) return;
        Vec3d c = Vec3d.ofCenter(p);
        double dx = c.x - mc.player.getX();
        double dy = c.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = c.z - mc.player.getZ();
        targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
    }

    private void interact(BlockPos p) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(p), Direction.UP, p, false));
    }

    private boolean isContainerOpen() {
        return mc.player != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
    }

    private void closeScreen() {
        if (mc.player != null) mc.player.closeHandledScreen();
    }

    private void cmd(String command) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendChatCommand(command);
    }

    private boolean isTpBar() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) return false;
        try {
            for (var bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
                if (bar == null) continue;
                String text = bar.getName().getString().toLowerCase(Locale.ROOT);
                if (text.contains("телепортация") || text.contains("teleport")) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private long getPvpTimeLeft() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) return 0L;
        try {
            for (var bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
                if (bar == null) continue;
                String text = bar.getName().getString();
                if (text.toLowerCase(Locale.ROOT).contains("pvp") || text.toLowerCase(Locale.ROOT).contains("пвп")) {
                    long secs = parseTimeToSeconds(text);
                    return secs > 0 ? secs * 1000L : 0L;
                }
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private boolean canUseComandsOrTeleport() {
        long pvpLeft = getPvpTimeLeft();
        if (pvpLeft <= 0) return true;
        return pvpLeft >= PVP_SAFE_BUFFER_MS;
    }

    private boolean tpDone() {
        if (tpSuccessDetected) {
            if (tick.finished(2000L)) {
                tpSuccessDetected = false;
                return true;
            }
            return false;
        }
        return tpSeen && !isTpBar() && tick.finished(TP_WAIT_MS);
    }

    private void configureBaritone(boolean enable) {
        if (enable) {
            BaritoneAPI.getSettings().allowBreak.value = false;
            BaritoneAPI.getSettings().allowPlace.value = false;
            BaritoneAPI.getSettings().allowSprint.value = true;
            BaritoneAPI.getSettings().allowParkour.value = false;
            BaritoneAPI.getSettings().freeLook.value = false;
            BaritoneAPI.getSettings().assumeWalkOnWater.value = false;
            BaritoneAPI.getSettings().allowWaterBucketFall.value = false;
            BaritoneAPI.getSettings().blockReachDistance.value = 4.5f;
        }
    }

    private void setGoal(BlockPos p) {
        if (p == null) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(p, 2));
    }

    private void cancelBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    private boolean isPathing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
    }

    private boolean isStuck() {
        if (!isPathing()) {
            stuckSince = -1L;
            return false;
        }
        if (mc.player == null) return false;
        boolean isMoving = mc.player.getVelocity().horizontalLengthSquared() > 0.001;
        if (isMoving) {
            stuckSince = -1L;
            return false;
        }
        if (stuckSince == -1L) {
            stuckSince = System.currentTimeMillis();
            return false;
        }
        return System.currentTimeMillis() - stuckSince > STUCK_MS;
    }

    private boolean inRange(BlockPos p) {
        return mc.player != null && mc.player.getBlockPos().getSquaredDistance(p) <= RANGE_SQ;
    }

    private boolean isLoot(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = Registries.ITEM.getId(stack.getItem()).getPath();
        for (String lootId : LOOT_IDS) {
            if (id.equals(lootId)) return true;
        }
        return false;
    }

    private boolean isInvisPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
            var cont = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (cont != null) {
                for (StatusEffectInstance ef : cont.getEffects()) {
                    if (ef.getEffectType().equals(StatusEffects.INVISIBILITY)) return true;
                }
            }
        }
        String keyword = invisKeyword.getValue().toLowerCase(Locale.ROOT);
        var customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName != null && customName.getString().toLowerCase(Locale.ROOT).contains(keyword)) return true;
        String itemName = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (itemName.contains(keyword)) return true;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (var line : lore.lines()) {
                if (line.getString().toLowerCase(Locale.ROOT).contains(keyword)) return true;
            }
        }
        return false;
    }

    private boolean hasInvis() {
        if (mc.player == null) return false;
        StatusEffectInstance eff = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
        return eff != null && eff.getDuration() > 200;
    }

    private int findInvisAny() {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int findInvisHotbar() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int findEmptyHotbar() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private void switchSlot(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(slot));
    }
}