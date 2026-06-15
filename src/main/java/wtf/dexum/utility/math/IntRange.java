package wtf.dexum.utility.math;

import java.util.Random;
import lombok.Generated;

public class IntRange {
   private final int start;
   private final int endInclusive;
   private final Random random = new Random();

   public IntRange(int start, int endInclusive) {
      if (start > endInclusive) {
         throw new IllegalArgumentException("Start must be less than or equal to endInclusive");
      } else {
         this.start = start;
         this.endInclusive = endInclusive;
      }
   }

   public int random() {
      return this.start + this.random.nextInt(this.endInclusive - this.start + 1);
   }

   public String toString() {
      return this.start + ".." + this.endInclusive;
   }

   @Generated
   public int getStart() {
      return this.start;
   }

   @Generated
   public int getEndInclusive() {
      return this.endInclusive;
   }
}