package wtf.dexum.client.screens.menu.wonderful;

import net.minecraft.client.util.Window;
import wtf.dexum.Dexum;
import wtf.dexum.base.animations.base.Animation;
import wtf.dexum.base.animations.base.Easing;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.setting.Setting;
import wtf.dexum.client.modules.api.setting.impl.*;

import java.util.*;

public class ClickGuiState {
    private static final Map<Character, Character> RU_TO_EN = new HashMap<>();

    static {
        String ru = "йцукенгшщзхъфывапролджэячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
        String en = "qwertyuiop[]asdfghjkl;'zxcvbnm,.QWERTYUIOP[]ASDFGHJKL;'ZXCVBNM,.";
        int length = Math.min(ru.length(), en.length());
        for (int i = 0; i < length; i++) {
            RU_TO_EN.put(ru.charAt(i), en.charAt(i));
        }
    }

    private final Map<Module, Animation> moduleOpenAnimation = new IdentityHashMap<>();
    private final Map<Module, Boolean> moduleOpenState = new IdentityHashMap<>();
    private final Map<Module, Animation> moduleDotAnimation = new IdentityHashMap<>();

    private final Map<BooleanSetting, Animation> booleanBackgroundAnimation = new HashMap<>();
    private final Map<BooleanSetting, Animation> booleanCircleAnimation = new HashMap<>();
    private final Map<NumberSetting, Animation> sliderAnimation = new HashMap<>();
    private final Map<String, Animation> modeAnimation = new HashMap<>();
    private final Animation themeDropdownAnimation = new Animation(220L, Easing.CUBIC_OUT);

    private final Map<Category, Float> categoryScrollTarget = new EnumMap<>(Category.class);
    private final Map<Category, Animation> categoryScrollAnimation = new EnumMap<>(Category.class);
    private final Map<Category, List<Module>> modulesByCategory = new EnumMap<>(Category.class);
    private final List<Module> allModules = new ArrayList<>();

    private float x;
    private float y;
    private Setting bindingSetting;
    private Module bindingModule;
    private float renderOffsetY;
    private boolean searchActive;
    private String searchText = "";
    private int searchCursor = 0;
    private StringSetting editingStringSetting;
    private int stringCursor = 0;
    private boolean themeDropdownOpen;
    private NumberSetting draggingSlider;

    public ClickGuiState() {
        refreshModules();
    }

    public void refreshModules() {
        allModules.clear();
        allModules.addAll(Dexum.getInstance().getModuleManager().getModules());
        for (Category category : Category.values()) {
            modulesByCategory.put(category, allModules.stream().filter(module -> module.getCategory() == category).toList());
            categoryScrollTarget.putIfAbsent(category, 0f);
            categoryScrollAnimation.putIfAbsent(category, new Animation(300L, Easing.CUBIC_OUT));
        }
    }

    public void updatePosition(Window window, int categoryCount) {
        float totalCategoriesWidth = ClickGuiLayout.getTotalCategoriesWidth(categoryCount);
        this.x = (window.getScaledWidth() / 2F) - (totalCategoriesWidth / 2F);
        this.y = (window.getScaledHeight() / 2F) - (ClickGuiLayout.HEIGHT / 2F);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getRenderOffsetY() { return renderOffsetY; }
    public void setRenderOffsetY(float renderOffsetY) { this.renderOffsetY = renderOffsetY; }

    public List<Module> getModules(Category category) {
        List<Module> modules = modulesByCategory.getOrDefault(category, List.of());
        if (searchText.isBlank()) {
            return modules;
        }

        String query = searchText.toLowerCase(Locale.ROOT);
        return modules.stream()
                .filter(module -> module.getName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    public List<Module> getAllModules() { return allModules; }

    public String toEnglish(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(RU_TO_EN.getOrDefault(c, c));
        }
        return result.toString();
    }

    public float getSliderPos(NumberSetting setting) {
        float delta = setting.getMax() - setting.getMin();
        return (setting.getCurrent() - setting.getMin()) / delta;
    }

    public float getSliderValue(NumberSetting setting, float posX, double mouseX) {
        float delta = setting.getMax() - setting.getMin();
        float clickedX = (float) mouseX - posX;
        float value = Math.max(0f, Math.min(1f, clickedX / ClickGuiLayout.SLIDER_WIDTH));
        float outValue = setting.getMin() + delta * value;
        float increment = setting.getIncrement();
        outValue = Math.round(outValue / increment) * increment;
        return Math.max(setting.getMin(), Math.min(setting.getMax(), outValue));
    }

    public float getScroll(Category category) {
        Animation animation = categoryScrollAnimation.computeIfAbsent(category, key -> new Animation(250L, Easing.CUBIC_OUT));
        animation.update(categoryScrollTarget.getOrDefault(category, 0f));
        return animation.getValue();
    }

    public void clampScroll(Category category, float contentHeight) {
        float totalHeight = getTotalModulesHeight(category);
        float maxScroll = Math.min(0f, contentHeight - totalHeight);
        float currentTarget = categoryScrollTarget.getOrDefault(category, 0f);
        if (currentTarget < maxScroll || currentTarget > 0f) {
            categoryScrollTarget.put(category, Math.max(maxScroll, Math.min(0f, currentTarget)));
        }
    }

    public void addScroll(Category category, double verticalAmount, float contentHeight) {
        float totalHeight = getTotalModulesHeight(category);
        float maxScroll = Math.min(0f, contentHeight - totalHeight);
        float currentTarget = categoryScrollTarget.getOrDefault(category, 0f);
        float newTarget = currentTarget + (float) (verticalAmount * 20);
        categoryScrollTarget.put(category, Math.max(maxScroll, Math.min(0f, newTarget)));
    }

    public float getTotalModulesHeight(Category category) {
        if (category == Category.THEMES) {
            return Dexum.getInstance().getThemeManager().getThemes().size() * 20.0f + 4.0f;
        }

        float totalHeight = 0f;
        for (Module module : getModules(category)) {
            totalHeight += ClickGuiLayout.MODULE_GAP + ClickGuiLayout.getModuleHeight(module, getOpenProgress(module));
        }
        return totalHeight;
    }

    public float getOpenProgress(Module module) {
        Animation animation = moduleOpenAnimation.computeIfAbsent(
                module,
                key -> new Animation(250L, Easing.CUBIC_OUT)
        );
        animation.update(isModuleOpen(module) ? 1f : 0f);
        return animation.getValue();
    }

    public boolean isModuleOpen(Module module) {
        return moduleOpenState.getOrDefault(module, false);
    }

    public void toggleModuleOpen(Module module) {
        moduleOpenState.put(module, !isModuleOpen(module));
    }

    public Animation getBooleanBackgroundAnimation(BooleanSetting setting) {
        return booleanBackgroundAnimation.computeIfAbsent(
                setting,
                key -> new Animation(200L, Easing.CUBIC_OUT)
        );
    }

    public Animation getBooleanCircleAnimation(BooleanSetting setting) {
        return booleanCircleAnimation.computeIfAbsent(
                setting,
                key -> new Animation(200L, Easing.CUBIC_OUT)
        );
    }

    public Animation getSliderAnimation(NumberSetting setting) {
        return sliderAnimation.computeIfAbsent(
                setting,
                key -> new Animation(300L, Easing.CUBIC_OUT)
        );
    }

    public Animation getModeAnimation(String key, boolean selected) {
        return modeAnimation.computeIfAbsent(
                key,
                unused -> new Animation(200L, Easing.CUBIC_OUT)
        );
    }

    public Animation getModuleDotAnimation(Module module) {
        return moduleDotAnimation.computeIfAbsent(
                module,
                key -> new Animation(200L, Easing.CUBIC_OUT)
        );
    }

    public Setting getBindingSetting() { return bindingSetting; }
    public void setBindingSetting(Setting bindingSetting) { this.bindingSetting = bindingSetting; }

    public Module getBindingModule() { return bindingModule; }
    public void setBindingModule(Module bindingModule) { this.bindingModule = bindingModule; }

    public boolean isSearchActive() { return searchActive; }
    public void setSearchActive(boolean searchActive) { this.searchActive = searchActive; }

    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public int getSearchCursor() { return searchCursor; }
    public void setSearchCursor(int searchCursor) { this.searchCursor = searchCursor; }

    public StringSetting getEditingStringSetting() { return editingStringSetting; }
    public void setEditingStringSetting(StringSetting editingStringSetting) {
        this.editingStringSetting = editingStringSetting;
        this.stringCursor = editingStringSetting == null ? 0 : editingStringSetting.getValue().length();
    }

    public int getStringCursor() { return stringCursor; }
    public void setStringCursor(int stringCursor) { this.stringCursor = stringCursor; }

    public boolean isThemeDropdownOpen() { return themeDropdownOpen; }
    public void setThemeDropdownOpen(boolean themeDropdownOpen) { this.themeDropdownOpen = themeDropdownOpen; }

    public boolean isDraggingSlider(NumberSetting setting) { return draggingSlider == setting; }
    public void setDraggingSlider(NumberSetting setting) { this.draggingSlider = setting; }

    public float getThemeDropdownProgress() {
        themeDropdownAnimation.update(themeDropdownOpen);
        return themeDropdownAnimation.getValue();
    }
}