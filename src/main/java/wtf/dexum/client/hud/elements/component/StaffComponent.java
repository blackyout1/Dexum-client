package wtf.dexum.client.hud.elements.component;

import com.mojang.authlib.GameProfile;
import org.joml.Vector4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.world.GameMode;
import net.minecraft.client.util.DefaultSkinHelper;

public class StaffComponent extends DraggableHudElement {
    private final Map<String, StaffModule> modules = new LinkedHashMap<>();
    private final Set<String> staffPrefix = Set.of("helper", "ᴀдмин", "moder", "staff", "admin", "curator", "стажёр", "сотрудник", "помощник", "админ", "модер", "ꔗ", "ꔥ", "ꔡ", "ꔳ");
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
    private long lastStaffUpdate = 0L;
    private long lastSkinCacheClear = 0L;
    private final Set<String> currentStaffKeys = new HashSet<>();
    private final Animation widthAnimation;
    private final Animation alpha;
    private final boolean v2;

    public StaffComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align, boolean v2) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
        this.alpha = new Animation(200L, Easing.CUBIC_OUT);
        this.v2 = v2;
    }

    public void render(CustomDrawContext ctx) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastStaffUpdate > 50L && mc.getNetworkHandler() != null) {
            this.updateStaffList();
            this.lastStaffUpdate = currentTime;
        }

        if (currentTime - this.lastSkinCacheClear > 30000L) {
            this.skinTextureCache.clear();
            this.lastSkinCacheClear = currentTime;
        }

        this.modules.entrySet().removeIf((entry) -> entry.getValue().isDelete());
        float posX = this.getX();
        float posY = this.getY();
        Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA themeColor = theme.getColor();

        if (v2) {
            renderV2(ctx, posX, posY, themeColor);
        } else {
            renderClassic(ctx, posX, posY, themeColor);
        }
    }

    private void renderV2(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;
        float staffHeight = 0.0F;
        float maxNameWidth = 0.0F;
        List<StaffModule> activeStaff = new java.util.ArrayList<>();

        for (Map.Entry<String, StaffModule> entry : this.modules.entrySet()) {
            entry.getValue().animation.update(this.currentStaffKeys.contains(entry.getKey()));
            float anim = entry.getValue().animation.getValue();
            if (anim > 0.01F) {
                float nameW = Fonts.REGULAR.getWidth(entry.getValue().displayNameText.getString(), 7.2F);
                maxNameWidth = Math.max(maxNameWidth, nameW);
                staffHeight += 11.0F * anim;
                activeStaff.add(entry.getValue());
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (this.alpha.getValue() < 0.01F) return;

        float headerHeight = 15.0F;
        float currentWidth = Math.max(maxNameWidth + 35.0F, 82.0F);
        this.widthAnimation.update(currentWidth);
        float animWidth = this.widthAnimation.getValue();

        float totalHeight = headerHeight + staffHeight + 4.0F;

        Vector4f rounding = new Vector4f(6.0F, 6.0F, 6.0F, 6.0F);

        DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, animWidth, totalHeight, 18.0F,
                BorderRadius.all(rounding.x),
                ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, animWidth, headerHeight,
                new BorderRadius(rounding.x, rounding.y, 0, 0),
                ColorRGBA.BLACK.withAlpha((int)(255 * this.alpha.getValue())));

        float bodyY = posY + headerHeight;
        float bodyHeight = totalHeight - headerHeight;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, bodyY, animWidth, bodyHeight,
                new BorderRadius(0, 0, rounding.z, rounding.w),
                new ColorRGBA(0, 0, 0, (int)(135 * this.alpha.getValue())));

        float headerTextWidth = Fonts.REGULAR.getWidth("Staff", 8.0F);
        float iconSize = 9.2f;
        float iconWidth = Fonts.NURIKI.getWidth("O", iconSize);
        float spacing = 3.5F;
        float totalHeaderWidth = iconWidth + spacing + headerTextWidth;
        float headerX = posX + (animWidth - totalHeaderWidth) / 2.0F;

        ctx.drawText(Fonts.NURIKI.getFont(iconSize), "O", headerX + 0.200F, posY + 5.3F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));
        ctx.drawText(Fonts.REGULAR.getFont(8.0F), "Staff", headerX + iconWidth + spacing, posY + 4.8F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

        float currentY = bodyY + 1.0F;
        for (StaffModule module : activeStaff) {
            float anim = module.animation.getValue();
            Identifier skin = this.skinTextureCache.get(module.name);
            if (skin == null) skin = DefaultSkinHelper.getSteve().texture();

            int a = (int)(255 * this.alpha.getValue() * anim);
            DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skin, posX + 6.0F, currentY + 1.5F, 8.0F, BorderRadius.all(2.0F), ColorRGBA.WHITE.withAlpha(a));
            ctx.drawText(Fonts.MEDIUM.getFont(7.2F), module.displayNameText.getString(), posX + 16.0F, currentY + 3.2f, ColorRGBA.WHITE.withAlpha(a));

            ColorRGBA statusColor = module.status == Status.NONE ? new ColorRGBA(32, 255, 32, a) : new ColorRGBA(255, 32, 32, a);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + animWidth - 8.0F, currentY + 3.5F, 4.0F, 4.0F, BorderRadius.all(2.0F), statusColor);

            currentY += 11.0F * anim;
        }

        this.width = animWidth;
        this.height = totalHeight;
    }

    private void renderClassic(CustomDrawContext ctx, float posX, float posY, ColorRGBA themeColor) {
        boolean isFound = false;

        for(Map.Entry<String, StaffModule> module : this.modules.entrySet()) {
            module.getValue().animation.update(this.currentStaffKeys.contains(module.getKey()));
            if (module.getValue().animation.getValue() > 0.01F) {
                isFound = true;
            }
        }

        if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
        } else {
            this.alpha.update(1.0F);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
        }

        float staffHeight = 0.0F;
        float maxNameWidth = 0.0F;

        for(Map.Entry<String, StaffModule> module : this.modules.entrySet()) {
            float anim = module.getValue().animation.getValue();
            if (anim > 0.01F) {
                float nameW = Fonts.REGULAR.getWidth(module.getValue().displayNameText.getString(), 7.2F);
                maxNameWidth = Math.max(maxNameWidth, nameW * anim);
                staffHeight += 11.0F * anim;
            }
        }

        float headerHeight = 15.0F;
        float footerHeight = 4.0F;
        float bodyHeight = staffHeight + footerHeight;
        float totalHeight = headerHeight + bodyHeight;
        float targetWidth = Math.max(maxNameWidth + 65.0F, 85.0F);
        this.widthAnimation.update(targetWidth);
        float currentWidth = this.widthAnimation.getValue();

        if (this.alpha.getValue() > 0.01F) {
            float rounding = 4.0F;
            ColorRGBA headerColor = new ColorRGBA(0, 0, 0, (int)(255 * this.alpha.getValue()));
            ColorRGBA bodyColor = new ColorRGBA(0, 0, 0, (int)(125 * this.alpha.getValue()));

            DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, 15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, totalHeight, BorderRadius.all(rounding), bodyColor);

            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, currentWidth, headerHeight, new BorderRadius(rounding, rounding, 0, 0), headerColor);

            ctx.drawText(Fonts.MEDIUM.getFont(8.0F), "StaffList", posX + 7.0F, posY + 4.5F, ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue())));
            ctx.drawText(Fonts.NURIKI.getFont(9F), "O", posX + currentWidth - 14.0F, posY + 5.5F, themeColor.withAlpha((int)(255 * this.alpha.getValue())));

            float staffY = posY + headerHeight + 2.0F;
            float statusDotX = posX + currentWidth - 10.0F;

            for(Map.Entry<String, StaffModule> module : this.modules.entrySet()) {
                float anim = module.getValue().animation.getValue();
                if (anim > 0.01F) {
                    Identifier skinTexture = this.skinTextureCache.get(module.getValue().name);
                    if (skinTexture == null && mc.getNetworkHandler() != null) {
                        PlayerListEntry player = mc.getNetworkHandler().getPlayerList().stream()
                                .filter((p) -> p.getProfile() != null && module.getValue().name.equals(p.getProfile().getName()))
                                .findFirst().orElse(null);
                        if (player != null && player.getSkinTextures() != null) {
                            skinTexture = player.getSkinTextures().texture();
                            this.skinTextureCache.put(module.getValue().name, skinTexture);
                        }
                    }

                    if (skinTexture == null) {
                        skinTexture = DefaultSkinHelper.getSteve().texture();
                    }

                    ColorRGBA textC = ColorRGBA.WHITE.withAlpha((int)(255 * this.alpha.getValue() * anim));

                    DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, posX + 6.0F, staffY + 1.5F, 8.0F, BorderRadius.all(2.0F), ColorRGBA.WHITE.withAlpha(anim * 255.0F * this.alpha.getValue()));

                    ctx.drawText(Fonts.MEDIUM.getFont(7.2F), module.getValue().displayNameText.getString(), posX + 22.0F, staffY + 2.5F, textC);

                    ColorRGBA statusColor = module.getValue().status == Status.NONE ?
                            new ColorRGBA(32, 255, 32, (int)(255 * anim * this.alpha.getValue())) :
                            new ColorRGBA(255, 32, 32, (int)(255 * anim * this.alpha.getValue()));

                    DrawUtil.drawRoundedRect(ctx.getMatrices(), statusDotX, staffY + 3.5F, 4.0F, 4.0F, BorderRadius.all(2.0F), statusColor);

                    staffY += 11.0F * anim;
                }
            }
        }

        this.width = currentWidth;
        this.height = totalHeight;
    }

    private void updateStaffList() {
        if (mc.getNetworkHandler() != null) {
            this.currentStaffKeys.clear();

            for(PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                GameProfile profile = entry.getProfile();
                Text displayName = entry.getDisplayName();
                if (displayName != null && profile != null) {
                    String display = displayName.getString();
                    String name = profile.getName();
                    String prefix = display.replace(name, "").trim();
                    String formattedPrefix = prefix.replaceAll("ꔗ", Formatting.BLUE + "MODER").replaceAll("ꔥ", Formatting.BLUE + "ST.MODER").replaceAll("ꔡ", Formatting.LIGHT_PURPLE + "MODER+").replaceAll("ꔀ", Formatting.GRAY + "PLAYER").replaceAll("ꔉ", Formatting.YELLOW + "HELPER").replaceAll("◆", "@").replaceAll("┃", "|").replaceAll("ꔳ", Formatting.AQUA + "ML.ADMIN");
                    formattedPrefix = formattedPrefix.replaceAll("ꔅ", Formatting.RED + "Y" + Formatting.WHITE + "T").replaceAll("ꔂ", Formatting.BLUE + "D.MODER").replaceAll("ꕠ", Formatting.YELLOW + "D.HELPER").replaceAll("ꕄ", Formatting.RED + "DRACULA").replaceAll("ꔖ", Formatting.AQUA + "OVERLORD").replaceAll("ꕈ", Formatting.GREEN + "COBRA").replaceAll("ꔨ", Formatting.LIGHT_PURPLE + "DRAGON").replaceAll("ꔤ", Formatting.RED + "IMPERATOR").replaceAll("ꔠ", Formatting.GOLD + "MAGISTER").replaceAll("ꔄ", Formatting.BLUE + "HERO").replaceAll("ꔒ", Formatting.GREEN + "AVENGER").replaceAll("ꕒ", Formatting.WHITE + "RABBIT").replaceAll("ꔈ", Formatting.YELLOW + "TITAN").replaceAll("ꕀ", Formatting.DARK_GREEN + "HYDRA").replaceAll("ꔶ", Formatting.GOLD + "TIGER").replaceAll("ꔲ", Formatting.DARK_PURPLE + "BULL").replaceAll("ꕖ", Formatting.BLACK + "BUNNY").replaceAll("ꕗꕘ", Formatting.YELLOW + "SPONSOR").replaceAll("\ud83d\udd25", "@").replaceAll("ᴀ", "A").replaceAll("ʙ", "B").replaceAll("ᴄ", "C").replaceAll("ᴅ", "D").replaceAll("ᴇ", "E").replaceAll("ғ", "F").replaceAll("ɢ", "G").replaceAll("ʜ", "H").replaceAll("ɪ", "I").replaceAll("ᴊ", "J").replaceAll("ᴋ", "K").replaceAll("ʟ", "L").replaceAll("ᴍ", "M").replaceAll("ɴ", "N").replaceAll("ꜱ", "S").replaceAll("ᴏ", "O").replaceAll("ᴘ", "P").replaceAll("ǫ", "Q").replaceAll("ʀ", "R").replaceAll("ᴛ", "T").replaceAll("ᴜ", "U").replaceAll("ᴠ", "V").replaceAll("ᴡ", "W").replaceAll("ꜰ", "F").replaceAll("ʏ", "Y").replaceAll("ᴢ", "Z");
                    if (formattedPrefix.length() >= 2 && this.containsAnyKeyword(formattedPrefix) && (!Dexum.getInstance().getServerHandler().getServer().equals("LonyGrief") || !formattedPrefix.contains("D.ADMIN") && !formattedPrefix.contains("sTAFF"))) {
                        Status status = entry.getGameMode() == GameMode.SPECTATOR ? Status.VANISHED : Status.NONE;
                        this.modules.computeIfAbsent(display, (k) -> new StaffModule(displayName, display, name, status));
                        this.currentStaffKeys.add(display);
                    }
                }
            }

        }
    }

    public boolean containsAnyKeyword(String text) {
        String lower = text.toLowerCase(Locale.US);

        for(String keyword : this.staffPrefix) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private class StaffModule {
        private final Animation animation;
        private final Animation animationColor;
        private final Text displayNameText;
        private final String key;
        private final String name;
        private final Status status;
        private final long appearTime;

        public StaffModule(Text displayNameText, String key, String name, Status status) {
            this.animation = new Animation(250L, 0.01F, Easing.CUBIC_OUT);
            this.animationColor = new Animation(200L, Easing.QUAD_IN_OUT);
            this.displayNameText = displayNameText;
            this.key = key;
            this.name = name;
            this.status = status;
            this.appearTime = System.currentTimeMillis();
        }

        public boolean isDelete() {
            return this.animation.getValue() == 0.0F;
        }
    }

    public static enum Status {
        NONE,
        VANISHED;
    }

    public static class Staff {
        private Text prefix;
        private String name;
        private boolean isSpec;
        private Status status;

        @Generated
        public Text getPrefix() {
            return this.prefix;
        }

        @Generated
        public String getName() {
            return this.name;
        }

        @Generated
        public boolean isSpec() {
            return this.isSpec;
        }

        @Generated
        public Status getStatus() {
            return this.status;
        }

        @Generated
        public void setPrefix(Text prefix) {
            this.prefix = prefix;
        }

        @Generated
        public void setName(String name) {
            this.name = name;
        }

        @Generated
        public void setSpec(boolean isSpec) {
            this.isSpec = isSpec;
        }

        @Generated
        public void setStatus(Status status) {
            this.status = status;
        }

        @Generated
        public Staff(Text prefix, String name, boolean isSpec, Status status) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
            this.status = status;
        }
    }
}