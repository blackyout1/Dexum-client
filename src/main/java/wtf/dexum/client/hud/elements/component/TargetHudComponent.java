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
    private final boolean v2;

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align, boolean v2) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
        this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
        this.toggleAnimationMetanoise = new Animation(1850L, Easing.CUBIC_OUT);
        this.v2 = v2;
    }

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
        this(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align, false);
    }

    @Native
    public void render(CustomDrawContext ctx) {
        Aura aura = Aura.INSTANCE;
        LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
        this.setTarget((LivingEntity)target);
        if (this.toggleAnimationMetanoise.getValue() != 0.0F && this.target != null) {
            if (v2) {
                renderV2(ctx, this.target, this.toggleAnimation.getValue());
            } else {
                renderClassic(ctx, this.target, this.toggleAnimation.getValue());
            }
        }
    }

    private void renderV2(CustomDrawContext ctx, LivingEntity target, float animation) {
        float posX = this.getX();
        float posY = this.getY();
        float width = 100.0F;
        float height = 36.0F;
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();

        this.healthAnimation.update(hp / target.getMaxHealth());
        if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
            this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
            this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
        } else {
            this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
        }
        this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());

        int a = (int)(255 * animation);
        Vector4f rectRounding = new Vector4f(6.0F, 6.0F, 6.0F, 6.0F);
        ColorRGBA bgColor = ColorRGBA.BLACK.withAlpha(a);

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, width, height, BorderRadius.all(rectRounding.x), bgColor);

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

        float headSize = 26.0F;
        DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, posX + 5.0F, posY + 5.0F, headSize, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(a));

        float nameX = posX + 5.0F + headSize + 5.0F;
        String name = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
        if (name.length() > 9) {
            name = name.substring(0, 9);
        }
        ctx.drawText(Fonts.MEDIUM.getFont(8.0F), name, nameX, posY + 6.75F, ColorRGBA.WHITE.withAlpha(a));

        String hpText = "HP: " + String.format("%.1f", hp).replace(",", ".");
        if (target.getAbsorptionAmount() > 0.0F) {
            hpText += " + " + String.format("%.1f", target.getAbsorptionAmount()).replace(",", ".");
        }
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), hpText, nameX, posY + 16.25F, new ColorRGBA(180, 180, 180, 255.0F * animation));

        float barX = nameX - 0.5F;
        float barY = posY + height - 10.5F;
        float barWidth = width - (nameX - posX) - 5.0F;
        float barHeight = 5.5F;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(0.5F),
                theme.getSecondColor().darker(0.5F).withAlpha(a),
                theme.getSecondColor().darker(0.5F).withAlpha(a),
                theme.getColor().darker(0.5F).withAlpha(a),
                theme.getColor().darker(0.5F).withAlpha(a));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.outdatedHealthAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F),
                theme.getSecondColor().darker(0.35F).withAlpha(a),
                theme.getSecondColor().darker(0.35F).withAlpha(a),
                theme.getColor().darker(0.35F).withAlpha(a),
                theme.getColor().darker(0.35F).withAlpha(a));

        if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.healthAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F),
                    theme.getSecondColor().withAlpha(a),
                    theme.getSecondColor().withAlpha(a),
                    theme.getColor().withAlpha(a),
                    theme.getColor().withAlpha(a));
        }

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.gappleAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F),
                new ColorRGBA(255, 209, 0, a),
                new ColorRGBA(255, 209, 0, a),
                new ColorRGBA(255, 246, 20, a),
                new ColorRGBA(255, 246, 20, a));

        if (target instanceof PlayerEntity) {
            this.drawArmor(ctx, (PlayerEntity)target, posX + 3.0F, posY - 12.0F, animation);
        }

        this.width = width;
        this.height = height;
    }

    @Native
    private void renderClassic(CustomDrawContext ctx, LivingEntity target, float animation) {
        float posX = this.getX();
        float posY = this.getY();
        float width = 100.0F;
        float height = 38.0F;
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
        this.healthAnimation.update(hp / target.getMaxHealth());
        if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
            this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
            this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
        } else {
            this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
        }

        this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());

        Vector4f rectRounding = new Vector4f(4.0F, 4.0F, 4.0F, 4.0F);
        ColorRGBA bgColor = new ColorRGBA(0, 0, 0, (int)(125 * animation));

        DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, width, height, 15.0F, BorderRadius.all(rectRounding.x), ColorRGBA.WHITE.withAlpha((int)(255 * animation)));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, width, height, BorderRadius.all(rectRounding.x), bgColor);

        Identifier skinTextures = null;
        Iterator var11 = mc.getNetworkHandler().getPlayerList().iterator();

        while(var11.hasNext()) {
            PlayerListEntry playerListEntry = (PlayerListEntry)var11.next();
            if (playerListEntry.getProfile().getName().equals(target.getNameForScoreboard())) {
                skinTextures = playerListEntry.getSkinTextures().texture();
            }
        }

        if (skinTextures == null) {
            skinTextures = DefaultSkinHelper.getSteve().texture();
        }

        float headSize = 24.5F;
        DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, posX + 4.5F, posY + 2.0F, headSize, BorderRadius.all(10.0F), ColorRGBA.WHITE.withAlpha(animation * 255.0F));

        float nameX = posX + 4.5F + headSize + 3.2F;
        String name = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
        if (name.length() > 8) {
            name = name.substring(0, 8);
        }
        ctx.drawText(Fonts.MEDIUM.getFont(8.5F), name, nameX, posY + 6.7F, ColorRGBA.WHITE.withAlpha(animation * 255.0F));

        String hpText = String.format("%.1f", hp).replace(",", ".") + "hp";
        float hpTextWidth = Fonts.REGULAR.getWidth(hpText, 7.0F);
        ctx.drawText(Fonts.REGULAR.getFont(7.0F), hpText, posX + width - hpTextWidth - 5.0F, posY + 7.2f, new ColorRGBA(255, 255, 255, 180.0F * animation));

        float barPadding = 5.0F;
        float barX = posX + barPadding;
        float barY = posY + height - 9.0F;
        float barWidth = width - barPadding * 2.0F;
        float barHeight = 5.0F;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(0.5F), theme.getSecondColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getSecondColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getColor().darker(0.5F).withAlpha(animation * 255.0F), theme.getColor().darker(0.5F).withAlpha(animation * 255.0F));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.outdatedHealthAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getColor().darker(0.35F).withAlpha(animation * 255.0F), theme.getColor().darker(0.35F).withAlpha(animation * 255.0F));

        if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.healthAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F));
        }

        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barWidth * this.gappleAnimation.getValue(), 0.0F, barWidth), barHeight, BorderRadius.all(0.5F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F));

        if (target instanceof PlayerEntity && animation > 0.01F) {
            this.drawArmor(ctx, (PlayerEntity)target, nameX - 1.0F, posY + 16.5F, animation);
        }

        this.width = width;
        this.height = height;
    }

    private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float animation) {
        float boxSizeItem = 10.0F;
        float paddingItem = 0.0F;
        float iconX = posX + (5.0F - animation * 5.0F);
        float iconY = posY + 1.0F + (5.0F - animation * 5.0F);
        List<ItemStack> armor = player.getInventory().armor;
        ItemStack[] items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), (ItemStack)armor.get(3), (ItemStack)armor.get(2), (ItemStack)armor.get(1), (ItemStack)armor.get(0)};

        for(ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate((double)iconX + ((double)boxSizeItem - 9.6D) / 2.0D, (double)iconY + ((double)boxSizeItem - 9.6D) / 2.0D, 0.0D);
                ctx.getMatrices().scale(0.6F * animation, 0.6F * animation, 0.6F * animation);
                ctx.drawItem(stack, 0, 0);
                ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
                ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
                ctx.getMatrices().pop();
                iconX += boxSizeItem + paddingItem;
            }
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