package wtf.dexum.client.modules.impl.movement;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.combat.Aura;

@Getter
@Setter
@ModuleAnnotation(
        name = "ElytraBooster",
        category = Category.MOVEMENT,
        description = "Ускоряет на элитрах"
)
public final class ElytraBooster extends Module {
   private static final String[] RANGE_LABELS = {"0 - 5", "5 - 10", "10 - 15", "15 - 20", "20 - 25", "25 - 30", "30 - 35", "35 - 40", "40 - 45"};
   private static final long DEBUG_MESSAGE_INTERVAL_MS = 800L;
   public static final ElytraBooster INSTANCE = new ElytraBooster();

   private final NumberSetting[] yawSpeeds = new NumberSetting[9];
   private final NumberSetting[] pitchSpeeds = new NumberSetting[9];

   private final ModeSetting mode = new ModeSetting("Сервер", new String[]{});
   private final ModeSetting.Value modeCustom;
   private final ModeSetting.Value modeLonyGrief;
   private final ModeSetting.Value modeBravoHVH;
   private final ModeSetting.Value modeReallyWorld;
   private final ModeSetting.Value modeSlimeWorld;

   private final BooleanSetting debug = new BooleanSetting("Дебаг", false, () -> isCustomMode());
   private long lastDebugMessageAt;

   private ElytraBooster() {
      this.modeCustom = new ModeSetting.Value(this.mode, "Custom");
      this.modeLonyGrief = new ModeSetting.Value(this.mode, "LonyGrief");
      this.modeBravoHVH = new ModeSetting.Value(this.mode, "BravoHVH");
      this.modeReallyWorld = new ModeSetting.Value(this.mode, "ReallyWorld");
      this.modeSlimeWorld = new ModeSetting.Value(this.mode, "SlimeWorld");
      this.mode.setValue(this.modeCustom);

      for (int i = 0; i < yawSpeeds.length; i++) {
         yawSpeeds[i] = new NumberSetting("yaw " + RANGE_LABELS[i], 1.5f, 1.5f, 2.5f, 0.01f, () -> isCustomMode());
      }

      for (int i = 0; i < pitchSpeeds.length; i++) {
         pitchSpeeds[i] = new NumberSetting("pitch " + RANGE_LABELS[i], 1.5f, 1.5f, 2.5f, 0.01f, () -> isCustomMode());
      }
   }

   public ModeSetting getMode() {
      return mode;
   }

   public boolean isCustomMode() {
      return this.mode.getValue().getName().equals("Custom");
   }

   public Vec2f getBoostV2() {
      float yaw = mc.player != null ? mc.player.getYaw() : 0.0f;
      float pitch = mc.player != null ? mc.player.getPitch() : 0.0f;

      Aura aura = Aura.INSTANCE;
      if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
         float rotYaw = aura.lastYaw;
         float rotPitch = aura.lastPitch;
         yaw = rotYaw;
         pitch = rotPitch;
      }

      float normalizedYaw = convertValToRange(MathHelper.wrapDegrees(yaw));
      float normalizedPitch = convertValToRange(Math.abs(pitch));
      int yawIndex = getRangeIndex(normalizedYaw, yawSpeeds.length);
      int pitchIndex = getRangeIndex(normalizedPitch, pitchSpeeds.length);
      float yawSpeed = yawSpeeds[yawIndex].getCurrent();
      float pitchSpeed = pitchSpeeds[pitchIndex].getCurrent();

      if (pitchSpeed > yawSpeed) {
         yawSpeed = pitchSpeed;
      }

      logDebug(yawIndex, yawSpeed, pitchIndex, pitchSpeed);
      return new Vec2f(yawSpeed, pitchSpeed);
   }

   private void logDebug(int yawIndex, float yawSpeed, int pitchIndex, float pitchSpeed) {
      if (!debug.isEnabled()) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - lastDebugMessageAt < DEBUG_MESSAGE_INTERVAL_MS) {
         return;
      }

      lastDebugMessageAt = now;
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(
                 "yaw " + RANGE_LABELS[yawIndex] + ": " + yawSpeed
                         + " | pitch " + RANGE_LABELS[pitchIndex] + ": " + pitchSpeed
         ), false);
      }
   }

   private int getRangeIndex(float value, int length) {
      return Math.min((int) (value / 5.0F), length - 1);
   }

   private float convertValToRange(float value) {
      float result = Math.abs(value);
      if (result > 90.0F) {
         result = 180.0F - result;
      }
      if (result > 45.0F) {
         result = 90.0F - result;
      }
      return result;
   }
}