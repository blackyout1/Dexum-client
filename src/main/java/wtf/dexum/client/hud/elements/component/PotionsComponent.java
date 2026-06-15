package wtf.dexum.client.hud.elements.component;

import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Vector4f;

public class PotionsComponent extends DraggableHudElement {
    private final Animation widthAnimation;
    private final Animation xLine;
    private final Animation alpha;
    private final List<PotionItem> potionItems;
    private final boolean v2;

    public PotionsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align, boolean v2) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.xLine = new Animation(170L, Easing.SINE_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
        this.potionItems = new CopyOnWriteArrayList();
        this.v2 = v2;
    }

    public void render(CustomDrawContext ctx) {
        if (mc.player != null) {
            this.updatePotions();
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
    }

    private void renderV2(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;
        float maxW = 0.0F;
        float potionsHeight = 0.0F;
        List<PotionItem> activePotions = new java.util.ArrayList<>();

        for (PotionItem item : this.potionItems) {
            item.animation.update(item.active);
            float anim = item.animation.getValue();
            if (anim > 0.01F) {
                String name = I18n.translate(item.name, new Object[0]);
                if (item.amplifier > 0) name += " " + (item.amplifier + 1);
                String duration = this.formatDuration(item.durationTicks);
                float w = Fonts.REGULAR.getWidth(name + " " + duration, 7.2F);
                maxW = Math.max(maxW, w);
                potionsHeight += 11.0F * anim;
                activePotions.add(item);
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) return;

        float headerHeight = 15.0F;
        float currentWidth = Math.max(maxW + 25.0F, 85.0F);
        this.widthAnimation.update(currentWidth);
        float animWidth = this.widthAnimation.getValue();
        float totalHeight = headerHeight + potionsHeight + 4.0F;

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

        float headerTextWidth = Fonts.REGULAR.getWidth("Potions", 8.0F);
        float iconSize = 8F;
        float iconWidth = Fonts.ICONS.getWidth("Q", iconSize);
        float spacing = 3.5F;
        float totalHeaderWidth = iconWidth + spacing + headerTextWidth;

        float headerX = posX + (animWidth - totalHeaderWidth) / 2.0F;

        ctx.drawText(Fonts.ICONS.getFont(iconSize), "O", headerX + 0.5F, posY + 4.7F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));
        ctx.drawText(Fonts.REGULAR.getFont(8.0F), "Potions", headerX + iconWidth + spacing, posY + 4.9F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

        float currentY = bodyY + 1.0F;
        for (PotionItem item : activePotions) {
            float anim = item.animation.getValue();
            String name = I18n.translate(item.name, new Object[0]);
            if (item.amplifier > 0) name += " " + (item.amplifier + 1);
            String duration = this.formatDuration(item.durationTicks);
            Identifier icon = this.getEffectIcon((StatusEffect)item.effect.getEffectType().value());

            int a = (int)(255 * this.alpha.getValue() * anim);
            ctx.drawTexture(icon, posX + 4.0F, currentY + 1.2F, 8.0F, 8.0F, ColorRGBA.WHITE.withAlpha(a));
            ctx.drawText(Fonts.REGULAR.getFont(7.2F), name, posX + 14.0F, currentY + 3.0F, ColorRGBA.WHITE.withAlpha(a));
            float durW = Fonts.REGULAR.getWidth(duration, 7.2F);
            ctx.drawText(Fonts.REGULAR.getFont(7.2F), duration, posX + animWidth - durW - 6.0F, currentY + 3.0F, themeColor.withAlpha(a));

            currentY += 11.0F * anim;
        }

        this.width = animWidth;
        this.height = totalHeight;
    }

    private void renderClassic(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;
        float maxNameWidth = 0.0F;
        float maxDurationWidth = 0.0F;
        float potionsHeight = 0.0F;

        for(PotionItem item : this.potionItems) {
            item.animation.update(item.active);
            float anim = item.animation.getValue();
            if (anim > 0.01F) {
                String name = I18n.translate(item.name, new Object[0]);
                if (item.amplifier > 0) {
                    name = name + " " + (item.amplifier + 1);
                }
                String duration = this.formatDuration(item.durationTicks);
                float nameW = Fonts.REGULAR.getWidth(name, 7.2F);
                float durW = Fonts.REGULAR.getWidth(duration, 7.2F);

                maxNameWidth = Math.max(maxNameWidth, nameW * anim + 10.0F);
                maxDurationWidth = Math.max(maxDurationWidth, durW * anim);
                potionsHeight += 11.0F * anim;
                isFound = true;
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

        float headerHeight = 15.0F;
        float footerHeight = 4.0F;
        float bodyHeight = potionsHeight + footerHeight;
        float totalHeight = headerHeight + bodyHeight;
        float targetWidth = Math.max(maxNameWidth + maxDurationWidth + 25.0F, 80.0F);
        this.widthAnimation.update(targetWidth);
        float currentWidth = this.widthAnimation.getValue();

        if (this.alpha.getValue() > 0.01F) {
            float rounding = 4.0F;
            ColorRGBA headerColor = new ColorRGBA(0, 0, 0, (int)(255 * this.alpha.getValue()));
            ColorRGBA bodyColor = new ColorRGBA(0, 0, 0, (int)(125 * this.alpha.getValue()));

            DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, 15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, BorderRadius.all(rounding), bodyColor);

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, headerHeight, new BorderRadius(rounding, rounding, 0, 0), headerColor);

            ctx.drawText(Fonts.REGULAR.getFont(8.0F), "Potions", posX + 7.0F, posY + 4.5F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));
            ctx.drawText(Fonts.NURIKI.getFont(9.5f), "E", posX - 1f + currentWidth - 14.0F, posY + 5.5F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));

            float potionY = posY + headerHeight + 2.0F;
            float durEndX = posX + currentWidth - 8.0F;

            for(PotionItem item : this.potionItems) {
                float anim = item.animation.getValue();
                if (anim > 0.01F) {
                    String name = I18n.translate(item.name, new Object[0]);
                    if (item.amplifier > 0) {
                        name = name + " " + (item.amplifier + 1);
                    }
                    String duration = this.formatDuration(item.durationTicks);
                    Identifier icon = this.getEffectIcon((StatusEffect)item.effect.getEffectType().value());

                    ColorRGBA iconC = themeColor.withAlpha((int)(255 * this.alpha.getValue() * anim));
                    ColorRGBA textC = ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * anim));

                    ctx.drawTexture(icon, posX + 6.0F, potionY + 1.0F, 8.0F, 8.0F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * anim)));
                    ctx.drawText(Fonts.REGULAR.getFont(7.2F), name, posX + 16.5F, potionY + 2.5F, textC);
                    ctx.drawText(Fonts.REGULAR.getFont(7.2F), duration, durEndX - Fonts.REGULAR.getWidth(duration, 7.2F) + 1.0F, potionY + 2.5F, iconC);

                    potionY += 11.0F * anim;
                }
            }
        }

        this.width = currentWidth;
        this.height = totalHeight;
    }

    private String getAmplifierText(int amplifier) {
        return String.valueOf(amplifier + 1);
    }

    private String formatDuration(int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Identifier getEffectIcon(StatusEffect effect) {
        String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
        return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
    }

    public void updatePotions() {
        if (mc.player != null) {
            Map<String, StatusEffectInstance> currentEffects = (Map)mc.player.getStatusEffects().stream().collect(Collectors.toMap((e) -> {
                String var10000 = Text.translatable(e.getTranslationKey()).getString();
                return var10000 + ":" + e.getAmplifier();
            }, (e) -> e, (e1, e2) -> e1));
            this.potionItems.forEach((item) -> {
                String key = item.name + ":" + item.amplifier;
                StatusEffectInstance effect = (StatusEffectInstance)currentEffects.get(key);
                if (effect != null) {
                    item.durationTicks = effect.getDuration();
                    if (!item.active) {
                        item.animation.setValue(1.0F);
                    }

                    item.active = true;
                    currentEffects.remove(key);
                } else {
                    item.active = false;
                }

            });
            currentEffects.forEach((key, effect) -> this.potionItems.add(new PotionItem(Text.translatable(effect.getTranslationKey()).getString(), effect.getAmplifier(), effect.getDuration(), effect)));
            this.potionItems.removeIf((item) -> !item.active && item.animation.getValue() == 0.0F);
        }

    }

    private static class PotionItem {
        String name;
        int amplifier;
        int durationTicks;
        boolean active;
        StatusEffectInstance effect;
        Animation animation;

        PotionItem(String name, int amplifier, int durationTicks, StatusEffectInstance effect) {
            this.animation = new Animation(250L, Easing.CUBIC_OUT);
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
            this.active = true;
            this.effect = effect;
        }
    }
}