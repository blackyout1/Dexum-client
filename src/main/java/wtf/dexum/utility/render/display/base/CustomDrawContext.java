package wtf.dexum.utility.render.display.base;

import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.MsdfRenderer;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.mixin.accessors.DrawContextAccessor;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class CustomDrawContext extends DrawContext implements IMinecraft {
   public CustomDrawContext(Immediate vertexConsumerProvider) {
      super(mc, vertexConsumerProvider);
   }

   public CustomDrawContext(DrawContext originalContext) {
      super(mc, ((DrawContextAccessor)originalContext).getVertexConsumers());
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, String text, float x, float y, Gradient color) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color, this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, Text text, float x, float y) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, Text text, float x, float y, float alpha) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, (int)alpha);
   }

   public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtil.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
   }

   public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
      DrawUtil.drawRect(this.getMatrices(), x, y, width, height, color);
   }

   public int drawTextWithBackground(TextRenderer textRenderer, Text text, int x, int y, int width, BorderRadius borderRadius, ColorRGBA textColor, ColorRGBA backgroundColor) {
      int var10001 = x - 3;
      int var10002 = y - 2;
      int var10003 = width + 6;
      Objects.requireNonNull(textRenderer);
      this.drawRoundedRect((float)var10001, (float)var10002, (float)var10003, 13.0F, borderRadius, backgroundColor);
      return this.drawText(textRenderer, text, x, y, textColor.getRGB(), true);
   }

   public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
      DrawUtil.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
   }

   public void drawRoundedCorner(float x, float y, float width, float height, float borderThikenes, float widthCorner, ColorRGBA color, BorderRadius radius) {
      width = (float)Math.round(width);
      height = (float)Math.round(height);
      this.enableScissor((int)Math.ceil((double)(x - 10.0F)), (int)(y - 10.0F), (int)(x + widthCorner), (int)(y + widthCorner));
      this.drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
      this.disableScissor();
      this.enableScissor((int)(x + width - widthCorner), (int)(y - 10.0F), (int)(x + width + 10.0F), (int)(y + widthCorner));
      this.drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
      this.disableScissor();
      this.enableScissor((int)(x - 10.0F), (int)(y + height - widthCorner), (int)(x + widthCorner), (int)(y + height + 10.0F));
      this.drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
      this.disableScissor();
      this.enableScissor((int)(x + width - widthCorner), (int)(y + height - widthCorner), (int)(x + width + 10.0F), (int)(y + height + 10.0F));
      this.drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
      this.disableScissor();
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      DrawUtil.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      DrawUtil.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
   }

   public void pushMatrix() {
      this.getMatrices().push();
   }

   public void popMatrix() {
      this.getMatrices().pop();
   }
}