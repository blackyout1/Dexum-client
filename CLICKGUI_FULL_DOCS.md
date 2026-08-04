# Полная документация ClickGUI

## 4. ClickGuiSettingRenderer.java

### Назначение
Отвечает за отрисовку различных типов настроек внутри модулей.

### Главный метод render()

```java
public void render(DrawContext context, Module module, float panelX, float moduleY, 
                  float openProgress, int colorTheme, double mouseX, double mouseY, 
                  ClickGuiState state, float alphaMul)
```

**Параметры:**
- `openProgress` - прогресс анимации открытия (0.0 = закрыт, 1.0 = открыт)
- `alphaMul` - множитель прозрачности для общей анимации GUI

**Логика:**
1. Проверяет есть ли настройки и больше ли прогресс 0.001
2. Вычисляет область для отсечения (scissor) - настройки появляются плавно при открытии
3. Проходит по видимым настройкам и вызывает соответствующий render метод
4. Отключает scissor

**Scissor (отсечение):**
```java
// Ограничивает область рисования - настройки не вылезают за пределы при анимации
float settingsClipHeight = maxSettingHeight * openProgress;  // Высота зависит от анимации
context.enableScissor(x1, y1, x2, y2);  // Включаем
// ... рисование ...
context.disableScissor();  // Выключаем
```

### renderBooleanSetting()

**Что рисует:**
- Название настройки слева
- Бинд клавиши (если есть) рядом с названием
- iOS-style toggle switch справа

**Анимация тумблера:**
```java
Animation backgroundAnimation = state.getBooleanBackgroundAnimation(booleanSetting);
backgroundAnimation.update(booleanSetting.isEnabled());  // Обновляем к целевому состоянию
float progress = backgroundAnimation.getValue();  // 0.0 - 1.0

// Интерполируем цвет фона между выключенным и включенным
ColorRGBA trackOff = new ColorRGBA(35, 35, 45, alpha);  // Серый
ColorRGBA trackOn = themeColor.withAlpha(alpha);  // Цвет темы
int r = (int) (trackOff.getRed() + (trackOn.getRed() - trackOff.getRed()) * progress);
// ... аналогично для g, b
ColorRGBA trackColor = new ColorRGBA(r, g, b, alpha);

// Позиция кружка
float knobX = knobMinX + (knobMaxX - knobMinX) * progress;  // Движется слева направо
```

**Обрезка длинных названий:**
```java
if (nameW > maxNameW && maxNameW > 0) {
    // Обрезаем и добавляем ".."
    StringBuilder truncated = new StringBuilder();
    float currentW = 0;
    for (int ci = 0; ci < name.length(); ci++) {
        float charW = Fonts.REGULAR.getWidth(String.valueOf(name.charAt(ci)), 6.5f);
        if (currentW + charW + dotsW > maxNameW) break;  // Не помещается
        truncated.append(name.charAt(ci));
        currentW += charW;
    }
    name = truncated + "..";
}
```

### renderFloatSetting() (NumberSetting)

**Что рисует:**
- Название настройки вверху слева
- Текущее значение вверху справа
- Слайдер снизу (трек + заполнение + кружок-ручка)

**Анимация слайдера:**
```java
Animation sliderAnimation = state.getSliderAnimation(floatSetting);
float target = state.getSliderPos(floatSetting);  // Целевая позиция (0.0-1.0)

if (state.isDraggingSlider(floatSetting)) {
    // Если тащим - мгновенно устанавливаем
    sliderAnimation.setValue(target);
} else {
    // Иначе плавно анимируем
    sliderAnimation.update(target);
}
float animatedPos = sliderAnimation.getValue();
```

**Структура слайдера:**
```java
// 1. Фон слайдера (темный)
DrawUtil.drawRoundedRect(..., slW, slH, ..., темный_цвет);

// 2. Заполненная часть (цвет темы)
DrawUtil.drawRoundedRect(..., slW * animatedPos, slH, ..., цвет_темы);

// 3. Кружок-ручка (белый)
float knobX = slX + slW * animatedPos;  // Позиция зависит от значения
DrawUtil.drawRoundedRect(..., knobSize, knobSize, ..., белый);
```


### renderModeSetting()

**Назначение:** Рисует настройку выбора режима (кнопки-чипы).

**Что рисует:**
- Название настройки вверху
- Чипы (кнопки) для каждого значения
- Чипы автоматически переносятся на новую строку если не помещаются

**Размещение чипов:**
```java
float x = panelX + ClickGuiLayout.SETTING_LEFT;  // Начальная X
float y = settingY + 10.0f;  // Начальная Y (под названием)
float rowHeight = 11.0f;

for (ModeSetting.Value val : modeSetting.getValues()) {
    float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
    float chipW = textW + (ClickGuiLayout.CHIP_PADDING_X * 2);  // Ширина с отступами

    // Проверяем помещается ли
    if (x + chipW > panelX + ClickGuiLayout.SETTING_RIGHT) {
        x = panelX + ClickGuiLayout.SETTING_LEFT;  // Переход на новую строку
        y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
    }

    // Рисуем чип
    boolean selected = modeSetting.getValue() == val;
    DrawUtil.drawBlur(...);  // Блюр фон
    DrawUtil.drawRoundedRect(...);  // Темный прямоугольник
    customDrawContext.drawText(..., val.getName(), ..., цвет);  // Текст

    x += chipW + ClickGuiLayout.CHIP_GAP_X;  // Смещаемся вправо
}
```

**Цвет текста:**
```java
ColorRGBA textColor = selected 
    ? ColorRGBA.WHITE.withAlpha(alpha)  // Выбранный - белый
    : new ColorRGBA(180, 180, 180, alpha);  // Обычный - серый
```

### renderMultiBooleanSetting()

**Назначение:** Рисует настройку с множественным выбором (несколько чипов, каждый можно включить/выключить).

**Что рисует:**
- Название настройки слева вверху
- Счетчик "X/Y" справа вверху (X - включено, Y - всего)
- Чипы для каждого значения (аналогично ModeSetting)

**Счетчик:**
```java
int enabledCount = (int) multiBooleanSetting.getBooleanSettings().stream()
    .filter(MultiBooleanSetting.Value::isEnabled)
    .count();
int totalCount = multiBooleanSetting.getBooleanSettings().size();
String counter = enabledCount + "/" + totalCount;  // Например "3/5"

// Рисуем справа
customDrawContext.drawText(..., counter, 
    panelX + ClickGuiLayout.SETTING_RIGHT - counterW, ..., серый_цвет);
```

**Логика чипов:** Аналогична ModeSetting, но каждый чип независим (вкл/выкл отдельно).

### renderBindSetting() (KeySetting)

**Назначение:** Рисует настройку привязки клавиши.

**Что рисует:**
- Название настройки слева
- Кнопку с текущей клавишей справа

**Состояния:**
```java
boolean binding = state.getBindingSetting() == bindSetting;  // Сейчас выбирается клавиша
String bindString = binding 
    ? "..."  // Ожидание нажатия
    : Keyboard.getKeyName(bindSetting.getKeyCode());  // Название клавиши

// Рисуем кнопку справа
DrawUtil.drawBlur(...);  // Блюр
DrawUtil.drawRoundedRect(...);  // Фон кнопки
customDrawContext.drawText(..., bindString, ...);  // Текст клавиши
```

### renderStringSetting()

**Назначение:** Рисует настройку для ввода текста.

**Что рисует:**
- Название настройки вверху
- Поле ввода снизу (с текстом или курсором при редактировании)

**Режим редактирования:**
```java
boolean editing = state.getEditingStringSetting() == stringSetting;
String value = stringSetting.getValue();
if (value.isEmpty() && !editing) {
    value = "...";  // Плейсхолдер если пусто
}

if (editing) {
    // Вставляем курсор "|" в позицию
    int cursor = state.getStringCursor();
    display = value.substring(0, cursor) + "|" + value.substring(cursor);
}

// Если текст длинный - сдвигаем влево чтобы курсор был виден
float textW = Fonts.REGULAR.getWidth(display, 6.0f);
float textX = fieldX + 3.0f;
if (textW > fieldW - 6.0f) {
    textX = fieldX + fieldW - 3.0f - textW;  // Выравниваем по правому краю
}
```

### renderColorSetting()

**Назначение:** Рисует настройку выбора цвета.

**Что рисует:**
- Название настройки слева
- Квадратик с текущим цветом справа (при клике открывается color picker)

**Квадратик цвета:**
```java
float boxSize = 10f;
float boxX = panelX + ClickGuiLayout.SETTING_RIGHT - boxSize;
float boxY = settingY + 1.5f;

// Рамка (светлая обводка)
DrawUtil.drawRoundedRect(..., boxSize + 1f, boxSize + 1f, ..., светлый);

// Сам цвет
DrawUtil.drawRoundedRect(..., boxSize, boxSize, ..., color);
```

---

## 5. ClickGuiInputHandler.java

### Назначение
Обрабатывает все события ввода: клики мыши, скролл, клавиатуру, перетаскивание.

### Поля класса
```java
private final ClickGuiState state;  // Состояние GUI
private NumberSetting draggingSlider;  // Слайдер который сейчас тащим
private float draggingSliderX;  // X позиция начала слайдера
final ColorPickerPopup colorPickerPopup;  // Попап выбора цвета (один на всё GUI)
```

### mouseClicked() - Обработка кликов

**Приоритет обработки:**
1. Color picker (если открыт) - первый приоритет
2. Привязка клавиши к модулю (средняя кнопка мыши)
3. Привязка клавиши к настройке (средняя кнопка мыши)
4. Поле поиска
5. Содержимое панелей категорий

**Привязка клавиш (binding):**
```java
// Для модуля
if (state.getBindingModule() != null && button >= 2) {
    state.getBindingModule().setKeyCode(button);  // Сохраняем кнопку мыши
    state.setBindingModule(null);  // Выходим из режима привязки
    return true;
}

// Для настройки
if (state.getBindingSetting() != null && button >= 2) {
    if (state.getBindingSetting() instanceof KeySetting key) {
        key.setKeyCode(button);
    } else if (state.getBindingSetting() instanceof BooleanSetting bool) {
        bool.setKeyCode(button);
    }
    state.setBindingSetting(null);
    return true;
}
```

**Клик по полю поиска:**
```java
float searchX = ClickGuiLayout.getSearchX(...);
float searchY = ClickGuiLayout.getSearchY(...);

if (button == 0 && MathUtil.isHovered(mouseX, mouseY, searchX, searchY, width, height)) {
    state.setSearchActive(true);  // Активируем поиск
    state.setSearchCursor(state.getSearchText().length());  // Курсор в конец
    return true;
}
```

**Клик по панели категории:**
```java
for (Category category : categories) {
    float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);
    
    // Проверяем что клик внутри панели
    if (!MathUtil.isHovered(mouseX, mouseY, panelX, contentY, width, contentHeight)) {
        continue;  // Пропускаем эту категорию
    }

    // Для категории THEMES - обрабатываем клики по темам
    if (category == Category.THEMES) {
        float themeY = contentY + 2.0f + state.getScroll(category);
        for (Theme themeItem : themes) {
            if (MathUtil.isHovered(mouseX, mouseY, ...)) {
                Dexum.getInstance().getThemeManager().setCurrentTheme(themeItem);
                return true;
            }
            themeY += 20.0f;
        }
        continue;
    }

    // Для остальных категорий - обрабатываем клики по модулям
    for (Module module : state.getModules(category)) {
        // Клик по заголовку модуля
        if (MathUtil.isHovered(mouseX, mouseY, ..., MODULE_HEADER_HEIGHT)) {
            if (button == 0) {
                module.toggle();  // ЛКМ - вкл/выкл модуль
                return true;
            }
            if (button == 1) {
                // ПКМ - открыть/закрыть настройки
                if (ClickGuiLayout.hasVisibleSettings(module)) {
                    state.toggleModuleOpen(module);
                }
                return true;
            }
            if (button == 2) {
                // СКМ - привязать клавишу
                state.setBindingModule(module);
                return true;
            }
        }

        // Клик по настройкам модуля (если открыт)
        if (state.isModuleOpen(module) && openProgress > 0.1f) {
            if (handleSettingClick(...)) {
                return true;
            }
        }
    }
}
```

### handleSettingClick() - Обработка кликов по настройкам

**Логика для каждого типа:**

**BooleanSetting:**
```java
if (button == 0 && MathUtil.isHovered(...)) {
    booleanSetting.toggle();  // ЛКМ - переключить
    return true;
}
if (button == 2 && MathUtil.isHovered(...)) {
    state.setBindingSetting(booleanSetting);  // СКМ - привязать клавишу
    return true;
}
```

**NumberSetting (слайдер):**
```java
if (button == 0 && MathUtil.isHovered(mouseX, mouseY, sliderX, sliderY, width, height)) {
    // Устанавливаем значение по позиции клика
    floatSetting.setCurrent(state.getSliderValue(floatSetting, sliderX, mouseX));
    
    // Начинаем перетаскивание
    draggingSlider = floatSetting;
    state.setDraggingSlider(floatSetting);
    draggingSliderX = sliderX;
    return true;
}
```

**ModeSetting (чипы режимов):**
```java
float x = moduleX + ClickGuiLayout.SETTING_LEFT;
float y = settingY + 12.0f;

for (ModeSetting.Value val : modeSetting.getValues()) {
    float chipW = ...;  // Вычисляем ширину чипа
    
    // Переход на новую строку если не помещается
    if (x + chipW > moduleX + ClickGuiLayout.SETTING_RIGHT) {
        x = moduleX + ClickGuiLayout.SETTING_LEFT;
        y += rowHeight + ClickGuiLayout.CHIP_GAP_Y;
    }
    
    // Проверяем клик по чипу
    if (button == 0 && MathUtil.isHovered(mouseX, mouseY, x, y, chipW, rowHeight)) {
        modeSetting.setValue(val);  // Выбираем это значение
        return true;
    }
    
    x += chipW + ClickGuiLayout.CHIP_GAP_X;
}
```

**MultiBooleanSetting:** Аналогично ModeSetting, но переключает вкл/выкл каждого значения.

**ColorSetting:**
```java
// Позиция квадратика с цветом
float boxX = moduleX - MODULE_PADDING + SETTING_RIGHT - 10f;
float boxY = settingY + 1.5f;

if (button == 0 && MathUtil.isHovered(mouseX, mouseY, boxX, boxY, 10f, 10f)) {
    // Открываем попап справа от квадратика
    float spawnX = boxX + 14f;
    float spawnY = boxY - 4f;
    colorPickerPopup.toggle(colorSetting, spawnX, spawnY);
    return true;
}
```

### charTyped() - Ввод символов

**Обработка для разных режимов:**

```java
// 1. Color picker (если открыт) - для ввода HEX
if (colorPickerPopup.isOpen()) {
    return colorPickerPopup.charTyped(chr);
}

// 2. Редактирование StringSetting
if (state.getEditingStringSetting() != null) {
    if (Character.isISOControl(chr)) return false;  // Игнорируем управляющие
    StringSetting setting = state.getEditingStringSetting();
    String text = setting.getValue();
    
    if (text.length() >= setting.getMaxLength()) return true;  // Достигнут лимит
    
    // Вставляем символ в позицию курсора
    int cursor = state.getStringCursor();
    setting.setValue(text.substring(0, cursor) + chr + text.substring(cursor));
    state.setStringCursor(cursor + 1);  // Сдвигаем курсор
    return true;
}

// 3. Поиск
if (state.isSearchActive()) {
    if (Character.isISOControl(chr)) return false;
    String text = state.getSearchText();
    
    if (text.length() >= SEARCH_MAX_CHARS) return true;  // Лимит символов
    
    int cursor = state.getSearchCursor();
    state.setSearchText(text.substring(0, cursor) + chr + text.substring(cursor));
    state.setSearchCursor(cursor + 1);
    return true;
}
```

