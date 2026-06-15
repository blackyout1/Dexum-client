package wtf.dexum.client.hud.elements.component;

import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayListComponent extends DraggableHudElement {
    private final Animation widthAnimation;
    private final Animation alpha;

    public ArrayListComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
    }

    @Override
    public void render(CustomDrawContext ctx) {
        List<Module> activeModules = Dexum.getInstance().getModuleManager().getModules().stream()
                .filter(m -> m.getAnimation().getValue() > 0.001f)
                .sorted(Comparator.comparingDouble(m -> -Fonts.REGULAR.getWidth(m.getName(), 8.0F)))
                .collect(Collectors.toList());

        boolean isFound = !activeModules.isEmpty();
        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) return;

        float posX = this.getX();
        float posY = this.getY();
        float currentY = posY;
        float maxWidth = 0;

        for (Module module : activeModules) {
            float textWidth = Fonts.REGULAR.getWidth(module.getName(), 8.0F);
            maxWidth = Math.max(maxWidth, textWidth + 8.0F);
        }

        this.width = maxWidth;

        float blurY = posY;
        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            float anim = module.getAnimation().getValue();
            if (anim > 0.01F) {
                float textWidth = Fonts.REGULAR.getWidth(module.getName(), 8.0F);
                float width = textWidth + 8.0F;
                float height = 12.0F * anim;
                float x = posX + (this.width - width);
                float currentAlpha = this.alpha.getValue() * anim;

                float r = 2.0F;
                BorderRadius radius = new BorderRadius(r, (i == 0) ? r : 0.0F, (i == activeModules.size() - 1) ? r : 0.0F, r);

                DrawUtil.drawBlur(ctx.getMatrices(), x, blurY, width, height, 15.0F, radius, ColorRGBA.WHITE.withAlpha(currentAlpha * 255.0F));
                blurY += height;
            }
        }

        float rectY = posY;
        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            float anim = module.getAnimation().getValue();
            if (anim > 0.01F) {
                float textWidth = Fonts.REGULAR.getWidth(module.getName(), 8.0F);
                float width = textWidth + 8.0F;
                float height = 12.0F * anim;
                float x = posX + (this.width - width);
                float currentAlpha = this.alpha.getValue() * anim;

                float r = 2.0F;
                BorderRadius radius = new BorderRadius(r, (i == 0) ? r : 0.0F, (i == activeModules.size() - 1) ? r : 0.0F, r);

                DrawUtil.drawRoundedRect(ctx.getMatrices(), x, rectY, width, height, radius, new ColorRGBA(0, 0, 0, currentAlpha * 125.0F));
                rectY += height;
            }
        }

        float textY = posY;
        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            float anim = module.getAnimation().getValue();
            if (anim > 0.01F) {
                float textWidth = Fonts.REGULAR.getWidth(module.getName(), 8.0F);
                float width = textWidth + 8.0F;
                float height = 12.0F * anim;
                float x = posX + (this.width - width);
                float currentAlpha = this.alpha.getValue() * anim;

                ColorRGBA textC = ColorRGBA.WHITE.withAlpha((int) (255 * currentAlpha));
                Font font = Fonts.REGULAR.getFont(8.0F);
                ctx.drawText(font, module.getName(), x + 4.0F, textY + (height - font.height()) / 2.0F + 1.0F, textC);

                textY += height;
            }
        }

        this.height = textY - posY;
    }
}