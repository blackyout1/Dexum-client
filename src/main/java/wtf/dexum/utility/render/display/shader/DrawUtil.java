package wtf.dexum.utility.render.display.shader;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.utility.interfaces.IWindow;
import wtf.dexum.utility.math.MathUtil;
import wtf.dexum.utility.render.display.Render2DUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomSprite;
import wtf.dexum.utility.render.display.base.Gradient;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

public final class DrawUtil implements IWindow {
   public static final float DEFAULT_SMOOTHNESS = 0.8F;
   public static GlProgram rectangleProgram;
   private static GlProgram squircleProgram;
   private static GlProgram roundedTextureProgram;
   private static GlProgram squircleTextureProgram;
   private static GlProgram borderProgram;
   private static GlProgram figmaBorderProgram;
   private static GlProgram loadingProgram;
   private static GlProgram gradientRectangleProgram;
   private static GlProgram blurProgram;
   private static GlProgram metaballsProgram;
   private static final CustomRenderTarget buffer = new CustomRenderTarget(false);
   private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> {
      return new SimpleFramebuffer(1920, 1024, false);
   });
   private static final Framebuffer MAIN_FBO = MinecraftClient.getInstance().getFramebuffer();

   public static void initializeShaders() {
      rectangleProgram = new GlProgram(Dexum.id("rectangle/data"), VertexFormats.POSITION_COLOR);
      squircleProgram = new GlProgram(Dexum.id("squircle/data"), VertexFormats.POSITION_COLOR);
      squircleTextureProgram = new GlProgram(Dexum.id("squircle_texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      roundedTextureProgram = new GlProgram(Dexum.id("texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      borderProgram = new GlProgram(Dexum.id("border/data"), VertexFormats.POSITION_COLOR);
      figmaBorderProgram = new GlProgram(Dexum.id("corner/data"), VertexFormats.POSITION_COLOR);
      loadingProgram = new GlProgram(Dexum.id("loading/data"), VertexFormats.POSITION_COLOR);
      gradientRectangleProgram = new GlProgram(Dexum.id("gradient_rectangle/data"), VertexFormats.POSITION_COLOR);
      blurProgram = new GlProgram(Dexum.id("blur/data"), VertexFormats.POSITION_COLOR);
      metaballsProgram = new GlProgram(Dexum.id("metanoise/data"), VertexFormats.POSITION_COLOR);
   }

   public static void updateBuffer() {
      buffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.setup();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      mc.getFramebuffer().beginRead();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
      drawQuad(0.0F, 0.0F, (float)mw.getScaledWidth(), (float)mw.getScaledHeight(), true);
      mc.getFramebuffer().endRead();
      RenderSystem.disableBlend();
      mc.getFramebuffer().beginWrite(true);
      buffer.stop();
   }

   private static void drawQuad(float x, float y, float width, float height, boolean flip) {
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      int color = 0xFFFFFF;
      float vTop = flip ? 0.0F : 1.0F;
      float vBottom = flip ? 1.0F : 0.0F;
      builder.vertex(x, y, 0.0F).texture(0.0F, vBottom).color(-1);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, vTop).color(-1);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, vTop).color(-1);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, vBottom).color(-1);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, from.x, from.y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, to.x, to.y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }

   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

         for(int i = 0; i <= resolution; ++i) {
            float t = (float)i / (float)resolution;
            float x = (float)MathUtil.cubicBezier((double)t, (double)p0.x, (double)p1.x, (double)p2.x, (double)p3.x);
            float y = (float)MathUtil.cubicBezier((double)t, (double)p0.y, (double)p1.y, (double)p2.y, (double)p3.y);
            builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   private static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
      float u = 1.0F - t;
      float tt = t * t;
      float uu = u * u;
      return uu * u * p0 + 3.0F * uu * t * p1 + 3.0F * u * tt * p2 + tt * t * p3;
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      if (squircleProgram == null || squircleProgram.backingProgram == null) {
         drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
         return;
      }

      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      squircleProgram.use();
      squircleProgram.findUniform("Size").set(width, height);
      squircleProgram.findUniform("Radius").set(borderRadius.topLeftRadius() * squirt / 2.0F, borderRadius.bottomLeftRadius() * squirt / 2.0F, borderRadius.topRightRadius() * squirt / 2.0F, borderRadius.bottomRightRadius() * squirt / 2.0F);
      squircleProgram.findUniform("Smoothness").set(smoothness);
      squircleProgram.findUniform("CornerSmoothness").set(squirt);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawLoadingRect(MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
      if (loadingProgram == null || loadingProgram.backingProgram == null) {
         drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
         return;
      }

      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      loadingProgram.use();
      loadingProgram.findUniform("Size").set(width, height);
      loadingProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      loadingProgram.findUniform("Smoothness").set(smoothness);
      loadingProgram.findUniform("Progress").set(progress);
      loadingProgram.findUniform("StripeWidth").set(0.0F);
      loadingProgram.findUniform("Fade").set(0.5F);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      if (rectangleProgram == null || rectangleProgram.backingProgram == null) {

         drawRect(matrices, x, y, width, height, color);
         return;
      }

      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      rectangleProgram.use();
      rectangleProgram.findUniform("Size").set(width, height);
      rectangleProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      rectangleProgram.findUniform("Smoothness").set(smoothness);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color1, ColorRGBA color2, ColorRGBA color3, ColorRGBA color4) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      gradientRectangleProgram.use();
      gradientRectangleProgram.findUniform("Size").set(width, height);
      gradientRectangleProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      gradientRectangleProgram.findUniform("Smoothness").set(smoothness);
      gradientRectangleProgram.findUniform("TopLeftColor").set((float)color1.getRed() / 255.0F, (float)color1.getGreen() / 255.0F, (float)color1.getBlue() / 255.0F, (float)color1.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("BottomLeftColor").set((float)color2.getRed() / 255.0F, (float)color2.getGreen() / 255.0F, (float)color2.getBlue() / 255.0F, (float)color2.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("BottomRightColor").set((float)color3.getRed() / 255.0F, (float)color3.getGreen() / 255.0F, (float)color3.getBlue() / 255.0F, (float)color3.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("TopRightColor").set((float)color4.getRed() / 255.0F, (float)color4.getGreen() / 255.0F, (float)color4.getBlue() / 255.0F, (float)color4.getAlpha() / 255.0F);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color1.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color2.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color3.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color4.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      drawRoundedRect(matrices, x, y, width, height, borderRadius, gradient.getTopLeftColor(), gradient.getBottomLeftColor(), gradient.getBottomRightColor(), gradient.getTopRightColor());
   }

   public static void drawRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.8F;
      float externalSmoothness = 1.0F;
      borderProgram.use();
      borderProgram.findUniform("Size").set(width, height);
      borderProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
      borderProgram.findUniform("Thickness").set(borderThickness);
      drawSetup();
      float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
      float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawMetanoise(MatrixStack matrices, float x, float y, float w, float h, float time, float radius, ColorRGBA bgColor, ColorRGBA outlineColor) {
      if (metaballsProgram == null || metaballsProgram.backingProgram == null) {

         drawRoundedRect(matrices, x, y, w, h, BorderRadius.all(radius), bgColor);
         return;
      }

      matrices.push();
      Matrix4f mat = matrices.peek().getPositionMatrix();
      metaballsProgram.use();
      metaballsProgram.findUniform("Size").set(w, h);
      metaballsProgram.findUniform("Time").set(time);
      metaballsProgram.findUniform("BgColor").set((float)bgColor.getRed() / 255.0F, (float)bgColor.getGreen() / 255.0F, (float)bgColor.getBlue() / 255.0F, (float)bgColor.getAlpha() / 255.0F);
      metaballsProgram.findUniform("OutlineColor").set((float)outlineColor.getRed() / 255.0F, (float)outlineColor.getGreen() / 255.0F, (float)outlineColor.getBlue() / 255.0F, (float)outlineColor.getAlpha() / 255.0F);
      metaballsProgram.findUniform("Radius").set(radius, radius, radius, radius);
      metaballsProgram.findUniform("Smoothness").set(0.8F);
      drawSetup();
      BufferBuilder bb = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bb.vertex(mat, x, y, 0.0F).color(255, 255, 255, 255);
      bb.vertex(mat, x, y + h, 0.0F).color(255, 255, 255, 255);
      bb.vertex(mat, x + w, y + h, 0.0F).color(255, 255, 255, 255);
      bb.vertex(mat, x + w, y, 0.0F).color(255, 255, 255, 255);
      BufferRenderer.drawWithGlobalProgram(bb.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedCorner(MatrixStack matrices, float x, float y, float width, float height, float borderThikenes, float delta, ColorRGBA color, BorderRadius radius) {
      x -= 0.3F;
      y -= 0.3F;
      width += 0.6F;
      height += 0.6F;
      drawRoundedCornerOnly(matrices, x, y, delta, delta, borderThikenes, radius, color, 0.0F);
      drawRoundedCornerOnly(matrices, x + width - delta, y, delta, delta, borderThikenes, radius, color, 1.0F);
      drawRoundedCornerOnly(matrices, x, y + height - delta, delta, delta, borderThikenes, radius, color, 2.0F);
      drawRoundedCornerOnly(matrices, x + width - delta, y + height - delta, delta, delta, borderThikenes, radius, color, 3.0F);
   }

   public static void drawRoundedCornerOnly(MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor, float cornerIdex) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.8F;
      float externalSmoothness = 1.0F;
      figmaBorderProgram.use();
      figmaBorderProgram.findUniform("Size").set(width, height);
      figmaBorderProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      figmaBorderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
      figmaBorderProgram.findUniform("Thickness").set(borderThickness);
      figmaBorderProgram.findUniform("CornerIndex").set(cornerIdex);
      drawSetup();
      float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
      float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, Gradient textureColor) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getTopLeftColor().getRGB());
      builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getBottomLeftColor().getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getBottomRightColor().getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getTopRightColor().getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      matrices.push();
      int color = clor.getRGB();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float x2 = x + width;
      float y2 = y + height;
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
      builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
      builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
      builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
      RenderSystem.disableBlend();
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      drawTexture(matrices, sprite.getTexture(), x, y, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, ColorRGBA.WHITE);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      roundedTextureProgram.use();
      RenderSystem.setShaderTexture(0, identifier);
      roundedTextureProgram.findUniform("Size").set(width, height);
      roundedTextureProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      roundedTextureProgram.findUniform("Smoothness").set(smoothness);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      rectangleProgram.use();
      rectangleProgram.findUniform("Size").set(width, height);
      rectangleProgram.findUniform("Radius").set(borderRadius.topLeftRadius() * 3.0F, borderRadius.bottomLeftRadius() * 3.0F, borderRadius.topRightRadius() * 3.0F, borderRadius.bottomRightRadius() * 3.0F);
      rectangleProgram.findUniform("Smoothness").set(softness);
      drawSetup();
      float horizontalPadding = -softness / 2.0F + softness * 2.0F;
      float verticalPadding = softness / 2.0F + softness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawBlurHud(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
   }

   public static void drawBlurHudBooleanCheck(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color, boolean d, boolean f) {
   }

   public static void drawGlow(MatrixStack matrixStack, float x, float y, float width, float height, int glowRadius) {
      Render2DUtil.drawGradientBlurredShadow(matrixStack, x, y, width, height, glowRadius, Gradient.of(Dexum.getInstance().getThemeManager().getCurrentTheme().getColor(), Dexum.getInstance().getThemeManager().getCurrentTheme().getColor(), Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor(), Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor()));
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      Framebuffer MAIN_FBO = MinecraftClient.getInstance().getFramebuffer();
      SimpleFramebuffer fbo = (SimpleFramebuffer)TEMP_FBO_SUPPLIER.get();
      if (fbo.textureWidth != MAIN_FBO.textureWidth || fbo.textureHeight != MAIN_FBO.textureHeight) {
         fbo.resize(MAIN_FBO.textureWidth, MAIN_FBO.textureHeight);
      }

      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      fbo.beginWrite(false);
      MAIN_FBO.draw(fbo.textureWidth, fbo.textureHeight);
      MAIN_FBO.beginWrite(false);
      RenderSystem.setShaderTexture(0, fbo.getColorAttachment());
      blurProgram.use();
      blurProgram.findUniform("Size").set(width, height);
      blurProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      blurProgram.findUniform("Smoothness").set(1.0F);
      blurProgram.findUniform("BlurRadius").set(blurRadius);
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      float u = x / (float)screenWidth;
      float v = ((float)screenHeight - y - height) / (float)screenHeight;
      float texWidth = width / (float)screenWidth;
      float texHeight = height / (float)screenHeight;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      matrices.pop();
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawPlayerHeadWithRoundedShader(MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.125F, 0.125F, 0.25F, 0.25F);
   }

   private static void drawPlayerHatLayerWithRoundedShader(MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.625F, 0.125F, 0.75F, 0.25F);
      RenderSystem.disableBlend();
   }

   public static void drawRoundedTextureWithUV(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color, float u1, float v1, float u2, float v2) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.8F;
      roundedTextureProgram.use();
      RenderSystem.setShaderTexture(0, identifier);
      roundedTextureProgram.findUniform("Size").set(width, height);
      roundedTextureProgram.findUniform("Radius").set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      roundedTextureProgram.findUniform("Smoothness").set(smoothness);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u1, v1).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u1, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u2, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u2, v1).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void drawEnd() {
      RenderSystem.disableBlend();
   }

   @Generated
   private DrawUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static record HeadUV(float u1, float v1, float uSize, float vSize) {
      HeadUV(float u1, float v1, float uSize, float vSize) {
         this.u1 = u1;
         this.v1 = v1;
         this.uSize = uSize;
         this.vSize = vSize;
      }

      public float u1() {
         return this.u1;
      }

      public float v1() {
         return this.v1;
      }

      public float uSize() {
         return this.uSize;
      }

      public float vSize() {
         return this.vSize;
      }
   }
}