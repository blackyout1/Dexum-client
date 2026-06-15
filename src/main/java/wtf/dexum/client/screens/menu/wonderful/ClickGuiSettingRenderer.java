package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.gui.DrawContext;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.*;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.display.Keyboard;
import wtf.dexum.utility.render.display.StencilUtil;

import java.util.List;

public class ClickGuiSettingRenderer {

    public void render(DrawContext context, Module module, float panelX, float moduleY, float openProgress, int colorTheme, double mouseX, double mouseY, ClickGuiState state, float alphaMul) {
        List<Setting> settings = module.getSettings();
        if (settings == null || settings.isEmpty() || openProgress <= 0.001f || alphaMul <= 0.001f) {
            return;
        }

        float maxSettingHeight = ClickGuiLayout.calculateSettingsHeight(module);
        float settingsClipY = moduleY + ClickGuiLayout.SETTING_START_Y;
        float settingsClipHeight = maxSettingHeight * openProgress;

        CustomDrawContext customDrawContext = CustomDrawContext.of(context);

        context.enableScissor((int)(panelX + ClickGuiLayout.MODULE_PADDING), (int)settingsClipY, (int)(panelX + ClickGuiLayout.MODULE_PADDING + ClickGuiLayout.MODULE_INNER_WIDTH), (int)(settingsClipY + settingsClipHeight + 1));

        float settingYoffset = ClickGuiLayout.SETTING_START_Y;
        float globalGap = 2.5f;

        List<Setting> visibleSettings = settings.stream().filter(Setting::isVisible).toList();

        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting setting = visibleSettings.get(i);
            float settingY = moduleY + settingYoffset + ClickGuiLayout.SETTING_PADDING;
            int alpha = (int) (255 * openProgress * alphaMul);

            float currentAddedHeight = 0;
            if (setting instanceof BooleanSetting booleanSetting) {
                renderBooleanSetting(context, panelX, settingY, alpha, colorTheme, mouseX, mouseY, booleanSetting, state);
                currentAddedHeight = 12f;
            } else if (setting instanceof NumberSetting floatSetting) {
                renderFloatSetting(context, panelX, settingY, alpha, colorTheme, mouseX, mouseY, floatSetting, state);
                currentAddedHeight = 22f;
            } else if (setting instanceof ModeSetting modeSetting) {
                renderModeSetting(context, panelX, settingY, alpha, colorTheme, mouseX, mouseY, modeSetting, state);
                currentAddedHeight = ClickGuiLayout.calculateModeSettingHeight(modeSetting);
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                renderMultiBooleanSetting(context, panelX, settingY, alpha, colorTheme, mouseX, mouseY, multiBooleanSetting, state);
                currentAddedHeight = ClickGuiLayout.calculateMultiBooleanHeight(multiBooleanSetting);
            } else if (setting instanceof KeySetting bindSetting) {
                renderBindSetting(context, panelX, settingY, alpha, colorTheme, mouseX, mouseY, bindSetting, state);
                currentAddedHeight = 12f;
            } else if (setting instanceof StringSetting stringSetting) {
                renderStringSetting(context, panelX, settingY, alpha, colorTheme, stringSetting, state);
                currentAddedHeight = 22f;
            }

            settingYoffset += currentAddedHeight;
            if (i < visibleSettings.size() - 1) {
                settingYoffset += globalGap;
            }
        }

        context.disableScissor();
    }

    private void renderBooleanSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, double mouseX, double mouseY, BooleanSetting booleanSetting, ClickGuiState state) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        ColorRGBA themeColor = new ColorRGBA(colorTheme);

        String name = booleanSetting.getName();
        boolean binding = state.getBindingSetting() == booleanSetting;
        String bindText = "";
        if (binding) {
            bindText = "[...]";
        } else if (booleanSetting.getKeyCode() != -1) {
            bindText = "[" + Keyboard.getKeyName(booleanSetting.getKeyCode()) + "]";
        }

        float trackW = 14.0f;
        float trackH = 7.0f;
        float trackX = panelX + ClickGuiLayout.SETTING_RIGHT - trackW;
        float trackY = settingY + 2.0f;
        float trackRadius = 2.0f;

        float textStartX = panelX + ClickGuiLayout.SETTING_LEFT + 1.5f;
        float bindW = bindText.isEmpty() ? 0 : Fonts.REGULAR.getWidth(bindText, 6.0f) + 3.0f;
        float maxNameW = trackX - textStartX - 3.0f - bindW;

        float nameW = Fonts.REGULAR.getWidth(name, 6.5f);
        if (nameW > maxNameW && maxNameW > 0) {
            String dots = "..";
            float dotsW = Fonts.REGULAR.getWidth(dots, 6.5f);
            StringBuilder truncated = new StringBuilder();
            float currentW = 0;
            for (int ci = 0; ci < name.length(); ci++) {
                float charW = Fonts.REGULAR.getWidth(String.valueOf(name.charAt(ci)), 6.5f);
                if (currentW + charW + dotsW > maxNameW) break;
                truncated.append(name.charAt(ci));
                currentW += charW;
            }
            name = truncated + dots;
            nameW = Fonts.REGULAR.getWidth(name, 6.5f);
        }

        customDrawContext.drawText(Fonts.REGULAR.getFont(6.5f), name, textStartX, settingY + 2.5f, ColorRGBA.WHITE.withAlpha(alpha));

        if (!bindText.isEmpty()) {
            float bindX = textStartX + nameW + 4.0f;
            customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), bindText, bindX, settingY + 3.0f, ColorRGBA.WHITE.withAlpha(alpha));
        }

        Animation backgroundAnimation = state.getBooleanBackgroundAnimation(booleanSetting);
        backgroundAnimation.update(booleanSetting.isEnabled());
        float progress = backgroundAnimation.getValue();

        ColorRGBA trackOff = new ColorRGBA(35, 35, 45, alpha);
        ColorRGBA trackOn = themeColor.withAlpha(alpha);
        int r = (int)(trackOff.getRed() + (trackOn.getRed() - trackOff.getRed()) * progress);
        int g = (int)(trackOff.getGreen() + (trackOn.getGreen() - trackOff.getGreen()) * progress);
        int b = (int)(trackOff.getBlue() + (trackOn.getBlue() - trackOff.getBlue()) * progress);
        ColorRGBA trackColor = new ColorRGBA(r, g, b, alpha);

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), trackX, trackY, trackW, trackH, BorderRadius.all(trackRadius), trackColor);

        float knobPadding = 1.0f;
        float knobSize = trackH - (knobPadding * 2);
        float knobMinX = trackX + knobPadding;
        float knobMaxX = trackX + trackW - knobPadding - knobSize;
        float knobX = knobMinX + (knobMaxX - knobMinX) * progress;
        float knobY = trackY + knobPadding;

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), knobX, knobY, knobSize, knobSize, BorderRadius.all(knobSize / 2.0f), ColorRGBA.WHITE.withAlpha(alpha));
    }

    private void renderFloatSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, double mouseX, double mouseY, NumberSetting floatSetting, ClickGuiState state) {
        Animation sliderAnimation = state.getSliderAnimation(floatSetting);
        float target = state.getSliderPos(floatSetting);

        if (state.isDraggingSlider(floatSetting) || Math.abs(sliderAnimation.getValue() - target) < 0.001f) {
            sliderAnimation.setValue(target);
        } else {
            sliderAnimation.update(target);
        }
        float animatedPos = sliderAnimation.getValue();

        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        ColorRGBA themeColor = new ColorRGBA(colorTheme);

        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), floatSetting.getName(), panelX + ClickGuiLayout.SETTING_LEFT + 1.5f, settingY + 2.0f, ColorRGBA.WHITE.withAlpha(alpha));

        String valStr = String.format(java.util.Locale.ROOT, "%.1f", floatSetting.getCurrent());
        float valW = Fonts.REGULAR.getWidth(valStr, 6.0f);
        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), valStr, panelX + ClickGuiLayout.SETTING_RIGHT - valW, settingY + 2.0f, ColorRGBA.WHITE.withAlpha((int)(180 * (alpha / 255.0f))));

        float slX = panelX + ClickGuiLayout.SETTING_LEFT;
        float slY = settingY + 11.0f;
        float slW = ClickGuiLayout.SLIDER_WIDTH;
        float slH = 4.5f;
        float rounding = 1.0f;

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), slX, slY, slW, slH, BorderRadius.all(rounding), new ColorRGBA(10, 10, 15, alpha));

        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), slX, slY, slW * animatedPos, slH, BorderRadius.all(rounding), themeColor.withAlpha(alpha));

        float knobX = slX + slW * animatedPos;
        float knobSize = 6.0f;
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), knobX - knobSize / 2f, slY + (slH / 2f) - (knobSize / 2f), knobSize, knobSize, BorderRadius.all(knobSize / 2f), ColorRGBA.WHITE.withAlpha(alpha));
    }

    private void renderModeSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, double mouseX, double mouseY, ModeSetting modeSetting, ClickGuiState state) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        ColorRGBA themeColor = new ColorRGBA(colorTheme);

        customDrawContext.drawText(Fonts.REGULAR.getFont(7.0f), modeSetting.getName(), panelX + ClickGuiLayout.SETTING_LEFT + 1.5f, settingY + 1.5f, ColorRGBA.WHITE.withAlpha(alpha));

        float x = panelX + ClickGuiLayout.SETTING_LEFT;
        float y = settingY + 10.0f;
        float rowHeight = 11.0f;

        for (ModeSetting.Value val : modeSetting.getValues()) {
            boolean selected = modeSetting.getValue() == val;
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            float chipW = textW + (ClickGuiLayout.CHIP_PADDING_X * 2);

            if (x + chipW > panelX + ClickGuiLayout.SETTING_RIGHT) {
                x = panelX + ClickGuiLayout.SETTING_LEFT;
                y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
            }

            boolean hovered = wtf.dexum.utility.math.MathUtil.isHovered(mouseX, mouseY, x, y, chipW, rowHeight);
            val.getAnimation().update(selected || hovered ? 1.0f : 0.0f);
            float anim = val.getAnimation().getValue();

            DrawUtil.drawBlur(customDrawContext.getMatrices(), x, y, chipW, rowHeight, 15.0f, BorderRadius.all(2.0f), ColorRGBA.WHITE.withAlpha(alpha));
            ColorRGBA overlay = new ColorRGBA(10, 10, 15, (int)(160 * (alpha / 255.0f)));
            DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), x, y, chipW, rowHeight, BorderRadius.all(2.0f), overlay);

            ColorRGBA textColor = selected ? ColorRGBA.WHITE.withAlpha(alpha) : new ColorRGBA(180, 180, 180, alpha);
            customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), val.getName(), x + ClickGuiLayout.CHIP_PADDING_X, y + 3.5f, textColor);

            x += chipW + ClickGuiLayout.CHIP_GAP_X;
        }
    }

    private void renderMultiBooleanSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, double mouseX, double mouseY, MultiBooleanSetting multiBooleanSetting, ClickGuiState state) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        ColorRGBA themeColor = new ColorRGBA(colorTheme);

        int enabledCount = (int) multiBooleanSetting.getBooleanSettings().stream().filter(MultiBooleanSetting.Value::isEnabled).count();
        int totalCount = multiBooleanSetting.getBooleanSettings().size();
        String counter = enabledCount + "/" + totalCount;

        customDrawContext.drawText(Fonts.REGULAR.getFont(7.0f), multiBooleanSetting.getName(), panelX + ClickGuiLayout.SETTING_LEFT + 1.5f, settingY + 1.5f, ColorRGBA.WHITE.withAlpha(alpha));
        float counterW = Fonts.REGULAR.getWidth(counter, 6.5f);
        customDrawContext.drawText(Fonts.REGULAR.getFont(6.5f), counter, panelX + ClickGuiLayout.SETTING_RIGHT - counterW, settingY + 1.5f, new ColorRGBA(255, 255, 255, (int)(160 * (alpha / 255.0f))));

        float x = panelX + ClickGuiLayout.SETTING_LEFT;
        float y = settingY + 10.0f;
        float rowHeight = 11.0f;

        for (MultiBooleanSetting.Value val : multiBooleanSetting.getBooleanSettings()) {
            boolean selected = val.isEnabled();
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            float chipW = textW + (ClickGuiLayout.CHIP_PADDING_X * 2);

            if (x + chipW > panelX + ClickGuiLayout.SETTING_RIGHT) {
                x = panelX + ClickGuiLayout.SETTING_LEFT;
                y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
            }

            boolean hovered = wtf.dexum.utility.math.MathUtil.isHovered(mouseX, mouseY, x, y, chipW, rowHeight);
            val.getAnimation().update(selected || hovered ? 1.0f : 0.0f);
            float anim = val.getAnimation().getValue();

            DrawUtil.drawBlur(customDrawContext.getMatrices(), x, y, chipW, rowHeight, 15.0f, BorderRadius.all(2.0f), ColorRGBA.WHITE.withAlpha(alpha));
            ColorRGBA overlay = new ColorRGBA(10, 10, 15, (int)(160 * (alpha / 255.0f)));
            DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), x, y, chipW, rowHeight, BorderRadius.all(2.0f), overlay);

            ColorRGBA textColor = selected ? ColorRGBA.WHITE.withAlpha(alpha) : new ColorRGBA(180, 180, 180, alpha);
            customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), val.getName(), x + ClickGuiLayout.CHIP_PADDING_X, y + 3.5f, textColor);

            x += chipW + ClickGuiLayout.CHIP_GAP_X;
        }
    }

    private void renderBindSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, double mouseX, double mouseY, KeySetting bindSetting, ClickGuiState state) {
        boolean binding = state.getBindingSetting() == bindSetting;
        String bindString = binding ? "..." : Keyboard.getKeyName(bindSetting.getKeyCode());
        float bindTextWidth = Fonts.REGULAR.getWidth(bindString, 6.0f);
        float bindWidth = bindTextWidth + 6f;
        float bindX = panelX + ClickGuiLayout.SETTING_RIGHT - bindWidth;

        CustomDrawContext customDrawContext = CustomDrawContext.of(context);

        DrawUtil.drawBlur(customDrawContext.getMatrices(), bindX, settingY - 2.5f, bindWidth, 11, 15.0f, BorderRadius.all(2.0f), ColorRGBA.WHITE.withAlpha(alpha));
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), bindX, settingY - 2.5f, bindWidth, 11, BorderRadius.all(2.0f), new ColorRGBA(10, 10, 15, (int)(160 * (alpha / 255.0f))));
        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), bindString, bindX + 3, settingY + 1.0f, ColorRGBA.WHITE.withAlpha(alpha));
        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), bindSetting.getName(), panelX + ClickGuiLayout.SETTING_LEFT + 1.5f, settingY + 0.5f, new ColorRGBA(245, 245, 248, alpha));
    }

    private void renderStringSetting(DrawContext context, float panelX, float settingY, int alpha, int colorTheme, StringSetting stringSetting, ClickGuiState state) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        boolean editing = state.getEditingStringSetting() == stringSetting;
        String value = stringSetting.getValue();
        if (value.isEmpty() && !editing) {
            value = "...";
        }

        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), stringSetting.getName(), panelX + ClickGuiLayout.SETTING_LEFT + 1.5f, settingY + 1.0f, ColorRGBA.WHITE.withAlpha(alpha));

        float fieldX = panelX + ClickGuiLayout.SETTING_LEFT;
        float fieldY = settingY + 10.0f;
        float fieldW = ClickGuiLayout.SLIDER_WIDTH;
        float fieldH = 10.0f;
        DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), fieldX, fieldY, fieldW, fieldH, BorderRadius.all(2.0f), new ColorRGBA(10, 10, 15, (int)(180 * (alpha / 255.0f))));

        String display = value;
        if (editing) {
            int cursor = Math.max(0, Math.min(state.getStringCursor(), stringSetting.getValue().length()));
            display = stringSetting.getValue().substring(0, cursor) + "|" + stringSetting.getValue().substring(cursor);
        }

        float textW = Fonts.REGULAR.getWidth(display, 6.0f);
        float textX = fieldX + 3.0f;
        if (textW > fieldW - 6.0f) {
            textX = fieldX + fieldW - 3.0f - textW;
        }
        customDrawContext.drawText(Fonts.REGULAR.getFont(6.0f), display, textX, fieldY + 2.5f, new ColorRGBA(220, 220, 220, alpha));
    }
}