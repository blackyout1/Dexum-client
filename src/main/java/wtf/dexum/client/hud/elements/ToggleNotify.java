package wtf.dexum.client.hud.elements;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.events.impl.other.EventModuleToggle;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class ToggleNotify implements IMinecraft {
    private Module lastModule;
    private boolean lastState;
    private long timestamp;
    private final Animation alpha = new Animation(200L, Easing.CUBIC_OUT);

    public ToggleNotify() {
        EventManager.register(this);
    }

    @EventTarget
    public void onToggle(EventModuleToggle event) {
        this.lastModule = event.getModule();
        this.lastState = event.isEnabled();
        this.timestamp = System.currentTimeMillis();
        this.alpha.setValue(0.0f);
        this.alpha.update(1.0f);
    }

    @EventTarget
    public void onRender(EventHudRender event) {
        if (lastModule == null) return;

        long diff = System.currentTimeMillis() - timestamp;
        if (diff > 1200L) {
            alpha.update(0.0f);
        }

        if (alpha.getValue() < 0.01f) return;

        CustomDrawContext ctx = event.getContext();
        float screenWidth = (float)mc.getWindow().getScaledWidth();
        float screenHeight = (float)mc.getWindow().getScaledHeight();

        String status = lastState ? " Включен!" : " Выключен!";
        String text = lastModule.getName() + status;
        float textWidth = Fonts.REGULAR.getWidth(text, 7.5f);
        float iconSize = 9.0f;
        String icon = lastState ? "B" : "A";
        float iconWidth = Fonts.NURIKI.getWidth(icon, iconSize);

        float notificationHeight = 18.0F;
        float iconBgWidth = 17.0F;
        float width = iconBgWidth + textWidth + 12.0F;

        float x = (screenWidth - width) / 2.0f;
        float y = (screenHeight - notificationHeight) / 2.0f + 40.0f;

        float alphaVal = alpha.getValue();
        int a = (int)(255 * alphaVal);
        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().withAlpha(a);

        DrawUtil.drawBlur(ctx.getMatrices(), x, y, width, notificationHeight, 15.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(a));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, iconBgWidth, notificationHeight, BorderRadius.left(5.0F, 5.0F), new ColorRGBA(0, 0, 0, a));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x + iconBgWidth, y, width - iconBgWidth, notificationHeight, BorderRadius.right(5.0F, 5.0F), new ColorRGBA(0, 0, 0, (int)(125 * alphaVal)));

        float iconX = x + (iconBgWidth - iconWidth) / 2.0F;
        float iconY = y + (notificationHeight - 7.0F) / 2.0F;
        ctx.drawText(Fonts.NURIKI.getFont(iconSize), icon, iconX, iconY + 0.5F, lastState ? themeColor : new ColorRGBA(255, 76, 76, a));

        float textX = x + iconBgWidth + 5.0F;
        float textY = y + (notificationHeight - 7.5F) / 2.0F;
        ctx.drawText(Fonts.REGULAR.getFont(7.5f), text, textX, textY, ColorRGBA.WHITE.withAlpha(a));
    }
}