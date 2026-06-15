package wtf.dexum.client.hud.elements.component;

import java.util.List;
import org.joml.Vector4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.utility.render.display.Keyboard;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import net.minecraft.client.gui.screen.ChatScreen;

public class KeybindsComponent extends DraggableHudElement {
    private final Animation widthAnimation;
    private final Animation xLine;
    private final Animation alpha;
    private final boolean v2;

    public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align, boolean v2) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.xLine = new Animation(170L, Easing.SINE_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
        this.v2 = v2;
    }

    public void render(CustomDrawContext ctx) {
        float posX = this.getX();
        float posY = this.getY();
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA themeColor = theme.getColor();

        if (v2) {
            renderV2(ctx, posX, posY, themeColor);
        } else {
            renderClassic(ctx, posX, posY, themeColor);
        }
    }

    private void renderV2(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;
        for (Module module : Dexum.getInstance().getModuleManager().getModules()) {
            if (module.getKeyCode() != -1 && module.isEnabled()) {
                isFound = true;
                break;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) return;

        float modulesHeight = 0.0F;
        float maxWidth = 0.0F;
        List<String[]> activeBinds = new java.util.ArrayList<>();

        for (Module module : Dexum.getInstance().getModuleManager().getModules()) {
            if (module.getKeyCode() != -1 && module.getAnimation().getValue() > 0.01F) {
                String bind = Keyboard.getKeyName(module.getKeyCode());
                activeBinds.add(new String[]{module.getName(), bind, String.valueOf(module.getAnimation().getValue())});
                float w = Fonts.REGULAR.getWidth(module.getName() + " " + bind, 7.2F);
                maxWidth = Math.max(maxWidth, w);
                modulesHeight += 11.0F * module.getAnimation().getValue();
            }
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting bool && bool.getKeyCode() != -1) {
                    float sAnim = bool.getAnimation().getValue();
                    if (sAnim > 0.01F) {
                        String bind = Keyboard.getKeyName(bool.getKeyCode());
                        activeBinds.add(new String[]{bool.getName(), bind, String.valueOf(sAnim)});
                        float w = Fonts.REGULAR.getWidth(bool.getName() + " " + bind, 7.2F);
                        maxWidth = Math.max(maxWidth, w);
                        modulesHeight += 11.0F * sAnim;
                    }
                }
            }
        }

        float headerHeight = 15.0F;
        float currentWidth = Math.max(maxWidth + 13.0F, 82.0F);
        this.widthAnimation.update(currentWidth);
        float animWidth = this.widthAnimation.getValue();

        float totalHeight = headerHeight + modulesHeight + 4.0F;

        Vector4f rounding = new Vector4f(6.0F, 6.0F, 6.0F, 6.0F);

        DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, animWidth, totalHeight, 18.0F,
                BorderRadius.all(rounding.x),
                ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, animWidth, headerHeight,
                new BorderRadius(rounding.x, rounding.y, 0, 0),
                ColorRGBA.BLACK.withAlpha((int)(255 * this.alpha.getValue())));

        float bodyY = posY + headerHeight;
        float bodyHeight = totalHeight - headerHeight;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, bodyY, animWidth, bodyHeight,
                new BorderRadius(0, 0, rounding.z, rounding.w),
                new ColorRGBA(0, 0, 0, (int)(135 * this.alpha.getValue())));

        float headerTextWidth = Fonts.REGULAR.getWidth("Keybinds", 8.0F);
        float iconSize = 7.2F;
        float iconWidth = Fonts.ICONS.getWidth("L", iconSize);
        float spacing = 3.5F;
        float totalHeaderWidth = iconWidth + spacing + headerTextWidth;

        float headerX = posX + (animWidth - totalHeaderWidth) / 2.0F;

        ctx.drawText(Fonts.ICONS.getFont(iconSize), "L", headerX + 0.5F, posY + 4.7F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));
        ctx.drawText(Fonts.REGULAR.getFont(8.0F), "Keybinds", headerX + iconWidth + spacing, posY + 4.8F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

        float currentY = bodyY + 1.0F;
        for (String[] bindInfo : activeBinds) {
            float anim = Float.parseFloat(bindInfo[2]);
            String text = bindInfo[0];
            String key = bindInfo[1];

            float keyW = Fonts.REGULAR.getWidth(key, 7.2F);

            float maxNameW = animWidth - keyW - 18.0F;
            float textW = Fonts.REGULAR.getWidth(text, 7.2F);
            if (textW > maxNameW && maxNameW > 0) {
                String dots = "..";
                float dotsW = Fonts.REGULAR.getWidth(dots, 7.2F);
                StringBuilder truncated = new StringBuilder();
                float cw = 0;
                for (int ci = 0; ci < text.length(); ci++) {
                    float charW = Fonts.REGULAR.getWidth(String.valueOf(text.charAt(ci)), 7.2F);
                    if (cw + charW + dotsW > maxNameW) break;
                    truncated.append(text.charAt(ci));
                    cw += charW;
                }
                text = truncated + dots;
            }

            ctx.drawText(Fonts.REGULAR.getFont(7.2F), text, posX + 6.0F, currentY + 3.0F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * anim)));
            ctx.drawText(Fonts.REGULAR.getFont(7.2F), key, posX + animWidth - keyW - 6.0F, currentY + 3.0F, themeColor.withAlpha((int)(255 * this.alpha.getValue() * anim)));

            currentY += 11.0F * anim;
        }

        this.width = animWidth;
        this.height = totalHeight;
    }

    private void renderClassic(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;

        for(Module module : Dexum.getInstance().getModuleManager().getModules()) {
            if (module.getKeyCode() != -1) {
                isFound = true;
                break;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        float modulesHeight = 0.0F;
        float maxNameWidth = 0.0F;
        float maxBindWidth = 0.0F;

        for(Module module : Dexum.getInstance().getModuleManager().getModules()) {
            float anim = module.getAnimation().getValue();
            if (module.getKeyCode() != -1 && anim > 0.01F) {
                String bind = Keyboard.getKeyName(module.getKeyCode());
                float nameWidth = Fonts.REGULAR.getWidth(module.getName(), 7.2F);
                float bindWidth = Fonts.REGULAR.getWidth(bind, 7.2F);
                maxNameWidth = Math.max(maxNameWidth, nameWidth * anim);
                maxBindWidth = Math.max(maxBindWidth, bindWidth * anim);
                modulesHeight += 11.0F * anim;
            }
            for(Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting bool && bool.getKeyCode() != -1) {
                    float sAnim = bool.getAnimation().getValue();
                    if (sAnim > 0.01F) {
                        String bind = Keyboard.getKeyName(bool.getKeyCode());
                        float nameWidth = Fonts.REGULAR.getWidth(bool.getName(), 7.2F);
                        float bindWidth = Fonts.REGULAR.getWidth(bind, 7.2F);
                        maxNameWidth = Math.max(maxNameWidth, nameWidth * sAnim);
                        maxBindWidth = Math.max(maxBindWidth, bindWidth * sAnim);
                        modulesHeight += 11.0F * sAnim;
                    }
                }
            }
        }

        float headerHeight = 15.0F;
        float footerHeight = 4.0F;
        float bodyHeight = modulesHeight + footerHeight;
        float totalHeight = headerHeight + bodyHeight;
        float targetWidth = Math.max(maxNameWidth + maxBindWidth + 45.0F, 85.0F);
        this.widthAnimation.update(targetWidth);
        float currentWidth = this.widthAnimation.getValue();

        if (this.alpha.getValue() > 0.01F) {
            float rounding = 4.0F;
            ColorRGBA headerColor = new ColorRGBA(0, 0, 0, (int)(255 * this.alpha.getValue()));
            ColorRGBA bodyColor = new ColorRGBA(0, 0, 0, (int)(125 * this.alpha.getValue()));

            DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, 15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, BorderRadius.all(rounding), bodyColor);

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, headerHeight, new BorderRadius(rounding, rounding, 0, 0), headerColor);

            ctx.drawText(Fonts.MEDIUM.getFont(8.0F), "Hotkeys", posX + 7F, posY + 4.5F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));
            ctx.drawText(Fonts.NURIKI.getFont(9.5f), "C", posX - 1f + currentWidth - 14.0F, posY + 5.5F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));

            float moduleY = posY + headerHeight + 2.0F;
            float bindEndX = posX + currentWidth - 8.0F;

            for(Module module : Dexum.getInstance().getModuleManager().getModules()) {
                float anim = module.getAnimation().getValue();
                if (module.getKeyCode() != -1 && anim > 0.01F) {
                    String bind = Keyboard.getKeyName(module.getKeyCode());
                    float bindWidth = Fonts.MEDIUM.getWidth(bind, 7.2F);

                    ColorRGBA iconC = themeColor.withAlpha((int)(255 * this.alpha.getValue() * anim));
                    ColorRGBA textC = ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * anim));

                    ctx.drawText(Fonts.MEDIUM.getFont(7.2F), module.getName(), posX + 6.5F, moduleY + 2.5F, textC);
                    ctx.drawText(Fonts.MEDIUM.getFont(7.2F), bind, bindEndX - bindWidth + 1.0F, moduleY + 2.5F, iconC);
                    moduleY += 11.0F * anim;
                }
                for(Setting setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bool && bool.getKeyCode() != -1) {
                        float sAnim = bool.getAnimation().getValue();
                        if (sAnim > 0.01F) {
                            String bind = Keyboard.getKeyName(bool.getKeyCode());
                            float bindWidth = Fonts.MEDIUM.getWidth(bind, 7.2F);

                            ColorRGBA iconC = themeColor.withAlpha((int)(255 * this.alpha.getValue() * sAnim));
                            ColorRGBA textC = ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * sAnim));

                            ctx.drawText(Fonts.MEDIUM.getFont(7.2F), bool.getName(), posX + 6.5F, moduleY + 2.5F, textC);
                            ctx.drawText(Fonts.MEDIUM.getFont(7.2F), bind, bindEndX - bindWidth + 1.0F, moduleY + 2.5F, iconC);
                            moduleY += 11.0F * sAnim;
                        }
                    }
                }
            }
        }

        this.width = currentWidth;
        this.height = totalHeight;
    }
}