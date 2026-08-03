package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

/**
 * FunTimeTest Rotation - Легитная ротация (плавно крутится на хитбокс)
 * Обычная ротация как у обычного игрока без аномальных поворотов
 */
public class FunTimeTestRotation extends RotationBase {
    private float currentYaw = 0.0F;
    private float currentPitch = 0.0F;
    private boolean isInitialized = false;

    public FunTimeTestRotation() {
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!isInitialized) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            this.isInitialized = true;
        }

        if (target == null) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            return;
        }

        // Получаем целевой угол
        float targetYaw = MathHelper.wrapDegrees(targetAngle.getYaw());
        float targetPitch = MathHelper.clamp(targetAngle.getPitch(), -90.0F, 90.0F);

        // Вычисляем разницу
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - this.currentYaw);
        float deltaPitch = targetPitch - this.currentPitch;

        // Ограничиваем скорость поворота (еще плавнее чем раньше)
        float maxStepYaw = 10.0F;
        float maxStepPitch = 6.0F;

        deltaYaw = MathHelper.clamp(deltaYaw, -maxStepYaw, maxStepYaw);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxStepPitch, maxStepPitch);

        // Применяем поворот
        this.currentYaw = MathHelper.wrapDegrees(this.currentYaw + deltaYaw);
        this.currentPitch = MathHelper.clamp(this.currentPitch + deltaPitch, -90.0F, 90.0F);

        // Обновляем компонент ротации с легитной скоростью
        Rotation finalRot = new Rotation(this.currentYaw, this.currentPitch);
        float visualSpeed = 15.0F + (Math.abs(deltaYaw) + Math.abs(deltaPitch)) * 0.3F;
        visualSpeed = MathHelper.clamp(visualSpeed, 10.0F, 30.0F);
        RotationComponent.update(finalRot, visualSpeed, visualSpeed, visualSpeed, visualSpeed, 0, 1, elytraVisual);

        this.lastYaw = this.currentYaw;
        this.lastPitch = this.currentPitch;
    }

    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        this.isInitialized = false;
        this.currentYaw = mc.player.getYaw();
        this.currentPitch = mc.player.getPitch();
    }

    @Override
    public float getYaw() {
        return this.lastYaw;
    }

    @Override
    public float getPitch() {
        return this.lastPitch;
    }

    @Override
    public void setYaw(float yaw) {
        this.currentYaw = yaw;
    }

    @Override
    public void setPitch(float pitch) {
        this.currentPitch = pitch;
    }
}
