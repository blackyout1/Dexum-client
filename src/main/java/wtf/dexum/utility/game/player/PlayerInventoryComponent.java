package wtf.dexum.utility.game.player;

import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.request.ScriptManager;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class PlayerInventoryComponent implements IMinecraft {
    public static final List<KeyBinding> moveKeys;
    public static ScriptManager.ScriptTask script;
    public static boolean canMove;

    public static void addTask(Runnable task) {
        if (MovingUtil.hasPlayerMovement()) {
            Dexum.getInstance().getScriptManager().addTask(script);
            String var1 = Dexum.getInstance().getServerHandler().getServer();
            byte var2 = -1;
            switch(var1.hashCode()) {
                case -495240450:
                    if (var1.equals("HolyWorld")) {
                        var2 = 1;
                    }
                    break;
                case -441313278:
                    if (var1.equals("CopyTime")) {
                        var2 = 3;
                    }
                    break;
                case 1087265806:
                    if (var1.equals("Pidaras")) {
                        var2 = 2;
                    }
                    break;
                case 1154553036:
                    if (var1.equals("FunTime")) {
                        var2 = 0;
                    }
            }

            switch(var2) {
                case 0:
                case 1:
                    script.schedule(EventUpdate.class, (eventUpdate) -> {
                        disableMoveKeys();
                        return true;
                    });
                    script.schedule(EventUpdate.class, (eventUpdate) -> {
                        task.run();
                        enableMoveKeys();
                        return true;
                    });
                    return;
                case 2:
                    if (mc.player.isOnGround()) {
                        script.schedule(EventUpdate.class, (eventUpdate) -> {
                            disableMoveKeys();
                            return true;
                        });
                        script.schedule(EventUpdate.class, (eventUpdate) -> {
                            disableMoveKeys();
                            return true;
                        });
                        script.schedule(EventUpdate.class, (eventUpdate) -> {
                            task.run();
                            return true;
                        });
                        script.schedule(EventUpdate.class, (eventUpdate) -> {
                            enableMoveKeys();
                            return true;
                        });
                        return;
                    }
                    break;
                case 3:
                    script.schedule(EventUpdate.class, (eventUpdate) -> {
                        disableMoveKeys();
                        return true;
                    });
                    script.schedule(EventUpdate.class, (eventUpdate) -> {
                        task.run();
                        return true;
                    });
                    script.schedule(EventUpdate.class, (eventUpdate) -> {
                        enableMoveKeys();
                        return true;
                    });
                    return;
            }
        }

        task.run();
    }

    public static void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public static void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public static void unPressMoveKeys() {
        moveKeys.forEach((keyBinding) -> {
            keyBinding.setPressed(false);
        });
    }

    public static void updateMoveKeys() {
        if (!Dexum.getInstance().getMenuScreen().search) {
            moveKeys.forEach((keyBinding) -> {
                keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode()));
            });
        }

    }

    public static boolean shouldSkipExecution() {
        if (mc.currentScreen == null) {
            return false;
        } else if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            return false;
        } else if (mc.currentScreen instanceof SignEditScreen) {
            return false;
        } else if (mc.currentScreen instanceof AnvilScreen) {
            return false;
        } else if (mc.currentScreen instanceof AbstractCommandBlockScreen) {
            return false;
        } else if (mc.currentScreen instanceof StructureBlockScreen) {
            return false;
        } else if (mc.player != null && mc.player.currentScreenHandler != null) {
            int slotCount = mc.player.currentScreenHandler.slots.size();
            return slotCount >= 27;
        } else {
            return false;
        }
    }

    @Generated
    private PlayerInventoryComponent() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
        script = new ScriptManager.ScriptTask();
        canMove = true;
    }
}