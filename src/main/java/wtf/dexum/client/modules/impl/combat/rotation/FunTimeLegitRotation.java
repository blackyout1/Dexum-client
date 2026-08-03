package wtf.dexum.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import wtf.dexum.utility.component.RotationComponent;
import wtf.dexum.utility.game.player.rotation.Rotation;

/**
 * FunTimeLegit Rotation - Полностью легитная ротация
 * Не делает никаких поворотов, использует ротацию игрока как есть
 * Атака происходит только если сам навёлся на хитбокс
 */
public class FunTimeLegitRotation extends RotationBase {
    private float currentYaw = 0.0F;
    private float currentPitch = 0.0F;
    private boolean isInitialized = false;

    public FunTimeLegitRotation() {
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {
        if (!isInitialized) {
            this.currentYaw = mc.player.getYaw();
            this.currentPitch = mc.player.getPitch();
            this.isInitialized = true;
        }

        // Не делаем никаких поворотов - используем ротацию игрока как есть
        this.currentYaw = mc.player.getYaw();
        this.currentPitch = mc.player.getPitch();

        // Обновляем компонент ротации (просто передаём ротацию игрока)
        Rotation finalRot = new Rotation(this.currentYaw, this.currentPitch);
        RotationComponent.update(finalRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);

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
