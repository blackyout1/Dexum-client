package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.gui.DrawContext;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.setting.impl.ColorSetting;
import wtf.dexum.utility.math.MathUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

/**
 * Floating, draggable HSV color picker popup.
 *
 * Layout (160 x 148):
 *   [4px pad] Title bar (drag zone, 16px)
 *   [4px pad] SV square  (152 x 90)
 *   [4px gap] Hue slider (152 x 8)
 *   [4px gap] Hex field  (152 x 14)
 *   [4px pad bottom]
 */
public class ColorPickerPopup {

    // --- dimensions ---
    public static final float W = 160f;
    public static final float SV_W = 152f;
    public static final float SV_H = 90f;
    private static final float PAD = 4f;
    private static final float TITLE_H = 16f;
    private static final float HUE_H = 8f;
    private static final float HEX_H = 14f;
    public static final float H = PAD + TITLE_H + PAD + SV_H + PAD + HUE_H + PAD + HEX_H + PAD;

    // --- state ---
    private ColorSetting target;
    private float x, y;
    private boolean open = false;

    // drag window
    private boolean draggingWindow = false;
    private float dragOffX, dragOffY;

    // drag SV picker
    private boolean draggingSV = false;

    // drag hue slider
    private boolean draggingHue = false;

    // hex editing
    private boolean editingHex = false;
    private String hexBuffer = "";

    // per-setting HSV cache so we don't lose hue when S or V hits 0
    private float cachedHue = 0f;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    public boolean isOpen() { return open; }
    public ColorSetting getTarget() { return target; }

    public void open(ColorSetting setting, float spawnX, float spawnY) {
        this.target = setting;
        this.x = spawnX;
        this.y = spawnY;
        this.open = true;
        this.editingHex = false;
        // seed hue from current color
        ColorRGBA c = setting.getColor();
        float[] hsb = toHSB(c.getRed(), c.getGreen(), c.getBlue());
        if (hsb[1] > 0.01f || hsb[2] > 0.01f) {
            cachedHue = hsb[0];
        }
        hexBuffer = toHex(c);
    }

    public void close() {
        open = false;
        target = null;
        draggingWindow = false;
        draggingSV = false;
        draggingHue = false;
        editingHex = false;
    }

    public void toggle(ColorSetting setting, float spawnX, float spawnY) {
        if (open && target == setting) {
            close();
        } else {
            open(setting, spawnX, spawnY);
        }
    }

    // ---------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------

    public void render(DrawContext context, double mouseX, double mouseY) {
        if (!open || target == null) return;

        CustomDrawContext ctx = CustomDrawContext.of(context);
        ColorRGBA color = target.getColor();
        float[] hsb = toHSB(color.getRed(), color.getGreen(), color.getBlue());
        float hue = cachedHue;
        float sat = hsb[1];
        float bri = hsb[2];

        // --- background + blur ---
        DrawUtil.drawBlur(ctx.getMatrices(), x, y, W, H, 18f, BorderRadius.all(5f), ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, W, H, BorderRadius.all(5f),
                new ColorRGBA(18, 18, 24, 230));
        // thin border
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, W, H, 1f, BorderRadius.all(5f),
                new ColorRGBA(255, 255, 255, 30));

        // --- title bar ---
        float titleY = y + PAD;
        ctx.drawText(Fonts.REGULAR.getFont(7f), "Color", x + PAD + 2, titleY + 4f,
                ColorRGBA.WHITE.withAlpha(200));
        // close button
        float closeX = x + W - PAD - 10f;
        float closeY = titleY + 3f;
        boolean closeHov = MathUtil.isHovered(mouseX, mouseY, closeX, closeY, 10f, 10f);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), closeX, closeY, 10f, 10f, BorderRadius.all(2f),
                new ColorRGBA(255, 80, 80, closeHov ? 200 : 120));
        ctx.drawText(Fonts.REGULAR.getFont(6f), "x", closeX + 2.5f, closeY + 1.5f, ColorRGBA.WHITE);

        // --- SV square ---
        float svX = x + PAD;
        float svY = y + PAD + TITLE_H + PAD;

        // Pure hue color for top-right corner
        ColorRGBA pureHue = ColorRGBA.fromHSB(hue, 1f, 1f);

        // 4-corner gradient: TL=white, BL=black, BR=black, TR=pureHue
        DrawUtil.drawRoundedRect(ctx.getMatrices(), svX, svY, SV_W, SV_H, BorderRadius.all(3f),
                new ColorRGBA(255, 255, 255, 255),  // TL white
                new ColorRGBA(0, 0, 0, 255),          // BL black
                new ColorRGBA(0, 0, 0, 255),          // BR black
                pureHue);                              // TR pure hue

        // saturation overlay: left=opaque white, right=transparent (already baked above)
        // brightness overlay: bottom=opaque black
        // The 4-corner approach already does this correctly.

        // SV cursor
        float cursorX = svX + sat * SV_W;
        float cursorY = svY + (1f - bri) * SV_H;
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), cursorX - 4f, cursorY - 4f, 8f, 8f, 1.5f,
                BorderRadius.all(4f), ColorRGBA.WHITE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), cursorX - 2.5f, cursorY - 2.5f, 5f, 5f,
                BorderRadius.all(2.5f), color);

        // --- Hue slider ---
        float hueSliderX = x + PAD;
        float hueSliderY = svY + SV_H + PAD;

        // rainbow gradient: 7 stops
        float segW = SV_W / 6f;
        float[] hues = {0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f};
        for (int i = 0; i < 6; i++) {
            ColorRGBA c1 = ColorRGBA.fromHSB(hues[i], 1f, 1f);
            ColorRGBA c2 = ColorRGBA.fromHSB(hues[i + 1], 1f, 1f);
            float segX = hueSliderX + i * segW;
            BorderRadius br;
            if (i == 0) br = new BorderRadius(2f, 2f, 0f, 0f);
            else if (i == 5) br = new BorderRadius(0f, 0f, 2f, 2f);
            else br = BorderRadius.all(0f);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), segX, hueSliderY, segW, HUE_H, br,
                    c1, c1, c2, c2);
        }

        // hue knob
        float hueKnobX = hueSliderX + hue * SV_W;
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), hueKnobX - 3.5f, hueSliderY - 1f, 7f, HUE_H + 2f, 1.5f,
                BorderRadius.all(3f), ColorRGBA.WHITE);

        // --- Hex field ---
        float hexY = hueSliderY + HUE_H + PAD;
        float hexFieldX = x + PAD;
        float hexFieldW = SV_W;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), hexFieldX, hexY, hexFieldW, HEX_H, BorderRadius.all(3f),
                new ColorRGBA(10, 10, 18, 200));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), hexFieldX, hexY, hexFieldW, HEX_H, 1f,
                BorderRadius.all(3f), editingHex
                        ? new ColorRGBA(100, 150, 255, 180)
                        : new ColorRGBA(255, 255, 255, 30));

        // color preview square inside hex field
        DrawUtil.drawRoundedRect(ctx.getMatrices(), hexFieldX + 3f, hexY + 2f, 10f, 10f,
                BorderRadius.all(2f), color);

        String hexDisplay = editingHex ? hexBuffer : "#" + toHex(color);
        if (editingHex) {
            hexDisplay = hexBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        }
        ctx.drawText(Fonts.REGULAR.getFont(6.5f), hexDisplay,
                hexFieldX + 16f, hexY + 3.5f, ColorRGBA.WHITE.withAlpha(220));
    }

    // ---------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------

    /** Returns true if this popup consumed the event */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || target == null) return false;

        // close button
        float titleY = y + PAD;
        float closeX = x + W - PAD - 10f;
        float closeY = titleY + 3f;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, closeX, closeY, 10f, 10f)) {
            close();
            return true;
        }

        // click outside — close
        if (!MathUtil.isHovered(mouseX, mouseY, x, y, W, H)) {
            close();
            return false; // let the click through to the rest of GUI
        }

        // SV square
        float svX = x + PAD;
        float svY = y + PAD + TITLE_H + PAD;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, svX, svY, SV_W, SV_H)) {
            draggingSV = true;
            applySV(mouseX, mouseY, svX, svY);
            return true;
        }

        // Hue slider
        float hueSliderX = x + PAD;
        float hueSliderY = svY + SV_H + PAD;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, hueSliderX, hueSliderY, SV_W, HUE_H)) {
            draggingHue = true;
            applyHue(mouseX, hueSliderX);
            return true;
        }

        // Hex field
        float hexY = hueSliderY + HUE_H + PAD;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x + PAD, hexY, SV_W, HEX_H)) {
            editingHex = true;
            hexBuffer = "#" + toHex(target.getColor());
            return true;
        }

        // Title bar drag
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, y, W, PAD + TITLE_H + PAD)) {
            draggingWindow = true;
            dragOffX = (float) mouseX - x;
            dragOffY = (float) mouseY - y;
            return true;
        }

        return true; // consumed but no action
    }

    public boolean mouseReleased(int button) {
        if (button == 0) {
            draggingWindow = false;
            draggingSV = false;
            draggingHue = false;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!open || target == null || button != 0) return false;

        if (draggingWindow) {
            x = (float) mouseX - dragOffX;
            y = (float) mouseY - dragOffY;
            return true;
        }

        if (draggingSV) {
            float svX = x + PAD;
            float svY = y + PAD + TITLE_H + PAD;
            applySV(mouseX, mouseY, svX, svY);
            return true;
        }

        if (draggingHue) {
            float hueSliderX = x + PAD;
            applyHue(mouseX, hueSliderX);
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr) {
        if (!editingHex || target == null) return false;
        if (hexBuffer.length() >= 8) return true; // #RRGGBB = 7 chars

        char upper = Character.toUpperCase(chr);
        if ((upper >= '0' && upper <= '9') || (upper >= 'A' && upper <= 'F') || chr == '#') {
            if (chr == '#' && hexBuffer.isEmpty()) {
                hexBuffer = "#";
            } else if (chr != '#') {
                if (hexBuffer.isEmpty()) hexBuffer = "#";
                hexBuffer += upper;
            }
            tryApplyHex();
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!editingHex) return false;
        // GLFW_KEY_BACKSPACE = 259, GLFW_KEY_ESCAPE/ENTER = 256/257
        if (keyCode == 259) { // backspace
            if (!hexBuffer.isEmpty()) {
                hexBuffer = hexBuffer.substring(0, hexBuffer.length() - 1);
            }
            return true;
        }
        if (keyCode == 256 || keyCode == 257) { // escape or enter
            editingHex = false;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private void applySV(double mouseX, double mouseY, float svX, float svY) {
        if (target == null) return;
        float sat = (float) Math.max(0.0, Math.min(1.0, (mouseX - svX) / SV_W));
        float bri = (float) Math.max(0.0, Math.min(1.0, 1.0 - (mouseY - svY) / SV_H));
        ColorRGBA old = target.getColor();
        ColorRGBA newColor = ColorRGBA.fromHSB(cachedHue, sat, bri);
        target.setColor(newColor.withAlpha(old.getAlpha()));
    }

    private void applyHue(double mouseX, float sliderX) {
        if (target == null) return;
        cachedHue = (float) Math.max(0.0, Math.min(1.0, (mouseX - sliderX) / SV_W));
        ColorRGBA old = target.getColor();
        float[] hsb = toHSB(old.getRed(), old.getGreen(), old.getBlue());
        ColorRGBA newColor = ColorRGBA.fromHSB(cachedHue, hsb[1], hsb[2]);
        target.setColor(newColor.withAlpha(old.getAlpha()));
    }

    private void tryApplyHex() {
        if (target == null) return;
        String hex = hexBuffer.startsWith("#") ? hexBuffer.substring(1) : hexBuffer;
        if (hex.length() == 6) {
            try {
                ColorRGBA parsed = ColorRGBA.fromHex(hex);
                target.setColor(parsed.withAlpha(target.getColor().getAlpha()));
                float[] hsb = toHSB(parsed.getRed(), parsed.getGreen(), parsed.getBlue());
                if (hsb[1] > 0.01f || hsb[2] > 0.01f) cachedHue = hsb[0];
            } catch (Exception ignored) {}
        }
    }

    /** Returns [hue, saturation, brightness] 0..1 */
    private static float[] toHSB(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float hue = 0f;
        if (delta > 0f) {
            if (max == rf)      hue = ((gf - bf) / delta) % 6f;
            else if (max == gf) hue = (bf - rf) / delta + 2f;
            else                hue = (rf - gf) / delta + 4f;
            hue /= 6f;
            if (hue < 0f) hue += 1f;
        }
        float sat = max == 0f ? 0f : delta / max;
        return new float[]{hue, sat, max};
    }

    private static String toHex(ColorRGBA c) {
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
