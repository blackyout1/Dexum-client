package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.events.impl.render.EventRenderName;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.utility.math.ProjectionUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleAnnotation(
        name = "NameTags",
        category = Category.RENDER,
        description = "Отображает теги над сущностями"
)
public class NameTags extends Module {
    public static final NameTags INSTANCE = new NameTags();
    public static Map<PlayerEntity, double[]> entityPositions = new HashMap<>();
    public static Map<ItemEntity, double[]> itemPositions = new HashMap<>();
    private static final List<PendingItemRender> pendingItems = new ArrayList<>();

    public MultiBooleanSetting entityType = new MultiBooleanSetting("Отображать",
            new MultiBooleanSetting.Value("Player", true),
            new MultiBooleanSetting.Value("Mobs", false),
            new MultiBooleanSetting.Value("Item", true)
    );
    public BooleanSetting armor = new BooleanSetting("Отображать броню", true);
    public BooleanSetting showMainHand = new BooleanSetting("Правая рука", true);
    public BooleanSetting showOffHand = new BooleanSetting("Левая рука", true);

    public NameTags() {
    }

    @EventTarget
    public void onRenderName(EventRenderName event) {
        if (this.isEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        this.updatePositions(event.getPartialTicks());
    }

    private void updatePositions(float tickDelta) {
        entityPositions.clear();
        if (mc.world != null && mc.player != null) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player) {
                    double x = MathHelper.lerp(tickDelta, p.lastRenderX, p.getX());
                    double y = MathHelper.lerp(tickDelta, p.lastRenderY, p.getY());
                    double z = MathHelper.lerp(tickDelta, p.lastRenderZ, p.getZ());
                    Vec3d headPos = new Vec3d(x, y + p.getHeight() + 0.35, z);
                    Vec3d legsPos = new Vec3d(x, y - 0.35, z);
                    Vec3d headScreen = ProjectionUtil.worldSpaceToScreenSpace(headPos);
                    Vec3d legsScreen = ProjectionUtil.worldSpaceToScreenSpace(legsPos);
                    if (headScreen != null
                            && legsScreen != null
                            && headScreen.z >= 0.0
                            && headScreen.z <= 1.0
                            && legsScreen.z >= 0.0
                            && legsScreen.z <= 1.0) {
                        entityPositions.put(
                                p,
                                new double[]{
                                        headScreen.x,
                                        headScreen.y,
                                        headScreen.z,
                                        legsScreen.x,
                                        legsScreen.y,
                                        legsScreen.z
                                }
                        );
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.world != null && mc.player != null) {
            CustomDrawContext ctx = event.getContext();
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

            for (PlayerEntity entity : entityPositions.keySet()) {
                if (entity != mc.player) {
                    double[] pos = entityPositions.get(entity);
                    if (pos != null && !(pos[2] < 0.0) && !(pos[2] > 1.0)) {
                        Vec3d entityPos = entity.getPos();
                        double distance = cameraPos.distanceTo(entityPos);
                        double baseDistance = 5.0;
                        float scale = (float) (baseDistance / Math.max(distance, 5.0));
                        scale = MathHelper.clamp(scale, 0.5F, 2.0F);

                        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
                        boolean isFriend = Dexum.getInstance().getFriendManager().isFriend(entity.getName().getString());

                        ColorRGBA bgColor = theme.getColor().darker(0.9F).withAlpha(255);

                        ctx.pushMatrix();
                        ctx.getMatrices().translate((float) pos[0], (float) pos[1], 0);
                        ctx.getMatrices().scale(scale, scale, 1.0f);

                        String name = entity.getName().getString();
                        float hp = entity.getHealth();
                        String hpText = "[" + String.format("%.1f", hp).replace(",", ".") + "]";
                        String friendTag = isFriend ? "[F] " : "";

                        float fontSize = 8.5F;
                        float friendTagWidth = isFriend ? Fonts.MEDIUM.getWidth(friendTag, fontSize) : 0.0F;
                        float nameWidth = Fonts.MEDIUM.getWidth(name, fontSize);
                        float spaceWidth = Fonts.MEDIUM.getWidth(" ", fontSize);
                        float hpWidth = Fonts.MEDIUM.getWidth(hpText, fontSize);
                        float totalWidth = friendTagWidth + nameWidth + spaceWidth + hpWidth;

                        float width = totalWidth + 6.0F;
                        float height = 13.0F;

                        ctx.drawRoundedRect(-width / 2.0F, -height / 2.0F, width, height, BorderRadius.all(2.0F), bgColor);

                        float textX = -width / 2.0F + 3.0F;
                        float textY = -height / 2.0F + 2.8F;

                        if (isFriend) {
                            ctx.drawText(Fonts.MEDIUM.getFont(fontSize), "[F] ", textX, textY, new ColorRGBA(85, 255, 85, 255));
                            textX += friendTagWidth;
                        }

                        ctx.drawText(Fonts.MEDIUM.getFont(fontSize), name, textX, textY, ColorRGBA.WHITE);
                        textX += nameWidth + spaceWidth;
                        ctx.drawText(Fonts.REGULAR.getFont(fontSize), hpText, textX, textY, getHealthColor(hp, entity.getMaxHealth()));

                        if (this.armor.isEnabled()) {
                            this.drawArmor(ctx, entity, 0, -height / 2.0F - 10.0F, 1.0F);
                        }

                        ctx.popMatrix();
                    }
                }
            }
        }
    }

    private ColorRGBA getHealthColor(float health, float maxHealth) {
        float ratio = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        if (ratio > 0.75F) return new ColorRGBA(85, 255, 85, 255);
        if (ratio > 0.5F) return new ColorRGBA(255, 255, 85, 255);
        if (ratio > 0.25F) return new ColorRGBA(255, 170, 0, 255);
        return new ColorRGBA(255, 85, 85, 255);
    }

    private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float x, float y, float animation) {
        float boxSizeItem = 10.0F;
        float paddingItem = 1.0F;

        List<ItemStack> armor = player.getInventory().armor;
        List<ItemStack> stacks = new ArrayList<>();
        if (showOffHand.isEnabled()) stacks.add(player.getOffHandStack());
        stacks.add(armor.get(0));
        stacks.add(armor.get(1));
        stacks.add(armor.get(2));
        stacks.add(armor.get(3));
        if (showMainHand.isEnabled()) stacks.add(player.getMainHandStack());

        stacks.removeIf(ItemStack::isEmpty);

        float totalWidth = stacks.size() * (boxSizeItem + paddingItem) - paddingItem;
        float iconX = x - totalWidth / 2.0F;
        float iconY = y;

        for (ItemStack stack : stacks) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(iconX + (boxSizeItem - 9.6F) / 2.0F, iconY + (boxSizeItem - 9.6F) / 2.0F, 0.0D);
            ctx.getMatrices().scale(0.6F * animation, 0.6F * animation, 0.6F * animation);
            ctx.drawItem(stack, 0, 0);
            ctx.getMatrices().pop();
            iconX += boxSizeItem + paddingItem;
        }
    }

    private void renderPendingItems(CustomDrawContext ctx) {

    }

    private static class PendingItemRender {
        PlayerEntity player;
        ItemStack stack;
        float x;
        float y;
        int seed;
        float scale;

        PendingItemRender(PlayerEntity player, ItemStack stack, float x, float y, int seed, float scale) {
            this.player = player;
            this.stack = stack;
            this.x = x;
            this.y = y;
            this.seed = seed;
            this.scale = scale;
        }
    }
}