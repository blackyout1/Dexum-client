package wtf.dexum.client.hud.elements.component;

import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import java.util.Locale;

public class InformationComponent extends DraggableHudElement {
    private float width;
    private float height;

    public InformationComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    public void render(CustomDrawContext ctx) {
        if (mc.player != null) {
            double deltaX = mc.player.getX() - mc.player.prevX;
            double deltaZ = mc.player.getZ() - mc.player.prevZ;
            double bps = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * (double)20.0F;
            String bpsText = String.format(Locale.US, "%.2f", bps);
            float screenWidth = (float)mc.getWindow().getScaledWidth();
            float screenHeight = (float)mc.getWindow().getScaledHeight();
            float fontSize = 8.0F;
            float textWidth = Fonts.REGULAR.getWidth(bpsText, fontSize);
            float posX = screenWidth / 2.0F - textWidth / 2.0F;
            float posY = screenHeight / 2.0F - fontSize / 2.0F + 15.0F;
            ctx.drawText(Fonts.REGULAR.getFont(fontSize), bpsText, posX, posY, new ColorRGBA(255, 255, 255, 255));
            this.width = textWidth;
            this.height = fontSize;
        }
    }
}