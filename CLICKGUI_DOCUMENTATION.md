# Документация ClickGUI

## Обзор архитектуры

ClickGUI состоит из 6 основных классов:

1. **ClickGuiState** - Хранит состояние (данные, анимации, позиции)
2. **ClickGuiLayout** - Константы размеров и расчеты позиций
3. **ClickGuiRenderer** - Отрисовка GUI
4. **ClickGuiSettingRenderer** - Отрисовка настроек модулей
5. **ClickGuiInputHandler** - Обработка мыши и клавиатуры
6. **ClickGuiThemeSelector** - Выбор тем

---

## 1. ClickGuiState.java

### Назначение
Центральное хранилище всего состояния GUI - позиции, анимации, открытые модули, текст поиска и т.д.

### Основные поля

#### Анимации
- `moduleOpenAnimation` - Анимация открытия/закрытия настроек модуля (0.0 = закрыт, 1.0 = открыт)
- `moduleOpenState` - Булево состояние открыт/закрыт для каждого модуля
- `moduleDotAnimation` - Анимация точки-индикатора возле включенного модуля
- `booleanBackgroundAnimation` - Анимация фона boolean-переключателя
- `booleanCircleAnimation` - Анимация кружка boolean-переключателя
- `sliderAnimation` - Анимация ползунка слайдера
- `modeAnimation` - Анимация кнопок выбора режима
- `themeDropdownAnimation` - Анимация выпадающего списка тем

#### Скроллинг
- `categoryScrollTarget` - Целевая позиция скролла (отрицательное число, 0 = верх)
- `categoryScrollAnimation` - Анимация плавной прокрутки
- `modulesByCategory` - Модули, отсортированные по категориям
- `allModules` - Кэш всех модулей для быстрого доступа

#### Позиция
- `x, y` - Координаты левого верхнего угла GUI
- `renderOffsetY` - Вертикальное смещение для анимации появления

#### Поиск
- `searchActive` - Активен ли режим поиска (фокус в поле)
- `searchText` - Текст поиска
- `searchCursor` - Позиция курсора в строке поиска

#### Привязка клавиш
- `bindingSetting` - Настройка, для которой выбирается клавиша
- `bindingModule` - Модуль, для которого выбирается клавиша

#### Редактирование
- `editingStringSetting` - String-настройка в режиме редактирования
- `stringCursor` - Позиция курсора при редактировании
- `draggingSlider` - Слайдер, который сейчас перетаскивается

### Ключевые методы

#### refreshModules()
```java
// Обновляет список модулей и создает структуру по категориям
// Вызывается при инициализации и при изменении списка модулей
allModules.clear();
allModules.addAll(Dexum.getInstance().getModuleManager().getModules());
for (Category category : Category.values()) {
    modulesByCategory.put(category, allModules.stream()
        .filter(module -> module.getCategory() == category)
        .toList());
}
```

#### updatePosition(Window window, int categoryCount)
```java
// Центрирует GUI на экране
float totalCategoriesWidth = ClickGuiLayout.getTotalCategoriesWidth(categoryCount);
this.x = (window.getScaledWidth() / 2F) - (totalCategoriesWidth / 2F);  // Центр по X
this.y = (window.getScaledHeight() / 2F) - (ClickGuiLayout.HEIGHT / 2F);  // Центр по Y
```

#### getModules(Category category)
```java
// Получает модули с учетом фильтра поиска
List<Module> modules = modulesByCategory.getOrDefault(category, List.of());

if (searchText.isBlank()) {
    return modules;  // Без фильтрации если поиск пустой
}

// Фильтруем по тексту поиска (без учета регистра)
String query = searchText.toLowerCase(Locale.ROOT);
return modules.stream()
    .filter(module -> module.getName().toLowerCase(Locale.ROOT).contains(query))
    .toList();
```

#### toEnglish(String text)
```java
// Конвертирует русские буквы в английские для поддержки русской раскладки при поиске
StringBuilder result = new StringBuilder();
for (char c : text.toCharArray()) {
    result.append(RU_TO_EN.getOrDefault(c, c));  // Заменяем или оставляем как есть
}
return result.toString();
```

#### getSliderPos(NumberSetting setting)
```java
// Вычисляет позицию ползунка слайдера от 0.0 до 1.0
float delta = setting.getMax() - setting.getMin();  // Диапазон значений
return (setting.getCurrent() - setting.getMin()) / delta;  // Нормализуем в 0.0-1.0
```

#### getSliderValue(NumberSetting setting, float posX, double mouseX)
```java
// Вычисляет значение слайдера по позиции мыши
float delta = setting.getMax() - setting.getMin();
float clickedX = (float) mouseX - posX;  // Смещение от начала слайдера
float value = Math.max(0f, Math.min(1f, clickedX / ClickGuiLayout.SLIDER_WIDTH));  // Нормализуем

float outValue = setting.getMin() + delta * value;  // Преобразуем в реальное значение

// Округляем до increment (шаг изменения)
float increment = setting.getIncrement();
outValue = Math.round(outValue / increment) * increment;

// Ограничиваем min/max
return Math.max(setting.getMin(), Math.min(setting.getMax(), outValue));
```

#### getScroll(Category category)
```java
// Получает текущую позицию скролла с анимацией
Animation animation = categoryScrollAnimation.computeIfAbsent(
    category, 
    key -> new Animation(250L, Easing.CUBIC_OUT)  // Создаем анимацию если нет
);
animation.update(categoryScrollTarget.getOrDefault(category, 0f));  // Обновляем до целевого значения
return animation.getValue();  // Возвращаем текущее значение анимации
```

#### clampScroll(Category category, float contentHeight)
```java
// Ограничивает скролл в пределах контента
float totalHeight = getTotalModulesHeight(category);  // Полная высота всех модулей
float maxScroll = Math.min(0f, contentHeight - totalHeight);  // Макс. скролл (отрицательное)

float currentTarget = categoryScrollTarget.getOrDefault(category, 0f);

// Если вышли за границы - корректируем
if (currentTarget < maxScroll || currentTarget > 0f) {
    categoryScrollTarget.put(category, Math.max(maxScroll, Math.min(0f, currentTarget)));
}
```

#### addScroll(Category category, double verticalAmount, float contentHeight)
```java
// Добавляет скролл при вращении колесика мыши
float totalHeight = getTotalModulesHeight(category);
float maxScroll = Math.min(0f, contentHeight - totalHeight);
float currentTarget = categoryScrollTarget.getOrDefault(category, 0f);

// Множитель 20 для скорости скролла
float newTarget = currentTarget + (float) (verticalAmount * 20);

// Ограничиваем новую позицию
categoryScrollTarget.put(category, Math.max(maxScroll, Math.min(0f, newTarget)));
```

#### getTotalModulesHeight(Category category)
```java
// Вычисляет полную высоту всех модулей в категории
if (category == Category.THEMES) {
    // Для категории тем - считаем по количеству тем
    return Dexum.getInstance().getThemeManager().getThemes().size() * 20.0f + 4.0f;
}

// Суммируем высоту всех модулей с учетом открытых настроек
float totalHeight = 0f;
for (Module module : getModules(category)) {
    totalHeight += ClickGuiLayout.MODULE_GAP + ClickGuiLayout.getModuleHeight(module, getOpenProgress(module));
}
return totalHeight;
```

#### getOpenProgress(Module module)
```java
// Получает прогресс анимации открытия модуля (0.0 = закрыт, 1.0 = открыт)
Animation animation = moduleOpenAnimation.computeIfAbsent(
    module,
    key -> new Animation(250L, Easing.CUBIC_OUT)  // Создаем если нет
);
animation.update(isModuleOpen(module) ? 1f : 0f);  // Обновляем до 0 или 1
return animation.getValue();  // Возвращаем промежуточное значение анимации
```

---

## 2. ClickGuiLayout.java

### Назначение
Содержит все константы размеров и методы для вычисления позиций элементов.

### Константы

#### Размеры категорий
```java
CATEGORY_HEADER_HEIGHT = 18.0f  // Высота заголовка категории
WIDTH = 105f                     // Ширина панели категории
HEIGHT = 260f                    // Высота панели категории
CATEGORY_PANEL_STEP = 115f       // Расстояние между панелями
```

#### Размеры модулей
```java
MODULE_PADDING = 2.5f           // Внутренний отступ модуля
MODULE_GAP = 2.5f               // Расстояние между модулями
MODULE_HEADER_HEIGHT = 19.0f    // Высота заголовка модуля
MODULE_INNER_WIDTH = 100.0f     // Внутренняя ширина модуля
```

#### Размеры настроек
```java
SETTING_START_Y = 17.5f         // Y начала настроек в модуле
SETTING_PADDING = 3.0f          // Отступ между настройками
SETTING_BOTTOM_PADDING = 6.0f   // Нижний отступ
SETTING_LEFT = 7.0f             // Левая граница
SETTING_RIGHT = 98.0f           // Правая граница
SLIDER_WIDTH = 88f              // Ширина слайдера
```

#### Размеры чипов (кнопок режимов)
```java
CHIP_GAP_X = 3.0f              // Горизонтальный отступ
CHIP_GAP_Y = 3.0f              // Вертикальный отступ
CHIP_PADDING_X = 4.0f          // Внутренний отступ по X
CHIP_PADDING_Y = 2.0f          // Внутренний отступ по Y
```

#### Размеры поиска
```java
SEARCH_MAX_CHARS = 24          // Макс. символов
SEARCH_WIDTH = 120f            // Ширина поля
SEARCH_HEIGHT = 22f            // Высота поля
SEARCH_GAP = 8f                // Отступ от GUI
SEARCH_ICON_X = 3.5f           // X позиция иконки
SEARCH_TEXT_X = 19f            // X позиция текста
SEARCH_RIGHT_PADDING = 8f      // Правый отступ
```

### Ключевые методы

#### getTotalCategoriesWidth(int categoryCount)
```java
// Вычисляет общую ширину всех панелей
// Формула: (количество-1) * шаг + ширина_одной
// Пример для 5 категорий: 4*115 + 105 = 565px
return ((categoryCount - 1) * CATEGORY_PANEL_STEP) + WIDTH;
```

#### getCategoryPanelX(float x, int index)
```java
// Вычисляет X координату панели по индексу
// Каждая следующая панель смещена на CATEGORY_PANEL_STEP
return x + (index * CATEGORY_PANEL_STEP);
```

#### hasVisibleSettings(Module module)
```java
// Проверяет есть ли видимые настройки
List<Setting> settings = module.getSettings();
if (settings == null || settings.isEmpty()) return false;

for (Setting setting : settings) {
    if (setting != null && setting.isVisible()) {
        return true;  // Нашли видимую настройку
    }
}
return false;  // Нет видимых настроек
```

#### calculateModeSettingHeight(ModeSetting modeSetting)
```java
// Вычисляет высоту настройки типа ModeSetting
// Чипы размещаются в несколько строк если не помещаются

float x = SETTING_LEFT;  // Текущая X позиция
float rowHeight = 11.0f;  // Высота одной строки
int rows = 1;  // Счетчик строк

for (ModeSetting.Value val : modeSetting.getValues()) {
    float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);  // Ширина текста
    float chipW = textW + (CHIP_PADDING_X * 2);  // Ширина чипа с отступами

    if (x + chipW > SETTING_RIGHT) {
        x = SETTING_LEFT;  // Не помещается - новая строка
        rows++;
    }
    x += chipW + CHIP_GAP_X;  // Смещаем для следующего чипа
}

// Итоговая высота = отступ_сверху + строки + отступы_между + отступ_снизу
return 9.0f + (rows * rowHeight) + ((rows - 1) * CHIP_GAP_Y) + 2.0f;
```

#### calculateSettingsHeight(Module module)
```java
// Вычисляет общую высоту всех настроек модуля

float height = 0f;
List<Setting> settings = module.getSettings();
if (settings == null || settings.isEmpty()) return 0f;

boolean hasVisibleSetting = false;
float globalGap = 2.5f;  // Отступ между настройками

// Фильтруем только видимые
List<Setting> visibleSettings = settings.stream()
    .filter(Setting::isVisible)
    .toList();

for (int i = 0; i < visibleSettings.size(); i++) {
    Setting setting = visibleSettings.get(i);
    hasVisibleSetting = true;

    float currentHeight = 0;
    
    // Высота зависит от типа настройки
    if (setting instanceof BooleanSetting || setting instanceof KeySetting) {
        currentHeight = 12f;  // Одна строка
    } else if (setting instanceof NumberSetting || setting instanceof StringSetting) {
        currentHeight = 22f;  // Две строки (название + слайдер/поле)
    } else if (setting instanceof ModeSetting modeSetting) {
        currentHeight = calculateModeSettingHeight(modeSetting);  // Динамическая
    } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
        currentHeight = calculateMultiBooleanHeight(multiBooleanSetting);  // Динамическая
    } else if (setting instanceof ColorSetting) {
        currentHeight = 14f;  // Одна строка (палитра в попапе)
    }
    
    height += currentHeight;
    
    // Отступ между настройками (кроме последней)
    if (i < visibleSettings.size() - 1) height += globalGap;
}

// Нижний отступ если были настройки
if (hasVisibleSetting) height += SETTING_BOTTOM_PADDING;

return height;
```

#### getModuleHeight(Module module, float openProgress)
```java
// Вычисляет полную высоту модуля с учетом анимации
// openProgress: 0.0 = закрыт (только заголовок), 1.0 = открыт (заголовок + настройки)

return MODULE_HEADER_HEIGHT + (calculateSettingsHeight(module) * openProgress);
```

---


## 3. ClickGuiRenderer.java

### Назначение
Отвечает за отрисовку всех элементов ClickGUI на экране.

### Основные компоненты

#### Поля класса
```java
private final ClickGuiState state;  // Состояние GUI
private final ClickGuiSettingRenderer settingRenderer;  // Рендерер настроек
private ClickGuiInputHandler inputHandler;  // Обработчик ввода (для попапов)
```

### Главный метод render()

```java
public void render(DrawContext context, int mouseX, int mouseY, Window window, float animationProgress) {
    if (window == null) return;

    float alphaMul = animationProgress;  // Прозрачность для анимации появления/исчезновения
    int colorTheme = Dexum.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB();
    
    // Рисуем все категории
    Category[] categories = Category.values();
    for (int i = 0; i < categories.length; i++) {
        Category category = categories[i];
        float panelX = ClickGuiLayout.getCategoryPanelX(state.getX(), i);  // Вычисляем X позицию панели
        renderCategoryPanel(context, mouseX, mouseY, panelX, category, colorTheme, alphaMul);
    }

    // Рисуем поле поиска под GUI
    renderSearchField(context, alphaMul, categories.length);

    // Color picker рисуется последним (поверх всего)
    if (inputHandler != null) {
        inputHandler.getColorPickerPopup().render(context, mouseX, mouseY);
    }
}
```

### getShortKeyName(int key)

Сокращает названия клавиш для компактного отображения:

```java
private String getShortKeyName(int key) {
    String name = Keyboard.getKeyName(key);
    if (name == null || name.isEmpty()) return "";

    return switch (name.toUpperCase()) {
        case "LEFT_SHIFT", "LSHIFT" -> "L_SHIFT";      // Left Shift
        case "RIGHT_SHIFT", "RSHIFT" -> "R_SHIFT";     // Right Shift
        case "LEFT_CONTROL", "LCONTROL", "LEFT_CTRL" -> "L_CTRL";
        case "RIGHT_CONTROL", "RCONTROL", "RIGHT_CTRL" -> "R_CTRL";
        case "BACKSPACE" -> "BKSP";                    // Backspace
        case "CAPS_LOCK", "CAPITAL" -> "CAPS";         // Caps Lock
        case "PAGE_UP" -> "PGUP";                      // Page Up
        case "PAGE_DOWN" -> "PGDN";                    // Page Down
        case "INSERT" -> "INS";                        // Insert
        case "DELETE" -> "DEL";                        // Delete
        case "PRINT_SCREEN" -> "PRTSC";                // Print Screen
        // ... и т.д.
        default -> name.length() > 8 ? name.substring(0, 7) + "…" : name;  // Обрезаем длинные
    };
}
```

### renderSearchField()

Отрисовка поля поиска:

```java
private void renderSearchField(DrawContext context, float alphaMul, int categoryCount) {
    CustomDrawContext customDrawContext = CustomDrawContext.of(context);

    // Вычисляем позицию (центр под GUI)
    float searchX = ClickGuiLayout.getSearchX(state.getX(), categoryCount, ClickGuiLayout.SEARCH_WIDTH);
    float searchY = ClickGuiLayout.getSearchY(state.getY() + state.getRenderOffsetY());

    // Рисуем блюр фон
    DrawUtil.drawBlur(customDrawContext.getMatrices(), searchX, searchY, 
        ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, 
        15.0f, BorderRadius.all(3.0f), ColorRGBA.WHITE.withAlpha((int) (255 * alphaMul)));
    
    // Рисуем темный прямоугольник поверх блюра
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), searchX, searchY, 
        ClickGuiLayout.SEARCH_WIDTH, ClickGuiLayout.SEARCH_HEIGHT, 
        BorderRadius.all(3.0f), new ColorRGBA(0, 0, 0, (int) (125 * alphaMul)));

    // Определяем что показывать
    String text = state.getSearchText();
    boolean hasText = text != null && !text.isEmpty();
    boolean isActive = state.isSearchActive();
    
    // Если есть текст или поле активно - показываем текст, иначе плейсхолдер "Поиск..."
    String shown = (hasText || isActive) ? (text != null ? text : "") : "\u041f\u043e\u0438\u0441\u043a...";
    ColorRGBA textColor = ColorRGBA.WHITE.withAlpha((int) ((hasText ? 230 : 200) * alphaMul));

    // Рисуем текст
    float textX = searchX + 7.0f;
    float textY = searchY + 8.0f;
    if (!shown.isEmpty()) {
        customDrawContext.drawText(Fonts.REGULAR.getFont(8.0f), shown, textX, textY, textColor);
    }

    // Рисуем иконку лупы справа от текста
    float shownW = Fonts.REGULAR.getWidth(shown, 8.0f);
    customDrawContext.drawText(Fonts.LUPA.getFont(8.0f), "A", textX + shownW + 4.0f, textY, 
        ColorRGBA.WHITE.withAlpha((int) (180 * alphaMul)));

    // Рисуем мигающий курсор если поле активно
    if (state.isSearchActive()) {
        boolean cursorVisible = (System.currentTimeMillis() / 500) % 2 == 0;  // Мигает каждые 500мс
        if (cursorVisible) {
            float cursorTextW = hasText ? Fonts.REGULAR.getWidth(text, 8.0f) : 0;
            float cursorX = textX + cursorTextW + 1.0f;
            DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), cursorX, searchY + 6.5f, 
                1.0f, 9.0f, BorderRadius.all(0.5f), ColorRGBA.WHITE.withAlpha((int) (220 * alphaMul)));
        }
    }
}
```

### renderCategoryPanel()

Отрисовка панели категории:

```java
private void renderCategoryPanel(DrawContext context, int mouseX, int mouseY, float panelX, 
                                 Category category, int colorTheme, float alphaMul) {
    float panelY = state.getY() + state.getRenderOffsetY();
    CustomDrawContext customDrawContext = CustomDrawContext.of(context);
    Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
    ColorRGBA themeColor = theme.getColor();

    float rounding = 3.0F;  // Радиус скругления углов
    float headerHeight = ClickGuiLayout.CATEGORY_HEADER_HEIGHT;

    // Получаем цвет GUI из настроек
    ColorRGBA guiBg = Menu.INSTANCE.guiColor.getColor();
    
    // Цвет заголовка (более непрозрачный)
    ColorRGBA headerColor = new ColorRGBA(
        (int) guiBg.getRed(),
        (int) guiBg.getGreen(),
        (int) guiBg.getBlue(),
        (int) (200 * alphaMul)  // Альфа с учетом анимации
    );
    
    // Цвет тела панели (более прозрачный)
    ColorRGBA bodyColor = new ColorRGBA(
        (int) guiBg.getRed(),
        (int) guiBg.getGreen(),
        (int) guiBg.getBlue(),
        (int) (160 * alphaMul)
    );

    // 1. Рисуем блюр фон (эффект размытия)
    DrawUtil.drawBlur(customDrawContext.getMatrices(), panelX, panelY, 
        ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, 
        15.0F, BorderRadius.all(rounding), ColorRGBA.WHITE.withAlpha((int)(255 * alphaMul)));

    // 2. Рисуем тело панели
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, panelY, 
        ClickGuiLayout.WIDTH, ClickGuiLayout.HEIGHT, 
        BorderRadius.all(rounding), bodyColor);

    // 3. Рисуем заголовок (скругление только сверху)
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, panelY, 
        ClickGuiLayout.WIDTH, headerHeight, 
        new BorderRadius(rounding, rounding, 0, 0),  // Скругление только сверху
        headerColor);

    // 4. Рисуем иконку и название категории по центру заголовка
    String icon = category.getIcon();
    float iconW = Fonts.FONT.getWidth(icon, 9.0f);
    float nameW = Fonts.REGULAR.getWidth(category.getName(), 10.0f);
    float gap = 4.0f;  // Отступ между иконкой и текстом
    float totalHeaderW = iconW + gap + nameW;

    // Центрируем по горизонтали
    float headerStartX = panelX + (ClickGuiLayout.WIDTH / 2.0f) - (totalHeaderW / 2.0f);

    // Небольшая корректировка позиции иконки для некоторых категорий
    float iconOffsetX = 0.0f;
    if (category == Category.PLAYER || category == Category.MISC || category == Category.THEMES) {
        iconOffsetX = 2.0f;
    }

    // Рисуем иконку (цветом темы)
    customDrawContext.drawText(Fonts.FONT.getFont(9.0f), icon, 
        headerStartX + iconOffsetX, panelY + 6.0f, 
        themeColor.withAlpha((int)(255 * alphaMul)));
    
    // Рисуем название (цветом темы)
    customDrawContext.drawText(Fonts.REGULAR.getFont(10.0f), category.getName(), 
        headerStartX + iconW + gap, panelY + 5.0F, 
        themeColor.withAlpha((int)(255 * alphaMul)));

    // 5. Подготавливаем область контента (с ограничением по маске)
    float contentY = panelY + headerHeight;
    float contentHeight = ClickGuiLayout.getContentHeight();
    
    // Ограничиваем скролл чтобы не выйти за границы
    state.clampScroll(category, contentHeight - 5);
    
    float moduleY = contentY + 2.0F + state.getScroll(category);  // Начальная Y с учетом скролла

    // 6. Создаем маску stencil для ограничения отрисовки (контент не выходит за границы)
    StencilUtil.push();  // Начинаем маску
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX, contentY, 
        ClickGuiLayout.WIDTH, contentHeight, 
        new BorderRadius(0, 0, rounding, rounding),  // Скругление только снизу
        ColorRGBA.WHITE);
    StencilUtil.read(1);  // Активируем маску (рисуем только внутри)

    // 7. Рисуем контент категории
    if (category == Category.THEMES) {
        // Для категории тем - рисуем список тем
        float themeY = contentY + 2.0F + state.getScroll(category);
        for (Theme themeItem : Dexum.getInstance().getThemeManager().getThemes()) {
            // Рисуем только видимые элементы (оптимизация)
            if (themeY + 18.0f >= contentY && themeY <= contentY + contentHeight && alphaMul > 0.01F) {
                renderThemeItem(context, mouseX, mouseY, panelX, themeY, themeItem, alphaMul);
            }
            themeY += 20.0f;  // Отступ между темами
        }
    } else {
        // Для остальных категорий - рисуем модули
        for (Module module : state.getModules(category)) {
            float openProgress = state.getOpenProgress(module);  // Прогресс анимации открытия
            float moduleHeight = ClickGuiLayout.getModuleHeight(module, openProgress);

            // Рисуем только видимые модули (оптимизация)
            if (moduleY + moduleHeight >= contentY && moduleY <= contentY + contentHeight && alphaMul > 0.01F) {
                renderModule(context, mouseX, mouseY, panelX, moduleY, module, 
                    openProgress, moduleHeight, colorTheme, alphaMul);
            }

            moduleY += ClickGuiLayout.MODULE_GAP + moduleHeight;  // Смещаемся вниз
        }
    }

    StencilUtil.pop();  // Завершаем маску
}
```

### renderThemeItem()

Отрисовка элемента темы:

```java
private void renderThemeItem(DrawContext context, int mouseX, int mouseY, float panelX, 
                            float themeY, Theme themeItem, float alphaMul) {
    CustomDrawContext customDrawContext = CustomDrawContext.of(context);
    Theme currentTheme = Dexum.getInstance().getThemeManager().getCurrentTheme();
    
    // Проверяем выбрана ли тема
    boolean isSelected = themeItem.getName().equalsIgnoreCase(currentTheme.getName());
    
    // Проверяем наведена ли мышь
    boolean isHovered = MathUtil.isHovered(mouseX, mouseY, panelX + 4, themeY, 
        ClickGuiLayout.WIDTH - 8, 18);

    // Определяем цвет фона
    ColorRGBA bg;
    if (isSelected) {
        // Выбранная тема - цветной фон
        bg = themeItem.getColor().withAlpha((int) (80 * alphaMul));
    } else if (isHovered) {
        // Наведение - светлый фон
        bg = new ColorRGBA(255, 255, 255, (int) (20 * alphaMul));
    } else {
        // Обычная - еле видимый фон
        bg = new ColorRGBA(255, 255, 255, (int) (10 * alphaMul));
    }

    // Рисуем фон элемента
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX + 4, themeY, 
        ClickGuiLayout.WIDTH - 8, 18, BorderRadius.all(2.0f), bg);

    // Рисуем квадратик с цветом темы слева
    DrawUtil.drawRoundedRect(customDrawContext.getMatrices(), panelX + 8, themeY + 5, 
        8, 8, BorderRadius.all(2.0f), themeItem.getColor().withAlpha((int) (255 * alphaMul)));

    // Рисуем название темы
    ColorRGBA textColor = isSelected 
        ? ColorRGBA.WHITE  // Выбранная - белый текст
        : new ColorRGBA(200, 200, 210, (int) (255 * alphaMul));  // Обычная - серый
    
    customDrawContext.drawText(Fonts.REGULAR.getFont(7.5f), themeItem.getName(), 
        panelX + 22, themeY + 6.5f, textColor.withAlpha((int) (255 * alphaMul)));
}
```

### renderModule()

Отрисовка модуля (самая сложная часть):

```java
private void renderModule(DrawContext context, int mouseX, int mouseY, float panelX, float moduleY, 
                         Module module, float openProgress, float moduleHeight, 
                         int colorTheme, float alphaMul) {
    CustomDrawContext customDrawContext = CustomDrawContext.of(context);
    Theme theme = Dexum.getInstance().getThemeManager().getCurrentTheme();
    ColorRGBA themeColor = theme.getColor();

    // 1. Рисуем фон модуля (слабая подсветка как в Delta)
    ColorRGBA moduleBg = module.isEnabled()
        ? new ColorRGBA(255, 255, 255, (int)(12 * alphaMul))  // Включен - чуть светлее
        : new ColorRGBA(255, 255, 255, (int)(6 * alphaMul));  // Выключен - еле видимый

    DrawUtil.drawRoundedRect(
        customDrawContext.getMatrices(),
        panelX + ClickGuiLayout.MODULE_PADDING,
        moduleY,
        ClickGuiLayout.MODULE_INNER_WIDTH,
        moduleHeight,
        BorderRadius.all(2.0f),
        moduleBg
    );

    // 2. Анимация тумблера (плавное включение/выключение)
    Animation toggleAnim = state.getModuleDotAnimation(module);
    toggleAnim.update(module.isEnabled());  // Обновляем до текущего состояния
    float toggleProgress = toggleAnim.getValue();  // Получаем промежуточное значение (0.0-1.0)

    // 3. Рисуем тумблер справа (iOS-style switch)
    float toggleWidth = 15f;
    float toggleHeight = 8.5f;
    float toggleX = panelX + ClickGuiLayout.WIDTH - 19f;  // Справа с отступом
    float toggleY = moduleY + 5.5f;

    // Цвет фона тумблера
    ColorRGBA toggleBg = module.isEnabled()
        ? themeColor.withAlpha((int)(200 * alphaMul))  // Включен - цвет темы
        : new ColorRGBA(55, 55, 65, (int)(180 * alphaMul));  // Выключен - серый

    // Рисуем фон тумблера (капсула)
    DrawUtil.drawRoundedRect(
        customDrawContext.getMatrices(),
        toggleX, toggleY,
        toggleWidth, toggleHeight,
        BorderRadius.all(toggleHeight / 2f),  // Радиус = половина высоты для капсулы
        toggleBg
    );

    // Рисуем кружок внутри тумблера
    float circleSize = 6.5f;
    
    // X позиция кружка зависит от toggleProgress (анимация движения)
    float circleX = toggleX + 1.3f + (toggleWidth - circleSize - 2.6f) * toggleProgress;
    float circleY = toggleY + 1.0f;

    DrawUtil.drawRoundedRect(
        customDrawContext.getMatrices(),
        circleX, circleY,
        circleSize, circleSize,
        BorderRadius.all(circleSize / 2f),  // Круглый
        ColorRGBA.WHITE.withAlpha((int)(255 * alphaMul))
    );

    // 4. Рисуем название модуля слева
    String moduleName = module.getName();
    ColorRGBA nameColor = module.isEnabled()
        ? ColorRGBA.WHITE  // Включен - белый
        : new ColorRGBA(200, 200, 210, (int)(255 * alphaMul));  // Выключен - серый

    customDrawContext.drawText(
        Fonts.REGULAR.getFont(7.5f),
        moduleName,
        panelX + ClickGuiLayout.SETTING_LEFT + 2.0f,
        moduleY + 7.5f,
        nameColor.withAlpha((int)(255 * alphaMul))
    );

    // 5. Рисуем бинд (привязку клавиши) слева от тумблера
    String keyText = "";
    if (state.getBindingModule() == module) {
        // Сейчас выбирается клавиша для этого модуля
        keyText = "[...]";
    } else {
        // Показываем текущую клавишу если есть
        int key = module.getKeyCode();
        if (key != -1) {
            keyText = "[" + getShortKeyName(key) + "]";
        }
    }

    if (!keyText.isEmpty()) {
        float keyWidth = Fonts.REGULAR.getWidth(keyText, 7.0f);
        float keyTextX = toggleX - 5f - keyWidth;  // Слева от тумблера

        ColorRGBA keyColor = new ColorRGBA(160, 160, 170, (int)(220 * alphaMul));
        customDrawContext.drawText(
            Fonts.REGULAR.getFont(7.0f),
            keyText,
            keyTextX,
            moduleY + 7.5f,
            keyColor
        );
    }

    // 6. Рисуем три точки "•••" (индикатор наличия настроек)
    if (ClickGuiLayout.hasVisibleSettings(module)) {
        float dotsX;

        if (!keyText.isEmpty()) {
            // Если есть бинд - рисуем левее бинда
            float keyWidth = Fonts.REGULAR.getWidth(keyText, 7.0f);
            dotsX = toggleX - 5f - keyWidth - 12f;
        } else {
            // Если нет бинда - рисуем левее тумблера
            dotsX = toggleX - 14f;
        }

        ColorRGBA dotsColor = module.isEnabled()
            ? ColorRGBA.WHITE.withAlpha((int)(200 * alphaMul))
            : ColorRGBA.WHITE.withAlpha((int)(140 * alphaMul));

        customDrawContext.drawText(
            Fonts.REGULAR.getFont(8.5f),
            "•••",  // Три точки
            dotsX,
            moduleY + 6.8f,
            dotsColor
        );
    }

    // 7. Рисуем настройки модуля (через settingRenderer)
    settingRenderer.render(context, module, panelX, moduleY, openProgress, 
        colorTheme, mouseX, mouseY, state, alphaMul);
}
```

---

## Итоговая схема рендеринга

```
render()
├─ renderCategoryPanel() для каждой категории
│  ├─ Рисуем блюр фон
│  ├─ Рисуем тело панели
│  ├─ Рисуем заголовок
│  ├─ Рисуем иконку и название
│  ├─ Создаем stencil маску для контента
│  └─ Если THEMES:
│     └─ renderThemeItem() для каждой темы
│        ├─ Рисуем фон элемента
│        ├─ Рисуем квадратик с цветом
│        └─ Рисуем название
│  └─ Иначе:
│     └─ renderModule() для каждого модуля
│        ├─ Рисуем фон модуля
│        ├─ Рисуем тумблер iOS-style
│        ├─ Рисуем название
│        ├─ Рисуем бинд клавиши
│        ├─ Рисуем три точки (если есть настройки)
│        └─ settingRenderer.render() для настроек
├─ renderSearchField()
│  ├─ Рисуем блюр фон
│  ├─ Рисуем прямоугольник
│  ├─ Рисуем текст/плейсхолдер
│  ├─ Рисуем иконку лупы
│  └─ Рисуем мигающий курсор (если активно)
└─ Рисуем color picker popup (если открыт)
```

