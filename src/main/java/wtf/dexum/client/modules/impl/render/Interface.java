package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.Vector2f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.input.EventMouse;
import wtf.dexum.base.events.impl.input.EventSetScreen;
import wtf.dexum.base.events.impl.other.EventWindowResize;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.render.EventHudRender;
import wtf.dexum.client.hud.elements.component.*;
import wtf.dexum.client.hud.elements.draggable.DraggableHudElement;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.MultiBooleanSetting;
import wtf.dexum.utility.math.MathUtil;
import wtf.dexum.utility.render.display.Render2DUtil;
import wtf.dexum.utility.render.display.base.CustomDrawContext;
import wtf.dexum.utility.render.display.base.GuiUtil;
import wtf.dexum.base.theme.Theme;
import wtf.dexum.client.modules.api.setting.impl.ColorSetting;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
@ModuleAnnotation(
        name = "HUD",
        category = Category.RENDER,
        description = "Интерфейс Клиента"
)
public final class Interface extends Module {
    public static final Interface INSTANCE = new Interface();
    private final wtf.dexum.client.modules.api.setting.impl.ModeSetting hudMode = new wtf.dexum.client.modules.api.setting.impl.ModeSetting("HUD Режим", "Hud1", "Hud2");
    private final MultiBooleanSetting elementsSetting = MultiBooleanSetting.create("Элементы", List.of("Ватермарка", "Эффекты", "Модераторы", "Уведомления", "Информация", "Бинды", "Таргет худ", "Список модулей", "Динамик Айленд"));
    public final ColorSetting hudColor = new ColorSetting("Цвет HUD", new ColorRGBA(56, 58, 61));

    /** Use this everywhere instead of getCurrentTheme().getColor() */
    public static ColorRGBA getHudColor() {
        return INSTANCE.hudColor.getColor();
    }

    /** Secondary color — slightly darker version of hudColor */
    public static ColorRGBA getHudSecondColor() {
        ColorRGBA c = INSTANCE.hudColor.getColor();
        return c.darker(0.3f);
    }
    private final List<DraggableHudElement> elementsHud1 = new ArrayList();
    private final List<DraggableHudElement> elementsHud2 = new ArrayList();
    private DraggableHudElement draggingElement = null;
    private DraggableHudElement selectedElement = null; // Элемент для которого открыты настройки
    private float dragOffsetX;
    private float dragOffsetY;
    long init = 0L;

    private Interface() {
        this.elementsHud1.add(new DynamicIslandComponent("DynamicIsland", 0.0F, 0.0F));
        this.elementsHud1.add(new WatermarkComponent("Watermark", 0.0F, 0.0F, 960.0F, 495.5F, 10.0F, 10.0F, DraggableHudElement.Align.TOP_LEFT, false));
        this.elementsHud1.add(new PotionsComponent("Potions", 0.0F, 0.0F, 960.0F, 495.5F, 119.15234F, 73.0F, DraggableHudElement.Align.TOP_LEFT, false));
        this.elementsHud1.add(new StaffComponent("Staff", 0.0F, 0.0F, 960.0F, 495.5F, 10.0F, 73.0F, DraggableHudElement.Align.TOP_LEFT, false));
        NotifyComponent notifyComponent = new NotifyComponent("Notify", 0.0F, 0.0F, 960.0F, 495.5F, 0.0F, 50.0F, DraggableHudElement.Align.CENTER);
        this.elementsHud1.add(notifyComponent);
        Dexum.getInstance().getNotifyManager().setNotifyComponent(notifyComponent);
        this.elementsHud1.add(new InformationComponent("Information", 0.0F, 0.0F, 960.0F, 495.5F, 10.0F, 41.5F, DraggableHudElement.Align.TOP_LEFT));
        this.elementsHud1.add(new KeybindsComponent("Keybinds", 349.0F, 0.0F, 960.0F, 495.5F, -122.0F, 73.0F, DraggableHudElement.Align.TOP_RIGHT, false));
        this.elementsHud1.add(new TargetHudComponent("TargetHUD", 166.5F, 128.5F, 960.0F, 495.5F, 0.0F, 31.75F, DraggableHudElement.Align.CENTER));
        this.elementsHud1.add(new ArrayListComponent("ArrayList", 0.0F, 0.0F, 960.0F, 495.5F, -10.0F, 10.0F, DraggableHudElement.Align.TOP_RIGHT));

        this.elementsHud2.add(new DynamicIslandComponent("DynamicIslandV2", 0.0F, 0.0F));
        this.elementsHud2.add(new WatermarkComponent("WatermarkV2", 0.0F, 0.0F, 960.0F, 495.5F, 5.0F, 5.0F, DraggableHudElement.Align.TOP_LEFT, true));
        this.elementsHud2.add(new KeybindsComponent("KeybindsV2", 0.0F, 0.0F, 960.0F, 495.5F, 5.0F, 30.0F, DraggableHudElement.Align.TOP_LEFT, true));
        this.elementsHud2.add(new PotionsComponent("PotionsV2", 0.0F, 0.0F, 960.0F, 495.5F, 5.0F, 100.0F, DraggableHudElement.Align.TOP_LEFT, true));
        this.elementsHud2.add(new TargetHudComponent("TargetHUDV2", 0.0F, 0.0F, 960.0F, 495.5F, 0.0F, 0.0F, DraggableHudElement.Align.CENTER));
        this.elementsHud2.add(new StaffComponent("StaffV2", 0.0F, 0.0F, 960.0F, 495.5F, -5.0F, 5.0F, DraggableHudElement.Align.TOP_RIGHT, true));
        this.elementsHud2.add(new ArrayListComponent("ArrayListV2", 0.0F, 0.0F, 960.0F, 495.5F, -5.0F, 5.0F, DraggableHudElement.Align.TOP_RIGHT));
    }

    private List<DraggableHudElement> getActiveElements() {
        return hudMode.is("Hud1") ? elementsHud1 : elementsHud2;
    }

    public boolean isLiquidHudEnabled() {
        return false;
    }

    public void onEnable() {
        this.init = System.currentTimeMillis();
        super.onEnable();
    }

    public JsonObject save() {
        JsonObject object = super.save();
        JsonObject propertiesObject = new JsonObject();

        for (DraggableHudElement element : this.elementsHud1) {
            propertiesObject.add(element.getName(), element.save());
        }
        for (DraggableHudElement element : this.elementsHud2) {
            propertiesObject.add(element.getName(), element.save());
        }

        object.add("HudElements", propertiesObject);
        return object;
    }

    public void load(JsonObject object) {
        super.load(object);
        if (object.has("HudElements") && object.get("HudElements").isJsonObject()) {
            JsonObject propertiesObject = object.getAsJsonObject("HudElements");

            for (DraggableHudElement element : this.elementsHud1) {
                String key = element.getName();
                if (propertiesObject.has(key) && propertiesObject.get(key).isJsonObject()) {
                    element.load(propertiesObject.getAsJsonObject(key));
                }
            }
            for (DraggableHudElement element : this.elementsHud2) {
                String key = element.getName();
                if (propertiesObject.has(key) && propertiesObject.get(key).isJsonObject()) {
                    element.load(propertiesObject.getAsJsonObject(key));
                }
            }
        }

    }

    private void addElement(DraggableHudElement element) {

    }

    @EventTarget
    public void onRender(EventHudRender event) {
        if (!(mc.currentScreen instanceof ChatScreen) && this.draggingElement != null) {
            this.draggingElement.release();
            this.draggingElement = null;
        }

        CustomDrawContext ctx = event.getContext();
        float width = (float)mc.getWindow().getWidth() / this.getCustomScale();
        float height = (float)mc.getWindow().getHeight() / this.getCustomScale();
        
        // Рисуем направляющие линии в режиме редактирования
        if (mc.currentScreen instanceof ChatScreen) {
            renderGuideLines(ctx, width, height);
        }
        
        if (!mc.options.hudHidden) {
            List<DraggableHudElement> elements = getActiveElements();
            Iterator var5 = elements.iterator();

            while(var5.hasNext()) {
                DraggableHudElement element = (DraggableHudElement)var5.next();
                if (this.shouldRender(element)) {
                    try {
                        element.render(ctx);
                    } catch (Exception var10) {

                    }

                    if (this.draggingElement != element && System.currentTimeMillis() - this.init < 5000L) {
                        element.windowResized(width, height);
                    }
                }
            }
        }

        if (mc.currentScreen instanceof ChatScreen && this.draggingElement != null) {
            Vector2f mousePos = GuiUtil.getMouse(this.getCustomScale());
            double mouseX = mousePos.getX();
            double mouseY = mousePos.getY();
            this.draggingElement.set(ctx, (float)mouseX - this.dragOffsetX, (float)mouseY - this.dragOffsetY, this, width, height);
        }
        
        // Рисуем панель настроек если элемент выбран
        if (mc.currentScreen instanceof ChatScreen && this.selectedElement != null) {
            renderSettingsPanel(ctx, this.selectedElement);
        }

    }

    private boolean shouldRender(DraggableHudElement element) {
        String name = element.getName().replace("V2", "");
        List<String> settingNames = List.of("Ватермарка", "Эффекты", "Модераторы", "Уведомления", "Информация", "Бинды", "Таргет худ", "Список модулей", "Динамик Айленд");
        List<String> componentNames = List.of("Watermark", "Potions", "Staff", "Notify", "Information", "Keybinds", "TargetHUD", "ArrayList", "DynamicIsland");

        int nameIndex = componentNames.indexOf(name);
        if (nameIndex != -1 && nameIndex < elementsSetting.getBooleanSettings().size()) {
            return elementsSetting.getBooleanSettings().get(nameIndex).isEnabled();
        }

        return true;
    }

    @EventTarget
    public void onMouse(EventMouse event) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            if (this.draggingElement != null) {
                this.draggingElement.release();
                this.draggingElement = null;
            }
            this.selectedElement = null;
        } else {
            Vector2f mousePos = GuiUtil.getMouse(this.getCustomScale());
            double mouseX = mousePos.getX();
            double mouseY = mousePos.getY();
            if (event.getAction() == 1 && event.getButton() == 0) {
                // Проверяем клик по панели настроек
                if (selectedElement != null && isMouseOverSettingsPanel(mouseX, mouseY)) {
                    handleSettingsPanelClick(mouseX, mouseY);
                    return;
                }
                
                List<DraggableHudElement> reversedElements = new ArrayList(getActiveElements());
                Collections.reverse(reversedElements);
                Iterator var8 = reversedElements.iterator();

                while(var8.hasNext()) {
                    DraggableHudElement element = (DraggableHudElement)var8.next();
                    if (this.shouldRender(element) && element.isMouseOver(mouseX, mouseY)) {
                        this.draggingElement = element;
                        this.dragOffsetX = (float)mouseX - element.getX();
                        this.dragOffsetY = (float)mouseY - element.getY();
                        break;
                    }
                }
            } else if (event.getAction() == 1 && event.getButton() == 1) {
                // Правый клик - открыть/закрыть настройки элемента
                List<DraggableHudElement> reversedElements = new ArrayList(getActiveElements());
                Collections.reverse(reversedElements);
                
                for (DraggableHudElement element : reversedElements) {
                    if (this.shouldRender(element) && element.isMouseOver(mouseX, mouseY)) {
                        // Переключаем настройки элемента
                        if (this.selectedElement == element) {
                            this.selectedElement = null; // Закрыть если уже открыто
                        } else if (!element.getSettings().isEmpty()) {
                            this.selectedElement = element; // Открыть настройки
                        }
                        break;
                    }
                }
            } else if (event.getAction() == 0) {
                if (this.draggingElement != null) {
                    this.draggingElement.release();
                    this.draggingElement = null;
                }
            }

        }
    }

    public float getCustomScale() {
        return 2.0F;
    }

    public org.joml.Vector2f getNearest(float x, float y) {
        float minDeltaX = Float.MAX_VALUE;
        float minDeltaY = Float.MAX_VALUE;
        float thoroughness = 10.0F; // Зона прилипания
        org.joml.Vector2f nearest = new org.joml.Vector2f(-1.0F, -1.0F);
        
        // Сначала проверяем прилипание к направляющим линиям
        float screenWidth = (float)mc.getWindow().getWidth() / this.getCustomScale();
        float screenHeight = (float)mc.getWindow().getHeight() / this.getCustomScale();
        
        float gridSpacing = 50.0f;
        
        // Прилипание к вертикальным линиям сетки
        for (float guideX = 0; guideX <= screenWidth; guideX += gridSpacing) {
            float delta = MathUtil.goodSubtract(guideX, x);
            if (delta < minDeltaX && delta < thoroughness) {
                minDeltaX = delta;
                nearest.x = guideX;
            }
        }
        
        // Прилипание к горизонтальным линиям сетки
        for (float guideY = 0; guideY <= screenHeight; guideY += gridSpacing) {
            float delta = MathUtil.goodSubtract(guideY, y);
            if (delta < minDeltaY && delta < thoroughness) {
                minDeltaY = delta;
                nearest.y = guideY;
            }
        }
        
        // Дополнительно к основным направляющим (края, центр)
        float[] keyX = {10.0f, screenWidth / 4, screenWidth / 2, screenWidth * 3 / 4, screenWidth - 10};
        float[] keyY = {10.0f, screenHeight / 4, screenHeight / 2, screenHeight * 3 / 4, screenHeight - 10};
        
        for (float kx : keyX) {
            float delta = MathUtil.goodSubtract(kx, x);
            if (delta < minDeltaX && delta < thoroughness) {
                minDeltaX = delta;
                nearest.x = kx;
            }
        }
        
        for (float ky : keyY) {
            float delta = MathUtil.goodSubtract(ky, y);
            if (delta < minDeltaY && delta < thoroughness) {
                minDeltaY = delta;
                nearest.y = ky;
            }
        }
        
        // Теперь проверяем прилипание к другим элементам HUD
        Iterator var7 = getActiveElements().iterator();

        float minX;
        float minY;
        float deltaX;
        float deltaY;
        while(var7.hasNext()) {
            DraggableHudElement s = (DraggableHudElement)var7.next();
            if (!s.equals(this.draggingElement)) {
                minX = s.getX();
                minY = s.getY();
                deltaX = s.getX() + s.getWidth();
                deltaY = s.getY() + s.getHeight();
                float tempXC = s.getX() + s.getWidth() / 2.0F;
                float tempYC = s.getY() + s.getHeight() / 2.0F;
                float nearestX = this.getNearest(minX, deltaX, tempXC, x);
                float nearestY = this.getNearest(minY, deltaY, tempYC, y);
                float nearestDeltaX = MathUtil.goodSubtract(nearestX, x);
                float nearestDeltaY = MathUtil.goodSubtract(nearestY, y);
                if (nearestDeltaX < minDeltaX) {
                    minDeltaX = nearestDeltaX;
                    if (nearestDeltaX < thoroughness) {
                        nearest.x = nearestX;
                    }
                }

                if (nearestDeltaY < minDeltaY) {
                    minDeltaY = nearestDeltaY;
                    if (nearestDeltaY < thoroughness) {
                        nearest.y = nearestY;
                    }
                }
            }
        }

        return nearest;
    }

    public float getNearest(float a, float b, float c, float target) {
        float nearest = a;
        if (MathUtil.goodSubtract(b, target) < MathUtil.goodSubtract(a, target)) {
            nearest = b;
        }

        if (MathUtil.goodSubtract(c, target) < MathUtil.goodSubtract(nearest, target)) {
            nearest = c;
        }

        return nearest;
    }

    public boolean isEnableScoreBar() {
        return false;
    }

    public boolean isEnableHotBar() {
        return false;
    }

    public boolean isEnableTab() {
        return false;
    }

    @EventTarget
    public void resize(EventWindowResize eventWindowResize) {
        float width = (float)mc.getWindow().getWidth() / this.getCustomScale();
        float height = (float)mc.getWindow().getHeight() / this.getCustomScale();
        Iterator var4 = getActiveElements().iterator();

        while(var4.hasNext()) {
            DraggableHudElement element = (DraggableHudElement)var4.next();
            element.windowResized(width, height);
        }

    }

    @EventTarget
    public void update(EventUpdate eventUpdate) {
        if (Render2DUtil.glowCache.size() > 400) {
            Render2DUtil.glowCache.values().removeIf((v) -> {
                if (v.tick()) {
                    v.destroy();
                    return true;
                } else {
                    return false;
                }
            });
        }

        Iterator var2 = getActiveElements().iterator();

        while(var2.hasNext()) {
            DraggableHudElement draggableHudElement = (DraggableHudElement)var2.next();
            draggableHudElement.tick();
        }

    }

    @EventTarget
    public void screenEvent(EventSetScreen event) {
        if (event.getScreen() instanceof ChatScreen) {
            this.init = System.currentTimeMillis();
        }

    }

    @Generated
    public DraggableHudElement getDraggingElement() {
        return this.draggingElement;
    }

    public int getGlowRadius() {
        return 10;
    }
    
    private void renderGuideLines(CustomDrawContext ctx, float screenWidth, float screenHeight) {
        // Цвет линий - полупрозрачный белый, более заметный
        ColorRGBA lineColor = new ColorRGBA(255, 255, 255, 60);
        ColorRGBA centerLineColor = new ColorRGBA(255, 255, 255, 100);
        float lineThickness = 1.0f;  // Увеличил толщину для видимости
        
        // Центральные линии (вертикальная и горизонтальная)
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;
        
        // Дополнительные линии через каждые 50px для лучшей сетки
        float gridSpacing = 50.0f;
        
        // Вертикальные линии по всему экрану
        for (float x = gridSpacing; x < screenWidth; x += gridSpacing) {
            boolean isCenter = Math.abs(x - centerX) < 2.0f;
            boolean isQuarter = Math.abs(x - screenWidth / 4) < 2.0f || Math.abs(x - screenWidth * 3 / 4) < 2.0f;
            boolean isEdge = x < 15 || x > screenWidth - 15;
            
            ColorRGBA color = isCenter ? centerLineColor : (isQuarter || isEdge ? new ColorRGBA(255, 255, 255, 80) : lineColor);
            
            wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
                ctx.getMatrices(), x, 0, lineThickness, screenHeight,
                wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
                color
            );
        }
        
        // Горизонтальные линии по всему экрану
        for (float y = gridSpacing; y < screenHeight; y += gridSpacing) {
            boolean isCenter = Math.abs(y - centerY) < 2.0f;
            boolean isQuarter = Math.abs(y - screenHeight / 4) < 2.0f || Math.abs(y - screenHeight * 3 / 4) < 2.0f;
            boolean isEdge = y < 15 || y > screenHeight - 15;
            
            ColorRGBA color = isCenter ? centerLineColor : (isQuarter || isEdge ? new ColorRGBA(255, 255, 255, 80) : lineColor);
            
            wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
                ctx.getMatrices(), 0, y, screenWidth, lineThickness,
                wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
                color
            );
        }
        
        // Основные направляющие (край, четверти, центр) - более яркие
        float edgeMargin = 10.0f;
        
        // Левая граница
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), edgeMargin, 0, lineThickness * 1.5f, screenHeight,
            wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
            new ColorRGBA(100, 150, 255, 100)
        );
        
        // Правая граница
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), screenWidth - edgeMargin, 0, lineThickness * 1.5f, screenHeight,
            wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
            new ColorRGBA(100, 150, 255, 100)
        );
        
        // Верхняя граница
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), 0, edgeMargin, screenWidth, lineThickness * 1.5f,
            wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
            new ColorRGBA(100, 150, 255, 100)
        );
        
        // Нижняя граница
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), 0, screenHeight - edgeMargin, screenWidth, lineThickness * 1.5f,
            wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
            new ColorRGBA(100, 150, 255, 100)
        );
    }
    
    private void renderSettingsPanel(CustomDrawContext ctx, DraggableHudElement element) {
        float panelWidth = 80;
        float settingHeight = 12;
        float padding = 4;
        float headerHeight = 16;
        
        List<wtf.dexum.client.modules.api.setting.Setting> settings = element.getSettings();
        float panelHeight = headerHeight + settings.size() * settingHeight + padding * 2 + 3;
        
        // Позиционируем панель справа от элемента
        float panelX = element.getX() + element.getWidth() + 8;
        float panelY = element.getY();
        
        // Если панель выходит за экран, показываем слева
        if (panelX + panelWidth > mc.getWindow().getScaledWidth()) {
            panelX = element.getX() - panelWidth - 8;
        }
        
        // Внешняя тень
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), panelX - 0.5f, panelY - 0.5f, panelWidth + 1, panelHeight + 1,
            wtf.dexum.utility.render.display.base.BorderRadius.all(4f),
            new ColorRGBA(0, 0, 0, 50)
        );
        
        // Блюр эффект
        wtf.dexum.utility.render.display.shader.DrawUtil.drawBlur(
            ctx.getMatrices(), panelX, panelY, panelWidth, panelHeight, 8,
            wtf.dexum.utility.render.display.base.BorderRadius.all(3.5f),
            ColorRGBA.WHITE.withAlpha(255)
        );
        
        // Полупрозрачный фон панели
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), panelX, panelY, panelWidth, panelHeight,
            wtf.dexum.utility.render.display.base.BorderRadius.all(3.5f),
            new ColorRGBA(32, 34, 37, 180)
        );
        
        // Заголовок
        String title = element.getName();
        ctx.drawText(wtf.dexum.base.font.Fonts.MEDIUM.getFont(5.5f), title, 
            panelX + padding, panelY + padding - 0.5f, ColorRGBA.WHITE);
        
        // Тонкая линия под заголовком
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), panelX + padding, panelY + headerHeight - 2, 
            panelWidth - padding * 2, 0.5f,
            wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
            new ColorRGBA(65, 68, 73, 120)
        );
        
        // Настройки
        float currentY = panelY + headerHeight + 2;
        
        Vector2f mousePos = GuiUtil.getMouse(this.getCustomScale());
        double mouseX = mousePos.getX();
        double mouseY = mousePos.getY();
        
        int index = 0;
        for (wtf.dexum.client.modules.api.setting.Setting setting : settings) {
            if (setting instanceof wtf.dexum.client.modules.api.setting.impl.BooleanSetting boolSetting) {
                boolean hovered = mouseX >= panelX + 1 && mouseX <= panelX + panelWidth - 1 &&
                                mouseY >= currentY && mouseY < currentY + settingHeight;
                renderBooleanSetting(ctx, boolSetting, panelX, currentY, panelWidth, padding, hovered);
                
                // Линия между настройками (кроме последней)
                if (index < settings.size() - 1) {
                    wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
                        ctx.getMatrices(), panelX + padding, currentY + settingHeight - 0.5f,
                        panelWidth - padding * 2, 0.5f,
                        wtf.dexum.utility.render.display.base.BorderRadius.ZERO,
                        new ColorRGBA(50, 53, 58, 80)
                    );
                }
                
                currentY += settingHeight;
                index++;
            }
        }
    }
    
    private void renderBooleanSetting(CustomDrawContext ctx, wtf.dexum.client.modules.api.setting.impl.BooleanSetting setting,
                                     float x, float y, float width, float padding, boolean hovered) {
        final float settingHeight = 12;
        
        // Фон при наведении
        if (hovered) {
            wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
                ctx.getMatrices(), x + 1, y + 0.5f, width - 2, settingHeight - 1,
                wtf.dexum.utility.render.display.base.BorderRadius.all(2),
                new ColorRGBA(42, 45, 50, 150)
            );
        }
        
        // Минималистичная иконка-точка слева (очень маленькая)
        float iconSize = 1.5f;
        float iconX = x + padding + 1;
        float iconY = y + (settingHeight / 2) - (iconSize / 2);
        
        ColorRGBA iconColor = setting.isEnabled() 
            ? Dexum.getInstance().getThemeManager().getCurrentTheme().getColor()
            : new ColorRGBA(80, 83, 88, 200);
            
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), iconX, iconY, iconSize, iconSize,
            wtf.dexum.utility.render.display.base.BorderRadius.all(iconSize / 2),
            iconColor
        );
        
        // Название настройки - мелкий шрифт, лучше отцентрировано
        ctx.drawText(wtf.dexum.base.font.Fonts.MEDIUM.getFont(4.8f), setting.getName(), 
            x + padding + 4, y + 4.3f, new ColorRGBA(220, 220, 225, 255));
        
        // Компактный iOS переключатель
        float toggleWidth = 16;
        float toggleHeight = 9;
        float toggleX = x + width - toggleWidth - padding;
        float toggleY = y + (settingHeight - toggleHeight) / 2;
        
        ColorRGBA toggleColor = setting.isEnabled() 
            ? Dexum.getInstance().getThemeManager().getCurrentTheme().getColor()
            : new ColorRGBA(48, 50, 54, 255);
        
        // Тень переключателя
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), toggleX, toggleY + 0.3f, toggleWidth, toggleHeight,
            wtf.dexum.utility.render.display.base.BorderRadius.all(toggleHeight / 2),
            new ColorRGBA(0, 0, 0, 25)
        );
        
        // Сам переключатель
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), toggleX, toggleY, toggleWidth, toggleHeight,
            wtf.dexum.utility.render.display.base.BorderRadius.all(toggleHeight / 2),
            toggleColor
        );
        
        // Кружок переключателя
        float circleSize = 7;
        float circleX = setting.isEnabled() 
            ? toggleX + toggleWidth - circleSize - 1
            : toggleX + 1;
        float circleY = toggleY + (toggleHeight - circleSize) / 2;
        
        // Мини-тень кружка
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), circleX, circleY + 0.2f, circleSize, circleSize,
            wtf.dexum.utility.render.display.base.BorderRadius.all(circleSize / 2),
            new ColorRGBA(0, 0, 0, 30)
        );
        
        // Сам кружок
        wtf.dexum.utility.render.display.shader.DrawUtil.drawRoundedRect(
            ctx.getMatrices(), circleX, circleY, circleSize, circleSize,
            wtf.dexum.utility.render.display.base.BorderRadius.all(circleSize / 2),
            ColorRGBA.WHITE
        );
    }
    
    private boolean isMouseOverSettingsPanel(double mouseX, double mouseY) {
        if (selectedElement == null) return false;
        
        float panelWidth = 80;
        float settingHeight = 12;
        float padding = 4;
        float headerHeight = 16;
        
        List<wtf.dexum.client.modules.api.setting.Setting> settings = selectedElement.getSettings();
        float panelHeight = headerHeight + settings.size() * settingHeight + padding * 2 + 3;
        
        float panelX = selectedElement.getX() + selectedElement.getWidth() + 8;
        float panelY = selectedElement.getY();
        
        if (panelX + panelWidth > mc.getWindow().getScaledWidth()) {
            panelX = selectedElement.getX() - panelWidth - 8;
        }
        
        return mouseX >= panelX && mouseX <= panelX + panelWidth &&
               mouseY >= panelY && mouseY <= panelY + panelHeight;
    }
    
    private void handleSettingsPanelClick(double mouseX, double mouseY) {
        if (selectedElement == null) return;
        
        float panelWidth = 80;
        float settingHeight = 12;
        float padding = 4;
        float headerHeight = 16;
        
        float panelX = selectedElement.getX() + selectedElement.getWidth() + 8;
        float panelY = selectedElement.getY();
        
        if (panelX + panelWidth > mc.getWindow().getScaledWidth()) {
            panelX = selectedElement.getX() - panelWidth - 8;
        }
        
        float currentY = panelY + headerHeight + 2;
        
        for (wtf.dexum.client.modules.api.setting.Setting setting : selectedElement.getSettings()) {
            if (setting instanceof wtf.dexum.client.modules.api.setting.impl.BooleanSetting boolSetting) {
                // Проверяем клик строго в пределах этой настройки
                if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                    mouseY >= currentY && mouseY < currentY + settingHeight) {
                    boolSetting.toggle();
                    return;
                }
                currentY += settingHeight;
            }
        }
    }
}