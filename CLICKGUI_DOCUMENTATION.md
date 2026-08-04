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

