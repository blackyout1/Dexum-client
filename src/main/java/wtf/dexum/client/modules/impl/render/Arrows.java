package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.ModeSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
import wtf.dexum.client.modules.impl.misc.PartyModuleCloud;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(
        name = "Arrows",
        category = Category.RENDER,
        description = "Показывает стрелки в сторону игроков"
)
public class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();
    private static final Identifier ARROW_ICON = Identifier.of("dexum", "icons/arrow.png");

    private final ModeSetting filter = new ModeSetting("Фильтр", "Все", "В броне", "Голые");
    private final ModeSetting targets = new ModeSetting("Цели", "Все", "Друзья", "Пати", "Друзья+Пати");
    private final NumberSetting radius = new NumberSetting("Радиус", 100, 50, 300, 5);
    private final NumberSetting size = new NumberSetting("Размер", 10, 5, 25, 1);

    private final Map<Integer, Float> rotationMap = new HashMap<>();

    @EventTarget
    private void onRenderHud(EventHudRender event) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = event.getContext().getMatrices();
        float centerX = mc.getWindow().getScaledWidth() / 2f;
        float centerY = mc.getWindow().getScaledHeight() / 2f;

        ColorRGBA themeColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        ColorRGBA secondColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor();
        if (secondColor == null) secondColor = themeColor.darker(0.3f);
        
        ColorRGBA friendColor = new ColorRGBA(85, 255, 85, 255); // Зелёный для друзей
        ColorRGBA partyColor = new ColorRGBA(0, 200, 0, 255); // Яркий зелёный для пати

        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            if (!(entity instanceof PlayerEntity isPlayer)) continue;

            boolean isFriend = Dexum.getInstance().getFriendManager().isFriend(entity.getName().getString());
            boolean isParty = PartyModuleCloud.INSTANCE.isPlayerInParty(entity.getUuidAsString());
            
            // Фильтр по целям
            if (targets.is("Друзья") && !isFriend) continue;
            if (targets.is("Пати") && !isParty) continue;
            if (targets.is("Друзья+Пати") && !isFriend && !isParty) continue;
            
            // Проверка фильтра брони
            if (filter.is("В броне")) {
                if (!hasArmor(isPlayer)) continue;
            } else if (filter.is("Голые")) {
                if (hasArmor(isPlayer)) continue;
            }

            double diffX = entity.getX() - mc.player.getX();
            double diffZ = entity.getZ() - mc.player.getZ();

            float targetAngle = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - mc.player.getYaw() - 90);

            int id = entity.getId();
            float currentAngle = rotationMap.getOrDefault(id, targetAngle);
            float delta = MathHelper.wrapDegrees(targetAngle - currentAngle);
            currentAngle += delta * 0.15f * (event.getTickDelta() + 1.0f);
            rotationMap.put(id, currentAngle);

            double dist = mc.player.distanceTo(entity);
            float alpha = (float) MathHelper.clamp(1.0 - (dist / 60.0), 0.3, 1.0);

            matrices.push();
            matrices.translate(centerX, centerY, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(currentAngle));
            matrices.translate(0, -radius.getCurrent(), 0);

            // Приоритет: пати > друзья > обычные
            if (isParty) {
                drawArrow(matrices, partyColor.withAlpha((int) (alpha * 255)), partyColor.darker(0.3f).withAlpha((int) (alpha * 255)), size.getCurrent());
            } else if (isFriend) {
                drawArrow(matrices, friendColor.withAlpha((int) (alpha * 255)), friendColor.darker(0.3f).withAlpha((int) (alpha * 255)), size.getCurrent());
            } else {
                drawArrow(matrices, themeColor.withAlpha((int) (alpha * 255)), secondColor.withAlpha((int) (alpha * 255)), size.getCurrent());
            }

            matrices.pop();
        }
        
        // Рендер стрелок для непрогруженных членов пати
        if (PartyModuleCloud.INSTANCE.isEnabled() && PartyModuleCloud.INSTANCE.isInParty() 
            && (targets.is("Пати") || targets.is("Друзья+Пати") || targets.is("Все"))) {
            
            String currentDim = mc.player.getWorld().getRegistryKey().getValue().toString();
            
            for (PartyModuleCloud.PartyMemberData member : PartyModuleCloud.INSTANCE.getPartyMembersData()) {
                // Проверяем что игрок не прогружен
                boolean isLoaded = false;
                for (net.minecraft.entity.Entity e : mc.world.getEntities()) {
                    if (e instanceof PlayerEntity && e.getUuidAsString().equals(member.uuid)) {
                        isLoaded = true;
                        break;
                    }
                }
                
                if (isLoaded) continue; // Уже отрендерили выше
                if (!member.dim.equals(currentDim)) continue; // Другое измерение
                
                double diffX = member.x - mc.player.getX();
                double diffZ = member.z - mc.player.getZ();
                
                float targetAngle = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - mc.player.getYaw() - 90);
                
                // Используем UUID как ключ для ротации
                int id = member.uuid.hashCode();
                float currentAngle = rotationMap.getOrDefault(id, targetAngle);
                float delta = MathHelper.wrapDegrees(targetAngle - currentAngle);
                currentAngle += delta * 0.15f * (event.getTickDelta() + 1.0f);
                rotationMap.put(id, currentAngle);
                
                double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                float alpha = (float) MathHelper.clamp(1.0 - (dist / 200.0), 0.5, 1.0);
                
                matrices.push();
                matrices.translate(centerX, centerY, 0);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(currentAngle));
                matrices.translate(0, -radius.getCurrent(), 0);
                
                // Рендерим ярко-зелёную стрелку для пати
                drawArrow(matrices, partyColor.withAlpha((int) (alpha * 255)), partyColor.darker(0.3f).withAlpha((int) (alpha * 255)), size.getCurrent());
                
                matrices.pop();
            }
        }

        if (mc.player.age % 100 == 0) {
            rotationMap.keySet().removeIf(entityId -> mc.world.getEntityById(entityId) == null);
        }
    }
    
    // Проверка наличия брони
    private boolean hasArmor(PlayerEntity player) {
        return !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ||
               !player.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
               !player.getEquippedStack(EquipmentSlot.LEGS).isEmpty() ||
               !player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    private void drawArrow(MatrixStack matrices, ColorRGBA c1, ColorRGBA c2, float s) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ARROW_ICON);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int argb = c1.getRGB();

        buffer.vertex(matrix, -s/2f, -s, 0).texture(0, 0).color(argb);
        buffer.vertex(matrix, s/2f, -s, 0).texture(1, 0).color(argb);
        buffer.vertex(matrix, s/2f, 0, 0).texture(1, 1).color(argb);
        buffer.vertex(matrix, -s/2f, 0, 0).texture(0, 1).color(argb);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}