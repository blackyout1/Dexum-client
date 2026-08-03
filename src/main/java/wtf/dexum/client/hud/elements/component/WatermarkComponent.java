package wtf.dexum.client.hud.elements.component;

import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.client.modules.impl.misc.NameProtect;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkComponent extends DraggableHudElement {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Настройки
    private final BooleanSetting showFps = new BooleanSetting("FPS", "Частота кадров", true);
    private final BooleanSetting showPing = new BooleanSetting("Ping", "Боковое отображение", true);
    private final BooleanSetting showTime = new BooleanSetting("Time", "Текущее время", true);
    private final BooleanSetting showCoords = new BooleanSetting("Coords", "Координаты", true);
    private final BooleanSetting showTps = new BooleanSetting("TPS", "Задержка сервера", true);
    private final BooleanSetting showBps = new BooleanSetting("BPS", "Скорость игрока", true);
    private final BooleanSetting showUsername = new BooleanSetting("Username", "Логин в клиенте", false);
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
            ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
            ColorRGBA hudBg = wtf.dexum.client.modules.impl.render.Interface.getHudColor();

            if (compact) {
                renderCompact(ctx, posX, posY, themeColor, hudBg);
            } else {
                renderClassic(ctx, posX, posY, themeColor, hudBg);
            }
        }
    }

    private void renderCompact(CustomDrawContext ctx, float x, float y, ColorRGBA themeColor, ColorRGBA hudBg) {

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

        ColorRGBA bg = hudBg;

        // Рисуем кружок и плашку
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, circleSize, circleSize, BorderRadius.all(circleRadius), bg);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), cardX, cardY, cardWidth, cardHeight, BorderRadius.all(4.0F), bg);

        // ====== БУКВА D В ЦЕНТРЕ КРУЖКА (ЖИРНАЯ, КРУПНАЯ, ТОЧНАЯ ЦЕНТРОВКА) ======
        String logo = "D";
        float logoSize = 16.0F;                // размер шрифта
        float logoWidth = Fonts.BOLD.getWidth(logo, logoSize);

        // Горизонтальное центрирование
        float logoX = x + (circleSize - logoWidth) / 2.0F;

        // Вертикальное центрирование: подбираем смещение, чтобы буква оказалась ровно по центру
        float verticalOffset = 3F;          // <-- МЕНЯЙ ЭТО ЗНАЧЕНИЕ, ЕСЛИ БУКВА НЕ ПО ЦЕНТРУ
        float logoY = y + (circleSize - logoSize) / 2.0F + verticalOffset;

        ctx.drawText(Fonts.BOLD.getFont(logoSize), logo, logoX, logoY, themeColor);
        // ========================================================================

        // Подпись (название клиента и имя игрока)
        float textX = cardX + textOffsetX;
        float iconOffsetLeft = 1.5F;
        ctx.drawText(Fonts.MEDIUM.getFont(8.0F), title, textX - 2.0f, cardY + 5.0F, new ColorRGBA(235, 235, 235, 255));
        ctx.drawText(Fonts.ICONS.getFont(6.2F), subtitleIcon, textX - iconOffsetLeft, cardY + 15.25F, new ColorRGBA(255, 255, 255, 255));
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), subtitle, textX - iconOffsetLeft + subtitleIconWidth + 2.0F, cardY + 15.25F, new ColorRGBA(165, 165, 165, 255));

        this.width = (cardX - x) + cardWidth;
        this.height = circleSize;
    }

    private void renderClassic(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor, ColorRGBA hudBg) {
        if (mc.player == null) return;

        // Собираем элементы для отображения
        List<String> items = new ArrayList<>();
        
        // Всегда показываем логотип первым
        items.add("LOGO");
        
        if (showUsername.isEnabled()) {
            items.add(NameProtect.getWatermarkName());
        }
        
        if (showPing.isEnabled()) {
            items.add(getPlayerPing() + " ms");
        }
        
        if (showTime.isEnabled()) {
            items.add(LocalTime.now().format(TIME_FORMATTER));
        }
        
        if (showFps.isEnabled()) {
            items.add(mc.getCurrentFps() + " FPS");
        }
        
        if (showCoords.isEnabled()) {
            items.add(String.format("x %d y %d z %d", 
                (int)mc.player.getX(), 
                (int)mc.player.getY(), 
                (int)mc.player.getZ()));
        }
        
        if (showTps.isEnabled()) {
            items.add("20.0 TPS");
        }
        
        if (showBps.isEnabled()) {
            items.add("0.00 BPS");
        }

        if (items.size() <= 1) { // только логотип
            this.width = 0;
            this.height = 0;
            return;
        }

        // Параметры отрисовки
        float fontSize = 7.5f;
        float logoFontSize = 9.0f;
        float capsuleHeight = 16.0f;
        float capsulePadding = 7.0f;
        float capsuleGap = 4.0f;
        float rowGap = 4.0f;

        // Рисуем все элементы как отдельные капсулы
        float currentX = posX;
        float currentY = posY;
        float maxWidth = 0;
        int itemsPerRow = 4;
        
        for (int i = 0; i < items.size(); i++) {
            String text = items.get(i);
            float textWidth;
            float capsuleWidth;
            
            if (text.equals("LOGO")) {
                // Для логотипа: буква D + текст "Dexum"
                float dWidth = Fonts.SEMIBOLD.getWidth("D", logoFontSize);
                float dexumWidth = Fonts.MEDIUM.getWidth("exum", fontSize);
                capsuleWidth = dWidth + dexumWidth + capsulePadding * 2;
            } else {
                textWidth = Fonts.MEDIUM.getWidth(text, fontSize);
                capsuleWidth = textWidth + capsulePadding * 2;
            }
            
            // Рисуем капсулу
            DrawUtil.drawRoundedRect(ctx.getMatrices(), currentX, currentY, capsuleWidth, capsuleHeight,
                BorderRadius.all(capsuleHeight / 2), hudBg.withAlpha(200));
            
            // Рисуем содержимое
            if (text.equals("LOGO")) {
                // Рисуем букву "D" цветом темы
                float dWidth = Fonts.SEMIBOLD.getWidth("D", logoFontSize);
                float logoY = currentY + (capsuleHeight - logoFontSize) / 2 + 1.0f;
                ctx.drawText(Fonts.SEMIBOLD.getFont(logoFontSize), "D", 
                    currentX + capsulePadding, logoY, themeColor);
                
                // Рисуем "exum" белым
                float contentY = currentY + (capsuleHeight - fontSize) / 2 + 0.5f;
                ctx.drawText(Fonts.MEDIUM.getFont(fontSize), "exum", 
                    currentX + capsulePadding + dWidth, contentY, ColorRGBA.WHITE);
            } else {
                // Обычный текст
                float contentY = currentY + (capsuleHeight - fontSize) / 2 + 0.5f;
                ctx.drawText(Fonts.MEDIUM.getFont(fontSize), text, 
                    currentX + capsulePadding, contentY, ColorRGBA.WHITE);
            }
            
            currentX += capsuleWidth + capsuleGap;
            maxWidth = Math.max(maxWidth, currentX - posX - capsuleGap);
            
            // Переход на новую строку
            if ((i + 1) % itemsPerRow == 0 && i + 1 < items.size()) {
                currentX = posX;
                currentY += capsuleHeight + rowGap;
            }
        }

        int rows = (int)Math.ceil((double)items.size() / itemsPerRow);
        this.width = maxWidth;
        this.height = rows * capsuleHeight + (rows - 1) * rowGap;
    }

    private int getPlayerPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}