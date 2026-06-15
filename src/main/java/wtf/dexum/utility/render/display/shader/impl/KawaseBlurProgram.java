package wtf.dexum.utility.render.display.shader.impl;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import wtf.dexum.utility.interfaces.IWindow;
import wtf.dexum.utility.render.display.shader.GlProgram;

public class KawaseBlurProgram extends GlProgram implements IWindow {
   private GlUniform resolutionUniform;
   private GlUniform offsetUniform;
   private GlUniform saturationUniform;
   private GlUniform tintIntensityUniform;
   private GlUniform tintColorUniform;

   public KawaseBlurProgram(Identifier identifier) {
      super(identifier, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   public void updateUniforms(float offset) {
      this.offsetUniform.set(offset);
      this.resolutionUniform.set(1.0F / (float)mw.getWidth(), 1.0F / (float)mw.getHeight());
      this.saturationUniform.set(1.0F);
      this.tintIntensityUniform.set(0.0F);
      this.tintColorUniform.set(1.0F, 1.0F, 1.0F);
   }

   protected void setup() {
      this.resolutionUniform = this.findUniform("Resolution");
      this.offsetUniform = this.findUniform("Offset");
      this.saturationUniform = this.findUniform("Saturation");
      this.tintIntensityUniform = this.findUniform("TintIntensity");
      this.tintColorUniform = this.findUniform("TintColor");
      super.setup();
   }
}