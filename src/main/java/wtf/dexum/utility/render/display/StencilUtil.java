package wtf.dexum.utility.render.display;

import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import wtf.dexum.utility.interfaces.IClient;

public class StencilUtil implements IClient {
   public static void push() {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (framebuffer.depthAttachment > -1) {
         mc.getFramebuffer().beginWrite(false);
         EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthAttachment);
         int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
         EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
         EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.getWindow().getWidth(), mc.getWindow().getHeight());
         EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
         EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
         framebuffer.depthAttachment = -1;
      }

      GL11.glStencilMask(255);
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 1);
      GL11.glStencilOp(7681, 7681, 7681);
      GL11.glDisable(2929);
      GL11.glColorMask(false, false, false, false);
   }

   public static void read(int ref) {
      GL11.glColorMask(true, true, true, true);
      GL11.glStencilFunc(514, ref, 1);
      GL11.glStencilOp(7680, 7680, 7680);
   }

   public static void pop() {
      GL11.glDisable(2960);
      GL11.glEnable(2929);
   }
}