package wtf.dexum.client.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

import java.util.List;

public class HudElementSettingsScreen extends Screen implements IMinecraft {
    private final DraggableHudElement element;
    private final List<Setting> settings;
    private static final float PANEL_WIDTH = 220;
    private static final float SETTING_HEIGHT = 25;
    private static final float PADDING = 10;

    public HudElementSettingsScreen(DraggableHudElement element) {
        super(Text.literal("HUD Element Settings"));
        this.element = element;
        this.settings = element.getSettings();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        CustomDrawContext ctx = new CustomDrawContext(context);
        
        float centerX = width / 2f;
        float centerY = height / 2f;
        float panelHeight = PADDING * 2 + settings.size() * SETTING_HEIGHT + 30;
        
        float panelX = centerX - PANEL_WIDTH / 2;
        float panelY = centerY - panelHeight / 2;
        
        // Фон панели
        DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, panelY, PANEL_WIDTH, panelHeight,
            BorderRadius.all(8), new ColorRGBA(30, 30, 35, 240));
        
        // Заголовок
        String title = element.getName() + " Settings";
        float titleWidth = Fonts.MEDIUM.getWidth(title, 9);
        ctx.drawText(Fonts.MEDIUM.getFont(9), title, 
            panelX + PANEL_WIDTH / 2 - titleWidth / 2, panelY + PADDING, ColorRGBA.WHITE);
        
        // Настройки
        float currentY = panelY + PADDING + 20;
        
        for (Setting setting : settings) {
            if (setting instanceof BooleanSetting boolSetting) {
                renderBooleanSetting(ctx, boolSetting, panelX + PADDING, currentY, 
                    PANEL_WIDTH - PADDING * 2, mouseX, mouseY);
                currentY += SETTING_HEIGHT;
            }
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderBooleanSetting(CustomDrawContext ctx, BooleanSetting setting, 
                                     float x, float y, float width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && 
                         mouseY >= y && mouseY <= y + SETTING_HEIGHT;
        
        // Фон при наведении
        if (hovered) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, SETTING_HEIGHT,
                BorderRadius.all(4), new ColorRGBA(50, 50, 55, 200));
        }
        
        // Название
        ctx.drawText(Fonts.MEDIUM.getFont(7.5f), setting.getName(), 
            x + 5, y + 8, ColorRGBA.WHITE);
        
        // Переключатель
        float toggleWidth = 35;
        float toggleHeight = 16;
        float toggleX = x + width - toggleWidth - 5;
        float toggleY = y + (SETTING_HEIGHT - toggleHeight) / 2;
        
        ColorRGBA toggleColor = setting.isEnabled() 
            ? new ColorRGBA(52, 199, 89, 255)  // Зеленый iOS
            : new ColorRGBA(60, 60, 67, 255);   // Серый iOS
        
        DrawUtil.drawRoundedRect(ctx.getMatrices(), toggleX, toggleY, toggleWidth, toggleHeight,
            BorderRadius.all(toggleHeight / 2), toggleColor);
        
        // Кружок переключателя
        float circleSize = 12;
        float circleX = setting.isEnabled() 
            ? toggleX + toggleWidth - circleSize - 2
            : toggleX + 2;
        float circleY = toggleY + (toggleHeight - circleSize) / 2;
        
        DrawUtil.drawRoundedRect(ctx.getMatrices(), circleX, circleY, circleSize, circleSize,
            BorderRadius.all(circleSize / 2), ColorRGBA.WHITE);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float centerX = width / 2f;
            float centerY = height / 2f;
            float panelHeight = PADDING * 2 + settings.size() * SETTING_HEIGHT + 30;
            
            float panelX = centerX - PANEL_WIDTH / 2;
            float panelY = centerY - panelHeight / 2;
            float currentY = panelY + PADDING + 20;
            
            for (Setting setting : settings) {
                if (setting instanceof BooleanSetting boolSetting) {
                    float x = panelX + PADDING;
                    float width = PANEL_WIDTH - PADDING * 2;
                    
                    if (mouseX >= x && mouseX <= x + width && 
                        mouseY >= currentY && mouseY <= currentY + SETTING_HEIGHT) {
                        boolSetting.toggle();
                        return true;
                    }
                    currentY += SETTING_HEIGHT;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public void close() {
        if (mc.player != null) {
            mc.setScreen(null);
        }
        super.close();
    }
}
