package wtf.dexum.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Generated;
import net.minecraft.item.Items;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.events.impl.other.EventTickMovement;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.KeySetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
   name = "ClickPearl",
   category = Category.COMBAT,
   description = "Кидает перку по бинду"
)
public final class ClickPearl extends Module {
   private final ModeSetting mode = new ModeSetting("Мод", new String[]{"Хвх", "Легит"});
   private final KeySetting bind = new KeySetting("Бинд");
   public static final ClickPearl INSTANCE = new ClickPearl();
   private boolean ignore;

   private ClickPearl() {
   }

   @EventTarget
   @Native
   private void onKey(wtf.dexum.base.events.impl.input.EventKey e) {
      if (e.isKeyDown(this.bind.getKeyCode())) {
         this.setIgnore(true);
         if (this.mode.is("Хвх")) {
            PlayerInventoryUtil.swapAndUseHvH(Items.ENDER_PEARL);
         } else {
            PlayerInventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
         }
         this.setIgnore(false);
      }
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ClickPearl)) {
         return false;
      } else {
         ClickPearl other = (ClickPearl)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else if (this.isIgnore() != other.isIgnore()) {
            return false;
         } else {
            Object this$mode = this.getMode();
            Object other$mode = other.getMode();
            if (this$mode == null) {
               if (other$mode != null) {
                  return false;
               }
            } else if (!this$mode.equals(other$mode)) {
               return false;
            }

            Object this$bind = this.getBind();
            Object other$bind = other.getBind();
            if (this$bind == null) {
               if (other$bind != null) {
                  return false;
               }
            } else if (!this$bind.equals(other$bind)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof ClickPearl;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = super.hashCode();
      result = result * 59 + (this.isIgnore() ? 79 : 97);
      Object $mode = this.getMode();
      result = result * 59 + ($mode == null ? 43 : $mode.hashCode());
      Object $bind = this.getBind();
      result = result * 59 + ($bind == null ? 43 : $bind.hashCode());
      return result;
   }

   @Generated
   public ModeSetting getMode() {
      return this.mode;
   }

   @Generated
   public KeySetting getBind() {
      return this.bind;
   }

   @Generated
   public boolean isIgnore() {
      return this.ignore;
   }

   @Generated
   public void setIgnore(boolean ignore) {
      this.ignore = ignore;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getMode());
      return "ClickPearl(mode=" + var10000 + ", bind=" + this.getBind() + ", ignore=" + this.isIgnore() + ")";
   }
}