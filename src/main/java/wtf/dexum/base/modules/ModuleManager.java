package wtf.dexum.base.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.input.EventKey;
import wtf.dexum.base.events.impl.other.EventGameUpdate;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.base.macro.Macro;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.client.modules.impl.combat.*;
import wtf.dexum.client.modules.impl.misc.*;
import wtf.dexum.client.modules.impl.movement.*;
import wtf.dexum.client.modules.impl.misc.FakePlayer;
import wtf.dexum.client.modules.impl.player.*;
import wtf.dexum.client.modules.impl.player.NoFall;
import wtf.dexum.client.modules.impl.render.*;
import wtf.dexum.client.screens.menu.MenuScreen;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;
import wtf.dexum.utility.interfaces.IMinecraft;

public final class ModuleManager implements IMinecraft {
    private final List<Module> modules = new ArrayList<>();
    private boolean isBack;
    private boolean isRotated;
    private float acceleration;

    private long lastKeyPressTime = 0;
    private int lastKeyCode = -1;
    private static final long DEBOUNCE_THRESHOLD_MS = 200;

    public ModuleManager() {
        init();
        EventManager.register(this);
    }

    private void init() {
        registerCombat();
        registerMovement();
        registerRender();
        registerPlayer();
        registerMisc();
    }

    private void registerCombat() {
        registerModule(TriggerBot.INSTANCE);
        registerModule(AntiBot.INSTANCE);
        registerModule(Aura.INSTANCE);
        registerModule(AutoExplosion.INSTANCE);
        registerModule(AutoTotem.INSTANCE);
        registerModule(ClickPearl.INSTANCE);
        registerModule(PearlTarget.INSTANCE);
        registerModule(PacketCriticals.INSTANCE);
        registerModule(TpsSync.INSTANCE);
        registerModule(AutoSwap.INSTANCE);
        registerModule(CrystalAura.INSTANCE);
        registerModule(AimBow.INSTANCE);
    }

    private void registerMovement() {
        registerModule(AutoSprint.INSTANCE);
        registerModule(ElytraBooster.INSTANCE);
        registerModule(ElytraRecast.INSTANCE);
        registerModule(GrimGlide.INSTANCE);
        registerModule(GuiWalk.INSTANCE);
        registerModule(NoSlow.INSTANCE);
        registerModule(Speed.INSTANCE);
        registerModule(AirStuck.INSTANCE);
        registerModule(ElytraMotion.INSTANCE);
        registerModule(NoWeb.INSTANCE);
        registerModule(NoFall.INSTANCE);
    }

    private void registerRender() {
        registerModule(Interface.INSTANCE);
        registerModule(AntiInvisible.INSTANCE);
        registerModule(NoRender.INSTANCE);
        registerModule(Predictions.INSTANCE);
        registerModule(SwingAnimation.INSTANCE);
        registerModule(Crosshair.INSTANCE);
        registerModule(ViewModel.INSTANCE);
        registerModule(Ambience.INSTANCE);
        registerModule(ShulkerPreview.INSTANCE);
        registerModule(FireworkESP.INSTANCE);
        registerModule(JumpCircle.INSTANCE);
        registerModule(Cosmetics.INSTANCE);
        registerModule(FullBright.INSTANCE);
        registerModule(Menu.INSTANCE);
        registerModule(EntityESP.INSTANCE);
        registerModule(Particles.INSTANCE);
        registerModule(TargetESP.INSTANCE);
        registerModule(KillEffect.INSTANCE);
        registerModule(BloomBlock.INSTANCE);
        registerModule(TotemPop.INSTANCE);
        registerModule(LineGlyphes.INSTANCE);
        registerModule(Arrows.INSTANCE);
        registerModule(Cubes.INSTANCE);
        registerModule(HitMarker.INSTANCE);
        registerModule(new ClientBow());
        registerModule(ClientSounds.INSTANCE);
        registerModule(AspectRatio.INSTANCE);
        registerModule(ShaderHands.INSTANCE);
        registerModule(CustomSky.INSTANCE);
    }

    private void registerPlayer() {
        registerModule(AutoWarden.INSTANCE);
        registerModule(AutoTool.INSTANCE);
        registerModule(AutoArmor.INSTANCE);
        registerModule(Blink.INSTANCE);
        registerModule(NoDelay.INSTANCE);
        registerModule(FastBreak.INSTANCE);
        registerModule(NoPush.INSTANCE);
        registerModule(FastUse.INSTANCE);
        registerModule(LevitationControl.INSTANCE);
    }

    private void registerMisc() {
        registerModule(ServerHelper.INSTANCE);
        registerModule(ElytraHelper.INSTANCE);
        registerModule(ItemScroller.INSTANCE);
        registerModule(ClickAction.INSTANCE);
        registerModule(FreeCam.INSTANCE);
        registerModule(NoInteract.INSTANCE);
        registerModule(AutoAccept.INSTANCE);
        registerModule(AutoRespawn.INSTANCE);
        registerModule(NameProtect.INSTANCE);
        registerModule(ScoreboardHealth.INSTANCE);
        registerModule(CrystalOptimizer.INSTANCE);
        registerModule(FakePlayer.INSTANCE);
        registerModule(ChatHelper.INSTANCE);
    }

    private void registerModule(Module module) {
        modules.add(module);
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Set<Module> getActiveModules() {
        Set<Module> active = new HashSet<>();
        for (Module module : modules) {
            if (module.isEnabled()) {
                active.add(module);
            }
        }
        return active;
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (mc.currentScreen == null && event.getAction() == 1) {
            int keyCode = event.getKeyCode();
            long currentTime = System.currentTimeMillis();

            if (keyCode == lastKeyCode && (currentTime - lastKeyPressTime) < DEBOUNCE_THRESHOLD_MS) {
                return;
            }

            lastKeyCode = keyCode;
            lastKeyPressTime = currentTime;

            for (Module module : modules) {
                if (module.getKeyCode() == keyCode && module.getKeyCode() != -1) {
                    module.toggle();
                }

                for (Setting setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting booleanSetting) {
                        if (booleanSetting.getKeyCode() == keyCode && booleanSetting.getKeyCode() != -1) {
                            booleanSetting.toggle();
                        }
                    }
                }
            }

            for (Macro macro : Dexum.getInstance().getMacroManager().getItems()) {
                if (keyCode == macro.getBind()) {
                    mc.getNetworkHandler().sendChatMessage(macro.getText());
                }
            }
        }
    }

    @EventTarget
    public void onRender(EventHudRender e) {
        Dexum.getInstance().getThemeManager().getCurrentTheme().getAnimation().update(1.0F);

        for (Module module : modules) {
            module.getAnimation().update(module.isEnabled());

            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting) {
                    booleanSetting.getAnimation().update(booleanSetting.isEnabled());
                } else if (setting instanceof ModeSetting modeSetting) {
                    for (ModeSetting.Value value : modeSetting.getValues()) {
                        value.getAnimation().update(value.isSelected());
                    }
                } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                    for (MultiBooleanSetting.Value value : multiBooleanSetting.getBooleanSettings()) {
                        value.getAnimation().update(value.isEnabled());
                    }
                }
            }
        }

        MenuScreen menuScreen = Dexum.getInstance().getMenuScreen();
        if (menuScreen.needToClose) {
            if (menuScreen.savedRunnable != null) {
                menuScreen.savedRunnable.run();
            }

            if (menuScreen.openAnimationMetanoise.getValue() <= 0.27F) {
                menuScreen.savedRunnable = null;
                menuScreen.needToClose = false;
                menuScreen.openAnimationMetanoise.setValue(0.0F);
                menuScreen.openAnimationMetanoise.setStartValue(0.0F);
            }
        }
    }

    @EventTarget
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof CloseScreenS2CPacket && mc.currentScreen instanceof MenuScreen) {
            e.cancel();
        }
    }

    @EventTarget
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null) return;

        if (!Aura.INSTANCE.isEnabled() || Aura.INSTANCE.getTarget() == null) {
            float cameraYaw = mc.gameRenderer.getCamera().getYaw();
            float cameraPitch = mc.gameRenderer.getCamera().getPitch();

            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                Aura.INSTANCE.lastYaw = cameraYaw - 180.0F;
                Aura.INSTANCE.lastPitch = -cameraPitch;
            } else {
                Aura.INSTANCE.lastYaw = cameraYaw;
                Aura.INSTANCE.lastPitch = cameraPitch;
            }

            if (Aura.INSTANCE.rotationMode.is("Vanilla")) {
                return;
            }

            Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
            float deltaYaw = MathHelper.wrapDegrees(cameraYaw - current.getYaw());
            float deltaPitch = cameraPitch - current.getPitch();

            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                deltaYaw = MathHelper.wrapDegrees(cameraYaw - 180.0F - current.getYaw());
                deltaPitch = -cameraPitch - current.getPitch();
            }

            acceleration += 0.0024F;
            float smooth = MathHelper.clamp(acceleration, 0.0F, 1.0F);
            float newYaw = current.getYaw() + deltaYaw * smooth;
            float newPitch = current.getPitch() + deltaPitch * (smooth / 2.0F);

            Rotation smoothRot = new Rotation(newYaw, newPitch);
            RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, false);
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public boolean isBack() {
        return isBack;
    }

    public boolean isRotated() {
        return isRotated;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public void setBack(boolean isBack) {
        this.isBack = isBack;
    }

    public void setRotated(boolean isRotated) {
        this.isRotated = isRotated;
    }

    public void setAcceleration(float acceleration) {
        this.acceleration = acceleration;
    }
}