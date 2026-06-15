package wtf.dexum.client.screens.menu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class Panel {
    private final Category category;
    private float x, y;
    private float width = 100;
    private float height;
    private boolean dragging;
    private float dragX, dragY;
    private boolean expanded = true;
    private final java.util.Map<Module, Animation> expandedModules;
    private float scrollY;

    public Panel(Category category, float x, float y, java.util.Map<Module, Animation> expandedModules) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.expandedModules = expandedModules;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, float alpha) {
        CustomDrawContext drawContext = CustomDrawContext.of(context);
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA themeColor = theme.getColor();

        List<Module> modules = Dexum.getInstance().getModuleManager().getModules().stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());

        float headerHeight = 20.0F;
        float moduleHeight = 18.0F;
        float moduleSpacing = 5.0F;

        ColorRGBA topColor = themeColor.darker(0.8F).withAlpha((int)(255 * alpha));
        ColorRGBA bottomColor = themeColor.darker(0.95F).withAlpha((int)(180 * alpha));

        DrawUtil.drawShadow(drawContext.getMatrices(), x, y, width, height, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha((int)(120 * alpha)));

        DrawUtil.drawBlur(drawContext.getMatrices(), x, y, width, height, 15.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha((int)(255 * alpha)));

        DrawUtil.drawRoundedRect(drawContext.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), bottomColor);

        DrawUtil.drawRoundedRect(drawContext.getMatrices(), x, y, width, headerHeight, new BorderRadius(4.0F, 4.0F, 0, 0), topColor);

        drawContext.drawText(Fonts.REGULAR.getFont(9.0F), category.getName(), x + 6.0F, y + 6.5F, ColorRGBA.WHITE.withAlpha((int)(255 * alpha)));
        drawContext.drawText(Fonts.ICONS.getFont(8.0F), category.getIcon(), x + width - 14.0F, y + 6.5F, themeColor.withAlpha((int)(255 * alpha)));

        drawContext.enableScissor((int)x, (int)(y + headerHeight), (int)(x + width), (int)(y + height));

        float currentModuleY = y + headerHeight + 5.0F + scrollY;
        for (Module module : modules) {
            module.getAnimation().update(module.isEnabled());

            float animValue = alpha;
            Animation settingsAnim = expandedModules.get(module);
            List<Setting> settings = module.getSettings().stream().filter(Setting::isVisible).collect(Collectors.toList());

            float moduleBlockHeight = moduleHeight;
            float settingsAnimatedHeight = 0;

            if (settingsAnim != null) {
                settingsAnim.update(settingsAnim.getTargetValue() == 1.0F);
                if (settingsAnim.getValue() > 0.001F) {
                    float totalSettingsStaticHeight = 0;
                    for (Setting s : settings) {
                        if (s instanceof BooleanSetting) totalSettingsStaticHeight += 12;
                        else if (s instanceof NumberSetting) totalSettingsStaticHeight += 22;
                        else if (s instanceof ModeSetting) totalSettingsStaticHeight += 12;
                        else totalSettingsStaticHeight += 12;
                    }
                    settingsAnimatedHeight = (totalSettingsStaticHeight + 4) * settingsAnim.getValue();
                    moduleBlockHeight += settingsAnimatedHeight;
                }
            }

            boolean hovered = isHovered(mouseX, mouseY, x + 3, currentModuleY, width - 6, moduleHeight);

            ColorRGBA moduleBg = new ColorRGBA(22, 22, 22, (int)(180 * animValue));
            if (hovered) moduleBg = moduleBg.brighter(0.15F);

            DrawUtil.drawRoundedRect(drawContext.getMatrices(), x + 3, currentModuleY, width - 6, moduleHeight, BorderRadius.all(3.0F), moduleBg);

            float nameFontSize = 9.0F;
            float nameW = Fonts.REGULAR.getWidth(module.getName(), nameFontSize);
            ColorRGBA nameColor = module.isEnabled() ? themeColor.withAlpha((int)(255 * animValue)) : ColorRGBA.WHITE.withAlpha((int)(255 * animValue));
            drawContext.drawText(Fonts.REGULAR.getFont(nameFontSize), module.getName(), x + (width / 2.0F) - (nameW / 2.0F), currentModuleY + 4.5F, nameColor);

            if (!settings.isEmpty()) {
                String indicator = settingsAnim != null && settingsAnim.getTargetValue() == 1.0F ? "-" : "+";
                drawContext.drawText(Fonts.REGULAR.getFont(7.5F), indicator, x + width - 12, currentModuleY + 5.0F, ColorRGBA.WHITE.withAlpha((int)(150 * animValue)));
            }

            if (settingsAnimatedHeight > 0.01F) {
                float progress = settingsAnim.getValue();
                float settingsY = currentModuleY + moduleHeight;

                DrawUtil.drawRoundedRect(drawContext.getMatrices(), x + 5, settingsY, width - 10, settingsAnimatedHeight - 2, BorderRadius.all(2.0F), new ColorRGBA(10, 10, 10, (int)(120 * animValue * progress)));

                float sY = settingsY + 3.0F;
                for (Setting setting : settings) {
                    float sH = 0;
                    if (setting instanceof BooleanSetting bool) {
                        sH = 12;
                        ColorRGBA sColor = bool.isEnabled() ? themeColor.withAlpha((int)(255 * animValue * progress)) : ColorRGBA.WHITE.withAlpha((int)(160 * animValue * progress));
                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), setting.getName(), x + 8, sY, sColor);
                    }
                    else if (setting instanceof NumberSetting num) {
                        sH = 22;

                        String valStr = String.format("%.2f", num.getCurrent()).replace(".", ",");
                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), setting.getName() + ":", x + 8, sY, ColorRGBA.WHITE.withAlpha((int)(220 * animValue * progress)));
                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), valStr, x + 8 + Fonts.REGULAR.getWidth(setting.getName() + ":", 7.0F), sY, themeColor.withAlpha((int)(255 * animValue * progress)));

                        float slX = x + 8;
                        float slY = sY + 10;
                        float slW = width - 16;
                        DrawUtil.drawRoundedRect(drawContext.getMatrices(), slX, slY, slW, 2.0F, BorderRadius.all(1.0F), new ColorRGBA(25, 25, 25, (int)(255 * animValue * progress)));
                        float percent = (num.getCurrent() - num.getMin()) / (num.getMax() - num.getMin());
                        DrawUtil.drawRoundedRect(drawContext.getMatrices(), slX, slY, slW * percent, 2.0F, BorderRadius.all(1.0F), themeColor.withAlpha((int)(255 * animValue * progress)));

                        DrawUtil.drawRoundedRect(drawContext.getMatrices(), slX + slW * percent - 1, slY - 1, 3, 4, BorderRadius.all(1.0F), ColorRGBA.WHITE.withAlpha((int)(255 * animValue * progress)));
                    }
                    else if (setting instanceof ModeSetting mode) {
                        sH = 12;

                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), setting.getName() + ":", x + 8, sY, ColorRGBA.WHITE.withAlpha((int)(200 * animValue * progress)));
                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), mode.getValue().getName(), x + 8 + Fonts.REGULAR.getWidth(setting.getName() + ":", 7.0F), sY, themeColor.withAlpha((int)(255 * animValue * progress)));
                    }
                    else {
                        sH = 12;
                        drawContext.drawText(Fonts.REGULAR.getFont(7.0F), setting.getName(), x + 8, sY, ColorRGBA.WHITE.withAlpha((int)(180 * animValue * progress)));
                    }
                    sY += sH * progress;
                }
            }

            currentModuleY += moduleBlockHeight + moduleSpacing;
        }

        drawContext.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y + 20, width, height - 20)) {
            float currentModuleY = y + 20 + 5.0F + scrollY;
            float moduleHeight = 18.0F;
            float moduleSpacing = 5.0F;
            List<Module> modules = Dexum.getInstance().getModuleManager().getModules().stream()
                    .filter(m -> m.getCategory() == category)
                    .collect(Collectors.toList());

            for (Module module : modules) {
                Animation settingsAnim = expandedModules.get(module);
                List<Setting> settings = module.getSettings().stream().filter(Setting::isVisible).collect(Collectors.toList());

                float moduleBlockHeight = moduleHeight;
                float settingsAnimatedHeight = 0;

                if (settingsAnim != null && settingsAnim.getValue() > 0.001F) {
                    float totalSettingsStaticHeight = 0;
                    for (Setting s : settings) {
                        if (s instanceof BooleanSetting) totalSettingsStaticHeight += 12;
                        else if (s instanceof NumberSetting) totalSettingsStaticHeight += 22;
                        else if (s instanceof ModeSetting) totalSettingsStaticHeight += 12;
                        else totalSettingsStaticHeight += 12;
                    }
                    settingsAnimatedHeight = (totalSettingsStaticHeight + 4) * settingsAnim.getValue();
                    moduleBlockHeight += settingsAnimatedHeight;
                }

                if (isHovered(mouseX, mouseY, x + 3, currentModuleY, width - 6, moduleHeight)) {
                    if (button == 0) {
                        module.toggle();
                    } else if (button == 1) {
                        if (settingsAnim != null) {
                            settingsAnim.setTargetValue(settingsAnim.getTargetValue() == 1.0F ? 0.0F : 1.0F);
                        }
                    }
                    return true;
                }

                if (settingsAnimatedHeight > 0.001F) {
                    float settingsY = currentModuleY + moduleHeight;
                    if (isHovered(mouseX, mouseY, x + 5, settingsY, width - 10, settingsAnimatedHeight)) {
                        float sY = settingsY + 3.0F;
                        float progress = settingsAnim.getValue();
                        for (Setting setting : settings) {
                            float sH = 0;
                            if (setting instanceof BooleanSetting bool) {
                                sH = 12;
                                if (isHovered(mouseX, mouseY, x + 8, sY, width - 16, 11)) {
                                    bool.toggle();
                                    return true;
                                }
                            } else if (setting instanceof NumberSetting num) {
                                sH = 22;
                                float slX = x + 8;
                                float slY = sY + 10;
                                float slW = width - 16;
                                if (isHovered(mouseX, mouseY, slX, slY - 2, slW, 6)) {
                                    float percent = (float)((mouseX - slX) / slW);
                                    num.setCurrent(num.getMin() + percent * (num.getMax() - num.getMin()));
                                    return true;
                                }
                            } else if (setting instanceof ModeSetting mode) {
                                sH = 12;
                                if (isHovered(mouseX, mouseY, x + 8, sY, width - 16, 11)) {
                                    int next = (mode.getValues().indexOf(mode.getValue()) + 1) % mode.getValues().size();
                                    mode.setValue(mode.getValues().get(next));
                                    return true;
                                }
                            } else {
                                sH = 12;
                            }
                            sY += sH * progress;
                        }
                    }
                }

                currentModuleY += moduleBlockHeight + moduleSpacing;
            }
        }

        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isHovered(mouseX, mouseY, x, y + 20, width, height - 20)) {
            scrollY += (float) (amount * 15.0F);
            scrollY = Math.min(0, scrollY);
            return true;
        }
        return false;
    }

    private boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}