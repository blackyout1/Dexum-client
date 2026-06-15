package wtf.dexum.utility.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @ModifyVariable(
        method = "setShader(Lnet/minecraft/client/gl/ShaderProgramKey;)Lnet/minecraft/client/gl/ShaderProgram;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static ShaderProgramKey modifyShaderKey(ShaderProgramKey key) {
        if (wtf.dexum.utility.render.shader.ShaderHandsRenderer.renderingHands) {
            ShaderProgramKey customKey = wtf.dexum.utility.render.shader.ShaderHandsRenderer.getInstance().getCustomShaderKey(key);
            if (customKey != null) {
                return customKey;
            }
        }
        return key;
    }
}