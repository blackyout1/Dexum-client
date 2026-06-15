package wtf.dexum.utility.render.display;

import lombok.Generated;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Font;
import wtf.dexum.utility.game.other.MouseButton;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.MathUtil;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

public class TextBox implements IMinecraft {
   private String text = "";
   private boolean selected;
   private boolean selectAll;
   private int cursor;
   private float posX;
   private Font font;
   private Vector2f position;
   private String emptyText;
   private float width;
   private long lastInputTime = System.currentTimeMillis();
   private int maxLength = Integer.MAX_VALUE;
   private TextBox.CharFilter charFilter;
   private float scrollOffset;
   Animation animation;

   public TextBox(Vector2f position, Font font, String emptyText, float width) {
      this.charFilter = TextBox.CharFilter.ANY;
      this.scrollOffset = 0.0F;
      this.animation = new Animation(400L, 0.2F, Easing.QUAD_IN_OUT);
      this.font = font;
      this.emptyText = emptyText;
      this.width = width;
      this.position = position;
   }

   public void render(CustomDrawContext context, float x, float y, ColorRGBA colorText, ColorRGBA colorEmpty) {
      this.position = new Vector2f(x, y);
      this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
      this.posX = x;
      boolean isEmpty = this.isEmpty();
      float cursorX = 0.0F;
      if (!isEmpty) {
         String textBeforeCursor = this.text.substring(0, this.cursor);
         cursorX = this.font.width(textBeforeCursor);
      }

      float availableWidth = this.width;

      int startIndex;
      for(startIndex = 0; this.font.width(this.text.substring(startIndex, this.cursor)) > availableWidth; ++startIndex) {
      }

      int endIndex;
      for(endIndex = this.cursor; endIndex < this.text.length() && this.font.width(this.text.substring(startIndex, endIndex)) < availableWidth; ++endIndex) {
      }

      String visibleText = this.text.substring(startIndex, endIndex);
      if (isEmpty) {
         context.drawText(this.font, this.emptyText, x, y, colorEmpty);
      } else {
         context.drawText(this.font, visibleText, x, y, colorText);
      }

      if (this.selected && System.currentTimeMillis() - this.lastInputTime > 200L) {
         float cursorDrawX = this.posX + cursorX - this.scrollOffset;
         this.animation.setDuration(250L);
         context.drawRect(cursorDrawX, y - 1.0F, 1.0F, this.font.height() + 2.0F, colorText.mulAlpha(this.animation.update(this.animation.getValue() == 0.2F ? 1.0F : (this.animation.getValue() == 1.0F ? 0.2F : this.animation.getTargetValue()))));
      }

      if (this.selectAll) {
         context.drawRect(x - 1.0F, y - 1.0F, this.font.width(visibleText) + 2.0F, this.font.height() + 2.0F, colorEmpty.mulAlpha(0.5F));
      }

   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      Vector2f pos = this.getPosition();
      this.selected = button.getButtonIndex() == 0 && MathUtil.isHovered(mouseX, mouseY, (double)pos.getX(), (double)(pos.getY() - 1.0F), (double)this.width, (double)(this.font.height() + 2.0F));
      if (this.selected) {
         this.selectAll = false;
      }

   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.selected) {
         return false;
      } else {
         this.lastInputTime = System.currentTimeMillis();
         this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
         if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
            if (keyCode == 86) {
               String clipboard = mc.keyboard.getClipboard();
               if (this.selectAll) {
                  this.text = "";
                  this.cursor = 0;
                  this.selectAll = false;
               }

               this.addText(clipboard, this.cursor);
               this.cursor += clipboard.length();
               this.selectAll = false;
            } else if (keyCode == 65) {
               this.selectAll = true;
               this.cursor = this.text.length();
            } else if (keyCode == 67 && this.selected && this.selectAll) {
               mc.keyboard.setClipboard(this.text);
            }
         } else if (keyCode == 261 && !this.text.isEmpty()) {
            this.removeText(this.cursor + 1);
            this.selectAll = false;
         } else if (keyCode == 259 && !this.text.isEmpty()) {
            if (this.selectAll) {
               this.text = "";
               this.cursor = 0;
               this.selectAll = false;
            } else {
               this.removeText(this.cursor);
               --this.cursor;
               if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
                  while(!this.text.isEmpty() && this.cursor > 0) {
                     this.removeText(this.cursor);
                     --this.cursor;
                  }
               }
            }
         } else if (keyCode == 262) {
            ++this.cursor;
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
               this.cursor = this.text.length();
            }

            this.selectAll = false;
         } else if (keyCode == 263) {
            --this.cursor;
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 341)) {
               this.cursor = 0;
            }

            this.selectAll = false;
         } else if (keyCode == 269) {
            this.cursor = this.text.length();
            this.selectAll = false;
         } else if (keyCode == 268) {
            this.cursor = 0;
            this.selectAll = false;
         }

         this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
         return true;
      }
   }

   public boolean charTyped(char codePoint, int modifiers) {
      if (!this.selected) {
         return false;
      } else {
         this.lastInputTime = System.currentTimeMillis();
         this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
         if (this.selectAll) {
            this.text = "";
            this.cursor = 0;
            this.selectAll = false;
         }

         this.addText(Character.toString(codePoint), this.cursor);
         ++this.cursor;
         this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
         return true;
      }
   }

   private void addText(String newText, int position) {
      StringBuilder filteredText = new StringBuilder();
      char[] var4 = newText.toCharArray();
      int available = var4.length;

      for(int var6 = 0; var6 < available; ++var6) {
         char c = var4[var6];
         if (this.charFilter.isAllowed(c)) {
            filteredText.append(c);
         }
      }

      String filtered = filteredText.toString();
      if (this.text.length() + filtered.length() > this.maxLength) {
         available = this.maxLength - this.text.length();
         if (available <= 0) {
            return;
         }

         filtered = filtered.substring(0, Math.min(available, filtered.length()));
      }

      StringBuilder newFinalText = new StringBuilder();
      boolean inserted = false;

      for(int i = 0; i < this.text.length(); ++i) {
         if (i == position) {
            inserted = true;
            newFinalText.append(filtered);
         }

         newFinalText.append(this.text.charAt(i));
      }

      if (!inserted) {
         newFinalText.append(filtered);
      }

      this.text = newFinalText.toString();
   }

   private void removeText(int position) {
      StringBuilder newText = new StringBuilder();

      for(int i = 0; i < this.text.length(); ++i) {
         if (i != position - 1) {
            newText.append(this.text.charAt(i));
         }
      }

      this.text = newText.toString();
   }

   public boolean isEmpty() {
      return this.text.isEmpty();
   }

   @Generated
   public String getText() {
      return this.text;
   }

   @Generated
   public boolean isSelected() {
      return this.selected;
   }

   @Generated
   public boolean isSelectAll() {
      return this.selectAll;
   }

   @Generated
   public int getCursor() {
      return this.cursor;
   }

   @Generated
   public float getPosX() {
      return this.posX;
   }

   @Generated
   public Font getFont() {
      return this.font;
   }

   @Generated
   public Vector2f getPosition() {
      return this.position;
   }

   @Generated
   public String getEmptyText() {
      return this.emptyText;
   }

   @Generated
   public float getWidth() {
      return this.width;
   }

   @Generated
   public long getLastInputTime() {
      return this.lastInputTime;
   }

   @Generated
   public int getMaxLength() {
      return this.maxLength;
   }

   @Generated
   public TextBox.CharFilter getCharFilter() {
      return this.charFilter;
   }

   @Generated
   public float getScrollOffset() {
      return this.scrollOffset;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public void setText(String text) {
      this.text = text;
   }

   @Generated
   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   @Generated
   public void setSelectAll(boolean selectAll) {
      this.selectAll = selectAll;
   }

   @Generated
   public void setCursor(int cursor) {
      this.cursor = cursor;
   }

   @Generated
   public void setPosX(float posX) {
      this.posX = posX;
   }

   @Generated
   public void setFont(Font font) {
      this.font = font;
   }

   @Generated
   public void setPosition(Vector2f position) {
      this.position = position;
   }

   @Generated
   public void setEmptyText(String emptyText) {
      this.emptyText = emptyText;
   }

   @Generated
   public void setWidth(float width) {
      this.width = width;
   }

   @Generated
   public void setLastInputTime(long lastInputTime) {
      this.lastInputTime = lastInputTime;
   }

   @Generated
   public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
   }

   @Generated
   public void setCharFilter(TextBox.CharFilter charFilter) {
      this.charFilter = charFilter;
   }

   @Generated
   public void setScrollOffset(float scrollOffset) {
      this.scrollOffset = scrollOffset;
   }

   @Generated
   public void setAnimation(Animation animation) {
      this.animation = animation;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof TextBox)) {
         return false;
      } else {
         TextBox other = (TextBox)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.isSelected() != other.isSelected()) {
            return false;
         } else if (this.isSelectAll() != other.isSelectAll()) {
            return false;
         } else if (this.getCursor() != other.getCursor()) {
            return false;
         } else if (Float.compare(this.getPosX(), other.getPosX()) != 0) {
            return false;
         } else if (Float.compare(this.getWidth(), other.getWidth()) != 0) {
            return false;
         } else if (this.getLastInputTime() != other.getLastInputTime()) {
            return false;
         } else if (this.getMaxLength() != other.getMaxLength()) {
            return false;
         } else if (Float.compare(this.getScrollOffset(), other.getScrollOffset()) != 0) {
            return false;
         } else {
            label103: {
               Object this$text = this.getText();
               Object other$text = other.getText();
               if (this$text == null) {
                  if (other$text == null) {
                     break label103;
                  }
               } else if (this$text.equals(other$text)) {
                  break label103;
               }

               return false;
            }

            Object this$font = this.getFont();
            Object other$font = other.getFont();
            if (this$font == null) {
               if (other$font != null) {
                  return false;
               }
            } else if (!this$font.equals(other$font)) {
               return false;
            }

            label89: {
               Object this$position = this.getPosition();
               Object other$position = other.getPosition();
               if (this$position == null) {
                  if (other$position == null) {
                     break label89;
                  }
               } else if (this$position.equals(other$position)) {
                  break label89;
               }

               return false;
            }

            Object this$emptyText = this.getEmptyText();
            Object other$emptyText = other.getEmptyText();
            if (this$emptyText == null) {
               if (other$emptyText != null) {
                  return false;
               }
            } else if (!this$emptyText.equals(other$emptyText)) {
               return false;
            }

            label75: {
               Object this$charFilter = this.getCharFilter();
               Object other$charFilter = other.getCharFilter();
               if (this$charFilter == null) {
                  if (other$charFilter == null) {
                     break label75;
                  }
               } else if (this$charFilter.equals(other$charFilter)) {
                  break label75;
               }

               return false;
            }

            Object this$animation = this.getAnimation();
            Object other$animation = other.getAnimation();
            if (this$animation == null) {
               if (other$animation != null) {
                  return false;
               }
            } else if (!this$animation.equals(other$animation)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof TextBox;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + (this.isSelected() ? 79 : 97);
      result = result * 59 + (this.isSelectAll() ? 79 : 97);
      result = result * 59 + this.getCursor();
      result = result * 59 + Float.floatToIntBits(this.getPosX());
      result = result * 59 + Float.floatToIntBits(this.getWidth());
      long $lastInputTime = this.getLastInputTime();
      result = result * 59 + (int)($lastInputTime >>> 32 ^ $lastInputTime);
      result = result * 59 + this.getMaxLength();
      result = result * 59 + Float.floatToIntBits(this.getScrollOffset());
      Object $text = this.getText();
      result = result * 59 + ($text == null ? 43 : $text.hashCode());
      Object $font = this.getFont();
      result = result * 59 + ($font == null ? 43 : $font.hashCode());
      Object $position = this.getPosition();
      result = result * 59 + ($position == null ? 43 : $position.hashCode());
      Object $emptyText = this.getEmptyText();
      result = result * 59 + ($emptyText == null ? 43 : $emptyText.hashCode());
      Object $charFilter = this.getCharFilter();
      result = result * 59 + ($charFilter == null ? 43 : $charFilter.hashCode());
      Object $animation = this.getAnimation();
      result = result * 59 + ($animation == null ? 43 : $animation.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = this.getText();
      return "TextBox(text=" + var10000 + ", selected=" + this.isSelected() + ", selectAll=" + this.isSelectAll() + ", cursor=" + this.getCursor() + ", posX=" + this.getPosX() + ", font=" + String.valueOf(this.getFont()) + ", position=" + String.valueOf(this.getPosition()) + ", emptyText=" + this.getEmptyText() + ", width=" + this.getWidth() + ", lastInputTime=" + this.getLastInputTime() + ", maxLength=" + this.getMaxLength() + ", charFilter=" + String.valueOf(this.getCharFilter()) + ", scrollOffset=" + this.getScrollOffset() + ", animation=" + String.valueOf(this.getAnimation()) + ")";
   }

   public static enum CharFilter {
      ANY,
      ENGLISH,
      ENGLISH_NUMBERS,
      CYRILLIC,
      NUMBERS_ONLY;

      public boolean isAllowed(char c) {
         boolean var10000;
         switch(this.ordinal()) {
         case 0:
            var10000 = true;
            break;
         case 1:
            var10000 = Character.isLetter(c) && c <= 127 && Character.isAlphabetic(c);
            break;
         case 2:
            var10000 = Character.isLetterOrDigit(c) && c <= 127;
            break;
         case 3:
            var10000 = String.valueOf(c).matches("[А-Яа-яЁё]");
            break;
         case 4:
            var10000 = Character.isDigit(c);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }

      private static TextBox.CharFilter[] $values() {
         return new TextBox.CharFilter[]{ANY, ENGLISH, ENGLISH_NUMBERS, CYRILLIC, NUMBERS_ONLY};
      }
   }
}