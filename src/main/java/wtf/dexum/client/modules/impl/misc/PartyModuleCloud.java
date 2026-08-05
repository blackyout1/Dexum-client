package wtf.dexum.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ModuleAnnotation(name = "PartyCloud", category = Category.MISC, description = "Облачная система пати через WebSocket")
public final class PartyModuleCloud extends Module {

    public static final PartyModuleCloud INSTANCE = new PartyModuleCloud();

    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // WebSocket клиент
    private PartyWebSocketClient wsClient;
    private String serverUrl = "ws://localhost:8080"; // Можно изменить на облачный URL
    
    // Данные участников
    private final ConcurrentHashMap<String, PartyMember> members = new ConcurrentHashMap<>();
    private String currentPartyCode = null;
    private int tickCounter = 0;
    
    private boolean showNametags = true;
    private boolean showWaypoints = true;

    private PartyModuleCloud() {}

    // === Внутренний класс данных участника ===
    private static class PartyMember {
        String uuid;
        String name;
        double x, y, z;
        String dim;
        long lastUpdate;

        PartyMember(String uuid, String name, double x, double y, double z, String dim) {
            this.uuid = uuid;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        connectToServer();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        disconnectFromServer();
    }

    // === WebSocket подключение ===
    private void connectToServer() {
        if (wsClient != null && wsClient.isOpen()) return;

        try {
            wsClient = new PartyWebSocketClient(new URI(serverUrl), this::handleServerMessage);
            wsClient.connect();
            
            // Ждём подключения и регистрируемся
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Ждём установки соединения
                    if (wsClient.isOpen() && client.player != null) {
                        String uuid = client.player.getUuid().toString();
                        String name = client.player.getName().getString();
                        wsClient.register(uuid, name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            sendMessage("§aПодключение к Party серверу...");
        } catch (Exception e) {
            sendMessage("§cОшибка подключения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        if (wsClient != null) {
            if (currentPartyCode != null) {
                wsClient.leaveParty();
            }
            wsClient.close();
            wsClient = null;
        }
        members.clear();
        currentPartyCode = null;
    }

    // === Обработка сообщений от сервера ===
    private void handleServerMessage(JsonObject msg) {
        String type = msg.get("type").getAsString();
        JsonObject payload = msg.has("payload") ? msg.getAsJsonObject("payload") : new JsonObject();

        switch (type) {
            case "REGISTERED":
                wsClient.setRegistered(true);
                sendMessage("§aПодключено к Party серверу!");
                break;

            case "PARTY_CREATED":
                currentPartyCode = payload.get("code").getAsString();
                sendMessage("§aПати создана! Код: §f" + currentPartyCode);
                break;

            case "PARTY_JOINED":
                currentPartyCode = payload.get("code").getAsString();
                updateMembersList(payload.getAsJsonArray("members"));
                sendMessage("§aПрисоединились к пати: §f" + currentPartyCode);
                break;

            case "PARTY_LEFT":
                sendMessage("§aВы покинули пати");
                currentPartyCode = null;
                members.clear();
                break;

            case "PARTY_UPDATE":
                updateMembersList(payload.getAsJsonArray("members"));
                break;

            case "MEMBERS_LIST":
                updateMembersList(payload.getAsJsonArray("members"));
                break;

            case "ERROR":
                String error = payload.get("error").getAsString();
                sendMessage("§c" + error);
                break;
        }
    }

    private void updateMembersList(JsonArray membersArray) {
        members.clear();
        for (JsonElement element : membersArray) {
            JsonObject memberObj = element.getAsJsonObject();
            String uuid = memberObj.get("uuid").getAsString();
            
            // Пропускаем себя
            if (client.player != null && uuid.equals(client.player.getUuid().toString())) {
                continue;
            }

            String name = memberObj.get("name").getAsString();
            double x = memberObj.get("x").getAsDouble();
            double y = memberObj.get("y").getAsDouble();
            double z = memberObj.get("z").getAsDouble();
            String dim = memberObj.get("dim").getAsString();

            PartyMember member = new PartyMember(uuid, name, x, y, z, dim);
            members.put(uuid, member);
        }
    }

    // === Публичные методы для команд ===
    public boolean isInParty() {
        return currentPartyCode != null;
    }

    public boolean isLeader() {
        // TODO: добавить логику лидера
        return false;
    }

    public boolean isPlayerInParty(String uuid) {
        return members.containsKey(uuid);
    }

    public void createParty(String code) {
        if (wsClient == null || !wsClient.isOpen()) {
            sendMessage("§cНе подключено к серверу");
            return;
        }
        wsClient.createParty(code);
    }

    public void joinParty(String code) {
        if (wsClient == null || !wsClient.isOpen()) {
            sendMessage("§cНе подключено к серверу");
            return;
        }
        wsClient.joinParty(code);
    }

    public void leaveParty() {
        if (wsClient == null || !wsClient.isOpen()) return;
        wsClient.leaveParty();
    }

    public void disbandParty() {
        leaveParty();
    }

    public void showMembers() {
        if (members.isEmpty()) {
            sendMessage("В пати никого нет");
            return;
        }
        sendMessage("§aУчастники пати (" + currentPartyCode + "):");
        for (PartyMember m : members.values()) {
            sendMessage("  §f" + m.name + " §7[" + String.format("%.0f, %.0f, %.0f", m.x, m.y, m.z) + "]");
        }
    }

    private void sendMessage(String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§a[Party] §f" + text), false);
        }
    }

    // === События ===
    @EventTarget
    public void onUpdate(EventGameUpdate event) {
        if (!isEnabled()) return;
        
        tickCounter++;
        if (tickCounter >= 20) { // Раз в секунду
            tickCounter = 0;
            sendPosition();
        }
    }

    private void sendPosition() {
        if (wsClient == null || !wsClient.isOpen() || currentPartyCode == null) return;
        if (client.player == null) return;

        ClientPlayerEntity player = client.player;
        String dim = player.getWorld().getRegistryKey().getValue().toString();
        
        wsClient.updatePosition(player.getX(), player.getY(), player.getZ(), dim);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (!isEnabled()) return;
        if (currentPartyCode == null) return;
        renderMembers(event.getMatrix());
    }

    private void renderMembers(MatrixStack matrices) {
        if (client.player == null) return;
        
        World currentWorld = client.player.getWorld();
        String currentDim = currentWorld.getRegistryKey().getValue().toString();
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        for (PartyMember member : members.values()) {
            // Проверяем измерение
            if (!member.dim.equals(currentDim)) continue;

            double x = member.x - cameraPos.x;
            double y = member.y - cameraPos.y;
            double z = member.z - cameraPos.z;

            if (showNametags) {
                renderNametag(matrices, member.name, x, y + 2.2, z);
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
        
        // Рисуем 3 круга для "мясистости"
        for (int layer = 0; layer < 3; layer++) {
            float radius = 0.6f + layer * 0.05f; // Увеличиваем радиус с каждым слоем
            int alpha = 255 - layer * 60; // Уменьшаем прозрачность к краям
            
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            
            for (int i = 0; i <= 72; i++) { // Увеличено с 36 до 72 для гладкости
                double angle = Math.PI * 2 * i / 72;
                double dx = radius * Math.cos(angle);
                double dz = radius * Math.sin(angle);
                buffer.vertex(matrices.peek().getPositionMatrix(), (float) dx, 0, (float) dz)
                      .color(0x55, 0xFF, 0x55, alpha);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        
        matrices.pop();
    }
}
