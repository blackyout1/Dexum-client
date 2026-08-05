package wtf.dexum.client.modules.impl.misc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class PartyWebSocketClient extends WebSocketClient {
    
    private static final Gson GSON = new Gson();
    private final Consumer<JsonObject> messageHandler;
    private boolean isRegistered = false;

    public PartyWebSocketClient(URI serverUri, Consumer<JsonObject> messageHandler) {
        super(serverUri);
        this.messageHandler = messageHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[Party] ✅ Подключено к серверу");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = GSON.fromJson(message, JsonObject.class);
            messageHandler.accept(json);
        } catch (Exception e) {
            System.err.println("[Party] ❌ Ошибка обработки сообщения: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[Party] ❌ Отключено: " + reason);
        isRegistered = false;
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[Party] ❌ Ошибка WebSocket: " + ex.getMessage());
    }

    public void sendMessage(String type, JsonObject payload) {
        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("payload", payload);
        send(GSON.toJson(message));
    }

    public void register(String uuid, String name) {
        JsonObject payload = new JsonObject();
        payload.addProperty("uuid", uuid);
        payload.addProperty("name", name);
        sendMessage("REGISTER", payload);
    }

    public void createParty(String code) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        sendMessage("CREATE_PARTY", payload);
    }

    public void joinParty(String code) {
        JsonObject payload = new JsonObject();
        payload.addProperty("code", code);
        sendMessage("JOIN_PARTY", payload);
    }

    public void leaveParty() {
        sendMessage("LEAVE_PARTY", new JsonObject());
    }

    public void updatePosition(double x, double y, double z, String dim) {
        JsonObject payload = new JsonObject();
        payload.addProperty("x", x);
        payload.addProperty("y", y);
        payload.addProperty("z", z);
        payload.addProperty("dim", dim);
        sendMessage("UPDATE_POSITION", payload);
    }

    public void listMembers() {
        sendMessage("LIST_MEMBERS", new JsonObject());
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }
}
