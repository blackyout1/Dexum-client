package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventFog;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.awt.Color;

@ModuleAnnotation(
        name = "Ambience",
        category = Category.RENDER,
        description = "Управляет атмосферой мира: временем и туманом"
)
public class Ambience extends Module {
    public static final Ambience INSTANCE = new Ambience();

    private final BooleanSetting customTime = new BooleanSetting("Свое время", true);
    public final NumberSetting timeSetting = new NumberSetting("Время", 18.0F, 0.0F, 24.0F, 0.1F, customTime::isEnabled);

    private final BooleanSetting customFog = new BooleanSetting("Свой туман", true);
    public final NumberSetting distanceSetting = new NumberSetting("Дальность", 120.0F, 10.0F, 500.0F, 5.0F, customFog::isEnabled);
    public final NumberSetting startMultiplier = new NumberSetting("Начало", 0.2F, 0.0F, 0.9F, 0.05F, customFog::isEnabled);
    public final NumberSetting colorIntensity = new NumberSetting("Насыщенность", 0.5F, 0.0F, 1.0F, 0.05F, customFog::isEnabled);

    private Ambience() {
    }

    @EventTarget
    public void onFog(EventFog e) {
        if (!customFog.isEnabled()) return;

        float endDistance = this.distanceSetting.getCurrent();
        e.setDistance(endDistance);

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        float[] hsb = Color.RGBtoHSB(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), null);

        float saturation = hsb[1] * this.colorIntensity.getCurrent();
        float brightness = Math.min(1.0F, hsb[2] + 0.15F);

        Color fogColor = Color.getHSBColor(hsb[0], saturation, brightness);
        e.setColor(fogColor.getRGB());
        e.setCancelled(true);
    }

    public boolean isTimeEnabled() {
        return isEnabled() && customTime.isEnabled();
    }
}