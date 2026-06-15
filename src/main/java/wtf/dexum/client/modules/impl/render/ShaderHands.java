package wtf.dexum.client.modules.impl.render;

import lombok.Getter;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.shader.ShaderHandsRenderer;

@Getter
@ModuleAnnotation(
        name = "ShaderHands",
        category = Category.RENDER,
        description = "Красивый шейдер на руки и предметы"
)
public final class ShaderHands extends Module {

    public static final ShaderHands INSTANCE = new ShaderHands();

    private static final ShaderHandsRenderer RENDERER = ShaderHandsRenderer.getInstance();

    public final NumberSetting waveSpeed = new NumberSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f);
    public final NumberSetting waveScale = new NumberSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f);

    public final NumberSetting outline = new NumberSetting("Ширина обводки", 1.2f, 0.1f, 5.0f, 0.1f);
    public final NumberSetting glow = new NumberSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f);
    public final NumberSetting fill = new NumberSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f);
    public final NumberSetting alpha = new NumberSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.05f);

    private ShaderHands() {
    }

    @Override
    public void onDisable() {
        RENDERER.invalidateState();
        super.onDisable();
    }
}