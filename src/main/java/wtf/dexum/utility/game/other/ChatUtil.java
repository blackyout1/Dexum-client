package wtf.dexum.utility.game.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class ChatUtil implements IMinecraft {
    public static void sendMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            MutableText text = Text.literal("")
                    .append(Text.literal("AutoVillageTrade").formatted(Formatting.AQUA))
                    .append(Text.literal(" Â» ").formatted(Formatting.WHITE))
                    .append(Text.literal(message).formatted(Formatting.GRAY));
            client.inGameHud.getChatHud().addMessage(text);
        } else {
            System.out.println("[AutoVillageTrade] " + message);
        }
    }

    private ChatUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}