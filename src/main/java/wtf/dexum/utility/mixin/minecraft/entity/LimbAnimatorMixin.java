package wtf.dexum.utility.mixin.minecraft.entity;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorMixin {
    @Accessor("pos")
    @Mutable
    void setPos(float pos);

    @Accessor("speed")
    @Mutable
    void setSpeedField(float speed);

    @Accessor("prevSpeed")
    @Mutable
    void setPrevSpeed(float prevSpeed);
}