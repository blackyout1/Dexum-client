package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import wtf.dexum.Dexum;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.*;
import wtf.dexum.utility.math.MathUtil;

import java.util.List;

public class ClickGuiInputHandler {
    private final ClickGuiState state;
    private NumberSetting draggingSlider;
    private float draggingSliderX;

    public ClickGuiInputHandler(ClickGuiState state) {
        this.state = state;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Window window) {
        if (window == null) return false;
        Category[] categories = Category.values();

        if (state.getBindingModule() != null && button >= 2) {
            state.getBindingModule().setKeyCode(button);
            state.setBindingModule(null);
            return true;
        }

        if (state.getBindingSetting() != null && button >= 2) {
            if (state.getBindingSetting() instanceof KeySetting key) {
                key.setKeyCode(button);
            } else if (state.getBindingSetting() instanceof BooleanSetting bool) {
                bool.setKeyCode(button);
            }
            state.setBindingSetting(null);
            return true;
        }

        float searchX = ClickGuiLayout.getSearchX(state.getX(), categories.length, ClickGuiLayout.SEARCH_WIDTH);
        float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, searchX, searchY, ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT)) {
            state.setSearchActive(true);
            state.setThemeDropdownOpen(false);
            state.setSearchCursor(state.getSearchText().length());
            return true;
        }

        if (button == 0 && state.isSearchActive()) {
            state.setSearchActive(false);
        }

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];

            float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);
            float panelY = state.getY() + state.getRenderOffsetY();
            float headerHeight = 18.0F;
            float contentY = panelY + headerHeight;
            float contentHeight = ClickGuiLayout.HEIGHT - headerHeight - 2.0f;

            if (!MathUtil.isHovered(mouseX, mouseY, panelX, contentY, ClickGuiLayout.WIDTH, contentHeight)) {
                continue;
            }

            if (category == Category.THEMES) {
                if (button == 0) {
                    float themeY = contentY + 2.0f + state.getScroll(category);
                    for (wtf.dexum.base.theme.Theme themeItem : Dexum.getInstance().getThemeManager().getThemes()) {
                        if (MathUtil.isHovered(mouseX, mouseY, panelX + 4, themeY, ClickGuiLayout.WIDTH - 8, 18)) {
                            Dexum.getInstance().getThemeManager().setCurrentTheme(themeItem);
                            return true;
                        }
                        themeY += 20.0f;
                    }
                }
                continue;
            }

            float moduleY = contentY + 2.0f + state.getScroll(category);
            for (Module module : state.getModules(category)) {
                float openProgress = state.getOpenProgress(module);
                float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);

                if (MathUtil.isHovered(mouseX, mouseY, panelX + ClickGuiLayout.MODULE_PADDING, moduleY, ClickGuiLayout.MODULE_INNER_WIDTH, ClickGuiLayout.MODULE_HEADER_HEIGHT)) {
                    if (button == 0) {
                        module.toggle();
                        return true;
                    }
                    if (button == 1) {
                        if (ClickGuiLayout.hasVisibleSettings(module)) {
                            state.toggleModuleOpen(module);
                            state.clampScroll(category, contentHeight);
                        }
                        return true;
                    }
                    if (button == 2) {
                        state.setBindingModule(module);
                        return true;
                    }
                    return true;
                }

            if (state.isModuleOpen(module) && openProgress > 0.1f) {
                if (handleSettingClick(mouseX, mouseY, button, panelX + ClickGuiLayout.MODULE_PADDING, moduleY, module.getSettings())) {
                    return true;
                }
            }

                moduleY += ClickGuiLayout.MODULE_GAP + moduleHeight;
            }
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (state.getEditingStringSetting() != null) {
            if (Character.isISOControl(chr)) {
                return false;
            }

            StringSetting setting = state.getEditingStringSetting();
            String text = setting.getValue();
            if (text.length() >= setting.getMaxLength()) {
                return true;
            }

            int cursor = Math.max(0, Math.min(state.getStringCursor(), text.length()));
            setting.setValue(text.substring(0, cursor) + chr + text.substring(cursor));
            state.setStringCursor(cursor + 1);
            return true;
        }

        if (!state.isSearchActive()) {
            return false;
        }

        if (Character.isISOControl(chr)) {
            return false;
        }

        String text = state.getSearchText();
        if (text.length() >= ClickGuiLayout.SEARCH_MAX_CHARS) {
            return true;
        }

        int cursor = Math.max(0, Math.min(state.getSearchCursor(), text.length()));
        state.setSearchText(text.substring(0, cursor) + chr + text.substring(cursor));
        state.setSearchCursor(cursor + 1);
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button == 0) {
            if (draggingSlider != null) {
                state.setDraggingSlider(null);
                draggingSlider = null;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button != 0 || draggingSlider == null) {
            return false;
        }
        draggingSlider.setCurrent(state.getSliderValue(draggingSlider, draggingSliderX, mouseX));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];

            float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);
            float panelY = state.getY() + state.getRenderOffsetY();
            float contentY = ClickGuiLayout.getContentY(panelY);
            float contentHeight = ClickGuiLayout.getContentHeight();

            if (MathUtil.isHovered(mouseX, mouseY, panelX, contentY, ClickGuiLayout.WIDTH, contentHeight)) {
                state.addScroll(category, verticalAmount, contentHeight);
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (state.getEditingStringSetting() != null) {
            StringSetting setting = state.getEditingStringSetting();
            String text = setting.getValue();
            int cursor = Math.max(0, Math.min(state.getStringCursor(), text.length()));
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                state.setEditingStringSetting(null);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (cursor > 0) {
                    setting.setValue(text.substring(0, cursor - 1) + text.substring(cursor));
                    state.setStringCursor(cursor - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursor < text.length()) {
                    setting.setValue(text.substring(0, cursor) + text.substring(cursor + 1));
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                state.setStringCursor(Math.max(0, cursor - 1));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                state.setStringCursor(Math.min(text.length(), cursor + 1));
                return true;
            }
            return true;
        }

        if (state.isSearchActive()) {
            String text = state.getSearchText();
            int cursor = Math.max(0, Math.min(state.getSearchCursor(), text.length()));
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                state.setSearchActive(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (cursor > 0) {
                    state.setSearchText(text.substring(0, cursor - 1) + text.substring(cursor));
                    state.setSearchCursor(cursor - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursor < text.length()) {
                    state.setSearchText(text.substring(0, cursor) + text.substring(cursor + 1));
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                state.setSearchCursor(Math.max(0, cursor - 1));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                state.setSearchCursor(Math.min(text.length(), cursor + 1));
                return true;
            }
            return true;
        }

        if (state.getBindingModule() != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                state.setBindingModule(null);
            } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                state.getBindingModule().setKeyCode(-1);
                state.setBindingModule(null);
            } else {
                state.getBindingModule().setKeyCode(keyCode);
                state.setBindingModule(null);
            }
            return true;
        }

        if (state.getBindingSetting() != null) {
            if (state.getBindingSetting() instanceof KeySetting key) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    state.setBindingSetting(null);
                } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    key.setKeyCode(-1);
                    state.setBindingSetting(null);
                } else {
                    key.setKeyCode(keyCode);
                    state.setBindingSetting(null);
                }
            } else if (state.getBindingSetting() instanceof BooleanSetting bool) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    state.setBindingSetting(null);
                } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    bool.setKeyCode(-1);
                    state.setBindingSetting(null);
                } else {
                    bool.setKeyCode(keyCode);
                    state.setBindingSetting(null);
                }
            }
            return true;
        }

        return false;
    }

    private boolean handleSettingClick(double mouseX, double mouseY, int button, float moduleX, float moduleY, List<Setting> settings) {
        float settingYoffset = ClickGuiLayout.SETTING_START_Y;
        float globalGap = 2.5f;

        List<Setting> visibleSettings = settings.stream().filter(Setting::isVisible).toList();

        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting setting = visibleSettings.get(i);
            float settingY = moduleY + settingYoffset + ClickGuiLayout.SETTING_PADDING;

            float currentAddedHeight = 0;
            if (setting instanceof BooleanSetting booleanSetting) {

                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, moduleX + ClickGuiLayout.SETTING_LEFT, settingY - 2, ClickGuiLayout.SETTING_RIGHT - ClickGuiLayout.SETTING_LEFT, 12)) {
                    booleanSetting.toggle();
                    return true;
                }
                if (button == 2 && MathUtil.isHovered(mouseX, mouseY, moduleX + ClickGuiLayout.SETTING_LEFT, settingY - 2, ClickGuiLayout.SETTING_RIGHT - ClickGuiLayout.SETTING_LEFT, 12)) {
                    state.setBindingSetting(booleanSetting);
                    return true;
                }
                currentAddedHeight = 12f;
            } else if (setting instanceof NumberSetting floatSetting) {

                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, moduleX + ClickGuiLayout.SETTING_LEFT, settingY + 9, ClickGuiLayout.SLIDER_WIDTH, 6)) {
                    floatSetting.setCurrent(state.getSliderValue(floatSetting, moduleX + ClickGuiLayout.SETTING_LEFT, mouseX));
                    draggingSlider = floatSetting;
                    state.setDraggingSlider(floatSetting);
                    draggingSliderX = moduleX + ClickGuiLayout.SETTING_LEFT;
                    return true;
                }
                currentAddedHeight = 22f;
            } else if (setting instanceof ModeSetting modeSetting) {
                float x = moduleX + ClickGuiLayout.SETTING_LEFT;
                float y = settingY + 12.0f;
                float rowHeight = 11.0f;

                for (ModeSetting.Value val : modeSetting.getValues()) {
                    float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
                    float chipW = textW + (ClickGuiLayout.CHIP_PADDING_X * 2);

                    if (x + chipW > moduleX + ClickGuiLayout.SETTING_RIGHT) {
                        x = moduleX + ClickGuiLayout.SETTING_LEFT;
                        y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
                    }

                    if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, y, chipW, rowHeight)) {
                        modeSetting.setValue(val);
                        return true;
                    }

                    x += chipW + ClickGuiLayout.CHIP_GAP_X;
                }
                currentAddedHeight = ClickGuiLayout.calculateModeSettingHeight(modeSetting);
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                float x = moduleX + ClickGuiLayout.SETTING_LEFT;
                float y = settingY + 12.0f;
                float rowHeight = 11.0f;

                for (MultiBooleanSetting.Value val : multiBooleanSetting.getBooleanSettings()) {
                    float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
                    float chipW = textW + (ClickGuiLayout.CHIP_PADDING_X * 2);

                    if (x + chipW > moduleX + ClickGuiLayout.SETTING_RIGHT) {
                        x = moduleX + ClickGuiLayout.SETTING_LEFT;
                        y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
                    }

                    if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, y, chipW, rowHeight)) {
                        val.setEnabled(!val.isEnabled());
                        return true;
                    }

                    x += chipW + ClickGuiLayout.CHIP_GAP_X;
                }
                currentAddedHeight = ClickGuiLayout.calculateMultiBooleanHeight(multiBooleanSetting);
            } else if (setting instanceof KeySetting bindSetting) {
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, moduleX + ClickGuiLayout.SETTING_LEFT, settingY - 2, ClickGuiLayout.WIDTH - ClickGuiLayout.SETTING_LEFT, 12)) {
                    state.setBindingSetting(bindSetting);
                    return true;
                }
                currentAddedHeight = 12f;
            } else if (setting instanceof StringSetting stringSetting) {
                if (button == 0 && MathUtil.isHovered(mouseX, mouseY, moduleX + ClickGuiLayout.SETTING_LEFT, settingY + 9, ClickGuiLayout.SLIDER_WIDTH, 10)) {
                    state.setEditingStringSetting(stringSetting);
                    return true;
                }
                currentAddedHeight = 22f;
            }

            settingYoffset += currentAddedHeight;
            if (i < visibleSettings.size() - 1) {
                settingYoffset += globalGap;
            }
        }
        return false;
    }
}