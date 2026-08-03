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
public class ClickGuiRenderer {
    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer;
    private ClickGuiInputHandler inputHandler;

    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer) {
        this.state = state;
        this.settingRenderer = settingRenderer;
    }

    /** Must be called after inputHandler is created so we can access the popup */
    public void setInputHandler(ClickGuiInputHandler handler) {
        this.inputHandler = handler;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
        if (window == null) return;

        float alphaMul = animationProgress;
        int colorTheme = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB();
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);
            renderCategoryPanel(context, mouseX, mouseY, panelX, category, colorTheme, alphaMul);
        }

        renderSearchField(context, alphaMul, categories.length);

        // Color picker popup rendered last — on top of everything
        if (inputHandler != null) {
            inputHandler.getColorPickerPopup().render(context, mouseX, mouseY);
        }
    }

    private String getShortKeyName(int key) {
        String name = Keyboard.getKeyName(key);
        if (name == null || name.isEmpty()) return "";

        return switch (name.toUpperCase()) {
            case "LEFT_SHIFT", "LSHIFT" -> "L_SHIFT";
            case "RIGHT_SHIFT", "RSHIFT" -> "R_SHIFT";
            case "LEFT_CONTROL", "LCONTROL", "LEFT_CTRL" -> "L_CTRL";
            case "RIGHT_CONTROL", "RCONTROL", "RIGHT_CTRL" -> "R_CTRL";
            case "LEFT_ALT", "LALT" -> "L_ALT";
            case "RIGHT_ALT", "RALT" -> "R_ALT";
            case "LEFT_SUPER", "LEFT_WIN", "LWIN", "LEFT_META" -> "L_WIN";
            case "RIGHT_SUPER", "RIGHT_WIN", "RWIN", "RIGHT_META" -> "R_WIN";
            case "BACKSPACE" -> "BKSP";
            case "CAPS_LOCK", "CAPITAL" -> "CAPS";
            case "PAGE_UP" -> "PGUP";
            case "PAGE_DOWN" -> "PGDN";
            case "INSERT" -> "INS";
            case "DELETE" -> "DEL";
            case "PRINT_SCREEN" -> "PRTSC";
            case "SCROLL_LOCK" -> "SCRL";
            case "NUM_LOCK" -> "NUM";
            case "MOUSE4", "BUTTON4" -> "M4";
            case "MOUSE5", "BUTTON5" -> "M5";
            case "MOUSE3", "BUTTON3", "MIDDLE" -> "M3";
            case "NUMPAD0", "KP_0" -> "NUM0";
            case "NUMPAD1", "KP_1" -> "NUM1";
            case "NUMPAD2", "KP_2" -> "NUM2";
            case "NUMPAD3", "KP_3" -> "NUM3";
            case "NUMPAD4", "KP_4" -> "NUM4";
            case "NUMPAD5", "KP_5" -> "NUM5";
            case "NUMPAD6", "KP_6" -> "NUM6";
            case "NUMPAD7", "KP_7" -> "NUM7";
            case "NUMPAD8", "KP_8" -> "NUM8";
            case "NUMPAD9", "KP_9" -> "NUM9";
            default -> name.length() > 8 ? name.substring(0, 7) + "…" : name;
        };
    }

    private void renderSearchField(DrawContext context, float alphaMul, int categoryCount) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);

        float searchX = ClickGuiLayout.getSearchX(state.getX(), categoryCount, ClickGuiLayout.SEARCH_WIDTH);
        float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());

        DrawUtil.drawBlur(customDrawContext.getMatrices(), searchX, searchY, ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, 15.0f, BorderRadius.all(3.0f), ColorRGBA.WHITE.withAlpha((int) (255 * alphaMul)));
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), searchX, searchY, ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, BorderRadius.all(3.0f), new ColorRGBA(0, 0, 0, (int) (125 * alphaMul)));

        String text = state.getSearchText();
        boolean hasText = text != null && !text.isEmpty();
        boolean isActive = state.isSearchActive();
        String shown = (hasText || isActive) ? (text != null ? text : "") : "\u041f\u043e\u0438\u0441\u043a...";
        ColorRGBA textColor = ColorRGBA.WHITE.withAlpha((int) ((hasText ? 230 : 200) * alphaMul));

        float textX = searchX + 7.0f;
        float textY = searchY + 8.0f;
        if (!shown.isEmpty()) {
            customDrawContext.drawText(Fonts.REGULAR.getFont(8.0f), shown, textX, textY, textColor);
        }

        float shownW = Fonts.REGULAR.getWidth(shown, 8.0f);
        customDrawContext.drawText(Fonts.LUPA.getFont(8.0f), "A", textX + shownW + 4.0f, textY, ColorRGBA.WHITE.withAlpha((int) (180 * alphaMul)));

        if (state.isSearchActive()) {
            boolean cursorVisible = (System.currentTimeMillis() / 500) % 2 == 0;
            if (cursorVisible) {
                float cursorTextW = hasText ? Fonts.REGULAR.getWidth(text, 8.0f) : 0;
                float cursorX = textX + cursorTextW + 1.0f;
                DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), cursorX, searchY + 6.5f, 1.0f, 9.0f, BorderRadius.all(0.5f), ColorRGBA.WHITE.withAlpha((int) (220 * alphaMul)));
            }
        }
    }

    private void renderCategoryPanel(DrawContext context, int mouseX, int mouseY, float panelX, Category category, int colorTheme, float alphaMul) {
        float panelY = state.getY() + state.getRenderOffsetY();
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA themeColor = theme.getColor();

        float rounding = 3.0F;
        float headerHeight = ClickGuiLayout.CATEGORY_HEADER_HEIGHT;

        ColorRGBA guiBg = Menu.INSTANCE.guiColor.getColor();
        ColorRGBA headerColor = new ColorRGBA(
                (int) guiBg.getRed(),
                (int) guiBg.getGreen(),
                (int) guiBg.getBlue(),
                (int) (200 * alphaMul)
        );
        ColorRGBA bodyColor = new ColorRGBA(
                (int) guiBg.getRed(),
                (int) guiBg.getGreen(),
                (int) guiBg.getBlue(),
                (int) (160 * alphaMul)
        );

        DrawUtil.drawBlur(customDrawContext.getMatrices(), panelX, panelY, ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, 15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha((int)(255 * alphaMul)));

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, panelY, ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, BorderRadius.all(rounding), bodyColor);

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, panelY, ClickGuiLayout.WIDTH, headerHeight, new BorderRadius(rounding, rounding, 0, 0), headerColor);

        String icon = category.getIcon();
        float iconW = Fonts.FONT.getWidth(icon, 9.0f);
        float nameW = Fonts.REGULAR.getWidth(category.getName(), 10.0f);
        float gap = 4.0f;
        float totalHeaderW = iconW + gap + nameW;

        float headerStartX = panelX + (ClickGuiLayout.WIDTH / 2.0f) - (totalHeaderW / 2.0f);

        float iconOffsetX = 0.0f;
        if (category == Category.PLAYER || category == Category.MISC || category == Category.THEMES) {
            iconOffsetX = 2.0f;
        }

        customDrawContext.drawText(Fonts.FONT.getFont(9.0f), icon, headerStartX + iconOffsetX, panelY + 6.0f, themeColor.withAlpha((int)(255 * alphaMul)));
        customDrawContext.drawText(Fonts.REGULAR.getFont(10.0f), category.getName(), headerStartX + iconW + gap, panelY + 5.0F, themeColor.withAlpha((int)(255 * alphaMul)));

        float contentY = panelY + headerHeight;
        float contentHeight = ClickGuiLayout.getContentHeight();
        state.clampScroll(category, contentHeight - 5);
        float moduleY = contentY + 2.0F + state.getScroll(category);

        StencilUtil.push();
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, contentY, ClickGuiLayout.WIDTH, contentHeight, new BorderRadius(0, 0, rounding, rounding), ColorRGBA.WHITE);
        StencilUtil.read(1);

        if (category == Category.THEMES) {
            float themeY = contentY + 2.0F + state.getScroll(category);
            for (Theme themeItem : Dexum.getInstance().getThemeManager().getThemes()) {
                if (themeY + 18.0f >= contentY && themeY <= contentY + contentHeight && alphaMul > 0.01F) {
                    renderThemeItem(context, mouseX, mouseY, panelX, themeY, themeItem, alphaMul);
                }
                themeY += 20.0f;
            }
        } else {
            for (Module module : state.getModules(category)) {
                float openProgress = state.getOpenProgress(module);
                float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);

                if (moduleY + moduleHeight >= contentY && moduleY <= contentY + contentHeight && alphaMul > 0.01F) {
                    renderModule(context, mouseX, mouseY, panelX, moduleY, module, openProgress, moduleHeight, colorTheme, alphaMul);
                }

                moduleY += ClickGuiLayout.MODULE_GAP + moduleHeight;
            }
        }

        StencilUtil.pop();
    }

    private void renderThemeItem(DrawContext context, int mouseX, int mouseY, float panelX, float themeY, Theme themeItem, float alphaMul) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        Theme currentTheme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        boolean isSelected = themeItem.getName().equalsIgnoreCase(currentTheme.getName());
        boolean isHovered = MathUtil.isHovered(mouseX, mouseY, panelX + 4, themeY, ClickGuiLayout.WIDTH - 8, 18);

        ColorRGBA bg = isSelected ? themeItem.getColor().withAlpha((int) (80 * alphaMul)) : (isHovered ? new ColorRGBA(255, 255, 255, (int) (20 * alphaMul)) : new ColorRGBA(255, 255, 255, (int) (10 * alphaMul)));

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX + 4, themeY, ClickGuiLayout.WIDTH - 8, 18, BorderRadius.all(2.0f), bg);

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX + 8, themeY + 5, 8, 8, BorderRadius.all(2.0f), themeItem.getColor().withAlpha((int) (255 * alphaMul)));

        ColorRGBA textColor = isSelected ? ColorRGBA.WHITE : new ColorRGBA(200, 200, 210, (int) (255 * alphaMul));
        customDrawContext.drawText(Fonts.REGULAR.getFont(7.5f), themeItem.getName(), panelX + 22, themeY + 6.5f, textColor.withAlpha((int) (255 * alphaMul)));
    }

    private void renderModule(DrawContext context, int mouseX, int mouseY, float panelX, float moduleY, Module module, float openProgress, float moduleHeight, int colorTheme, float alphaMul) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA themeColor = theme.getColor();

        // Слабая подсветка как в Delta
        ColorRGBA moduleBg = module.isEnabled()
                ? new ColorRGBA(255, 255, 255, (int)(12 * alphaMul))
                : new ColorRGBA(255, 255, 255, (int)(6 * alphaMul));

        DrawUtil.drawRoundedRect(
                customDrawContext.getMatrices(),
                panelX + ClickGuiLayout.MODULE_PADDING,
                moduleY,
                ClickGuiLayout.MODULE_INNER_WIDTH,
                moduleHeight,
                BorderRadius.all(2.0f),
                moduleBg
        );

        // Анимация тумблера
        Animation toggleAnim = state.getModuleDotAnimation(module);
        toggleAnim.update(module.isEnabled());
        float toggleProgress = toggleAnim.getValue();

        // === Тумблер справа ===
        float toggleWidth = 15f;
        float toggleHeight = 8.5f;
        float toggleX = panelX + ClickGuiLayout.WIDTH - 19f;
        float toggleY = moduleY + 5.5f;

        ColorRGBA toggleBg = module.isEnabled()
                ? themeColor.withAlpha((int)(200 * alphaMul))
                : new ColorRGBA(55, 55, 65, (int)(180 * alphaMul));

        DrawUtil.drawRoundedRect(
                customDrawContext.getMatrices(),
                toggleX, toggleY,
                toggleWidth, toggleHeight,
                BorderRadius.all(toggleHeight / 2f),
                toggleBg
        );

        float circleSize = 6.5f;
        float circleX = toggleX + 1.3f + (toggleWidth - circleSize - 2.6f) * toggleProgress;
        float circleY = toggleY + 1.0f;

        DrawUtil.drawRoundedRect(
                customDrawContext.getMatrices(),
                circleX, circleY,
                circleSize, circleSize,
                BorderRadius.all(circleSize / 2f),
                ColorRGBA.WHITE.withAlpha((int)(255 * alphaMul))
        );

        // Название модуля
        String moduleName = module.getName();
        ColorRGBA nameColor = module.isEnabled()
                ? ColorRGBA.WHITE
                : new ColorRGBA(200, 200, 210, (int)(255 * alphaMul));

        customDrawContext.drawText(
                Fonts.REGULAR.getFont(7.5f),
                moduleName,
                panelX + ClickGuiLayout.SETTING_LEFT + 2.0f,
                moduleY + 7.5f,
                nameColor.withAlpha((int)(255 * alphaMul))
        );

        // Бинд — слева от тумблера
        String keyText = "";
        if (state.getBindingModule() == module) {
            keyText = "[...]";
        } else {
            int key = module.getKeyCode();
            if (key != -1) {
                keyText = "[" + getShortKeyName(key) + "]";
            }
        }

        if (!keyText.isEmpty()) {
            float keyWidth = Fonts.REGULAR.getWidth(keyText, 7.0f);
            float keyTextX = toggleX - 5f - keyWidth;

            ColorRGBA keyColor = new ColorRGBA(160, 160, 170, (int)(220 * alphaMul));
            customDrawContext.drawText(
                    Fonts.REGULAR.getFont(7.0f),
                    keyText,
                    keyTextX,
                    moduleY + 7.5f,
                    keyColor
            );
        }

        // Три точки — ещё левее
        if (ClickGuiLayout.hasVisibleSettings(module)) {
            float dotsX;

            if (!keyText.isEmpty()) {
                float keyWidth = Fonts.REGULAR.getWidth(keyText, 7.0f);
                dotsX = toggleX - 5f - keyWidth - 12f;
            } else {
                dotsX = toggleX - 14f;
            }

            ColorRGBA dotsColor = module.isEnabled()
                    ? ColorRGBA.WHITE.withAlpha((int)(200 * alphaMul))
                    : ColorRGBA.WHITE.withAlpha((int)(140 * alphaMul));

            customDrawContext.drawText(
                    Fonts.REGULAR.getFont(8.5f),
                    "•••",
                    dotsX,
                    moduleY + 6.8f,
                    dotsColor
            );
        }

        settingRenderer.render(context, module, panelX, moduleY, openProgress, colorTheme, mouseX, mouseY, state, alphaMul);
    }
}