package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import wtf.dexum.base.events.impl.player.EventAttack;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "SwingAnimation",
        category = Category.RENDER,
        description = "Кастомные анимации удара"
)
public final class SwingAnimation extends Module {
    public static final SwingAnimation INSTANCE = new SwingAnimation();
    public final ModeSetting animationMode = new ModeSetting("Режим", new String[]{"Smooth", "Self", "Self2", "Down", "Forward", "Touch", "Pander", "Curt", "BlockHit"});
    public final BooleanSetting onlyOnHit = new BooleanSetting("Только при ударе", false);
    public NumberSetting swingPower = new NumberSetting("Сила", 5.0F, 1.0F, 10.0F, 1.0F, () -> {
        return !this.animationMode.is("BlockHit") && !this.animationMode.is("Pander") && !this.animationMode.is("Curt");
    });
    public NumberSetting speed = new NumberSetting("Скорость", 7.0F, 0.0F, 10.0F, 1.0F);
    public final NumberSetting angle = new NumberSetting("Угол", 0.0F, 0.0F, 360.0F, 1.0F, () -> this.animationMode.is("Self") || this.animationMode.is("Self2"));

    private long lastHitTime = 0L;
    private static final long HIT_ANIMATION_DURATION = 300L;

    private SwingAnimation() {
    }

    @EventTarget
    private void onAttack(EventAttack event) {
        this.lastHitTime = System.currentTimeMillis();
    }

    public boolean shouldApplyAnimation() {
        if (!this.onlyOnHit.isEnabled()) {
            return true;
        }
        return System.currentTimeMillis() - this.lastHitTime < HIT_ANIMATION_DURATION;
    }

    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        float anim = (float) Math.sin(swingProgress * (Math.PI / 2.0D) * 2.0D);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);

        switch (this.animationMode.get()) {
            case "Smooth" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float f = this.swingPower.getCurrent() * 10.0F;
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g * (-f / 4.0F)));
                float sinExtra = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sinExtra * -(f / 4.0F)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinExtra * -f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
            }
            case "Self" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.angle.getCurrent() - this.swingPower.getCurrent() * 10.0F * anim));
            }
            case "Self2" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-this.angle.getCurrent() - this.swingPower.getCurrent() * 10.0F * anim));
            }
            case "Down" -> {
                matrices.translate(0.56F, -0.52F - anim * this.swingPower.getCurrent() / 24.0F, -0.72F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            }
            case "Forward" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float f = 35.0F;
                matrices.translate(0.0D, 0.0D, -0.3D * sin2);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * f));
            }
            case "Touch" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                matrices.scale(1.0F, 1.0F, 1.0F + anim * this.swingPower.getCurrent() / 4.0F);
                matrices.translate(0.0F, 0.0F, -0.265F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            }
            case "Pander" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                matrices.scale(0.8F, 0.8F, 0.8F);
                float swingOffset = anim * 0.15F;
                matrices.translate(0.3D - swingOffset, 0.2D - equipProgress * 0.12D, -0.15D - anim * 0.13D);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F - 10.0F * anim));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16.0F - 8.0F * anim));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * anim));
            }
            case "Curt" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float sqrt = MathHelper.sqrt(swingProgress);
                float g = MathHelper.sin(sqrt * 3.1415927F);
                float sinExtra = MathHelper.sin(swingProgress * 3.1415927F);
                matrices.translate(0.4F - g * 0.2F, -0.2F + g * 0.3F, -0.5F - sinExtra * 0.2F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(91.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F + g * -100.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
            }
            case "BlockHit" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
                matrices.translate(0.4F, 0.2F, 0.2F);
                matrices.translate(-0.5F, 0.08F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
            }
        }
    }
}