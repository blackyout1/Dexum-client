package wtf.dexum.client.hud.elements.component;

import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.mixin.accessors.DrawContextAccessor;
import wtf.dexum.utility.mixin.accessors.InGameHudAccessor;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.profiler.Profilers;
import ru.nexusguard.protection.annotations.Native;

public class HootBarComponent extends DraggableHudElement {
    List<HotBarSlot> slots = new ArrayList<>();

    public HootBarComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        float itemSize = 24.0F;
        this.width = itemSize * 9.0F;
        this.height = itemSize;

        for(int i = 0; i < 9; ++i) {
            this.slots.add(new HotBarSlot(this, i));
        }
    }

    @Native
    public void render(CustomDrawContext ctx) {
        this.x = ((float)ctx.getScaledWindowWidth() - this.width) / 2.0F;
        float posX = this.getX();
        float posY = this.getY();
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        if (mc.interactionManager.hasCreativeInventory()) {
            this.renderHeldItemTooltip(ctx, posY - 35.0F);
            this.renderOverlayMessage(ctx, mc.getRenderTickCounter(), posY - 35.0F - 9.0F);
            Font font = Fonts.MEDIUM.getFont(7.0F);
            int k = mc.player.experienceLevel;
            ctx.drawText(font, String.valueOf(k), posX + this.width / 2.0F - font.width(String.valueOf(k)) / 2.0F, posY - 15.0F + font.height() / 2.0F, ColorRGBA.GREEN);
            DrawUtil.drawBlurHud(ctx.getMatrices(), this.x, this.y, this.width, this.height, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
            ctx.drawRoundedRect(posX, posY, this.width, 24.0F, BorderRadius.all(4.0F), theme.getForegroundColor());
            ItemStack offHand = mc.player.getOffHandStack();
            if (!offHand.isEmpty()) {
                float xSlot = posX - this.height - 12.0F;
                DrawUtil.drawBlurHud(ctx.getMatrices(), xSlot, posY, this.height, this.height, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
                ctx.drawRoundedRect(xSlot, posY, this.height, this.height, BorderRadius.all(4.0F), theme.getForegroundColor());
                ctx.drawRoundedBorder(xSlot, posY, this.height, this.height, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
                DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - this.height - 12.0F, posY, this.height, this.height, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
                ctx.pushMatrix();
                ctx.getMatrices().translate((double)xSlot + 5.6, (double)posY + 5.6, (double)1.0F);
                ctx.getMatrices().scale(0.8F, 0.8F, 0.8F);
                ctx.drawItem(offHand, 0, 0);
                ((DrawContextAccessor)ctx).callDrawItemBar(offHand, 0, 0);
                ((DrawContextAccessor)ctx).callDrawCooldownProgress(offHand, 0, 0);
                ctx.popMatrix();
                if (offHand.getCount() > 1) {
                    String countText = "x" + String.valueOf(offHand.getCount());
                    float countWidth = font.width(countText);
                    float countX = xSlot + 24.0F - countWidth - 1.0F;
                    float countY = posY + 24.0F - font.height() - 3.0F;
                    ctx.drawText(font, countText, countX, countY, theme.getGray());
                }
            }

            float xSlot = posX;

            for(HotBarSlot slot : this.slots) {
                slot.render(ctx, xSlot, posY, theme);
                xSlot += this.height;
            }

            ctx.drawRoundedBorder(this.x, this.y, this.width, 24.0F, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
            DrawUtil.drawRoundedCorner(ctx.getMatrices(), this.x, this.y, this.width, 24.0F, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
        } else if (mc.interactionManager.hasStatusBars()) {
            ctx.pushMatrix();
            ctx.getMatrices().translate((float)(-(ctx.getScaledWindowWidth() / 2 - 91)), (float)(-(ctx.getScaledWindowHeight() - 39)), 0.0F);
            ctx.getMatrices().scale(1.0F, 1.0F, 1.0F);
            ctx.getMatrices().translate(posX, 0.0F, 0.0F);
            ctx.getMatrices().translate(0.0F, posY - 15.0F, 0.0F);
            if (!mc.interactionManager.hasCreativeInventory()) {
                ((InGameHudAccessor)mc.inGameHud).invokeRenderStatusBars(ctx);
            }

            ctx.popMatrix();
            this.renderHeldItemTooltip(ctx, posY - 35.0F);
            this.renderOverlayMessage(ctx, mc.getRenderTickCounter(), posY - 35.0F - 9.0F);
            Font font = Fonts.MEDIUM.getFont(7.0F);
            int k = mc.player.experienceLevel;
            ctx.drawText(font, String.valueOf(k), posX + this.width / 2.0F - font.width(String.valueOf(k)) / 2.0F, posY - 15.0F + font.height() / 2.0F, ColorRGBA.GREEN);
            DrawUtil.drawBlurHud(ctx.getMatrices(), this.x, this.y, this.width, this.height, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
            DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, this.width, 24.0F, 11.0F, BorderRadius.all(4.0F), new ColorRGBA(80, 80, 80, 255));
            ItemStack offHand = mc.player.getOffHandStack();
            if (!offHand.isEmpty()) {
                float xSlot = posX - this.height - 12.0F;
                DrawUtil.drawBlurHud(ctx.getMatrices(), xSlot, posY, this.height, this.height, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);
                ctx.drawRoundedRect(xSlot, posY, this.height, this.height, BorderRadius.all(4.0F), theme.getForegroundColor());
                ctx.drawRoundedBorder(xSlot, posY, this.height, this.height, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
                DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - this.height - 12.0F, posY, this.height, this.height, 0.1F, 15.0F, theme.getColor(), BorderRadius.all(4.0F));
                ctx.pushMatrix();
                ctx.getMatrices().translate((double)xSlot + 5.6, (double)posY + 5.6, (double)1.0F);
                ctx.getMatrices().scale(0.8F, 0.8F, 0.8F);
                ctx.drawItem(offHand, 0, 0);
                ((DrawContextAccessor)ctx).callDrawItemBar(offHand, 0, 0);
                ((DrawContextAccessor)ctx).callDrawCooldownProgress(offHand, 0, 0);
                ctx.popMatrix();
                if (offHand.getCount() > 1) {
                    String countText = "x" + String.valueOf(offHand.getCount());
                    float countWidth = font.width(countText);
                    float countX = xSlot + 24.0F - countWidth - 1.0F;
                    float countY = posY + 24.0F - font.height() - 3.0F;
                    ctx.drawText(font, countText, countX, countY, theme.getGray());
                }
            }

            float xSlot = posX;

            for(HotBarSlot slot : this.slots) {
                slot.render(ctx, xSlot, posY, theme);
                xSlot += this.height;
            }
        }
    }

    private void renderHeldItemTooltip(CustomDrawContext context, float y) {
        Profilers.get().push("selectedItemName");
        if (mc.inGameHud.heldItemTooltipFade > 0 && !mc.inGameHud.currentStack.isEmpty()) {
            Text mutableText = mc.inGameHud.currentStack.getName().copy().formatted(mc.inGameHud.currentStack.getRarity().getFormatting());
            if (mc.inGameHud.currentStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                mutableText.getStyle().withItalic(true);
            }

            int i = mc.textRenderer.getWidth(mutableText);
            int j = (context.getScaledWindowWidth() - i) / 2;
            int k = (int)y;
            if (!mc.interactionManager.hasStatusBars() || mc.interactionManager.hasCreativeInventory()) {
                k += 14;
            }

            int l = (int)((float)mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                context.getMatrices().push();
                context.getMatrices().translate((float)j, (float)k, 0.0F);
                Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
                context.drawTextWithBackground(mc.textRenderer, mutableText, 0, 0, i, ColorHelper.withAlpha(l, -1));
                context.getMatrices().pop();
            }
        }

        Profilers.get().pop();
    }

    public final void renderOverlayMessage(CustomDrawContext context, RenderTickCounter tickCounter, float y) {
        TextRenderer textRenderer = mc.textRenderer;
        if (mc.inGameHud.overlayMessage != null && mc.inGameHud.overlayRemaining > 0) {
            Profilers.get().push("overlayMessage");
            float f = (float)mc.inGameHud.overlayRemaining - tickCounter.getTickDelta(false);
            int i = (int)(f * 255.0F / 20.0F);
            if (i > 255) {
                i = 255;
            }

            if (i > 8) {
                context.getMatrices().push();
                context.getMatrices().translate((float)(context.getScaledWindowWidth() / 2), y, 0.0F);
                int j;
                if (mc.inGameHud.overlayTinted) {
                    j = MathHelper.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
                } else {
                    j = ColorHelper.withAlpha(i, -1);
                }

                int k = textRenderer.getWidth(mc.inGameHud.overlayMessage);
                context.getMatrices().translate((float)(-k) / 2.0F, -4.0F, 0.0F);
                context.drawTextWithBackground(textRenderer, mc.inGameHud.overlayMessage, 0, 0, k, j);
                context.getMatrices().pop();
            }

            Profilers.get().pop();
        }

    }

    protected void renderXLine(CustomDrawContext ctx, DraggableHudElement.SheetCode nearest) {
    }

    class HotBarSlot {
        private final Animation animationEnable;
        private final BorderRadius borderRadius;
        private final int index;

        public HotBarSlot(final HootBarComponent this$0x, int index) {
            this.animationEnable = new Animation(150L, 0.0F, Easing.QUAD_IN_OUT);
            this.borderRadius = index == 0 ? BorderRadius.left(4.0F, 4.0F) : (index == 8 ? BorderRadius.right(4.0F, 4.0F) : BorderRadius.ZERO);
            this.index = index;
        }

        public void render(CustomDrawContext ctx, float x, float y, Theme theme) {
            this.animationEnable.setDuration(80L);
            Font font = Fonts.MEDIUM.getFont(6.0F);
            this.animationEnable.update(this.index == IMinecraft.mc.player.getInventory().selectedSlot ? 1.0F : 0.0F);
            ColorRGBA bgColor = ColorUtil.interpolate(ColorRGBA.TRANSPARENT, theme.getColor(), this.animationEnable.getValue());
            ColorRGBA textColor = theme.getGray().mix(theme.getWhite(), this.animationEnable.getValue());
            ItemStack stack = IMinecraft.mc.player.getInventory().main.get(this.index);
            ctx.drawRoundedRect(x, y, 24.0F, 24.0F, this.borderRadius, bgColor);
            ctx.pushMatrix();
            ctx.getMatrices().translate((double)x + 5.6, (double)y + 5.6, (double)1.0F);
            ctx.getMatrices().scale(0.8F, 0.8F, 0.8F);
            ctx.drawItem(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
            ctx.popMatrix();
            ctx.drawText(font, String.valueOf(this.index + 1), x + 2.0F, y + 2.0F, textColor);
            if (stack.getCount() > 1) {
                String countText = "x" + String.valueOf(stack.getCount());
                float countWidth = font.width(countText);
                float countX = x + 24.0F - countWidth - 1.0F;
                float countY = y + 24.0F - font.height() - 3.0F;
                ctx.drawText(font, countText, countX, countY, textColor);
            }

        }
    }
}