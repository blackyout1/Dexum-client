package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.display.Keyboard;
import wtf.dexum.utility.render.display.StencilUtil;
import wtf.dexum.utility.math.MathUtil;
import wtf.dexum.client.modules.impl.render.Menu;

/**
 * ClickGuiRenderer - Класс для отрисовки всего ClickGUI
 * 
 * Отвечает за:
 * - Рендеринг панелей категорий
 * - Рендеринг модулей и их настроек
 * - Рендеринг поля поиска
 * - Рендеринг тумблеров и индикаторов
 */
public class ClickGuiRenderer {
    /** Состояние GUI (позиции, анимации, данные) */
    private final ClickGuiState state;
    
    /** Отдельный рендерер для настроек модулей */
    private final ClickGuiSettingRenderer settingRenderer;
    
    /** Обработчик ввода (для доступа к попапам типа color picker) */
    private ClickGuiInputHandler inputHandler;

    /**
     * Конструктор рендерера
     * @param state состояние GUI
     * @param settingRenderer рендерер настроек
     */
    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer) {
        this.state = state;
        this.settingRenderer = settingRenderer;
    }

    /**
     * Устанавливает обработчик ввода
     * Должен вызываться после создания inputHandler
     * @param handler обработчик ввода
     */
    public void setInputHandler(ClickGuiInputHandler handler) {
        this.inputHandler = handler;
    }

