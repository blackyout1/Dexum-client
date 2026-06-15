package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import wtf.dexum.base.events.impl.other.EventModuleToggle;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "ClientSounds",
        category = Category.RENDER,
        description = "Звуки при включении/выключении модулей"
)
public class ClientSounds extends Module {
    public static final ClientSounds INSTANCE = new ClientSounds();
    private static final float LOUDNESS_MULTIPLIER = 1.35F;

    private static final SoundEvent ENABLE_SOUND = SoundEvent.of(Identifier.of("dexum", "enable"));
    private static final SoundEvent DISABLE_SOUND = SoundEvent.of(Identifier.of("dexum", "disable"));

    private final NumberSetting volume = new NumberSetting("Громкость", 1.8F, 0.1F, 4.0F, 0.1F);
    private final NumberSetting pitch = new NumberSetting("Питч", 1.0F, 0.5F, 2.0F, 0.1F);

    @EventTarget
    private void onToggle(EventModuleToggle event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getModule() == this) return;

        SoundEvent sound = event.isEnabled() ? ENABLE_SOUND : DISABLE_SOUND;
        float loudVolume = Math.min(4.0F, volume.getCurrent() * LOUDNESS_MULTIPLIER);
        mc.getSoundManager().play(
                PositionedSoundInstance.master(sound, pitch.getCurrent(), loudVolume)
        );
    }
}