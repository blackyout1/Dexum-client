package wtf.dexum;

import java.io.File;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.autobuy.AutoBuyManager;
import wtf.dexum.base.comand.CommandManager;
import wtf.dexum.base.config.ConfigManager;
import wtf.dexum.base.discord.DiscordManager;
import wtf.dexum.base.filemanager.impl.FriendManager;
import wtf.dexum.base.filemanager.impl.StaffManager;
import wtf.dexum.base.macro.MacroManager;
import wtf.dexum.base.modules.ModuleManager;
import wtf.dexum.base.notify.NotifyManager;
import wtf.dexum.base.repository.RCTRepository;
import wtf.dexum.base.request.ScriptManager;
import wtf.dexum.base.theme.ThemeManager;
import wtf.dexum.base.waypoint.WaypointManager;
import wtf.dexum.client.hud.elements.ToggleNotify;
import wtf.dexum.client.screens.menu.MenuScreen;
import wtf.dexum.utility.game.server.ServerHandler;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.display.shader.GlProgram;
import wtf.dexum.utility.render.shader.ShaderHandsRenderer;

public enum Dexum implements ClientModInitializer {
    INSTANCE;

    public static final String NAME = "dexum";
    public static final String VER = "";
    public static final String TYPE = "DEV";
    private static final String MOD_ID = "dexum".toLowerCase();
    public static File DIRECTORY;
    private ModuleManager moduleManager;
    private ThemeManager themeManager;
    private MenuScreen menuScreen;
    private ScriptManager scriptManager;
    private ServerHandler serverHandler;
    private FriendManager friendManager;
    private MacroManager macroManager;
    private StaffManager staffManager;
    private AutoBuyManager autoBuyManager;
    private WaypointManager waypointManager;
    private NotifyManager notifyManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RCTRepository rctRepository;
    private DiscordManager discordManager;
    private ToggleNotify toggleNotify;
    private boolean initialized = false;

    @Override
    public void onInitializeClient() {
        try {
            init();
        } catch (Exception e) {

            throw e;
        }
    }

    @Native
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "dexum");
            if (!DIRECTORY.exists()) {
                DIRECTORY.mkdirs();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                getInstance().shutdown();
            }));

            this.friendManager = new FriendManager();
            this.macroManager = new MacroManager();
            this.staffManager = new StaffManager();
            this.notifyManager = new NotifyManager();
            this.serverHandler = new ServerHandler();
            this.rctRepository = new RCTRepository();
            this.themeManager = new ThemeManager();
            this.moduleManager = new ModuleManager();
            this.configManager = new ConfigManager();
            this.autoBuyManager = new AutoBuyManager();
            this.commandManager = new CommandManager();
            this.scriptManager = new ScriptManager();
            try {
                this.discordManager = new DiscordManager();
            } catch (Throwable e) {
                this.discordManager = null;
            }
            this.toggleNotify = new ToggleNotify();
            this.waypointManager = new WaypointManager();
            this.menuScreen = new MenuScreen();
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Dexum.id("after_shader_load");
                }

                @Override
                public void reload(ResourceManager manager) {
                    GlProgram.loadAndSetupPrograms();
                }
            });
            DrawUtil.initializeShaders();
            ShaderHandsRenderer.initializeShaders();
            wtf.dexum.utility.render.shader.ShaderBlockRenderer.initializeShaders();
        } catch (Exception e) {

            throw new RuntimeException("dexum initialization failed", e);
        }
    }

    @Native
    public void shutdown() {
        this.friendManager.save();
        this.staffManager.save();
        this.configManager.save();
        this.macroManager.save();
        if (this.discordManager != null) {
            this.discordManager.stopRPC();
        }

    }

    public static Identifier id(String path) {
        return Identifier.of("dexum", path);
    }

    public static Dexum getInstance() {
        return INSTANCE;
    }

    public RCTRepository getRCTRepository() {
        return this.rctRepository;
    }

    @Generated
    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    @Generated
    public ThemeManager getThemeManager() {
        return this.themeManager;
    }

    @Generated
    public MenuScreen getMenuScreen() {
        return this.menuScreen;
    }

    @Generated
    public ScriptManager getScriptManager() {
        return this.scriptManager;
    }

    @Generated
    public ServerHandler getServerHandler() {
        return this.serverHandler;
    }

    @Generated
    public FriendManager getFriendManager() {
        return this.friendManager;
    }

    @Generated
    public MacroManager getMacroManager() {
        return this.macroManager;
    }

    @Generated
    public StaffManager getStaffManager() {
        return this.staffManager;
    }

    @Generated
    public AutoBuyManager getAutoBuyManager() {
        return this.autoBuyManager;
    }

    @Generated
    public WaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    @Generated
    public NotifyManager getNotifyManager() {
        return this.notifyManager;
    }

    @Generated
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Generated
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    @Generated
    public DiscordManager getDiscordManager() {
        return this.discordManager;
    }

    @Generated
    public ToggleNotify getToggleNotify() {
        return this.toggleNotify;
    }

    private static Dexum[] $values() {
        return new Dexum[]{INSTANCE};
    }
}