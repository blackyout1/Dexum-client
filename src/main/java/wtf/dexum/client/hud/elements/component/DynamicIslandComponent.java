package wtf.dexum.client.hud.elements.component;

import wtf.dexum.Dexum;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Queue;

public class DynamicIslandComponent extends DraggableHudElement {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final float PILL_HEIGHT = 15f;
    private static final float ORB_RADIUS = 3.2f;
    private static final float GAP = 6f;
    private static final long NOTIF_DURATION = 2200L;

    // Статическая очередь уведомлений, общая для всех экземпляров
    private static final Queue<Notif> queue = new ArrayDeque<>();
    private static volatile Notif activeNotif = null;
    private static volatile long activeNotifEnd = 0L;

    private float notifAnim = 0f;
    private long lastAnimTime = System.currentTimeMillis();

    /**
     * Конструктор для ручного создания (используется в Interface).
     */
    public DynamicIslandComponent(String name, float initialX, float initialY) {
        super(name, initialX, initialY, 80, 15, 0, 0, Align.CENTER);
    }
    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        updateNotifications(now);

        ColorRGBA textColor = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor();
        String time = LocalTime.now().format(TIME_FORMATTER);
        float timeWidth = Fonts.MEDIUM.getWidth(time, 7.2f);

        float pillWidth = 80f;
        float totalWidth = timeWidth + GAP + pillWidth + GAP + 16f;
        float scaledWidth = mc.getWindow().getScaledWidth();
        float startX = scaledWidth / 2f - totalWidth / 2f;
        startX = Math.max(2f, Math.min(startX, scaledWidth - totalWidth - 2f));

        float pillX = startX + timeWidth + GAP;
        float pillY = this.getY();
        float midY = pillY + PILL_HEIGHT / 2f;
        float textY = pillY + (PILL_HEIGHT - 8f) / 2f + 0.5f; // визуальное центрирование

        // Время слева
        ctx.drawText(Fonts.MEDIUM.getFont(7.2f), time, startX, textY, textColor);

        // Фон пилюли
        DrawUtil.drawRoundedRect(ctx.getMatrices(), pillX, pillY, pillWidth, PILL_HEIGHT,
                BorderRadius.all(7.5f), new ColorRGBA(0, 0, 0, 210));

        // Содержимое пилюли
        if (activeNotif != null) {
            float slide = (1f - notifAnim) * 7f;
            ColorRGBA dotColor = new ColorRGBA(activeNotif.color.getRGB());
            DrawUtil.drawRoundedRect(ctx.getMatrices(), pillX + 6 + slide, midY - ORB_RADIUS,
                    ORB_RADIUS * 2, ORB_RADIUS * 2, BorderRadius.all(ORB_RADIUS), dotColor);
            ctx.drawText(Fonts.MEDIUM.getFont(7.2f), activeNotif.text,
                    pillX + 6 + ORB_RADIUS * 2 + 5 + slide, textY, textColor);
        } else {
            // Логотип Dexum с цветной точкой
            int orbColor = blendColors(textColor.getRGB(), textColor.getRGB(), 0.5f);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), pillX + 6, midY - ORB_RADIUS,
                    ORB_RADIUS * 2, ORB_RADIUS * 2, BorderRadius.all(ORB_RADIUS), new ColorRGBA(orbColor));
            ctx.drawText(Fonts.MEDIUM.getFont(7.2f), "Dexum",
                    pillX + 6 + ORB_RADIUS * 2 + 5, textY, textColor);
        }

        // Пинг справа
        int ping = getPlayerPing();
        ctx.drawText(Fonts.MEDIUM.getFont(7.2f), ping + " ms",
                pillX + pillWidth + GAP, textY, textColor);

        // Обновляем размеры для перетаскивания
        this.x = (int) startX;
        this.y = (int) pillY;
        this.width = (int) Math.ceil(totalWidth);
        this.height = (int) PILL_HEIGHT;
    }

    private void updateNotifications(long now) {
        if (activeNotif != null && now >= activeNotifEnd) {
            activeNotif = queue.poll();
            activeNotifEnd = activeNotif != null ? now + NOTIF_DURATION : 0;
        }
        float target = (activeNotif != null) ? 1f : 0f;
        notifAnim += (target - notifAnim) * Math.min(1f, (now - lastAnimTime) / 1000f * 12f);
        notifAnim = Math.max(0f, Math.min(1f, notifAnim));
        lastAnimTime = now;
    }

    /**
     * Добавляет уведомление в общую очередь. Можно вызывать из любого модуля.
     */
    public static void addNotification(String text, Color color) {
        if (text == null || text.isEmpty()) return;
        Notif notif = new Notif(text, color);
        if (activeNotif == null || System.currentTimeMillis() >= activeNotifEnd) {
            activeNotif = notif;
            activeNotifEnd = System.currentTimeMillis() + NOTIF_DURATION;
        } else {
            queue.offer(notif);
        }
    }

    private int getPlayerPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private int blendColors(int c1, int c2, float ratio) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private record Notif(String text, Color color) {}
}