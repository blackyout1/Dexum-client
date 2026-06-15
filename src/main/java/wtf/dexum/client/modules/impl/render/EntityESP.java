package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import org.joml.Vector4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.impl.misc.NameProtect;
import wtf.dexum.client.modules.impl.misc.ScoreboardHealth;
import wtf.dexum.utility.game.other.ReplaceUtil;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.math.ProjectionUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.level.Render3DUtil;

@ModuleAnnotation(
        name = "NameTags",
        category = Category.RENDER,
        description = "Показывает информацию о игроке"
)
public final class EntityESP extends Module {
    public static final EntityESP INSTANCE = new EntityESP();
    private final HashMap<Entity, Vector4f> positions = new HashMap();
    private final BooleanSetting box3D = new BooleanSetting("3D Box", false);

    @EventTarget
    private void onRender(EventRender2D e) {
        if (mc.world != null && mc.player != null) {
            float tickDelta = e.getTickDelta();
            this.renderPlayerTags(tickDelta, e);
            this.renderItemTags(tickDelta, e);
        }
    }

    @EventTarget
    private void onRender3D(EventRender3D e) {
        if (!box3D.isEnabled() || mc.world == null || mc.player == null) {
            return;
        }

        float tickDelta = e.getPartialTicks();
        int baseRGB = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() & 0x00FFFFFF;
        int color = (150 << 24) | baseRGB;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player && !mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
                continue;
            }

            if (!entity.isAlive() || entity.isRemoved()) {
                continue;
            }

            double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());

            Box localBox = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
            Render3DUtil.drawBox(localBox.offset(x, y, z), color, 1.3f, true, true, false);
        }
    }

    private void renderPlayerTags(float tickDelta, EventRender2D e) {
        ColorRGBA themeDark = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().darker(0.92F).withAlpha(255);
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player && !mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
                continue;
            }

            if (!entity.isAlive() || entity.isRemoved()) {
                continue;
            }

            if (!ProjectionUtil.canSee(entity.getBoundingBox().getCenter())) {
                continue;
            }

            double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.2D;
            double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());

            if (!mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
                Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
                Vec3d entityPosRel = new Vec3d(x, y, z).subtract(cameraPos);

                float pitch = mc.getEntityRenderDispatcher().camera.getPitch();
                float yaw = mc.getEntityRenderDispatcher().camera.getYaw();
                float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
                float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
                float f2 = -MathHelper.cos(-pitch * 0.017453292F);
                float f3 = MathHelper.sin(-pitch * 0.017453292F);
                Vec3d actualLookVec = new Vec3d(f1 * f2, f3, f * f2);

                if (entityPosRel.dotProduct(actualLookVec) < 0) {
                    continue;
                }
            }

            Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
            if (pos.z <= 0.0D || pos.z >= 1.0D) {
                continue;
            }

            Vector4d position = ProjectionUtil.getVector4D(entity);
            if (position == null) continue;

            float posY = (float)(position.y - 11.0D);
            float hp = ScoreboardHealth.INSTANCE.isEnabled() && entity != mc.player ? PlayerIntersectionUtil.getHealth(entity) : entity.getHealth();
            Text name = entity == mc.player && NameProtect.INSTANCE.isEnabled() ? Text.literal(NameProtect.getCustomName()) : ReplaceUtil.replaceSymbols(entity.getDisplayName());
            Text nameWithHp = ((Text)name).copy().append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY))).append(Text.literal(String.valueOf((int)hp)).setStyle(Style.EMPTY.withColor(Formatting.RED))).append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            float textWidth = Fonts.REGULAR.getWidth(nameWithHp.getString(), 6.5F);

            float headSize = 8.0F;
            float totalTagWidth = textWidth + headSize + 6.0F;
            float tagX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalTagWidth / 2.0F));

            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX, posY - 3.5F, totalTagWidth, 12.0F, BorderRadius.all(1.5F), Dexum.getInstance().getFriendManager().isFriend(entity.getNameForScoreboard()) ? new ColorRGBA(0, 166, 0, 255) : themeDark);

            DrawUtil.drawPlayerHeadWithRoundedShader(e.getContext().getMatrices(), entity instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity)entity).getSkinTextures().texture() : DefaultSkinHelper.getSteve().texture(), tagX + 2.0F, posY - 1.5F, headSize, BorderRadius.all(1.5F), ColorRGBA.WHITE);

            e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), nameWithHp, tagX + headSize + 4.0F, posY, 255.0F);

            ItemStack[] itemArray = new ItemStack[6];
            int itemCount = 0;
            EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

            for (EquipmentSlot slot : slots) {
                ItemStack stack = entity.getEquippedStack(slot);
                if (!stack.isEmpty()) {
                    itemArray[itemCount++] = stack;
                }
            }

            ItemStack mainHand = entity.getMainHandStack();
            if (!mainHand.isEmpty()) {
                itemArray[itemCount++] = mainHand;
            }

            ItemStack offHand = entity.getOffHandStack();
            if (!offHand.isEmpty()) {
                itemArray[itemCount++] = offHand;
            }

            if (itemCount == 0) continue;

            float iconSize = 16.0F;
            float spacing = 0.0F;
            float totalWidth = (float)itemCount * iconSize + (float)(itemCount - 1) * spacing;
            float startX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalWidth / 2.0F) + 7.5D);
            float iconY = posY - 12.0F;
            MatrixStack matrices = e.getContext().getMatrices();

            for (int i = 0; i < itemCount; ++i) {
                ItemStack stack = itemArray[i];
                if (stack != null && !stack.isEmpty()) {
                    float x2 = startX + (float)i * (iconSize + spacing);
                    ItemEnchantmentsComponent enchComp = EnchantmentHelper.getEnchantments(stack);

                    if (!enchComp.isEmpty()) {
                        Map<RegistryEntry<Enchantment>, Integer> enchMap = enchComp.getEnchantmentEntries().stream()
                                .collect(Collectors.toMap(Entry::getKey, it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue));
                        float enchantmentY = iconY - 16.0F;

                        for (Map.Entry<RegistryEntry<Enchantment>, Integer> enchEntry : enchMap.entrySet()) {
                            int lvl = enchEntry.getValue();
                            if (lvl <= 0) continue;

                            String fullName = Enchantment.getName(enchEntry.getKey(), lvl).getString();
                            String shortName = fullName.length() > 2 ? fullName.substring(0, 2) : fullName;
                            String enchantmentText = shortName + lvl;
                            float enchantmentTextWidth = Fonts.REGULAR.getWidth(enchantmentText, 6.0F);
                            int color = -1;
                            if ((shortName.equalsIgnoreCase("Sh") && lvl > 5) || (shortName.equalsIgnoreCase("Pr") && lvl > 4)) {
                                color = (new ColorRGBA(212, 45, 43, 255)).getRGB();
                            }

                            e.getContext().drawText(Fonts.REGULAR.getFont(6.0F), enchantmentText, x2 - enchantmentTextWidth / 2.0F, enchantmentY, new ColorRGBA(color));
                            enchantmentY -= 8.0F;
                        }
                    }

                    float scale = 0.7F;
                    float offset = -18.0F;
                    matrices.push();
                    matrices.translate(x2 + offset, iconY + offset, 0.0F);
                    matrices.scale(scale, scale, 1.0F);
                    int drawX = (int)(-offset);
                    int drawY = (int)(-offset);
                    e.getContext().drawItem(stack, drawX, drawY);
                    e.getContext().drawStackOverlay(mc.textRenderer, stack, drawX, drawY);
                    matrices.pop();
                }
            }
        }
    }

    private void renderItemTags(float tickDelta, EventRender2D e) {
        ColorRGBA themeDark = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().darker(0.92F).withAlpha(255);
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity)) continue;

            ItemEntity itemEntity = (ItemEntity) entity;
            if (!ProjectionUtil.canSee(itemEntity.getBoundingBox().getCenter())) {
                continue;
            }

            double x = MathHelper.lerp((double) tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp((double) tickDelta, entity.lastRenderY, entity.getY()) + (double) entity.getHeight() + 0.1D;
            double z = MathHelper.lerp((double) tickDelta, entity.lastRenderZ, entity.getZ());

            if (!mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
                Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
                Vec3d entityPosRel = new Vec3d(x, y, z).subtract(cameraPos);

                float pitch = mc.getEntityRenderDispatcher().camera.getPitch();
                float yaw = mc.getEntityRenderDispatcher().camera.getYaw();
                float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
                float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
                float f2 = -MathHelper.cos(-pitch * 0.017453292F);
                float f3 = MathHelper.sin(-pitch * 0.017453292F);
                Vec3d actualLookVec = new Vec3d(f1 * f2, f3, f * f2);

                if (entityPosRel.dotProduct(actualLookVec) < 0) {
                    continue;
                }
            }

            Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
            if (pos.z <= 0.0D || pos.z >= 1.0D) {
                continue;
            }

            Vector4d position = ProjectionUtil.getVector4D(entity);
            if (position == null) continue;

            float posY = (float) (position.y - 11.0D);
            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) continue;

            int rarityOrdinal = stack.getRarity().ordinal();
            Formatting rarityColor = switch (rarityOrdinal) {
                case 1 -> Formatting.YELLOW;
                case 2 -> Formatting.AQUA;
                case 3 -> Formatting.LIGHT_PURPLE;
                default -> Formatting.WHITE;
            };

            String itemName = stack.getName().getString();
            Text nameText = Text.literal(itemName).setStyle(Style.EMPTY.withColor(rarityColor));
            if (!stack.getName().getSiblings().isEmpty()) {
                nameText = stack.getName();
            }

            Text countComponent = stack.getCount() > 1 ? Text.literal(" х" + stack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)) : Text.empty();
            Text textComponent = nameText.copy().append(countComponent);
            float textWidth = Fonts.REGULAR.getFont(6.5F).width(textComponent);

            float iconSize = 8.0F;
            float totalTagWidth = textWidth + iconSize + 6.0F;
            float tagX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalTagWidth / 2.0F));

            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX, (float)(position.y - 14.5D), totalTagWidth, 12.0F, BorderRadius.all(1.5F), themeDark);

            MatrixStack matrices = e.getContext().getMatrices();
            float scale = 0.5F;
            float offset = -16.0F;
            matrices.push();
            matrices.translate(tagX + 2.0F + 8.0F * scale, (float)position.y - 14.5F + 6.0F, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            e.getContext().drawItem(stack, -8, -8);
            matrices.pop();

            e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), textComponent, tagX + iconSize + 4.0F, (float) position.y - 11.5F, 255.0F);
        }
    }

    public static void drawBox(double x, double y, double width, double height, double size, int color, BufferBuilder bufferbuilder) {
        drawRectBuilding(x + size, y, width - size, y + size, color, bufferbuilder);
        drawRectBuilding(x, y, x + size, height, color, bufferbuilder);
        drawRectBuilding(width - size, y, width, height, color, bufferbuilder);
        drawRectBuilding(x + size, height - size, width - size, height, color, bufferbuilder);
    }

    public static void drawBoxTest(double x, double y, double width, double height, double size, Vector4f colors, BufferBuilder bufferbuilder) {
        drawMCHorizontalBuilding(x + size, y, width - size, y + size, (int)colors.x(), (int)colors.y(), bufferbuilder);
        drawMCVerticalBuilding(width - size, y + size, width, height - size, (int)colors.y(), (int)colors.z(), bufferbuilder);
        drawMCHorizontalBuilding(x + size, height - size, width - size, height, (int)colors.w(), (int)colors.z(), bufferbuilder);
        drawMCVerticalBuilding(x, y + size, x + size, height - size, (int)colors.x(), (int)colors.w(), bufferbuilder);
    }

    public static void drawRectBuilding(double left, double top, double right, double bottom, int color, BufferBuilder bufferbuilder) {
        double j;
        if (left < right) {
            j = left;
            left = right;
            right = j;
        }

        if (top < bottom) {
            j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        bufferbuilder.vertex((float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
        bufferbuilder.vertex((float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
        bufferbuilder.vertex((float)right, (float)top, 0.0F).color(f, f1, f2, f3);
        bufferbuilder.vertex((float)left, (float)top, 0.0F).color(f, f1, f2, f3);
    }

    public static void drawMCHorizontalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
        float a1 = (float)(start >> 24 & 255) / 255.0F;
        float r1 = (float)(start >> 16 & 255) / 255.0F;
        float g1 = (float)(start >> 8 & 255) / 255.0F;
        float b1 = (float)(start & 255) / 255.0F;
        float a2 = (float)(end >> 24 & 255) / 255.0F;
        float r2 = (float)(end >> 16 & 255) / 255.0F;
        float g2 = (float)(end >> 8 & 255) / 255.0F;
        float b2 = (float)(end & 255) / 255.0F;
        bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r1, g1, b1, a1);
        bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
        bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r2, g2, b2, a2);
        bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
    }

    public static void drawMCVerticalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
        float a1 = (float)(start >> 24 & 255) / 255.0F;
        float r1 = (float)(start >> 16 & 255) / 255.0F;
        float g1 = (float)(start >> 8 & 255) / 255.0F;
        float b1 = (float)(start & 255) / 255.0F;
        float a2 = (float)(end >> 24 & 255) / 255.0F;
        float r2 = (float)(end >> 16 & 255) / 255.0F;
        float g2 = (float)(end >> 8 & 255) / 255.0F;
        float b2 = (float)(end & 255) / 255.0F;
        bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r2, g2, b2, a2);
        bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
        bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r1, g1, b1, a1);
        bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
    }
}