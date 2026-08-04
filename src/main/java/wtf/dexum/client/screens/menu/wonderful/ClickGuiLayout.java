package wtf.dexum.client.screens.menu.wonderful;

import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.*;

import java.util.List;

/**
 * ClickGuiLayout - Класс с константами и расчетами размеров для ClickGUI
 * 
 * Содержит все константы размеров элементов интерфейса и методы
 * для вычисления позиций и размеров динамических элементов
 */
public final class ClickGuiLayout {
    // ============== РАЗМЕРЫ КАТЕГОРИЙ ==============
    
    /** Высота заголовка категории (верхняя панель с названием) */
    public static final float CATEGORY_HEADER_HEIGHT = 18.0f;
    
    /** Ширина панели одной категории */
    public static final float WIDTH = 105f;
    
    /** Высота панели категории */
    public static final float HEIGHT = 260f;
    
    /** Расстояние между панелями категорий по горизонтали */
    public static final float CATEGORY_PANEL_STEP = 115f;

    // ============== РАЗМЕРЫ МОДУЛЕЙ ==============
    
    /** Внутренний отступ модуля от краев панели */
    public static final float MODULE_PADDING = 2.5f;
    
    /** Расстояние между модулями по вертикали */
    public static final float MODULE_GAP = 2.5f;
    
    /** Высота заголовка модуля (кнопка с названием) */
    public static final float MODULE_HEADER_HEIGHT = 19.0f;
    
    /** Внутренняя ширина модуля (для размещения контента) */
    public static final float MODULE_INNER_WIDTH = 100.0f;
    
    /** Y позиция начала настроек внутри модуля */
    public static final float SETTING_START_Y = 17.5f;
    
    /** Отступ между настройками по вертикали */
    public static final float SETTING_PADDING = 3.0f;
    
    /** Нижний отступ под последней настройкой */
    public static final float SETTING_BOTTOM_PADDING = 6.0f;
    
    /** Левая граница для размещения настроек */
    public static final float SETTING_LEFT = 7.0f;
    
    /** Правая граница для размещения настроек */
    public static final float SETTING_RIGHT = 98.0f;
    
    /** Ширина слайдера для числовых настроек */
    public static final float SLIDER_WIDTH = 88f;

    // ============== РАЗМЕРЫ ЧИПОВ (кнопок режимов) ==============
    
    /** Горизонтальный отступ между чипами */
    public static final float CHIP_GAP_X = 3.0f;
    
    /** Вертикальный отступ между строками чипов */
    public static final float CHIP_GAP_Y = 3.0f;
    
    /** Внутренний горизонтальный отступ внутри чипа */
    public static final float CHIP_PADDING_X = 4.0f;
    
    /** Внутренний вертикальный отступ внутри чипа */
    public static final float CHIP_PADDING_Y = 2.0f;
    
    /** Ширина кликабельной области (для чипов и кнопок) */
    public static final float CLICKABLE_WIDTH = 79f;

    // ============== РАЗМЕРЫ ПОИСКА ==============
    
    /** Максимальное количество символов в строке поиска */
    public static final int SEARCH_MAX_CHARS = 24;
    
    /** Ширина поля поиска */
    public static final float SEARCH_WIDTH = 120f;
    
    /** Высота поля поиска */
    public static final float SEARCH_HEIGHT = 22f;
    
    /** Отступ поля поиска от GUI сверху */
    public static final float SEARCH_GAP = 8f;
    
    /** X позиция иконки поиска внутри поля */
    public static final float SEARCH_ICON_X = 3.5f;
    
    /** X позиция текста поиска внутри поля */
    public static final float SEARCH_TEXT_X = 19f;
    
    /** Правый отступ текста в поле поиска */
    public static final float SEARCH_RIGHT_PADDING = 8f;

    /**
     * Приватный конструктор - класс только со статическими методами
     * Нельзя создать экземпляр класса
     */
    private ClickGuiLayout() {
    }

    /**
     * Вычисляет общую ширину всех панелей категорий
     * @param categoryCount количество категорий
     * @return общая ширина в пикселях
     */
    public static float getTotalCategoriesWidth(int categoryCount) {
        // Формула: (количество-1) * шаг + ширина_одной_панели
        // Например для 5 категорий: 4*115 + 105 = 565px
        return ((categoryCount - 1) * CATEGORY_PANEL_STEP) + WIDTH;
    }

    /**
     * Вычисляет X координату панели категории по индексу
     * @param x начальная X координата GUI
     * @param index индекс категории (0, 1, 2...)
     * @return X координата панели
     */
    public static float getCategoryPanelX(float x, int index) {
        // Каждая следующая панель смещена на CATEGORY_PANEL_STEP вправо
        return x + (index * CATEGORY_PANEL_STEP);
    }

    /**
     * Вычисляет Y координату начала контента (после заголовка)
     * @param y Y координата верха панели
     * @return Y координата начала контента
     */
    public static float getContentY(float y) {
        // Контент начинается после заголовка
        return y + CATEGORY_HEADER_HEIGHT;
    }

    /**
     * Получает высоту области контента (без заголовка)
     * @return высота контента
     */
    public static float getContentHeight() {
        // Общая высота минус заголовок минус отступ снизу
        return HEIGHT - CATEGORY_HEADER_HEIGHT - 2.0f;
    }

    /**
     * Вычисляет X координату поля поиска (центрирует его)
     * @param x начальная X координата GUI
     * @param categoryCount количество категорий
     * @param searchWidth ширина поля поиска
     * @return X координата поля поиска
     */
    public static float getSearchX(float x, int categoryCount, float searchWidth) {
        // Центрируем поиск относительно всей ширины GUI
        return x + (getTotalCategoriesWidth(categoryCount) / 2f) - (searchWidth / 2f);
    }

    /**
     * Вычисляет Y координату поля поиска (под GUI)
     * @param y Y координата верха GUI
     * @return Y координата поля поиска
     */
    public static float getSearchY(float y) {
        // Поиск размещается под GUI с отступом SEARCH_GAP
        return y + HEIGHT + SEARCH_GAP;
    }

    /**
     * Проверяет есть ли у модуля видимые настройки
     * @param module модуль для проверки
     * @return true если есть хотя бы одна видимая настройка
     */
    public static boolean hasVisibleSettings(Module module) {
        // Получаем список настроек
        List<Setting> settings = module.getSettings();
        
        // Если список пустой или null - настроек нет
        if (settings == null || settings.isEmpty()) return false;
        
        // Проверяем каждую настройку
        for (Setting setting : settings) {
            // Если настройка видима - возвращаем true
            if (setting != null && setting.isVisible()) {
                return true;
            }
        }
        
        // Ни одной видимой настройки не найдено
        return false;
    }

    /**
     * Вычисляет высоту настройки типа ModeSetting (выбор режима)
     * Чипы размещаются в несколько строк если не помещаются
     * @param modeSetting настройка режима
     * @return высота в пикселях
     */
    public static float calculateModeSettingHeight(ModeSetting modeSetting) {
        float x = SETTING_LEFT;  // Текущая X позиция для размещения чипа
        float rowHeight = 11.0f;  // Высота одной строки чипов
        int rows = 1;  // Счетчик строк (начинаем с 1)

        // Перебираем все варианты режима
        for (ModeSetting.Value val : modeSetting.getValues()) {
            // Вычисляем ширину текста названия режима
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            
            // Ширина чипа = ширина текста + отступы с двух сторон
            float chipW = textW + (CHIP_PADDING_X * 2);

            // Если чип не помещается в текущую строку
            if (x + chipW > SETTING_RIGHT) {
                x = SETTING_LEFT;  // Сбрасываем X в начало
                rows++;  // Увеличиваем счетчик строк
            }
            
            // Смещаем X для следующего чипа
            x += chipW + CHIP_GAP_X;
        }
        
        // Итоговая высота = отступ сверху + (количество_строк * высота_строки) + отступы_между_строками + отступ снизу
        return 9.0f + (rows * rowHeight) + ((rows - 1) * CHIP_GAP_Y) + 2.0f;
    }

    /**
     * Вычисляет высоту настройки типа MultiBooleanSetting
     * Аналогично ModeSetting - чипы размещаются в несколько строк
     * @param multiBooleanSetting настройка с несколькими boolean
     * @return высота в пикселях
     */
    public static float calculateMultiBooleanHeight(MultiBooleanSetting multiBooleanSetting) {
        float x = SETTING_LEFT;  // Текущая X позиция
        float rowHeight = 11.0f;  // Высота строки
        int rows = 1;  // Счетчик строк

        // Перебираем все boolean настройки
        for (MultiBooleanSetting.Value val : multiBooleanSetting.getBooleanSettings()) {
            // Ширина текста названия
            float textW = Fonts.REGULAR.getWidth(val.getName(), 6.0f);
            
            // Ширина чипа
            float chipW = textW + (CHIP_PADDING_X * 2);

            // Если не помещается - переходим на новую строку
            if (x + chipW > SETTING_RIGHT) {
                x = SETTING_LEFT;
                rows++;
            }
            
            x += chipW + CHIP_GAP_X;
        }
        
        // Итоговая высота
        return 9.0f + (rows * rowHeight) + ((rows - 1) * CHIP_GAP_Y) + 2.0f;
    }

    /**
     * Вычисляет общую высоту всех настроек модуля
     * @param module модуль
     * @return общая высота настроек в пикселях
     */
    public static float calculateSettingsHeight(Module module) {
        float height = 0f;  // Счетчик высоты
        
        // Получаем список настроек
        List<Setting> settings = module.getSettings();
        
        // Если настроек нет - высота 0
        if (settings == null || settings.isEmpty()) return 0f;

        boolean hasVisibleSetting = false;  // Флаг наличия видимых настроек
        float globalGap = 2.5f;  // Отступ между настройками

        // Фильтруем только видимые настройки
        List<Setting> visibleSettings = settings.stream().filter(Setting::isVisible).toList();
        
        // Перебираем видимые настройки
        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting setting = visibleSettings.get(i);
            hasVisibleSetting = true;  // Есть хотя бы одна видимая

            float currentHeight = 0;  // Высота текущей настройки
            
            // Определяем высоту в зависимости от типа настройки
            if (setting instanceof BooleanSetting || setting instanceof KeySetting) {
                // Boolean и Key - простые однострочные настройки
                currentHeight = 12f;
                
            } else if (setting instanceof NumberSetting || setting instanceof StringSetting) {
                // Number (слайдер) и String (текстовое поле) - двухстрочные
                currentHeight = 22f;
                
            } else if (setting instanceof ModeSetting modeSetting) {
                // Mode - высота зависит от количества режимов
                currentHeight = calculateModeSettingHeight(modeSetting);
                
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                // MultiBoolean - высота зависит от количества элементов
                currentHeight = calculateMultiBooleanHeight(multiBooleanSetting);
                
            } else if (setting instanceof ColorSetting) {
                // Color - одна строка (палитра открывается отдельно)
                currentHeight = 14f;
            }
            
            // Добавляем высоту текущей настройки
            height += currentHeight;

            // Добавляем отступ между настройками (кроме последней)
            if (i < visibleSettings.size() - 1) height += globalGap;
        }

        // Если были видимые настройки - добавляем нижний отступ
        if (hasVisibleSetting) height += SETTING_BOTTOM_PADDING;
        
        return height;
    }

    /**
     * Вычисляет полную высоту модуля с учетом прогресса открытия
     * @param module модуль
     * @param openProgress прогресс анимации открытия (0.0 закрыт, 1.0 открыт)
     * @return полная высота модуля
     */
    public static float getModuleHeight(Module module, float openProgress) {
        // Высота = заголовок + (высота_настроек * прогресс_открытия)
        // При openProgress=0 настройки не видны (высота=0)
        // При openProgress=1 настройки полностью видны
        return MODULE_HEADER_HEIGHT + (calculateSettingsHeight(module) * openProgress);
    }
}
