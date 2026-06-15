package wtf.dexum.utility.mixin.client;

import com.google.common.base.MoreObjects;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.HeldItemRenderer.HandRenderType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dexum.client.modules.impl.render.SwingAnimation;
import wtf.dexum.client.modules.impl.render.ViewModel;

@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack field_4047;
    @Shadow
    private float field_4043;
    @Shadow
    private float field_4053;
    @Shadow
    private float field_4051;
    @Shadow
    private float field_4052;
    @Shadow
    private ItemStack field_4048;

    @Shadow
    protected abstract void method_3228(AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, VertexConsumerProvider var9, int var10);

    @Shadow
    protected abstract void method_65816(float var1, float var2, MatrixStack var3, int var4, Arm var5);

    @Inject(
            method = {"renderFirstPersonItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 0
            )}
    )
    public void injectBeforeRenderCrossBowItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandScale(matrices, arm);
        }

    }

    @Inject(
            method = {"renderFirstPersonItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 1
            )}
    )
    public void injectBeforeRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandScale(matrices, arm);
        }

    }

    @Inject(
            method = {"renderFirstPersonItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = Shift.AFTER,
                    ordinal = 0
            )}
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.INSTANCE;
        if (viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandPosition(matrices, arm);
        }

    }

    @Redirect(
            method = {"renderFirstPersonItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        SwingAnimation swingAnimation = SwingAnimation.INSTANCE;
        if (swingAnimation.isEnabled() && swingAnimation.shouldApplyAnimation()) {
            if (arm == Arm.RIGHT) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                this.method_65816(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            this.method_65816(swingProgress, equipProgress, matrices, armX, arm);
        }

    }

    @Overwrite
    public void method_22976(float tickDelta, MatrixStack matrices, Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        float f = player.getHandSwingProgress(tickDelta);
        Hand hand = (Hand)MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float g = player.getLerpedPitch(tickDelta);
        HandRenderType handRenderType = HeldItemRenderer.getHandRenderType(player);

        boolean enabled = wtf.dexum.client.modules.impl.render.ShaderHands.INSTANCE.isEnabled();
        if (enabled) {
            wtf.dexum.utility.render.shader.ShaderHandsRenderer.renderingHands = true;
        }

        try {
            float j;
            float k;
            if (handRenderType.renderMainHand) {
                j = hand == Hand.MAIN_HAND ? f : 0.0F;
                k = 1.0F - MathHelper.lerp(tickDelta, this.field_4053, this.field_4043);
                this.method_3228(player, tickDelta, g, Hand.MAIN_HAND, j, this.field_4047, k, matrices, vertexConsumers, light);
            }

            if (handRenderType.renderOffHand) {
                j = hand == Hand.OFF_HAND ? f : 0.0F;
                k = 1.0F - MathHelper.lerp(tickDelta, this.field_4051, this.field_4052);
                this.method_3228(player, tickDelta, g, Hand.OFF_HAND, j, this.field_4048, k, matrices, vertexConsumers, light);
            }

            vertexConsumers.draw();
        } finally {
            if (enabled) {
                wtf.dexum.utility.render.shader.ShaderHandsRenderer.renderingHands = false;
            }
        }
    }
}