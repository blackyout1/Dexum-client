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

public class ClickGuiRenderer {
    private final ClickGuiState state;
    private final ClickGuiSettingRenderer settingRenderer;

    public ClickGuiRenderer(ClickGuiState state, ClickGuiSettingRenderer settingRenderer) {
        this.state = state;
        this.settingRenderer = settingRenderer;
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
    }

    private void renderSearchField(DrawContext context, float alphaMul, int categoryCount) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);

        float searchX = ClickGuiLayout.getSearchX(state.getX(), categoryCount, ClickGuiLayout.SEARCH_WIDTH);
        float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());

        DrawUtil.drawBlur(customDrawContext.getMatrices(), searchX, searchY, ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, 15.0f, BorderRadius.all(4.0f), ColorRGBA.WHITE.withAlpha((int) (255 * alphaMul)));
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), searchX, searchY, ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, BorderRadius.all(4.0f), new ColorRGBA(0, 0, 0, (int) (125 * alphaMul)));

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

        float rounding = 4.0F;
        float headerHeight = ClickGuiLayout.CATEGORY_HEADER_HEIGHT;

        ColorRGBA headerColor = new ColorRGBA(0, 0, 0, (int)(255 * alphaMul));
        ColorRGBA bodyColor = new ColorRGBA(0, 0, 0, (int)(125 * alphaMul));

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

        ColorRGBA moduleBg = module.isEnabled() ? themeColor.withAlpha((int)(80 * alphaMul)) : new ColorRGBA(255, 255, 255, (int)(10 * alphaMul));

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX + ClickGuiLayout.MODULE_PADDING, moduleY, ClickGuiLayout.MODULE_INNER_WIDTH, moduleHeight, BorderRadius.all(2.0f), moduleBg);

        Animation dotAnimation = state.getModuleDotAnimation(module);
        dotAnimation.update(module.isEnabled());
        float dotProgress = dotAnimation.getValue();

        float textOffsetX = 0f;
        if (dotProgress > 0.01f) {
            float dotSize = 3.0f;
            float dotX = panelX + ClickGuiLayout.SETTING_LEFT + 1.5f;
            float dotY = moduleY + 9.0f;
            DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), dotX, dotY, dotSize, dotSize, BorderRadius.all(dotSize / 2.0f), themeColor.withAlpha((int)(255 * alphaMul * dotProgress)));
            textOffsetX = (dotSize + 3.5f) * dotProgress;
        }

        String moduleName = module.getName();
        ColorRGBA nameColor = module.isEnabled() ? ColorRGBA.WHITE : new ColorRGBA(200, 200, 210, (int)(255 * alphaMul));
        customDrawContext.drawText(Fonts.REGULAR.getFont(7.5f), moduleName, panelX + ClickGuiLayout.SETTING_LEFT + textOffsetX + 2.0f, moduleY + 7.5f, nameColor.withAlpha((int)(255 * alphaMul)));

        String keyText = "";
        if (state.getBindingModule() == module) {
            keyText = "[...]";
        } else {
            int key = module.getKeyCode();
            if (key != -1) {
                keyText = "[" + Keyboard.getKeyName(key) + "]";
            }
        }

        if (!keyText.isEmpty()) {

            float keyTextX = panelX + ClickGuiLayout.WIDTH - 10.0f - Fonts.REGULAR.getWidth(keyText, 7.5f);
            ColorRGBA keyColor = new ColorRGBA(180, 180, 190, (int)(255 * alphaMul));
            customDrawContext.drawText(Fonts.REGULAR.getFont(7.5f), keyText, keyTextX, moduleY + 7.0f, keyColor);
        }

        if (ClickGuiLayout.hasVisibleSettings(module)) {
            renderModuleDots(context, panelX, moduleY, module, module.isEnabled(), alphaMul);
        }

        settingRenderer.render(context, module, panelX, moduleY, openProgress, colorTheme, mouseX, mouseY, state, alphaMul);
    }

    private void renderModuleDots(DrawContext context, float panelX, float moduleY, Module module, boolean enabled, float alphaMul) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        float dotsX = panelX + ClickGuiLayout.WIDTH - 12f;
        ColorRGBA dotsColor = enabled ? ColorRGBA.WHITE.withAlpha((int)(220 * alphaMul)) : ColorRGBA.WHITE.withAlpha((int)(150 * alphaMul));
        customDrawContext.drawText(Fonts.REGULAR.getFont(9.0f), "•••", dotsX, moduleY + 6.5f, dotsColor);
    }

}