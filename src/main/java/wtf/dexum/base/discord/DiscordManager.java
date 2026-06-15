package wtf.dexum.base.discord;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.interfaces.IMinecraft;

public class DiscordManager implements IMinecraft {
    private boolean running = false;

    public DiscordManager() {
        this.initRPC();
    }

    @Native
    private void initRPC() {
        try {
            DiscordIPC.start(1516036796167753900L, null);
            this.running = true;
            this.updatePresence();

            Thread daemon = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(15000L);
                        if (running) updatePresence();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "Discord-RPC-Daemon");
            daemon.setDaemon(true);
            daemon.start();
        } catch (Throwable e) {
            this.running = false;
        }
    }

    @Native
    public void updatePresence() {
        if (!running) return;
        try {
            RichPresence presence = new RichPresence();
            presence.setDetails("DexumClient");
            presence.setState("Made by [blackyout1]");
            presence.setLargeImage("logo", "Dexum v1.0");
            presence.setStart(System.currentTimeMillis() / 1000L);
            DiscordIPC.setActivity(presence);
        } catch (Exception e) {}
    }

    @Native
    public void stopRPC() {
        this.running = false;
        try {
            DiscordIPC.stop();
        } catch (Exception ignored) {}
    }

    public boolean isRunning() {
        return this.running;
    }
}