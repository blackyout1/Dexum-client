package wtf.dexum.utility.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import lombok.Generated;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.Gradient;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.base.color.ColorUtil;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public final class Render2DUtil implements IMinecraft {
   public static HashMap<Render2DUtil.GlowKey, Render2DUtil.GlowRect> glowCache = new HashMap();
   public static HashMap<Integer, Render2DUtil.GlowRect> shadowCache1 = new HashMap();
   static final Stack<Render2DUtil.Rectangle> clipStack = new Stack();
   private static final List<Render2DUtil.Quad> QUAD = new ArrayList();
   private static final ExecutorService GLOW_GENERATOR = Executors.newSingleThreadExecutor();

   public static void endScissor() {
      RenderSystem.disableScissor();
   }

   public static void setRectPoints(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color c1, Color c2, Color c3, Color c4) {
      bufferBuilder.vertex(matrix, x, y1, 0.0F).color(c1.getRGB());
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(c2.getRGB());
      bufferBuilder.vertex(matrix, x1, y, 0.0F).color(c3.getRGB());
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(c4.getRGB());
   }

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
   }

   public static void drawBlurredShadow(MatrixStack matrices, float x, float y, float width, float height, int blurRadius, Color color) {
   }

   public static void onRender(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      if (!QUAD.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD.forEach((quad) -> {
            quad(matrix4f, buffer, quad.x, quad.y, quad.width, quad.height, quad.color);
         });
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.disableBlend();
         QUAD.clear();
      }

   }

   public static void drawQuad(float x, float y, float width, float height, int color) {
      QUAD.add(new Render2DUtil.Quad(x, y, width, height, ColorUtil.multAlpha(color, RenderSystem.getShaderColor()[3])));
   }

   public static void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height) {
      buffer.vertex(matrix4f, x, y, 0.0F);
      buffer.vertex(matrix4f, x, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y, 0.0F);
   }

   public static void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix4f, x, y, 0.0F).color(color);
      buffer.vertex(matrix4f, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).color(color);
   }

   public static void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x, y, 0.0F).texture(1.0F, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public static void quadTexture(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x, y, 0.0F).texture(1.0F, 0.0F).color(color);
   }

   private static Render2DUtil.GlowKey findSimilarKey(int width, int height, int blurRadius) {
      Render2DUtil.GlowKey closest = null;
      int minDiff = Integer.MAX_VALUE;
      Iterator var5 = glowCache.keySet().iterator();

      while(var5.hasNext()) {
         Render2DUtil.GlowKey k = (Render2DUtil.GlowKey)var5.next();
         int diff = Math.abs(k.width() - width) + Math.abs(k.height() - height) + Math.abs(k.blurRadius() - blurRadius);
         if (diff < minDiff) {
            minDiff = diff;
            closest = k;
         }
      }

      return closest;
   }

   public static void drawGradientBlurredShadow(MatrixStack matrices, float x, float y, float width, float height, int blurRadius, Gradient gradient) {
      width += (float)(blurRadius * 2);
      height += (float)(blurRadius * 2);
      x -= (float)blurRadius;
      y -= (float)blurRadius;
      Render2DUtil.GlowKey existing = findSimilarKey((int)width, (int)height, blurRadius);
      if (existing == null || (int)(Math.abs((float)existing.width() - width) + Math.abs((float)existing.height() - height) + (float)Math.abs(existing.blurRadius() - blurRadius)) >= 5) {
         Render2DUtil.GlowKey key = new Render2DUtil.GlowKey((int)width, (int)height, blurRadius);
         mc.execute(() -> {
            BufferedImage original = new BufferedImage(key.width(), key.height(), 2);
            Graphics g = original.getGraphics();
            g.setColor(new Color(-1));
            g.fillRect(blurRadius, blurRadius, key.width() - blurRadius * 2, key.height() - blurRadius * 2);
            g.dispose();
            GaussianFilter op = new GaussianFilter((float)blurRadius);
            BufferedImage blurred = op.filter(original, (BufferedImage)null);
            glowCache.put(key, new Render2DUtil.GlowRect(blurred));
         });
      }

      Render2DUtil.GlowRect glowRect = (Render2DUtil.GlowRect)glowCache.getOrDefault(existing, null);
      if (glowRect != null) {
         glowRect.reset();
         DrawUtil.drawTexture(matrices, glowRect.id.getId(), x, y, width, height, gradient);
      }
   }

   public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(bi, "png", baos);
         byte[] bytes = baos.toByteArray();
         registerTexture(i, bytes);
      } catch (Exception var4) {
      }

   }

   public static void registerTexture(Texture i, byte[] content) {
      try {
         ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
         data.flip();
         NativeImageBackedTexture tex = new NativeImageBackedTexture(NativeImage.read(data));
         mc.execute(() -> {
            mc.getTextureManager().registerTexture(i.getId(), tex);
         });
      } catch (Exception var4) {
      }

   }

   public static void renderTexture(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
      double x1 = x0 + width;
      double y1 = y0 + height;
      double z = 0.0D;
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      buffer.vertex(matrix, (float)x0, (float)y1, (float)z).texture(u / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
      buffer.vertex(matrix, (float)x1, (float)y1, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight);
      buffer.vertex(matrix, (float)x1, (float)y0, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, v / (float)textureHeight);
      buffer.vertex(matrix, (float)x0, (float)y0, (float)z).texture(u / (float)textureWidth, (v + 0.0F) / (float)textureHeight);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public static void renderGradientTexture(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      renderGradientTextureInternal(buffer, matrices, x0, y0, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, c1, c2, c3, c4);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public static void renderGradientTextureInternal(BufferBuilder buff, MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
      double x1 = x0 + width;
      double y1 = y0 + height;
      double z = 0.0D;
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      buff.vertex(matrix, (float)x0, (float)y1, (float)z).texture(u / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight).color(c1.getRGB());
      buff.vertex(matrix, (float)x1, (float)y1, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, (v + (float)regionHeight) / (float)textureHeight).color(c2.getRGB());
      buff.vertex(matrix, (float)x1, (float)y0, (float)z).texture((u + (float)regionWidth) / (float)textureWidth, v / (float)textureHeight).color(c3.getRGB());
      buff.vertex(matrix, (float)x0, (float)y0, (float)z).texture(u / (float)textureWidth, (v + 0.0F) / (float)textureHeight).color(c4.getRGB());
   }

   public static void setupRender() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawTracerPointer(MatrixStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
   }

   public static void drawNewArrow(MatrixStack matrices, float x, float y, float size, Color color) {
   }

   public static void drawDefaultArrow(MatrixStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
      if (glow) {
         drawBlurredShadow(matrices, x - size * tracerWidth, y, x + size * tracerWidth - (x - size * tracerWidth), size, 10, injectAlpha(new Color(color), 140));
      }

      matrices.push();
      setupRender();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x - size * tracerWidth, y + size, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y + size - downHeight, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      color = darker(new Color(color), 0.8F).getRGB();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y + size - downHeight, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x + size * tracerWidth, y + size, 0.0F).color(color);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
      if (down) {
         color = darker(new Color(color), 0.6F).getRGB();
         bufferBuilder.vertex(matrix, x - size * tracerWidth, y + size, 0.0F).color(color);
         bufferBuilder.vertex(matrix, x + size * tracerWidth, y + size, 0.0F).color(color);
         bufferBuilder.vertex(matrix, x, y + size - downHeight, 0.0F).color(color);
         bufferBuilder.vertex(matrix, x - size * tracerWidth, y + size, 0.0F).color(color);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      endRender();
      matrices.pop();
   }

   public static void endRender() {
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static float scrollAnimate(float endPoint, float current, float speed) {
      boolean shouldContinueAnimation = endPoint > current;
      if (speed < 0.0F) {
         speed = 0.0F;
      } else if (speed > 1.0F) {
         speed = 1.0F;
      }

      float dif = Math.max(endPoint, current) - Math.min(endPoint, current);
      float factor = dif * speed;
      return current + (shouldContinueAnimation ? factor : -factor);
   }

   public static Color injectAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public static Color TwoColoreffect(Color cl1, Color cl2, double speed, double count) {
      int angle = (int)(((double)System.currentTimeMillis() / speed + count) % 360.0D);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      return interpolateColorC(cl1, cl2, (float)angle / 360.0F);
   }

   public static Color astolfo(boolean clickgui, int yOffset) {
      float speed = clickgui ? 3500.0F : 3000.0F;
      float hue = (float)(System.currentTimeMillis() % (long)((int)speed) + (long)yOffset);
      if (hue > speed) {
         hue -= speed;
      }

      hue /= speed;
      if (hue > 0.5F) {
         hue = 0.5F - (hue - 0.5F);
      }

      hue += 0.5F;
      return Color.getHSBColor(hue, 0.4F, 1.0F);
   }

   public static Color rainbow(int delay, float saturation, float brightness) {
      double rainbow = Math.ceil((double)((float)(System.currentTimeMillis() + (long)delay) / 16.0F));
      rainbow %= 360.0D;
      return Color.getHSBColor((float)(rainbow / 360.0D), saturation, brightness);
   }

   public static Color skyRainbow(int speed, int index) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      return Color.getHSBColor((double)((float)((double)(angle %= 360) / 360.0D)) < 0.5D ? -((float)((double)angle / 360.0D)) : (float)((double)angle / 360.0D), 0.5F, 1.0F);
   }

   public static Color getAnalogousColor(Color color) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
      float degree = 0.84F;
      float newHueSubtracted = hsb[0] - degree;
      return new Color(Color.HSBtoRGB(newHueSubtracted, hsb[1], hsb[2]));
   }

   public static Color applyOpacity(Color color, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity));
   }

   public static int applyOpacity(int color_int, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      Color color = new Color(color_int);
      return (new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity))).getRGB();
   }

   public static Color darker(Color color, float factor) {
      return new Color(Math.max((int)((float)color.getRed() * factor), 0), Math.max((int)((float)color.getGreen() * factor), 0), Math.max((int)((float)color.getBlue() * factor), 0), color.getAlpha());
   }

   public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      float hue = (float)angle / 360.0F;
      Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int)(opacity * 255.0F))));
   }

   public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      return trueColor ? interpolateColorHue(start, end, (float)angle / 360.0F) : interpolateColorC(start, end, (float)angle / 360.0F);
   }

   public static Color interpolateColorC(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      return new Color(interpolateInt(color1.getRed(), color2.getRed(), (double)amount), interpolateInt(color1.getGreen(), color2.getGreen(), (double)amount), interpolateInt(color1.getBlue(), color2.getBlue(), (double)amount), interpolateInt(color1.getAlpha(), color2.getAlpha(), (double)amount));
   }

   public static Color interpolateColorHue(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), (float[])null);
      float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), (float[])null);
      Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], (double)amount), interpolateFloat(color1HSB[1], color2HSB[1], (double)amount), interpolateFloat(color1HSB[2], color2HSB[2], (double)amount));
      return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), interpolateInt(color1.getAlpha(), color2.getAlpha(), (double)amount));
   }

   public static double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
      return (float)interpolate((double)oldValue, (double)newValue, (double)((float)interpolationValue));
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return (int)interpolate((double)oldValue, (double)newValue, (double)((float)interpolationValue));
   }

   public static BufferBuilder preShaderDraw(MatrixStack matrices, float x, float y, float width, float height) {
      setupRender();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION);
      setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
      return buffer;
   }

   public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
      buffer.vertex(matrix, x, y, 0.0F);
      buffer.vertex(matrix, x, y1, 0.0F);
      buffer.vertex(matrix, x1, y1, 0.0F);
      buffer.vertex(matrix, x1, y, 0.0F);
   }

   public static boolean isDark(Color color) {
      return isDark((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F);
   }

   public static boolean isDark(float r, float g, float b) {
      return colorDistance(r, g, b, 0.0F, 0.0F, 0.0F) < colorDistance(r, g, b, 1.0F, 1.0F, 1.0F);
   }

   public static float colorDistance(float r1, float g1, float b1, float r2, float g2, float b2) {
      float a = r2 - r1;
      float b = g2 - g1;
      float c = b2 - b1;
      return (float)Math.sqrt((double)(a * a + b * b + c * c));
   }

   @NotNull
   public static Color getColor(@NotNull Color start, @NotNull Color end, float progress, boolean smooth) {
      if (!smooth) {
         return (double)progress >= 0.95D ? end : start;
      } else {
         int rDiff = end.getRed() - start.getRed();
         int gDiff = end.getGreen() - start.getGreen();
         int bDiff = end.getBlue() - start.getBlue();
         int aDiff = end.getAlpha() - start.getAlpha();
         return new Color(fixColorValue(start.getRed() + (int)((float)rDiff * progress)), fixColorValue(start.getGreen() + (int)((float)gDiff * progress)), fixColorValue(start.getBlue() + (int)((float)bDiff * progress)), fixColorValue(start.getAlpha() + (int)((float)aDiff * progress)));
      }
   }

   private static int fixColorValue(int colorVal) {
      return colorVal > 255 ? 255 : Math.max(colorVal, 0);
   }

   public static void endBuilding(BufferBuilder bb) {
      BuiltBuffer builtBuffer = bb.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

   }

   public static void drawTexture(DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor) {
      drawTexture(context, id, x, y, size, round, uvSize, regionSize, textureSize, backgroundColor, -1);
   }

   public static void drawTexture(DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor, int foregroundColor) {
      MatrixStack matrix = context.getMatrices();
      if (id != null) {
         if (round > 0.0F) {
            DrawUtil.drawRoundedTextureWithUV(matrix, id, x, y, size, size, BorderRadius.all(round), new ColorRGBA(foregroundColor == -1 ? -1 : foregroundColor), 0.0F, 0.0F, 1.0F, 1.0F);
         } else {
            matrix.push();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, id);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix4f = matrix.peek().getPositionMatrix();
            int color = foregroundColor == -1 ? -1 : foregroundColor;
            buffer.vertex(matrix4f, x, y + size, 0.0F).texture(0.0F, 1.0F).color(color);
            buffer.vertex(matrix4f, x + size, y + size, 0.0F).texture(1.0F, 1.0F).color(color);
            buffer.vertex(matrix4f, x + size, y, 0.0F).texture(1.0F, 0.0F).color(color);
            buffer.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
            matrix.pop();
         }
      }

   }

   private static void drawTextureInternal(MatrixStack matrix, Identifier texture, int x, int y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
      drawTextureInternal(matrix, texture, (float)x, (float)x + width, (float)y, (float)y + height, 0.0F, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
   }

   private static void drawTextureInternal(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float z, int regionWidth, int regionHeight, float u, float v, int textureWidth, int textureHeight, int color) {
      drawTexturedQuad(matrix, texture, x1, x2, y1, y2, (u + 0.0F) / (float)textureWidth, (u + (float)regionWidth) / (float)textureWidth, (v + 0.0F) / (float)textureHeight, (v + (float)regionHeight) / (float)textureHeight, color);
   }

   private static void drawTexturedQuad(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color) {
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      buffer.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1).color(color);
      buffer.vertex(matrix4f, x1, y2, 0.0F).texture(u1, v2).color(color);
      buffer.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
      buffer.vertex(matrix4f, x2, y1, 0.0F).texture(u2, v1).color(color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   @Generated
   private Render2DUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static record Quad(float x, float y, float width, float height, int color) {
      public Quad(float x, float y, float width, float height, int color) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         this.color = color;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }

      public int color() {
         return this.color;
      }
   }

   static record GlowKey(int width, int height, int blurRadius) {
      GlowKey(int width, int height, int blurRadius) {
         this.width = width;
         this.height = height;
         this.blurRadius = blurRadius;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public int blurRadius() {
         return this.blurRadius;
      }
   }

   public static class GlowRect {
      public final Texture id = new Texture("shadow_" + RandomStringUtils.randomAlphanumeric(8));
      private int ticksSinceUse = 0;

      public GlowRect(BufferedImage img) {
         Render2DUtil.registerBufferedImageTexture(this.id, img);
      }

      public void reset() {
         this.ticksSinceUse = 0;
      }

      public boolean tick() {
         return ++this.ticksSinceUse > 300;
      }

      public void destroy() {
         IMinecraft.mc.getTextureManager().destroyTexture(this.id.getId());
      }
   }

   public static record Rectangle(float x, float y, float x1, float y1) {
      public Rectangle(float x, float y, float x1, float y1) {
         this.x = x;
         this.y = y;
         this.x1 = x1;
         this.y1 = y1;
      }

      public boolean contains(double x, double y) {
         return x >= (double)this.x && x <= (double)this.x1 && y >= (double)this.y && y <= (double)this.y1;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float x1() {
         return this.x1;
      }

      public float y1() {
         return this.y1;
      }
   }

   public static class GlowRect2 {
      public final int id;
      private int ticksSinceUse = 0;

      public GlowRect2(int id) {
         this.id = id;
      }

      public void reset() {
         this.ticksSinceUse = 0;
      }

      public boolean tick() {
         return ++this.ticksSinceUse > 4;
      }
   }
}