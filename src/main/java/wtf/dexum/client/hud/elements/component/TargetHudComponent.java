package wtf.dexum.client.hud.elements.component;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.impl.combat.Aura;
import wtf.dexum.client.modules.impl.misc.NameProtect;
import wtf.dexum.client.modules.impl.misc.ScoreboardHealth;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.mixin.accessors.DrawContextAccessor;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

public class TargetHudComponent extends DraggableHudElement {
    private final Animation healthAnimation;
    private final Animation outdatedHealthAnimation;
    private final Animation gappleAnimation;
    private final Animation toggleAnimation;
    private final Animation toggleAnimationMetanoise;
    private LivingEntity target;

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
        this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimationMetanoise = new Animation(1850L, Easing.CUBIC_OUT);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        Aura aura = Aura.INSTANCE;
        LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
        this.setTarget((LivingEntity)target);
        if (this.toggleAnimationMetanoise.getValue() != 0.0F && this.target != null) {
            renderClassic(ctx, this.target, this.toggleAnimation.getValue());
        }
    }

    @Native
    private void renderClassic(CustomDrawContext ctx, LivingEntity target, float animation) {
        float posX = this.getX();
        float posY = this.getY();
        float width = 160.0F; // компактнее
        float height = 50.0F; // нормальная высота
        ColorRGBA hudC = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        ColorRGBA hudC2 = Dexum.getInstance().getThemeManager().getCurrentTheme().getSecondColor();
        ColorRGBA hudBg = wtf.dexum.client.modules.impl.render.Interface.getHudColor();
        
        float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
        float maxHp = target.getMaxHealth();

        this.healthAnimation.update(hp / maxHp);
        if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
            this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
            this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
        } else {
            this.outdatedHealthAnimation.update(hp / maxHp);
        }
        this.gappleAnimation.update(target.getAbsorptionAmount() / maxHp);

        int a = (int)(255 * animation);
        Vector4f rectRounding = new Vector4f(8.0F, 8.0F, 8.0F, 8.0F);

        // Фон из настроек палитры
        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, width, height, BorderRadius.all(rectRounding.x), hudBg.withAlpha(a));

        // Получаем скин игрока
        Identifier skinTextures = null;
        Iterator<PlayerListEntry> var11 = mc.getNetworkHandler().getPlayerList().iterator();
        while(var11.hasNext()) {
            PlayerListEntry playerListEntry = var11.next();
            if (playerListEntry.getProfile().getName().equals(target.getNameForScoreboard())) {
                skinTextures = playerListEntry.getSkinTextures().texture();
            }
        }
        if (skinTextures == null) {
            skinTextures = DefaultSkinHelper.getSteve().texture();
        }

        // Голова игрока слева (больше)
        float headSize = 40.0F;
        DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, posX + 5.0F, posY + 5.0F, headSize, BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha(a));

        float contentX = posX + headSize + 10.0F;
        
        // Ник игрока сверху (крупный)
        String name = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
        ctx.drawText(Fonts.MEDIUM.getFont(11.0F), name, contentX, posY + 5.0F, ColorRGBA.WHITE.withAlpha(a));

        // Броня (4 иконки) + предмет левой руки справа (под ником) 
        if (target instanceof PlayerEntity) {
            this.drawArmorAndOffhand(ctx, (PlayerEntity)target, contentX, posY + 18.0F, animation, a);
        }

        // HP текст: "20/20hp" слева внизу
        String hpText = String.format("%.0f/%.0fhp", hp, maxHp);
        float hpY = posY + height - 16.0F;
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), hpText, contentX, hpY, ColorRGBA.WHITE.withAlpha(a));
        
        // "100%" рядом
        String percentText = String.format("%.0f%%", (hp / maxHp) * 100);
        float percentX = contentX + Fonts.REGULAR.getWidth(hpText, 7.0F) + 5.0F;
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), percentText, percentX, hpY, ColorRGBA.WHITE.withAlpha(a));

        // HP bar снизу (градиентный от темы)
        float barX = contentX;
        float barY = posY + height - 7.0F;
        float barWidth = width - (contentX - posX) - 8.0F;
        float barHeight = 3.5F;

        // Фон бара
        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(2.0F),
                new ColorRGBA(200, 200, 200, a));

        // Градиентный HP bar
        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, 
                MathHelper.clamp(barWidth * this.healthAnimation.getValue(), 0.0F, barWidth), barHeight, 
                BorderRadius.all(2.0F),
                hudC2.withAlpha(a),
                hudC2.withAlpha(a),
                hudC.withAlpha(a),
                hudC.withAlpha(a));

        // Золотые сердца (поглощение урона)
        if (target.getAbsorptionAmount() > 0.0F) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, 
                    MathHelper.clamp(barWidth * this.gappleAnimation.getValue(), 0.0F, barWidth), barHeight, 
                    BorderRadius.all(2.0F),
                    new ColorRGBA(255, 209, 0, a),
                    new ColorRGBA(255, 209, 0, a),
                    new ColorRGBA(255, 246, 20, a),
                    new ColorRGBA(255, 246, 20, a));
        }

        this.width = width;
        this.height = height;
    }
    
    // Отрисовка брони (4 иконки) + предмет левой руки справа
    private void drawArmorAndOffhand(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float animation, int alpha) {
        float boxSize = 16.0F; // стандартный размер слота
        float padding = 3.0F;
        float iconX = posX;
        List<ItemStack> armor = player.getInventory().armor;
        
        // Порядок: шлем, нагрудник, штаны, ботинки (4 иконки брони)
        ItemStack[] armorItems = new ItemStack[]{
            armor.get(3), // шлем
            armor.get(2), // нагрудник
            armor.get(1), // штаны
            armor.get(0)  // ботинки
        };

        // Рисуем 4 иконки брони
        for(int i = 0; i < armorItems.length; i++) {
            ItemStack stack = armorItems[i];
            if (!stack.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(iconX, posY, 0.0);
                ctx.getMatrices().scale(animation, animation, animation);
                ctx.drawItem(stack, 0, 0);
                ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
                ctx.getMatrices().pop();
            }
            iconX += boxSize + padding;
        }
        
        // Предмет левой руки справа (с отступом)
        ItemStack offHandStack = player.getOffHandStack();
        if (!offHandStack.isEmpty()) {
            float offhandX = iconX + 5.0F; // дополнительный отступ
            
            // Название предмета СВЕРХУ над иконкой (компактно)
            String itemName = offHandStack.getName().getString();
            if (itemName.length() > 10) {
                itemName = itemName.substring(0, 10) + "..";
            }
            
            float textWidth = Fonts.REGULAR.getWidth(itemName, 6.0F);
            float textX = offhandX + (boxSize - textWidth) / 2.0F - 10.0F; // сдвигаем левее на 10px
            float textY = posY - 8.0F; // над иконкой
            ctx.drawText(Fonts.REGULAR.getFont(6.0F), itemName, 
                        textX, textY, 
                        new ColorRGBA(200, 200, 200, alpha));
            
            // Сама иконка
            ctx.getMatrices().push();
            ctx.getMatrices().translate(offhandX, posY, 0.0);
            ctx.getMatrices().scale(animation, animation, animation);
            ctx.drawItem(offHandStack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawItemBar(offHandStack, 0, 0);
            ctx.getMatrices().pop();
        }
    }

    public void setTarget(LivingEntity target) {
        if (target == null) {
            this.toggleAnimation.update(0.0F);
            this.toggleAnimationMetanoise.update(0.0F);
            this.toggleAnimationMetanoise.setDuration(2200L);
            this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
            if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
                this.target = null;
            }
        } else {
            this.target = target;
            this.toggleAnimationMetanoise.update(1.0F);
            this.toggleAnimationMetanoise.setDuration(1300L);
            this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
            this.toggleAnimation.update(1.0F);
        }

    }
}