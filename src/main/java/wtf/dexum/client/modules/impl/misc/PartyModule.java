package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ModuleAnnotation(name = "Party", category = Category.MISC, description = "Создавайте партии и видите друзей на расстоянии")
public final class PartyModule extends Module {

    public static final PartyModule INSTANCE = new PartyModule();

    private final MinecraftClient client = MinecraftClient.getInstance();

    // === Внутренние классы данных ===
    private static class PartyMember {
        private final UUID uuid;
        private String name;
        private double x, y, z;
        private World world;
        private long lastUpdate;

        public PartyMember(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
            this.lastUpdate = System.currentTimeMillis();
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public void setPosition(double x, double y, double z, World world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
            this.lastUpdate = System.currentTimeMillis();
        }

        public World getWorld() { return world; }
        public long getLastUpdate() { return lastUpdate; }
        public boolean isExpired(long timeout) {
            return System.currentTimeMillis() - lastUpdate > timeout;
        }
    }

    private static class PartyData {
        private final String code;
        private UUID leader;
        private boolean validated; // Партия подтверждена (получено хотя бы одно сообщение)

        public PartyData(String code, UUID leader) {
            this.code = code;
            this.leader = leader;
            this.validated = (leader != null); // Если создаём сами - сразу validated
        }

        public String getCode() { return code; }
        public UUID getLeader() { return leader; }
        public void setLeader(UUID leader) { this.leader = leader; }
        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }
    }

    // === Поля модуля ===
    private PartyData currentParty;
    private final ConcurrentHashMap<UUID, PartyMember> members = new ConcurrentHashMap<>();
    private int tickCounter = 0;
    private int validationTimer = 0; // Таймер для проверки существования партии

    private boolean showNametags = true;
    private boolean showWaypoints = true;
    
    private static final int VALIDATION_TIMEOUT = 100; // 5 секунд (100 тиков)

    private PartyModule() {}

    // === Жизненный цикл модуля ===
    @Override
    public void onEnable() {
        super.onEnable();
        if (client.player != null && currentParty != null) {
            addSelf();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (isInParty()) {
            leaveParty();
        }
    }

    // === Обработка событий ===
    @EventTarget
    public void onUpdate(EventGameUpdate event) {
        if (!isEnabled()) return;
        onTick();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (!isEnabled()) return;
        onRenderWorld(event.getMatrix(), event.getPartialTicks());
    }

    @EventTarget
    public void onPacket(wtf.dexum.base.events.impl.server.EventPacket event) {
        if (!isEnabled()) return;
        
        // Обрабатываем входящие сообщения чата для синхронизации позиций
        if (event.isReceive()) {
            if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.GameMessageS2CPacket packet) {
                String message = packet.content().getString();
                if (message.startsWith("!party_pos ")) {
                    parsePositionUpdate(message);
                    // СКРЫВАЕМ сообщение от чата - игроки не должны видеть координаты
                    event.cancel();
                }
            }
        }
    }

    // === Публичные методы для CommandManager ===
    public boolean isInParty() {
        return currentParty != null;
    }

    public boolean isLeader() {
        return isInParty() && currentParty.getLeader() != null
                && currentParty.getLeader().equals(client.player.getUuid());
    }

    private String getPartyCode() {
        return isInParty() ? currentParty.getCode() : null;
    }

    // === Публичные методы для команд ===
    public void createParty(String code) {
        if (isInParty()) leaveParty();
        currentParty = new PartyData(code, client.player.getUuid());
        members.clear();
        addSelf();
        sendMessage("Пати создана! Код: " + code);
    }

    public void joinParty(String code) {
        if (isInParty()) leaveParty();
        currentParty = new PartyData(code, null);
        members.clear();
        addSelf();
        validationTimer = 0; // Запускаем таймер валидации
        sendMessage("Подключение к пати " + code + "... Ожидание подтверждения.");
    }

    public void leaveParty() {
        currentParty = null;
        members.clear();
    }

    public void disbandParty() {
        currentParty = null;
        members.clear();
        sendMessage("Пати распущена.");
    }

    public void showMembers() {
        if (members.isEmpty()) {
            sendMessage("В пати никого нет.");
            return;
        }
        StringBuilder sb = new StringBuilder("Участники пати (" + getPartyCode() + "): ");
        for (PartyMember m : members.values()) {
            sb.append(m.getName()).append(", ");
        }
        sendMessage(sb.substring(0, sb.length() - 2));
    }

    private void addSelf() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        PartyMember self = new PartyMember(player.getUuid(), player.getName().getString());
        self.setPosition(player.getX(), player.getY(), player.getZ(), player.getWorld());
        members.put(self.getUuid(), self);
    }

    private void sendMessage(String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[Party] §f" + text), false);
        }
    }

    // === Синхронизация позиций ===
    private void onTick() {
        if (!isInParty()) return;
        
        // Проверка валидации партии при join
        if (!currentParty.isValidated() && !isLeader()) {
            validationTimer++;
            if (validationTimer >= VALIDATION_TIMEOUT) {
                sendMessage("§cПати с кодом " + currentParty.getCode() + " не найдена. Возможно, никто не создал эту пати.");
                leaveParty();
                return;
            }
        }
        
        tickCounter++;
        if (tickCounter >= 20) { // Раз в секунду
            tickCounter = 0;
            sendMyPosition();
        }
        long now = System.currentTimeMillis();
        members.values().removeIf(m -> m.isExpired(5000));
    }

    private void sendMyPosition() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        String code = getPartyCode();
        if (code == null) return;
        World world = player.getWorld();
        String dim = world.getRegistryKey().getValue().toString();
        String msg = String.format("!party_pos %s %s %.2f %.2f %.2f %s",
                code,
                player.getName().getString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                dim
        );
        player.networkHandler.sendChatMessage(msg);
    }

    private void parsePositionUpdate(String msg) {
        String[] parts = msg.split(" ");
        if (parts.length < 7) return;
        String code = parts[1];
        if (!code.equals(getPartyCode())) return;
        
        // Партия подтверждена - кто-то реально отправляет позиции
        if (!currentParty.isValidated()) {
            currentParty.setValidated(true);
            sendMessage("§aПати " + code + " найдена! Подключено.");
        }
        
        String name = parts[2];
        try {
            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);
            double z = Double.parseDouble(parts[5]);
            String dim = parts[6];
            World world = findWorldByDimension(dim);
            if (world == null) return;

            UUID uuid = findUuidByName(name);
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(name.getBytes());
            }
            addOrUpdateMember(uuid, name, x, y, z, world);
        } catch (NumberFormatException ignored) {}
    }

    private World findWorldByDimension(String dimStr) {
        if (client.world == null) return null;
        String currentDim = client.world.getRegistryKey().getValue().toString();
        if (currentDim.equals(dimStr)) {
            return client.world;
        }
        return null;
    }

    private UUID findUuidByName(String name) {
        for (PartyMember m : members.values()) {
            if (m.getName().equals(name)) return m.getUuid();
        }
        return null;
    }

    private void addOrUpdateMember(UUID uuid, String name, double x, double y, double z, World world) {
        PartyMember member = members.get(uuid);
        if (member == null) {
            member = new PartyMember(uuid, name);
            members.put(uuid, member);
        } else {
            member.setName(name);
        }
        member.setPosition(x, y, z, world);
        if (currentParty != null && currentParty.getLeader() == null && !uuid.equals(client.player.getUuid())) {
            currentParty.setLeader(uuid);
        }
    }

    // === Рендер ===
    private void onRenderWorld(MatrixStack matrices, float partialTicks) {
        if (!isInParty()) return;
        if (client.player == null) return;

        ClientPlayerEntity player = client.player;
        World currentWorld = player.getWorld();
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        for (PartyMember member : members.values()) {
            if (member.getUuid().equals(player.getUuid())) continue;
            if (member.getWorld() != currentWorld) continue;

            double x = member.getX() - cameraPos.x;
            double y = member.getY() - cameraPos.y;
            double z = member.getZ() - cameraPos.z;

            if (showNametags) {
                renderNametag(matrices, member.getName(), x, y + 2.2, z);
            }
            if (showWaypoints) {
                renderWaypoint(matrices, x, y + 0.1, z);
            }
        }
    }

    private void renderNametag(MatrixStack matrices, String text, double x, double y, double z) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        
        float distance = (float) Math.sqrt(x * x + y * y + z * z);
        float scale = 0.025f * Math.max(1.0f, distance / 10.0f);
        matrices.scale(-scale, -scale, scale);

        TextRenderer textRenderer = client.textRenderer;
        int width = textRenderer.getWidth(text);
        int color = 0x55FF55;

        // Рисуем фон
        int bgX1 = -width / 2 - 2;
        int bgX2 = width / 2 + 2;
        int bgY1 = -2;
        int bgY2 = 10;
        
        // Рисуем текст
        textRenderer.draw(text, -width / 2f, 0, color, false,
                matrices.peek().getPositionMatrix(), client.getBufferBuilders().getEntityVertexConsumers(),
                TextRenderer.TextLayerType.NORMAL, 0x80000000, 15728880);

        client.getBufferBuilders().getEntityVertexConsumers().draw();
        matrices.pop();
    }

    private void renderWaypoint(MatrixStack matrices, double x, double y, double z) {
        matrices.push();
        matrices.translate(x, y, z);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        // Рисуем зелёный круг
        for (int i = 0; i <= 36; i++) {
            double angle = Math.PI * 2 * i / 36;
            double dx = 0.6 * Math.cos(angle);
            double dz = 0.6 * Math.sin(angle);
            buffer.vertex(matrices.peek().getPositionMatrix(), (float) dx, 0, (float) dz)
                  .color(0x55, 0xFF, 0x55, 0xFF);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }
}