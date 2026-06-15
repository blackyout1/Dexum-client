package wtf.dexum.base.macro;

import lombok.Generated;

public class Macro {
   private int bind;
   private String text;

   @Generated
   public int getBind() {
      return this.bind;
   }

   @Generated
   public String getText() {
      return this.text;
   }

   @Generated
   public void setBind(int bind) {
      this.bind = bind;
   }

   @Generated
   public void setText(String text) {
      this.text = text;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Macro)) {
         return false;
      } else {
         Macro other = (Macro)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getBind() != other.getBind()) {
            return false;
         } else {
            Object this$text = this.getText();
            Object other$text = other.getText();
            if (this$text == null) {
               if (other$text != null) {
                  return false;
               }
            } else if (!this$text.equals(other$text)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof Macro;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1; result = result * 59 + this.getBind();
      Object $text = this.getText();
      result = result * 59 + ($text == null ? 43 : $text.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      int var10000 = this.getBind();
      return "Macro(bind=" + var10000 + ", text=" + this.getText() + ")";
   }

   @Generated
   public Macro(int bind, String text) {
      this.bind = bind;
      this.text = text;
   }
}