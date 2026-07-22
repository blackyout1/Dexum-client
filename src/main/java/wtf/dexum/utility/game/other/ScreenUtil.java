package wtf.dexum.utility.game.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class ScreenUtil implements IMinecraft {
    private ScreenUtil() {
    }

    public static <T extends Screen> T get(MinecraftClient client, Screen screen, Class<T> clazz) {
        if (client != null && clazz.isInstance(client.currentScreen)) {
            return clazz.cast(client.currentScreen);
        } else {
            return (screen instanceof HandledScreen && client != null && client.player != null && screen instanceof HandledScreen)
                    ? (T) (clazz.isInstance(screen) ? clazz.cast(screen) : null)
                    : null;
        }
    }

    public static boolean isScreenHandled(MinecraftClient client, Screen screen) {
        return client != null && client.player != null && screen instanceof HandledScreen
                ? client.player.currentScreenHandler == ((HandledScreen) screen).getScreenHandler()
                : false;
    }

    public static boolean hasOpenScreen(MinecraftClient client) {
        return client != null
                && client.player != null
                && client.player.currentScreenHandler != client.player.playerScreenHandler;
    }

    public static boolean isOpen(MinecraftClient client, Screen screen) {
        return client != null && (client.currentScreen != null || isScreenHandled(client, screen));
    }
}