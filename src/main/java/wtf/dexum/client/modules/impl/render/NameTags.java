package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.render.EventRenderName;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.NumberSetting;
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
public class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();
    
    // Настройки отображения
    private final MultiBooleanSetting display = new MultiBooleanSetting("Отображать",
            new MultiBooleanSetting.Value("Player", true),
            new MultiBooleanSetting.Value("Mobs", false),
            new MultiBooleanSetting.Value("Item", true)
    );
    
    private final BooleanSetting showName = new BooleanSetting("Показывать имя", true);
    private final BooleanSetting showHP = new BooleanSetting("Показывать HP", true);
    private final BooleanSetting showDistance = new BooleanSetting("Показывать дистанцию", false);
    private final BooleanSetting showPing = new BooleanSetting("Показывать пинг", false);
    private final BooleanSetting showFriendTag = new BooleanSetting("Показывать тег друга [F]", true);
    private final BooleanSetting hpBarUnderName = new BooleanSetting("HP бар под неймтегом", false);
    
    // Настройки предметов
    private final BooleanSetting showArmor = new BooleanSetting("Отображать броню", true);
    private final BooleanSetting showMainHand = new BooleanSetting("Правая рука", true);
    private final BooleanSetting showOffHand = new BooleanSetting("Левая рука", true);
    private final BooleanSetting offHandItemName = new BooleanSetting("Имя предмета левой руки", true);
    
    // Другие настройки
    private final NumberSetting size = new NumberSetting("Размер", 1.0f, 0.5f, 2.0f, 0.1f);
    private final NumberSetting backgroundAlpha = new NumberSetting("Прозрачность фона", 200.0f, 0.0f, 255.0f, 5.0f);
    private final BooleanSetting box3D = new BooleanSetting("3D Box", false);

    public NameTags() {
    }

    @EventTarget
    public void onRenderName(EventRenderName event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    private void onRender2D(EventRender2D e) {
        if (mc.world != null && mc.player != null) {
            float tickDelta = e.getTickDelta();
            
            // Игроки
            if (display.isEnable("Player")) {
                this.renderPlayerTags(tickDelta, e);
            }
            
            // Мобы
            if (display.isEnable("Mobs")) {
                this.renderMobTags(tickDelta, e);
            }
            
            // Предметы
            if (display.isEnable("Item")) {
                this.renderItemTags(tickDelta, e);
            }
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

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            Box localBox = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
            Render3DUtil.drawBox(localBox.offset(x, y, z), color, 1.3f, true, true, false);
        }
    }

    private void renderPlayerTags(float tickDelta, EventRender2D e) {
        // Проверяем включено ли отображение игроков
        if (!display.isEnable("Player")) {
            return;
        }
        
        ColorRGBA themeDark = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().darker(0.92F);
        int alpha = (int) backgroundAlpha.getCurrent();
        themeDark = themeDark.withAlpha(alpha);
        
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

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.2D;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

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

            float scale = size.getCurrent();
            float posY = (float)(position.y - 11.0D);
            
            // Вычисляем расстояние
            double distance = mc.player.getPos().distanceTo(entity.getPos());
            
            // Формируем текст
            Text nameText = Text.empty();
            boolean isFriend = Dexum.getInstance().getFriendManager().isFriend(entity.getNameForScoreboard());
            
            // Тег друга
            if (showFriendTag.isEnabled() && isFriend) {
                nameText = nameText.copy().append(Text.literal("[F] ").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            }
            
            // Имя
            if (showName.isEnabled()) {
                Text name = entity == mc.player && NameProtect.INSTANCE.isEnabled() 
                    ? Text.literal(NameProtect.getCustomName()) 
                    : ReplaceUtil.replaceSymbols(entity.getDisplayName());
                nameText = nameText.copy().append(name);
            }
            
            // HP
            float hp = ScoreboardHealth.INSTANCE.isEnabled() && entity != mc.player 
                ? PlayerIntersectionUtil.getHealth(entity) 
                : entity.getHealth();
                
            if (showHP.isEnabled()) {
                nameText = nameText.copy()
                    .append(Text.literal(" ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                    .append(Text.literal(String.format("%.1f", hp)).setStyle(Style.EMPTY.withColor(Formatting.RED)));
            }
            
            // Дистанция
            if (showDistance.isEnabled()) {
                nameText = nameText.copy()
                    .append(Text.literal(" " + String.format("%.0f", distance) + "m").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            }
            
            // Пинг
            if (showPing.isEnabled() && entity instanceof AbstractClientPlayerEntity player) {
                int ping = mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null 
                    ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()).getLatency() 
                    : 0;
                nameText = nameText.copy()
                    .append(Text.literal(" " + ping + "ms").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
            }

            float textWidth = Fonts.REGULAR.getWidth(nameText.getString(), 6.5F * scale);
            float headSize = 8.0F * scale;
            float tagPadding = 2.0F * scale;
            float totalTagWidth = headSize + tagPadding + textWidth + tagPadding * 2;
            float tagX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalTagWidth / 2.0F));
            
            // Высота с учетом HP бара
            float tagHeight = 10.0F * scale;
            float hpBarHeight = hpBarUnderName.isEnabled() && showHP.isEnabled() ? 3.0F * scale : 0;
            float totalHeight = tagHeight + hpBarHeight;
            
            float tagY = posY - 1.0F * scale;

            // Фон тега
            ColorRGBA bgColor = isFriend && showFriendTag.isEnabled() 
                ? new ColorRGBA(0, 166, 0, alpha) 
                : themeDark;
            
            // Тень
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX - 0.5F, tagY - 0.5F + 1.0F, totalTagWidth + 1.0F, totalHeight + 1.0F, BorderRadius.all(2.0F), new ColorRGBA(0, 0, 0, alpha / 3));
            
            // Основной фон
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX, tagY, totalTagWidth, totalHeight, BorderRadius.all(2.0F), bgColor);

            // Голова игрока
            DrawUtil.drawPlayerHeadWithRoundedShader(
                e.getContext().getMatrices(), 
                entity instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity)entity).getSkinTextures().texture() : DefaultSkinHelper.getSteve().texture(), 
                tagX + tagPadding, 
                tagY + 1.0F * scale, 
                headSize, 
                BorderRadius.all(1.5F), 
                ColorRGBA.WHITE
            );

            // Текст
            e.getContext().drawText(Fonts.REGULAR.getFont(6.5F * scale), nameText, tagX + headSize + tagPadding * 2, tagY + 2.0F * scale, 255.0F);

            // HP бар под неймтегом (как в Delta)
            if (hpBarUnderName.isEnabled() && showHP.isEnabled()) {
                float maxHp = entity.getMaxHealth();
                float hpRatio = MathHelper.clamp(hp / maxHp, 0.0f, 1.0f);
                
                float barWidth = totalTagWidth - 4.0F * scale;
                float barHeight = 2.5F * scale;
                float barX = tagX + 2.0F * scale;
                float barY = tagY + tagHeight + 0.5F * scale;
                
                // Фон HP бара
                DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(1.0F), new ColorRGBA(0, 0, 0, 100));
                
                // HP бар с градиентом
                ColorRGBA hpColor;
                if (hpRatio > 0.75f) {
                    hpColor = new ColorRGBA(85, 255, 85, 255);
                } else if (hpRatio > 0.5f) {
                    hpColor = new ColorRGBA(170, 255, 85, 255);
                } else if (hpRatio > 0.25f) {
                    hpColor = new ColorRGBA(255, 255, 85, 255);
                } else {
                    hpColor = new ColorRGBA(255, 85, 85, 255);
                }
                DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barWidth * hpRatio, barHeight, BorderRadius.all(1.0F), hpColor);
            }

            // Предметы
            if (showArmor.isEnabled()) {
                ItemStack[] itemArray = new ItemStack[6];
                int[] itemTypes = new int[6]; // 0 = armor, 1 = mainHand, 2 = offHand
                int itemCount = 0;
                EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

                for (EquipmentSlot slot : slots) {
                    ItemStack stack = entity.getEquippedStack(slot);
                    if (!stack.isEmpty()) {
                        itemArray[itemCount] = stack;
                        itemTypes[itemCount] = 0; // armor
                        itemCount++;
                    }
                }

                if (showMainHand.isEnabled()) {
                    ItemStack mainHand = entity.getMainHandStack();
                    if (!mainHand.isEmpty()) {
                        itemArray[itemCount] = mainHand;
                        itemTypes[itemCount] = 1; // mainHand
                        itemCount++;
                    }
                }

                if (showOffHand.isEnabled()) {
                    ItemStack offHand = entity.getOffHandStack();
                    if (!offHand.isEmpty()) {
                        itemArray[itemCount] = offHand;
                        itemTypes[itemCount] = 2; // offHand
                        itemCount++;
                    }
                }

                if (itemCount > 0) {
                    float iconSize = 16.0F * scale;
                    float spacing = 0.0F;
                    float totalWidth = (float)itemCount * iconSize + (float)(itemCount - 1) * spacing;
                    float startX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalWidth / 2.0F) + 7.5D * scale);
                    float iconY = posY - 12.0F * scale;
                    MatrixStack matrices = e.getContext().getMatrices();

                    for (int i = 0; i < itemCount; ++i) {
                        ItemStack stack = itemArray[i];
                        int itemType = itemTypes[i];
                        if (stack != null && !stack.isEmpty()) {
                            float x2 = startX + (float)i * (iconSize + spacing);
                            ItemEnchantmentsComponent enchComp = EnchantmentHelper.getEnchantments(stack);

                            if (!enchComp.isEmpty()) {
                                Map<RegistryEntry<Enchantment>, Integer> enchMap = enchComp.getEnchantmentEntries().stream()
                                        .collect(Collectors.toMap(Entry::getKey, it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue));
                                float enchantmentY = iconY - 16.0F * scale;

                                for (Map.Entry<RegistryEntry<Enchantment>, Integer> enchEntry : enchMap.entrySet()) {
                                    int lvl = enchEntry.getValue();
                                    if (lvl <= 0) continue;

                                    String fullName = Enchantment.getName(enchEntry.getKey(), lvl).getString();
                                    String shortName = fullName.length() > 2 ? fullName.substring(0, 2) : fullName;
                                    String enchantmentText = shortName + lvl;
                                    float enchantmentTextWidth = Fonts.REGULAR.getWidth(enchantmentText, 6.0F * scale);
                                    int color = -1;
                                    if ((shortName.equalsIgnoreCase("Sh") && lvl > 5) || (shortName.equalsIgnoreCase("Pr") && lvl > 4)) {
                                        color = (new ColorRGBA(212, 45, 43, 255)).getRGB();
                                    }

                                    e.getContext().drawText(Fonts.REGULAR.getFont(6.0F * scale), enchantmentText, x2 - enchantmentTextWidth / 2.0F, enchantmentY, new ColorRGBA(color));
                                    enchantmentY -= 8.0F * scale;
                                }
                            }

                            // Название предмета в левой руке (itemType == 2 означает offHand)
                            if (offHandItemName.isEnabled() && itemType == 2) {
                                String itemName = stack.getName().getString();
                                float itemNameWidth = Fonts.REGULAR.getWidth(itemName, 5.5F * scale);
                                e.getContext().drawText(
                                    Fonts.REGULAR.getFont(5.5F * scale), 
                                    itemName, 
                                    x2 + (iconSize - itemNameWidth) / 2.0F, 
                                    iconY + iconSize + 2.0F * scale, 
                                    new ColorRGBA(255, 255, 255, 255)
                                );
                            }

                            float itemScale = 0.7F * scale;
                            float offset = -18.0F;
                            matrices.push();
                            matrices.translate(x2 + offset, iconY + offset, 0.0F);
                            matrices.scale(itemScale, itemScale, 1.0F);
                            int drawX = (int)(-offset);
                            int drawY = (int)(-offset);
                            e.getContext().drawItem(stack, drawX, drawY);
                            e.getContext().drawStackOverlay(mc.textRenderer, stack, drawX, drawY);
                            matrices.pop();
                        }
                    }
                }
            }
        }
    }

    private void renderMobTags(float tickDelta, EventRender2D e) {
        ColorRGBA themeDark = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().darker(0.92F);
        int alpha = (int) backgroundAlpha.getCurrent();
        themeDark = themeDark.withAlpha(alpha);
        float scale = size.getCurrent();
        
        for (Entity entity : mc.world.getEntities()) {
            // Пропускаем игроков и предметы
            if (entity instanceof PlayerEntity || entity instanceof ItemEntity) {
                continue;
            }
            
            // Только живые сущности (мобы, животные)
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (!livingEntity.isAlive() || livingEntity.isRemoved()) {
                continue;
            }

            if (!ProjectionUtil.canSee(livingEntity.getBoundingBox().getCenter())) {
                continue;
            }

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.2D;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

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
            
            // Формируем текст
            Text nameText = Text.empty();
            
            // Имя моба
            if (showName.isEnabled()) {
                String mobName = livingEntity.getDisplayName().getString();
                nameText = Text.literal(mobName).setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
            }
            
            // HP
            if (showHP.isEnabled()) {
                float hp = livingEntity.getHealth();
                nameText = nameText.copy()
                    .append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                    .append(Text.literal(String.valueOf((int)hp)).setStyle(Style.EMPTY.withColor(Formatting.RED)))
                    .append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            }
            
            // Дистанция
            if (showDistance.isEnabled()) {
                double distance = mc.player.getPos().distanceTo(entity.getPos());
                nameText = nameText.copy()
                    .append(Text.literal(" " + String.format("%.1f", distance) + "m").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            }

            if (nameText.getString().isEmpty()) {
                continue;
            }

            float textWidth = Fonts.REGULAR.getWidth(nameText.getString(), 6.5F * scale);
            float totalTagWidth = textWidth + 6.0F * scale;
            float tagX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalTagWidth / 2.0F));
            float tagHeight = 10.0F * scale;

            // Фон
            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX, posY - 2.5F * scale, totalTagWidth, tagHeight, BorderRadius.all(2.0F), themeDark);

            // Текст
            e.getContext().drawText(Fonts.REGULAR.getFont(6.5F * scale), nameText, tagX + 3.0F * scale, posY, 255.0F);

            // HP бар
            if (hpBarUnderName.isEnabled() && showHP.isEnabled()) {
                float hp = livingEntity.getHealth();
                float maxHp = livingEntity.getMaxHealth();
                float hpRatio = MathHelper.clamp(hp / maxHp, 0.0f, 1.0f);
                
                float barWidth = totalTagWidth - 4.0F * scale;
                float barHeight = 2.0F * scale;
                float barX = tagX + 2.0F * scale;
                float barY = posY + tagHeight - 3.0F * scale;
                
                DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(1.0F), new ColorRGBA(0, 0, 0, 150));
                
                ColorRGBA hpColor = hpRatio > 0.5f ? new ColorRGBA(85, 255, 85, 255) : hpRatio > 0.25f ? new ColorRGBA(255, 255, 85, 255) : new ColorRGBA(255, 85, 85, 255);
                DrawUtil.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barWidth * hpRatio, barHeight, BorderRadius.all(1.0F), hpColor);
            }
        }
    }

    private void renderItemTags(float tickDelta, EventRender2D e) {
        // Проверяем включено ли отображение предметов
        if (!display.isEnable("Item")) {
            return;
        }
        
        ColorRGBA themeDark = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().darker(0.92F);
        int alpha = (int) backgroundAlpha.getCurrent();
        themeDark = themeDark.withAlpha(alpha);
        float scale = size.getCurrent();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            if (!ProjectionUtil.canSee(itemEntity.getBoundingBox().getCenter())) {
                continue;
            }

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + (double) entity.getHeight() + 0.1D;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

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
            float textWidth = Fonts.REGULAR.getFont(6.5F * scale).width(textComponent);

            float iconSize = 8.0F * scale;
            float totalTagWidth = textWidth + iconSize + 6.0F * scale;
            float tagX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalTagWidth / 2.0F));

            DrawUtil.drawRoundedRect(e.getContext().getMatrices(), tagX, (float)(position.y - 14.5D), totalTagWidth, 12.0F * scale, BorderRadius.all(1.5F), themeDark);

            MatrixStack matrices = e.getContext().getMatrices();
            float itemScale = 0.5F * scale;
            float offset = -16.0F;
            matrices.push();
            matrices.translate(tagX + 2.0F * scale + 8.0F * itemScale, (float)position.y - 14.5F * scale + 6.0F * scale, 0.0F);
            matrices.scale(itemScale, itemScale, 1.0F);
            e.getContext().drawItem(stack, -8, -8);
            matrices.pop();

            e.getContext().drawText(Fonts.REGULAR.getFont(6.5F * scale), textComponent, tagX + iconSize + 4.0F * scale, (float) position.y - 11.5F * scale, 255.0F);
        }
    }
}