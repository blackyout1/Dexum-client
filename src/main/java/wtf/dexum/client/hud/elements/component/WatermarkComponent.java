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
import wtf.dexum.client.modules.impl.misc.NameProtect;

public class WatermarkComponent extends DraggableHudElement {
    private final Animation widthAnimation;
    private final boolean compact;

    public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align, boolean compact) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.compact = compact;
    }

    public void render(CustomDrawContext ctx) {
        if (mc.player != null) {
            float posX = this.getX();
            float posY = this.getY();
            Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA themeColor = theme.getColor();

            if (compact) {
                renderCompact(ctx, posX, posY, themeColor);
            } else {
                renderClassic(ctx, posX, posY, themeColor);
            }
        }
    }

    private void renderCompact(CustomDrawContext ctx, float x, float y, ColorRGBA themeColor) {
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();

        float circleSize = 32.0F;
        float circleRadius = circleSize / 2.0F;
        float cardHeight = 24.0F;
        float cardX = x + circleRadius - 4.0F;
        float cardY = y + (circleSize - cardHeight) / 2.0F;
        float textOffsetX = 22.0F;
        float rightPadding = 14.0F;

        String title = "DexumClient";
        String subtitle = NameProtect.getWatermarkName();
        String subtitleIcon = "2";
        float titleWidth = Fonts.MEDIUM.getWidth(title, 8.0F);
        float subtitleIconWidth = Fonts.ICONS.getWidth(subtitleIcon, 6.2f);
        float subtitleWidth = subtitleIconWidth + 2.0F + Fonts.REGULAR.getWidth(subtitle, 7.0F);
        float contentWidth = Math.max(titleWidth, subtitleWidth);
        float cardWidth = textOffsetX + contentWidth + rightPadding;

        ColorRGBA bg = ColorRGBA.BLACK;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, circleSize, circleSize, BorderRadius.all(circleRadius), bg);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), cardX, cardY, cardWidth, cardHeight, BorderRadius.all(4.0F), bg);

        String logo = "8";
        float logoSize = 13.0F;
        float logoX = x + (circleSize - Fonts.ICONS.getWidth(logo, logoSize)) / 2.0F + 0.5F;
        float logoY = y + (circleSize - logoSize) / 1.7F - 0.6F;
        ctx.drawText(Fonts.ICONS.getFont(logoSize), logo, logoX, logoY, theme.getColor());

        float textX = cardX + textOffsetX;
        float iconOffsetLeft = 1.5F;
        ctx.drawText(Fonts.MEDIUM.getFont(8.0F), title, textX - 2.0f, cardY + 5.0F, new ColorRGBA(235, 235, 235, 255));
        ctx.drawText(Fonts.ICONS.getFont(6.2F), subtitleIcon, textX - iconOffsetLeft, cardY + 15.25F, new ColorRGBA(255, 255, 255, 255));
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), subtitle, textX - iconOffsetLeft + subtitleIconWidth + 2.0F, cardY + 15.25F, new ColorRGBA(165, 165, 165, 255));

        this.width = (cardX - x) + cardWidth;
        this.height = circleSize;
    }

    private void renderClassic(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        String name = NameProtect.getWatermarkName();
        String fpsVal = String.valueOf(mc.getCurrentFps());
        String ip = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";
        String version = " 1.21.4";

        float padding = 6.0F;
        float spacing = 12.0F;
        float interSectionGap = 8.0F;

        float logoW = Fonts.MEDIUM.getWidth("dexum " + version, 7.5F);
        float nameW = Fonts.MEDIUM.getWidth(name, 7.2F) + 10.5F;
        float fpsW = Fonts.MEDIUM.getWidth(fpsVal + " fps", 7.2F) + 10.0F;
        float ipW = Fonts.MEDIUM.getWidth(ip, 7.2F) + 10.0F;

        float targetWidth = padding * 2 + logoW + 8.0F + nameW + interSectionGap + fpsW + interSectionGap + ipW + 2.0F;
        this.widthAnimation.update(targetWidth);
        float currentWidth = this.widthAnimation.getValue();
        float totalHeight = 18.0F;
        float rounding = 4.0F;

        DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, 15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha(255));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, BorderRadius.all(rounding), new ColorRGBA(0, 0, 0, 125));

        float logoPartWidth = logoW + padding * 2;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, logoPartWidth, totalHeight, new BorderRadius(rounding, 0, 0, rounding), new ColorRGBA(0, 0, 0, 255));

        ctx.drawText(Fonts.MEDIUM.getFont(7.5F), "dexum ", posX + padding, posY + 6.0F, ColorRGBA.WHITE);
        ctx.drawText(Fonts.MEDIUM.getFont(7.5F), version, posX + padding + Fonts.MEDIUM.getWidth("dexum ", 7.5F), posY + 6.0F, themeColor);

        float currentX = posX + logoPartWidth + 7.0F;
        float iconY = posY + 7.0F;
        float textY = posY + 7.0F;

        ctx.drawText(Fonts.FONT.getFont(8.5F), "e", currentX - 2.0F, iconY, themeColor);
        ctx.drawText(Fonts.MEDIUM.getFont(7.2F), name, currentX + 8.0F, textY - 0.2F, ColorRGBA.WHITE);
        currentX += nameW + interSectionGap;

        ctx.drawText(Fonts.FONT.getFont(8.5F), "m", currentX - 3.5F, iconY + 0.2F, themeColor);
        ctx.drawText(Fonts.MEDIUM.getFont(7.2F), fpsVal, currentX + 7.5F, textY - 0.2F, ColorRGBA.WHITE);
        ctx.drawText(Fonts.MEDIUM.getFont(7.2F), "FPS", currentX + 7.5F + Fonts.MEDIUM.getWidth(fpsVal, 7.2F) + 1.5F, textY - 0.2F, new ColorRGBA(180, 180, 180, 255));
        currentX += fpsW + interSectionGap;

        ctx.drawText(Fonts.FONT.getFont(8.8F), "q", currentX - 3.0F, iconY + 0.1F, themeColor);
        ctx.drawText(Fonts.MEDIUM.getFont(7.2F), ip, currentX + 7.5F, textY - 0.6F, ColorRGBA.WHITE);

        this.width = currentWidth;
        this.height = totalHeight;
    }
}