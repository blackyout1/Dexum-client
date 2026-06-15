package wtf.dexum.utility.render.display.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import wtf.dexum.utility.interfaces.IWindow;

public class CustomRenderTarget extends Framebuffer implements IWindow {
   private boolean linear;

   public CustomRenderTarget(boolean useDepth) {
      super(useDepth);
   }

   public CustomRenderTarget(int width, int height, boolean useDepth) {
      super(useDepth);
      this.resize(width, height);
   }

   public CustomRenderTarget setLinear() {
      this.linear = true;
      RenderSystem.recordRenderCall(() -> {
         this.setTexFilter(9729);
      });
      return this;
   }

   public void setTexFilter(int framebufferFilterIn) {
      super.setTexFilter(this.linear ? 9729 : framebufferFilterIn);
   }

   private void resizeFramebuffer() {
      if (this.needsNewFramebuffer()) {
         this.initFbo(Math.max(mw.getWidth(), 1), Math.max(mw.getHeight(), 1));
      }

   }

   public void setup(boolean clear) {
      this.resizeFramebuffer();
      if (clear) {
         this.clear();
      }

      this.beginWrite(false);
   }

   public void setup() {
      this.setup(true);
   }

   public void stop() {
      this.endWrite();
      mc.getFramebuffer().beginWrite(false);
   }

   public void stopAndClearMain() {
      this.endWrite();
      mc.getFramebuffer().beginWrite(true);
   }

   private boolean needsNewFramebuffer() {
      return this.textureWidth != mw.getWidth() || this.textureHeight != mw.getHeight();
   }
}