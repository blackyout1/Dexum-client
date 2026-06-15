package wtf.dexum.utility.render.display;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Kernel;
import java.util.Hashtable;
import org.jetbrains.annotations.NotNull;

public class GaussianFilter {
   protected float radius;
   protected Kernel kernel;

   public GaussianFilter(float radius) {
      this.setRadius(radius);
   }

   public static void convolveAndTranspose(@NotNull Kernel kernel, int[] inPixels, int[] outPixels, int width, int height, boolean alpha, boolean premultiply, boolean unpremultiply, int edgeAction) {
      float[] matrix = kernel.getKernelData((float[])null);
      int cols = kernel.getWidth();
      int cols2 = cols / 2;

      for(int y = 0; y < height; ++y) {
         int index = y;
         int ioffset = y * width;

         for(int x = 0; x < width; ++x) {
            float r = 0.0F;
            float g = 0.0F;
            float b = 0.0F;
            float a = 0.0F;
            int moffset = cols2;

            int ia;
            int ix;
            int rgb;
            for(ia = -cols2; ia <= cols2; ++ia) {
               float f = matrix[moffset + ia];
               if (f != 0.0F) {
                  ix = x + ia;
                  if (ix < 0) {
                     if (edgeAction == 1) {
                        ix = 0;
                     } else if (edgeAction == 2) {
                        ix = (x + width) % width;
                     }
                  } else if (ix >= width) {
                     if (edgeAction == 1) {
                        ix = width - 1;
                     } else if (edgeAction == 2) {
                        ix = (x + width) % width;
                     }
                  }

                  rgb = inPixels[ioffset + ix];
                  int pa = rgb >> 24 & 255;
                  int pr = rgb >> 16 & 255;
                  int pg = rgb >> 8 & 255;
                  int pb = rgb & 255;
                  if (premultiply) {
                     float a255 = (float)pa * 0.003921569F;
                     pr = (int)((float)pr * a255);
                     pg = (int)((float)pg * a255);
                     pb = (int)((float)pb * a255);
                  }

                  a += f * (float)pa;
                  r += f * (float)pr;
                  g += f * (float)pg;
                  b += f * (float)pb;
               }
            }

            if (unpremultiply && a != 0.0F && a != 255.0F) {
               float f = 255.0F / a;
               r *= f;
               g *= f;
               b *= f;
            }

            ia = alpha ? clamp((int)((double)a + 0.5D)) : 255;
            int ir = clamp((int)((double)r + 0.5D));
            ix = clamp((int)((double)g + 0.5D));
            rgb = clamp((int)((double)b + 0.5D));
            outPixels[index] = ia << 24 | ir << 16 | ix << 8 | rgb;
            index += height;
         }
      }

   }

   public static int clamp(int c) {
      return c < 0 ? 0 : Math.min(c, 255);
   }

   public static Kernel makeKernel(float radius) {
      int r = (int)Math.ceil((double)radius);
      int rows = r * 2 + 1;
      float[] matrix = new float[rows];
      float sigma = radius / 3.0F;
      float sigma22 = 2.0F * sigma * sigma;
      float sigmaPi2 = 6.2831855F * sigma;
      float sqrtSigmaPi2 = (float)Math.sqrt((double)sigmaPi2);
      float radius2 = radius * radius;
      float total = 0.0F;
      int index = 0;

      int i;
      for(i = -r; i <= r; ++i) {
         float distance = (float)(i * i);
         if (distance > radius2) {
            matrix[index] = 0.0F;
         } else {
            matrix[index] = (float)Math.exp((double)(-distance / sigma22)) / sqrtSigmaPi2;
         }

         total += matrix[index];
         ++index;
      }

      for(i = 0; i < rows; ++i) {
         matrix[i] /= total;
      }

      return new Kernel(rows, 1, matrix);
   }

   public void setRadius(float radius) {
      this.radius = radius;
      this.kernel = makeKernel(radius);
   }

   public BufferedImage filter(BufferedImage src, BufferedImage dst) {
      int width = src.getWidth();
      int height = src.getHeight();
      if (dst == null) {
         dst = this.createCompatibleDestImage(src, (ColorModel)null);
      }

      int[] inPixels = new int[width * height];
      int[] outPixels = new int[width * height];
      src.getRGB(0, 0, width, height, inPixels, 0, width);
      if (this.radius > 0.0F) {
         convolveAndTranspose(this.kernel, inPixels, outPixels, width, height, true, true, false, 1);
         convolveAndTranspose(this.kernel, outPixels, inPixels, height, width, true, false, true, 1);
      }

      dst.setRGB(0, 0, width, height, inPixels, 0, width);
      return dst;
   }

   public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
      if (dstCM == null) {
         dstCM = src.getColorModel();
      }

      return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), (Hashtable)null);
   }
}