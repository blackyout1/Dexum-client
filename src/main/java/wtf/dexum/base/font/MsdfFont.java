package wtf.dexum.base.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Generated;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.utility.game.other.ReplaceUtil;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.render.display.base.Gradient;

public final class MsdfFont implements IMinecraft {
   private final String name;
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Map<Integer, MsdfGlyph> glyphs;
   private final Map<Integer, Map<Integer, Float>> kernings;

   private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
      this.name = name;
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
   }

   public int getTextureId() {
      return this.texture.getGlId();
   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
      this.texture.setFilter(true, true);
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      boolean skipNext = false;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c == 7424) {
            c = 1040;
         }

         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  x += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevChar = c;
            }
         }
      }

   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, Gradient color) {
      this.texture.setFilter(true, true);
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      boolean skipNext = false;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  x += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevChar = c;
            }
         }
      }

   }

   public float getWidth(String text, float size) {
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      float width = 0.0F;
      boolean skipNext = false;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c == 7424) {
            c = 1040;
         }

         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  width += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               width += glyph.getWidth(size);
               prevChar = c;
            }
         }
      }

      return width;
   }

   public float getTextWidth(Text text, float size) {
      return this.getWidth(text.getString(), size);
   }

   public Font getFont(float size) {
      return new Font(this, size);
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   @Generated
   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public static class Builder {
      private String name = "?";
      private Identifier dataIdentifer;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String dataFileName) {
         this.dataIdentifer = Dexum.id("fonts/msdf/" + dataFileName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String atlasFileName) {
         this.atlasIdentifier = Dexum.id("fonts/msdf/" + atlasFileName + ".png");
         return this;
      }

      public MsdfFont build() {
         FontData data = (FontData)ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
         AbstractTexture texture = IMinecraft.mc.getTextureManager().getTexture(this.atlasIdentifier);
         if (data == null) {
            throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() + "; Are you sure this is json file? Try to check the correctness of its syntax.");
         } else {
            RenderSystem.recordRenderCall(() -> {
               texture.setFilter(true, false);
            });
            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = (Map)data.glyphs().stream().collect(Collectors.toMap(FontData.GlyphData::unicode, (glyphData) -> {
               return new MsdfGlyph(glyphData, aWidth, aHeight);
            }));
            Map<Integer, Map<Integer, Float>> kernings = new HashMap();
            data.kernings().forEach((kerning) -> {
               Map<Integer, Float> map = (Map)kernings.computeIfAbsent(kerning.leftChar(), (k) -> {
                  return new HashMap();
               });
               map.put(kerning.rightChar(), kerning.advance());
            });
            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
         }
      }
   }
}