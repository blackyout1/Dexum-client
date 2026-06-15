package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import wtf.dexum.Dexum;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.math.MathUtil;

import java.util.List;

public class ClickGuiThemeSelector {
    private static final float BUTTON_WIDTH = 154f;
    private static final float BUTTON_HEIGHT = 14f;
    private static final float DROPDOWN_WIDTH = 170f;
    private static final float ROW_HEIGHT = 12f;
    private static final float TOP_OFFSET = 22f;
    private static final float DROPDOWN_GAP = 4f;

    public void render(DrawContext context, Window window, ClickGuiState state, int categoryCount, float alphaMul) {
        if (context == null || window == null) return;

        List<Theme> themes = Dexum.getInstance().getThemeManager().getThemes();
        if (themes == null || themes.isEmpty()) return;

        float centerX = state.getX() + ClickGuiLayout.getTotalCategoriesWidth(categoryCount) / 2f;
        float buttonX = centerX - BUTTON_WIDTH / 2f;
        float buttonY = state.getY() + state.getRenderOffsetY() - TOP_OFFSET;

        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        ColorRGBA bodyColor = new ColorRGBA(10, 10, 15, (int) (205 * alphaMul));
        Theme selected = Dexum.getInstance().getThemeManager().getCurrentTheme();

        DrawUtil.drawBlur(customDrawContext.getMatrices(), buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 10.0f, BorderRadius.all(4.0f), ColorRGBA.WHITE.withAlpha((int) (255 * alphaMul)));
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, BorderRadius.all(4.0f), bodyColor);

        String arrow = state.isThemeDropdownOpen() ? "^" : "v";
        customDrawContext.drawText(wtf.dexum.base.font.Fonts.REGULAR.getFont(7.0f), "Theme", buttonX + 7.0f, buttonY + 4.0f, ColorRGBA.WHITE.withAlpha((int) (230 * alphaMul)));
        customDrawContext.drawText(wtf.dexum.base.font.Fonts.REGULAR.getFont(7.0f), selected.getName(), buttonX + 42.0f, buttonY + 4.0f, ColorRGBA.WHITE.withAlpha((int) (205 * alphaMul)));
        customDrawContext.drawText(wtf.dexum.base.font.Fonts.REGULAR.getFont(7.0f), arrow, buttonX + BUTTON_WIDTH - 10.0f, buttonY + 4.0f, ColorRGBA.WHITE.withAlpha((int) (210 * alphaMul)));

        float dropdownProgress = state.getThemeDropdownProgress();
        if (dropdownProgress <= 0.01f) {
            return;
        }

        float dropdownHeight = themes.size() * ROW_HEIGHT + 6.0f;
        float animatedHeight = dropdownHeight * dropdownProgress;
        float dropdownX = centerX - DROPDOWN_WIDTH / 2f;
        float dropdownY = buttonY - animatedHeight - DROPDOWN_GAP;

        DrawUtil.drawBlur(customDrawContext.getMatrices(), dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, 14.0f, BorderRadius.all(5.0f), ColorRGBA.WHITE.withAlpha((int) (255 * alphaMul * dropdownProgress)));
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, BorderRadius.all(5.0f), new ColorRGBA(12, 12, 18, (int) (220 * alphaMul * dropdownProgress)));

        context.enableScissor((int) dropdownX, (int) dropdownY, (int) (dropdownX + DROPDOWN_WIDTH), (int) (dropdownY + animatedHeight));

        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            float rowY = dropdownY + 3.0f + i * ROW_HEIGHT + (1.0f - dropdownProgress) * 6.0f;
            boolean isSelected = theme == selected;
            int rowAlpha = (int) (alphaMul * dropdownProgress * 255.0f);

            if (isSelected) {
                DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), dropdownX + 2.0f, rowY, DROPDOWN_WIDTH - 4.0f, ROW_HEIGHT - 1.0f, BorderRadius.all(3.0f), new ColorRGBA(255, 255, 255, (int) (22 * alphaMul * dropdownProgress)));
            }

            DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), dropdownX + 6.0f, rowY + 2.0f, 7.0f, 7.0f, BorderRadius.all(2.0f), theme.getColor().withAlpha(rowAlpha));
            customDrawContext.drawText(wtf.dexum.base.font.Fonts.REGULAR.getFont(6.8f), theme.getName(), dropdownX + 17.0f, rowY + 3.0f, ColorRGBA.WHITE.withAlpha((int) (225 * alphaMul * dropdownProgress)));
        }

        context.disableScissor();
    }

    public boolean handleClick(Window window, double mouseX, double mouseY, int button, ClickGuiState state, int categoryCount) {
        if (window == null || button != 0) return false;

        List<Theme> themes = Dexum.getInstance().getThemeManager().getThemes();
        if (themes == null || themes.isEmpty()) return false;

        float centerX = state.getX() + ClickGuiLayout.getTotalCategoriesWidth(categoryCount) / 2f;
        float buttonX = centerX - BUTTON_WIDTH / 2f;
        float buttonY = state.getY() + state.getRenderOffsetY() - TOP_OFFSET;

        if (MathUtil.isHovered(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            state.setThemeDropdownOpen(!state.isThemeDropdownOpen());
            return true;
        }

        if (!state.isThemeDropdownOpen()) {
            return false;
        }

        float dropdownHeight = themes.size() * ROW_HEIGHT + 6.0f;
        float dropdownX = centerX - DROPDOWN_WIDTH / 2f;
        float dropdownY = buttonY - dropdownHeight - DROPDOWN_GAP;

        if (MathUtil.isHovered(mouseX, mouseY, dropdownX, dropdownY, DROPDOWN_WIDTH, dropdownHeight)) {
            for (int i = 0; i < themes.size(); i++) {
                float rowY = dropdownY + 3.0f + i * ROW_HEIGHT;
                if (MathUtil.isHovered(mouseX, mouseY, dropdownX + 2.0f, rowY, DROPDOWN_WIDTH - 4.0f, ROW_HEIGHT - 1.0f)) {
                    Dexum.getInstance().getThemeManager().setCurrentTheme(themes.get(i));
                    state.setThemeDropdownOpen(false);
                    return true;
                }
            }
            return true;
        }

        state.setThemeDropdownOpen(false);
        return false;
    }
}