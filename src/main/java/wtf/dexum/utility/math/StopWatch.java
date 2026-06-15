package wtf.dexum.utility.math;

import lombok.Generated;

public class StopWatch {
   private long lastTime;

   public StopWatch() {
      this.reset();
   }

   public boolean every(long delay) {
      if (System.currentTimeMillis() - this.lastTime >= delay) {
         this.reset();
         return true;
      } else {
         return false;
      }
   }

   public void reset() {
      this.lastTime = System.currentTimeMillis();
   }

   public long getElapsedTime() {
      return System.currentTimeMillis() - this.lastTime;
   }

   @Generated
   public long getLastTime() {
      return this.lastTime;
   }

   @Generated
   public void setLastTime(long lastTime) {
      this.lastTime = lastTime;
   }
}