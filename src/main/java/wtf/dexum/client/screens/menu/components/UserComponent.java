package wtf.dexum.client.screens.menu.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.discord.DiscordManager;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.font.MsdfRenderer;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class UserComponent {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public void render(DrawContext context, float x, float y, float alpha) {
      CustomDrawContext drawContext = CustomDrawContext.of(context);
      Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
      DiscordManager discord = Dexum.getInstance().getDiscordManager();
      Identifier skinTexture = mc.player != null && mc.player.getSkinTextures() != null ? mc.player.getSkinTextures().texture() : null;
      String username = "pasterok";
      float avatarSize = 24.0F;
      float avatarY = y - 22.0F;
      float maxWidth;

      if (skinTexture != null) {
         DrawUtil.drawPlayerHeadWithRoundedShader(context.getMatrices(), skinTexture, x - 4.1F, avatarY, avatarSize, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(alpha * 255.0F));
      } else {
         drawContext.drawText(Fonts.ICONS.getFont(12.0F), "", x + 2.5F, avatarY + 2.5F, theme.getColor().withAlpha(alpha * 255.0F));
      }

      maxWidth = 49.0F;
      MsdfRenderer.renderText(Fonts.REGULAR.getFont(8.0F).getFont(), username, 8.0F, theme.getColor().withAlpha(alpha * 255.0F).getRGB(), context.getMatrices().peek().getPositionMatrix(), x + 24.0F, avatarY + 6.0F, 0.0F, true, 0.7F, 1.0F, maxWidth);
      String uid = "[UID 228]";
      drawContext.drawText(Fonts.REGULAR.getFont(6.0F), uid, x + 24.0F, avatarY + 14.5F, new ColorRGBA(155, 155, 155, alpha * 255.0F));
   }

   private void drawTextureWithRound(DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int color) {
      MatrixStack matrix = context.getMatrices();
      CustomDrawContext drawContext = CustomDrawContext.of(context);
      if (id != null) {
         if (round > 0.0F) {
            DrawUtil.drawRoundedRect(matrix, x, y, size, size, BorderRadius.all(round), new ColorRGBA(0, 0, 0, 255));
         }

         matrix.push();
         matrix.translate(x, y, 0.0F);
         matrix.scale(size, size, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         this.drawTexture(matrix, id, 0, 0, 1.0F, 1.0F, (float)uvSize, (float)uvSize, regionSize, regionSize, textureSize, textureSize, color);
         RenderSystem.disableBlend();
         matrix.translate(-x, -y, 0.0F);
         matrix.pop();
      }

   }

   private void drawTexture(MatrixStack matrix, Identifier texture, int x, int y, float width, float height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
      this.drawTexture(matrix, texture, (float)x, (float)x + width, (float)y, (float)y + height, 0.0F, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
   }

   private void drawTexture(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float z, int regionWidth, int regionHeight, float u, float v, int textureWidth, int textureHeight, int color) {
      this.drawTexturedQuad(matrix, texture, x1, x2, y1, y2, (u + 0.0F) / (float)textureWidth, (u + (float)regionWidth) / (float)textureWidth, (v + 0.0F) / (float)textureHeight, (v + (float)regionHeight) / (float)textureHeight, color);
   }

   private void drawTexturedQuad(MatrixStack matrix, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color) {
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
}