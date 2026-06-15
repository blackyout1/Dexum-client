package wtf.dexum.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public class EventHandledScreen implements Event {
   private final DrawContext drawContext;
   private final Slot slotHover;
   private final int backgroundWidth;
   private final int backgroundHeight;

   @Generated
   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   @Generated
   public Slot getSlotHover() {
      return this.slotHover;
   }

   @Generated
   public int getBackgroundWidth() {
      return this.backgroundWidth;
   }

   @Generated
   public int getBackgroundHeight() {
      return this.backgroundHeight;
   }

   @Generated
   public EventHandledScreen(DrawContext drawContext, Slot slotHover, int backgroundWidth, int backgroundHeight) {
      this.drawContext = drawContext;
      this.slotHover = slotHover;
      this.backgroundWidth = backgroundWidth;
      this.backgroundHeight = backgroundHeight;
   }
}