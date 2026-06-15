package wtf.dexum.client.hud.elements.component;

import com.google.common.collect.Lists;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ChatScreen;

public class NotifyComponentV2 extends DraggableHudElement {
    private final Animation toggleAnimation;
    private final List<BaseNotification> notifications;

    public NotifyComponentV2(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.toggleAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.notifications = new ArrayList<>();
    }

    public void addNotification(Module module, boolean enabled) {
        this.notifications.addLast(new ModuleNotification(module, enabled));
    }

    public void addTextNotification(String icon, Text text) {
        this.notifications.addLast(new TextNotification(icon, text));
    }

    public void addTotemNotification(String name, boolean enchanted) {
        this.notifications.addLast(new TotemNotification(name, enchanted));
    }

    public void addItemNotification(String itemName, String icon) {
        this.notifications.addLast(new ItemNotification(itemName, icon));
    }

    public void render(CustomDrawContext ctx) {
        Iterator<BaseNotification> iterator = this.notifications.iterator();
        while(iterator.hasNext()) {
            BaseNotification notification = iterator.next();
            if (!notification.fadingOut && System.currentTimeMillis() - notification.timestamp > 2000L) {
                notification.fadingOut = true;
                notification.alphaAnimation.update(0.0F);
            }

            if (notification.fadingOut && notification.alphaAnimation.getValue() < 0.01F) {
                iterator.remove();
            } else {
                notification.alphaAnimation.update(notification.fadingOut ? 0.0F : 1.0F);
            }
        }

        this.toggleAnimation.update(mc.currentScreen instanceof ChatScreen || !this.notifications.isEmpty());
        if (this.toggleAnimation.getValue() < 0.01F) return;

        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        Font textFont = Fonts.REGULAR.getFont(6.75F);
        float notificationHeight = 18.0F;

        float currentY = this.getY();
        float maxW = 0;
        Align align = this.getAlign();
        boolean bottom = align == Align.BOTTOM_LEFT || align == Align.BOTTOM_CENTER || align == Align.BOTTOM_RIGHT;

        for (BaseNotification n : Lists.reverse(this.notifications)) {
            float anim = n.alphaAnimation.getValue();
            if (anim > 0.001F) {
                float currentW = renderNotification(ctx, this.getX(), currentY, textFont, theme, notificationHeight, n);
                maxW = Math.max(maxW, currentW);
                if (bottom) {
                    currentY -= (notificationHeight + 4.0F) * anim;
                } else {
                    currentY += (notificationHeight + 4.0F) * anim;
                }
            }
        }
        this.width = maxW > 0 ? maxW : 100.0F;
        this.height = Math.abs(currentY - this.getY());
    }

    private float renderNotification(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float height, BaseNotification n) {
        float alpha = n.alphaAnimation.getValue() * this.toggleAnimation.getValue();
        if (alpha < 0.001F) return 0.0F;

        String icon = "O";
        String mainText = "";
        String statusText = "";
        ColorRGBA statusColor = ColorRGBA.WHITE;
        ColorRGBA iconColor = theme.getColor().withAlpha((int)(255 * alpha));

        if (n instanceof ModuleNotification mn) {
            icon = mn.enabled ? "J" : "K";
            mainText = mn.module.getName() + " ";
            statusText = mn.enabled ? "Включен!" : "Выключен!";
            statusColor = mn.enabled ? new ColorRGBA(32, 255, 32, (int)(255 * alpha)) : new ColorRGBA(255, 32, 32, (int)(255 * alpha));
        } else if (n instanceof TextNotification tn) {
            icon = "O";
            mainText = tn.text.getString();
            statusColor = ColorRGBA.WHITE.withAlpha((int)(255 * alpha));
        } else if (n instanceof TotemNotification ttn) {
            icon = "O";
            mainText = ttn.name + " lost totem";
            statusColor = ColorRGBA.WHITE.withAlpha((int)(255 * alpha));
        } else if (n instanceof ItemNotification in) {
            icon = "O";
            mainText = in.itemName;
            statusColor = ColorRGBA.WHITE.withAlpha((int)(255 * alpha));
        }

        float iconSize = 13.0f;
        float iconW = Fonts.NURIKI.getWidth(icon, iconSize);
        float mainW = textFont.width(mainText);
        float statusW = textFont.width(statusText);
        float spacing = 4.0f;
        float padding = 6.0f;
        float totalWidth = padding * 2 + iconW + spacing + mainW + (statusText.isEmpty() ? 0 : spacing + statusW);

        float drawX = x;
        Align align = this.getAlign();
        if (align == Align.TOP_RIGHT || align == Align.CENTER_RIGHT || align == Align.BOTTOM_RIGHT) {
            drawX = x - totalWidth;
        } else if (align == Align.TOP_CENTER || align == Align.CENTER || align == Align.BOTTOM_CENTER) {
            drawX = x - totalWidth / 2f;
        }

        ColorRGBA bgColor = new ColorRGBA(0, 0, 0, (int)(255 * alpha));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), drawX, y, totalWidth, height, BorderRadius.all(2.5f), bgColor);

        float currentX = drawX + padding;
        ctx.drawText(Fonts.NURIKI.getFont(iconSize), icon, currentX, y + (height - 10.0f) / 2.0f, iconColor);
        currentX += iconW + spacing;

        ctx.drawText(textFont, mainText, currentX, y + (height - textFont.height()) / 2.0f, ColorRGBA.WHITE.withAlpha((int)(255 * alpha)));
        if (!statusText.isEmpty()) {
            currentX += mainW + spacing;
            ctx.drawText(textFont, statusText, currentX, y + (height - textFont.height()) / 2.0f, statusColor);
        }
        return totalWidth;
    }

    private static abstract class BaseNotification {
        long timestamp;
        boolean fadingOut = false;
        final Animation alphaAnimation;

        BaseNotification() {
            this.alphaAnimation = new Animation(300L, Easing.CUBIC_OUT);
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class ModuleNotification extends BaseNotification {
        final Module module;
        final boolean enabled;

        ModuleNotification(Module module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
        }
    }

    private static class TextNotification extends BaseNotification {
        final String icon;
        final Text text;

        TextNotification(String icon, Text text) {
            this.icon = icon;
            this.text = text;
        }
    }

    private static class TotemNotification extends BaseNotification {
        final String name;
        final boolean enchanted;

        TotemNotification(String name, boolean enchanted) {
            this.name = name;
            this.enchanted = enchanted;
        }
    }

    private static class ItemNotification extends BaseNotification {
        final String itemName;
        final String icon;

        ItemNotification(String itemName, String icon) {
            this.itemName = itemName;
            this.icon = icon;
        }
    }
}