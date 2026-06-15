package wtf.dexum.client.hud.elements.component;

import com.google.common.collect.Lists;
import net.minecraft.util.Identifier;
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

public class NotifyComponent extends DraggableHudElement {
    private final Animation toggleAnimation;
    private final List<BaseNotification> notifications;

    public NotifyComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.toggleAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.notifications = new ArrayList();
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
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        Font textFont = Fonts.REGULAR.getFont(6.75F);
        float notificationHeight = 18.0F;

        float y = (float)mc.getWindow().getScaledHeight() / 2.0F + 16.0F;

        for(BaseNotification n : Lists.reverse(this.notifications)) {
            float anim = n.alphaAnimation.getValue();
            if (anim > 0.001F) {
                y += 10.5F * anim;
                n.render(ctx, (float)mc.getWindow().getScaledWidth() / 2.0F, y - 4.0F, textFont, theme, notificationHeight, this);
                y += 8.0F * anim;
            }
        }
    }

    private static class ModuleNotification extends BaseNotification {
        final Module module;
        final boolean enabled;

        ModuleNotification(Module module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = 16.0F;
            float alpha = this.alphaAnimation.getValue();
            ColorRGBA themeColor = theme.getColor().withAlpha(alpha * 255.0F);
            ColorRGBA iconColor = this.enabled ? themeColor : new ColorRGBA(255, 76, 76, alpha * 255.0F);
            ColorRGBA statusColor = this.enabled ? themeColor : new ColorRGBA(255, 76, 76, alpha * 255.0F);
            ColorRGBA whiteColor = ColorRGBA.WHITE.withAlpha(alpha * 255.0F);

            String moduleName = " " + this.module.getName();
            String wasText = "    ";
            String statusText = this.enabled ? "Enabled!" : "Disabled!";

            float moduleNameWidth = textFont.width(moduleName);
            float wasTextWidth = textFont.width(wasText);
            float statusTextWidth = textFont.width(statusText);
            float width = iconBgWidth + 4.0F + moduleNameWidth + wasTextWidth + statusTextWidth + 10.0F;

            x -= width / 2.0F - 1.0F;

            DrawUtil.drawBlur(ctx.getMatrices(), x - 1.0F, y, width - 3.0F, notificationHeight, 15.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 1.0F, y, 17.0F, notificationHeight, BorderRadius.left(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 16.0F, y, width - 20.0F, notificationHeight, BorderRadius.right(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 125.0F));

            Font statusFont = Fonts.ICONS2.getFont(8.5F);
            String statusIcon = this.enabled ? "\uf058" : "\uf057";
            float iconX = x - 1.0F + (17.0F - statusFont.width(statusIcon)) / 2.0F;
            float iconY = y + (notificationHeight - statusFont.height()) / 2.0F;
            ctx.drawText(statusFont, statusIcon, iconX, iconY, iconColor);

            float textX = x + iconBgWidth + 8.0F;
            float textY = y + (notificationHeight - textFont.height()) / 2.0F;
            ctx.drawText(textFont, moduleName, textX - 4.0F, textY, whiteColor);
            ctx.drawText(textFont, wasText, textX - 4.2F + moduleNameWidth, textY, whiteColor);
            ctx.drawText(textFont, statusText, textX - 4.2F + moduleNameWidth + wasTextWidth, textY, statusColor);
        }
    }

    private static class TextNotification extends BaseNotification {
        final String icon;
        final Text text;

        TextNotification(String icon, Text text) {
            this.icon = icon;
            this.text = text;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = 16.0F;
            float alpha = this.alphaAnimation.getValue();
            ColorRGBA themeColor = theme.getColor().withAlpha(alpha * 255.0F);
            ColorRGBA whiteColor = ColorRGBA.WHITE.withAlpha(alpha * 255.0F);

            String displayText = " " + this.text.getString();
            float displayTextWidth = textFont.width(displayText);
            float width = iconBgWidth + 4.0F + displayTextWidth + 10.0F;

            x -= width / 2.0F - 1.0F;

            DrawUtil.drawBlur(ctx.getMatrices(), x - 1.0F, y, width - 3.0F, notificationHeight, 15.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 1.0F, y, 17.0F, notificationHeight, BorderRadius.left(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 16.0F, y, width - 20.0F, notificationHeight, BorderRadius.right(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 125.0F));

            Font iconFont = Fonts.ICONS2.getFont(8.5F);
            float iconX = x - 1.0F + (17.0F - iconFont.width(this.icon)) / 2.0F;
            float iconY = y + (notificationHeight - iconFont.height()) / 2.0F;
            ctx.drawText(iconFont, this.icon, iconX, iconY, themeColor);

            float textX = x + iconBgWidth + 8.0F;
            float textY = y + (notificationHeight - textFont.height()) / 2.0F;
            ctx.drawText(textFont, displayText, textX - 4.0F, textY, whiteColor);
        }
    }

    private static class TotemNotification extends BaseNotification {
        final String name;
        final boolean enchanted;

        TotemNotification(String icon, boolean enchanted) {
            this.name = icon;
            this.enchanted = enchanted;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = 16.0F;
            ColorRGBA themeColor = theme.getColor().withAlpha(this.alphaAnimation.getValue() * 255.0F);
            ColorRGBA whiteColor = ColorRGBA.WHITE.withAlpha(this.alphaAnimation.getValue() * 255.0F);
            String displayText = " " + this.name + " потерял тотем";
            float displayTextWidth = textFont.width(displayText);
            float width = iconBgWidth + 4.0F + displayTextWidth + 10.0F;
            Font iconFont = Fonts.ENERGY.getFont(6.75F);
            String icon = this.enchanted ? "\uf06a" : "\uf05a";
            x -= width / 2.0F - 1.0F;
            float alpha = this.alphaAnimation.getValue();
            DrawUtil.drawBlur(ctx.getMatrices(), x - 1.0F, y, width - 3.0F, notificationHeight, 15.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 1.0F, y, 17.0F, notificationHeight, BorderRadius.left(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 16.0F, y, width - 20.0F, notificationHeight, BorderRadius.right(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 125.0F));

            float iconX = x - 1.0F + (17.0F - 9.0F) / 2.0F;
            float iconY = y + (notificationHeight - 9.0F) / 2.0F;
            ctx.drawTexture(Identifier.of("minecraft", "textures/item/totem_of_undying.png"), iconX, iconY, 9.0F, 9.0F, ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            float textX = x + iconBgWidth + 8.0F;
            float textY = y + (notificationHeight - textFont.height()) / 2.0F;
            ctx.drawText(textFont, displayText, textX - 4.0F, textY, whiteColor);
        }
    }

    private static class ItemNotification extends BaseNotification {
        final String itemName;
        final String icon;

        ItemNotification(String itemName, String icon) {
            this.itemName = itemName;
            this.icon = icon;
        }

        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (this.timestamp == 0L) {
                this.timestamp = System.currentTimeMillis();
            }

            float iconBgWidth = 16.0F;
            float alpha = this.alphaAnimation.getValue();
            ColorRGBA themeColor = theme.getColor().withAlpha(alpha * 255.0F);
            ColorRGBA whiteColor = ColorRGBA.WHITE.withAlpha(alpha * 255.0F);

            String displayText = " " + this.itemName;
            float displayTextWidth = textFont.width(displayText);
            float width = iconBgWidth + 4.0F + displayTextWidth + 10.0F;

            x -= width / 2.0F - 1.0F;

            DrawUtil.drawBlur(ctx.getMatrices(), x - 1.0F, y, width - 3.0F, notificationHeight, 15.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 1.0F, y, 17.0F, notificationHeight, BorderRadius.left(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 255.0F));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 16.0F, y, width - 20.0F, notificationHeight, BorderRadius.right(5.0F, 5.0F), new ColorRGBA(0, 0, 0, alpha * 125.0F));

            Font iconFont = Fonts.ENERGY.getFont(8.5F);
            float iconX = x - 1.0F + (17.0F - iconFont.width(this.icon)) / 2.0F;
            float iconY = y + (notificationHeight - iconFont.height()) / 2.0F;
            ctx.drawText(iconFont, this.icon, iconX, iconY, themeColor);

            float textX = x + iconBgWidth + 8.0F;
            float textY = y + (notificationHeight - textFont.height()) / 2.0F;
            ctx.drawText(textFont, displayText, textX - 4.0F, textY, whiteColor);
        }
    }

    private abstract static class BaseNotification {
        long timestamp;
        boolean fadingOut = false;
        final Animation alphaAnimation;

        private BaseNotification() {
            this.alphaAnimation = new Animation(300L, Easing.CUBIC_OUT);
        }

        abstract void render(CustomDrawContext var1, float var2, float var3, Font var4, Theme var5, float var6, NotifyComponent var7);
    }
}