package wtf.dexum.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.utility.math.Timer;

@ModuleAnnotation(
   name = "Blink",
   category = Category.PLAYER,
   description = "Задержка пакетов движения"
)
public final class Blink extends Module {
   public static final Blink INSTANCE = new Blink();
   private final List<Packet<?>> packets = new ArrayList();
   private final Timer timer = new Timer();
   private final BooleanSetting pulse = new BooleanSetting("Пульсация", false);
   private final NumberSetting time = new NumberSetting("Время", 12.0F, 1.0F, 40.0F, 1.0F);
   private Vec3d lastPos;
   private boolean replaying;

   private Blink() {
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (this.pulse.isEnabled() && this.timer.finished((long)(this.time.getCurrent() * 50.0F))) {
         this.onDisable();
         this.onEnable();
         this.timer.reset();
      }

   }

   public void onEnable() {
      if (mc.player != null) {
         this.packets.clear();
         this.lastPos = mc.player.getPos();
         this.timer.reset();
         this.replaying = false;
      }
   }

   public void onDisable() {
      if (mc.player != null) {
         this.replaying = true;
         Iterator var1 = this.packets.iterator();

         while(var1.hasNext()) {
            Packet<?> p = (Packet)var1.next();
            mc.player.networkHandler.sendPacket(p);
         }

         this.replaying = false;
         this.packets.clear();
         this.lastPos = null;
      }
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public BooleanSetting getPulse() {
      return this.pulse;
   }

   @Generated
   public NumberSetting getTime() {
      return this.time;
   }
}