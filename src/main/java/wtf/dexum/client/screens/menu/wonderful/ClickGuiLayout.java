package wtf.dexum.client.screens.menu.wonderful;

import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.*;

import java.util.List;

public final class ClickGuiLayout {
    public static final float CATEGORY_HEADER_HEIGHT = 18.0f;
    public static final float WIDTH = 105f;
    public static final float HEIGHT = 260f;
    public static final float CATEGORY_PANEL_STEP = 115f;

    public static final float MODULE_PADDING = 2.5f;
    public static final float MODULE_GAP = 4.0f;
    public static final float MODULE_HEADER_HEIGHT = 19.0f;
    public static final float MODULE_INNER_WIDTH = 100.0f;
    public static final float SETTING_START_Y = 17.5f;
    public static final float SETTING_PADDING = 3.0f;
    public static final float SETTING_BOTTOM_PADDING = 6.0f;
    public static final float SETTING_LEFT = 7.0f;
    public static final float SETTING_RIGHT = 98.0f;
    public static final float SLIDER_WIDTH = 88f;

    public static final float CHIP_GAP_X = 3.0f;
    public static final float CHIP_GAP_Y = 3.0f;
    public static final float CHIP_PADDING_X = 4.0f;
    public static final float CHIP_PADDING_Y = 2.0f;
    public static final float CLICKABLE_WIDTH = 79f;

    public static final int SEARCH_MAX_CHARS = 24;
    public static final float SEARCH_WIDTH = 120f;
    public static final float SEARCH_HEIGHT = 22f;
    public static final float SEARCH_GAP = 8f;
    public static final float SEARCH_ICON_X = 3.5f;
    public static final float SEARCH_TEXT_X = 19f;
    public static final float SEARCH_RIGHT_PADDING = 8f;

    private ClickGuiLayout() {
    }

    public static float getTotalCategoriesWidth(int categoryCount) {
        return ((categoryCount - 1) * CATEGORY_PANEL_STEP) + WIDTH;
    }

    public static float getCategoryPanelX(float x, int index) {
        return x + (index * CATEGORY_PANEL_STEP);
    }

    public static float getContentY(float y) {
        return y + CATEGORY_HEADER_HEIGHT;
    }

    public static float getContentHeight() {
        return HEIGHT - CATEGORY_HEADER_HEIGHT - 2.0f;
    }

    public static float getSearchX(float x, int categoryCount, float searchWidth) {
        return x + (getTotalCategoriesWidth(categoryCount) / 2f) - (searchWidth / 2f);
    }

    public static float getSearchY(float y) {
        return y + HEIGHT + SEARCH_GAP;
    }

    public static boolean hasVisibleSettings(Module module) {
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty()) return false;
        for (Setting setting : settings) {
            if (setting != null && setting.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public static float calculateModeSettingHeight(ModeSetting modeSetting) {
        float x = SETTING_LEFT;
        float rowHeight = 11.0f;
        int rows = 1;

        for (ModeSetting.Value val : modeSetting.getValues()) {
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            float chipW = textW + (CHIP_PADDING_X * 2);

            if (x + chipW > SETTING_RIGHT) {
                x = SETTING_LEFT;
                rows++;
            }
            x += chipW + CHIP_GAP_X;
        }
        return 9.0f + (rows * rowHeight) + ((rows - 1) * CHIP_GAP_Y) + 2.0f;
    }

    public static float calculateMultiBooleanHeight(MultiBooleanSetting multiBooleanSetting) {
        float x = SETTING_LEFT;
        float rowHeight = 11.0f;
        int rows = 1;

        for (MultiBooleanSetting.Value val : multiBooleanSetting.getBooleanSettings()) {
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            float chipW = textW + (CHIP_PADDING_X * 2);

            if (x + chipW > SETTING_RIGHT) {
                x = SETTING_LEFT;
                rows++;
            }
            x += chipW + CHIP_GAP_X;
        }
        return 9.0f + (rows * rowHeight) + ((rows - 1) * CHIP_GAP_Y) + 2.0f;
    }

    public static float calculateSettingsHeight(Module module) {
        float height = 0f;
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty()) {
            return 0f;
        }

        boolean hasVisibleSetting = false;
        float globalGap = 2.5f;

        List<Setting> visibleSettings = settings.stream().filter(Setting::isVisible).toList();
        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting setting = visibleSettings.get(i);
            hasVisibleSetting = true;

            float currentHeight = 0;
            if (setting instanceof BooleanSetting || setting instanceof KeySetting) {
                currentHeight = 12f;
            } else if (setting instanceof NumberSetting || setting instanceof StringSetting) {
                currentHeight = 22f;
            } else if (setting instanceof ModeSetting modeSetting) {
                currentHeight = calculateModeSettingHeight(modeSetting);
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                currentHeight = calculateMultiBooleanHeight(multiBooleanSetting);
            }

            height += currentHeight;

            if (i < visibleSettings.size() - 1) {
                height += globalGap;
            }
        }

        if (hasVisibleSetting) {
            height += SETTING_BOTTOM_PADDING;
        }
        return height;
    }

    public static float getModuleHeight(Module module, float openProgress) {
        return MODULE_HEADER_HEIGHT + (calculateSettingsHeight(module) * openProgress);
    }
}