package wtf.dexum.client.hud.elements.component;

import net.minecraft.item.ItemStack;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Font;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.mixin.accessors.DrawContextAccessor;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class InventoryComponent extends DraggableHudElement {
   private final Animation toggleAnimation;
   private final Animation inventoryChangeAnimation;
   private String lastInventoryHash;
   private float lastWidth;
   private float lastHeight;

   public InventoryComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.toggleAnimation = new Animation(300L, Easing.SINE_IN_OUT);
      this.inventoryChangeAnimation = new Animation(150L, Easing.SINE_IN_OUT);
      this.lastInventoryHash = "";
      this.lastWidth = 0.0F;
      this.lastHeight = 0.0F;
   }

   public void render(CustomDrawContext ctx) {
      if (mc.player == null) {
         this.toggleAnimation.update(0.0F);
         if (this.toggleAnimation.getValue() > 0.01F) {
            this.renderInventory(ctx, this.toggleAnimation.getValue());
         }

      } else {
         this.toggleAnimation.update(1.0F);
         if (!(this.toggleAnimation.getValue() <= 0.01F)) {
            String currentInventoryHash = "";

            for(int i = 9; i < 36; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               currentInventoryHash = currentInventoryHash + stack.getItem().toString() + stack.getCount();
            }

            if (!currentInventoryHash.equals(this.lastInventoryHash)) {
               this.inventoryChangeAnimation.update(0.0F);
               this.lastInventoryHash = currentInventoryHash;
            }

            this.inventoryChangeAnimation.update(1.0F);
            this.renderInventory(ctx, this.toggleAnimation.getValue() * this.inventoryChangeAnimation.getValue());
         }
      }
   }

   @Native
   private void renderInventory(CustomDrawContext ctx, float animationValue) {
      if (mc.player == null) {
         Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
         ColorRGBA bgColor = theme.getForegroundColor();
         ctx.getMatrices().push();
         ctx.getMatrices().translate(this.getX() + this.lastWidth / 2.0F, this.getY() + this.lastHeight / 2.0F, 0.0F);
         ctx.getMatrices().scale(animationValue, animationValue, 1.0F);
         ctx.getMatrices().translate(-(this.getX() + this.lastWidth / 2.0F), -(this.getY() + this.lastHeight / 2.0F), 0.0F);
         ctx.drawRoundedRect(this.getX(), this.getY(), this.lastWidth, this.lastHeight, BorderRadius.all(4.0F), bgColor);
         ctx.getMatrices().pop();
      } else {
         Font countFont = Fonts.MEDIUM.getFont(6.0F);
         float slotSize = 20.0F;
         float borderRadius = 4.0F;
         Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
         ColorRGBA graySlotColor = theme.getForegroundColor();
         ColorRGBA themeSlotColor = theme.getForegroundLight();
         int columns = 9;
         int rows = 3;
         float gridWidth = (float)columns * slotSize;
         float gridHeight = (float)rows * slotSize;
         this.width = gridWidth;
         this.height = gridHeight;
         this.lastWidth = this.width;
         this.lastHeight = this.height;
         ctx.getMatrices().push();
         ctx.getMatrices().translate(this.getX() + this.width / 2.0F, this.getY() + this.height / 2.0F, 0.0F);
         ctx.getMatrices().scale(animationValue, animationValue, 1.0F);
         ctx.getMatrices().translate(-(this.getX() + this.width / 2.0F), -(this.getY() + this.height / 2.0F), 0.0F);
         DrawUtil.drawBlurHud(ctx.getMatrices(), this.getX(), this.getY(), this.width, this.height, 21.0F, BorderRadius.all(4.0F), ColorRGBA.WHITE);

         for(int row = 0; row < rows; ++row) {
            for(int col = 0; col < columns; ++col) {
               int slotIndex = 9 + row * 9 + col;
               ItemStack stack = mc.player.getInventory().getStack(slotIndex);
               float slotX = this.getX() + (float)col * slotSize;
               float slotY = this.getY() + (float)row * slotSize;
               ColorRGBA slotColor = (row + col) % 2 == 0 ? graySlotColor : themeSlotColor;
               float round = 4.0F;
               BorderRadius radius = col == 0 && row == 0 ? BorderRadius.top(round, 0.0F) : (col == 8 && row == 0 ? BorderRadius.top(0.0F, round) : (col == 0 && row == 2 ? BorderRadius.bottom(round, 0.0F) : (col == 8 && row == 2 ? BorderRadius.bottom(0.0F, round) : BorderRadius.ZERO)));
               ctx.drawRoundedRect(slotX, slotY, slotSize, slotSize, radius, slotColor);
               if (!stack.isEmpty()) {
                  ctx.pushMatrix();
                  ctx.getMatrices().translate((double)slotX + ((double)slotSize - 12.8D) / 2.0D, (double)slotY + ((double)slotSize - 12.8D) / 2.0D, 0.0D);
                  ctx.getMatrices().scale(0.8F, 0.8F, 1.0F);
                  ctx.drawItem(stack, 0, 0);
                  ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
                  ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
                  ctx.popMatrix();
               }
            }
         }

         ctx.drawRoundedBorder(this.getX(), this.getY(), gridWidth, gridHeight, 0.1F, BorderRadius.all(4.0F), theme.getForegroundStroke());
         DrawUtil.drawRoundedCorner(ctx.getMatrices(), this.getX(), this.getY(), gridWidth, gridHeight, 0.1F, 20.0F, theme.getColor(), BorderRadius.all(4.0F));
         ctx.getMatrices().pop();
      }
   }
}