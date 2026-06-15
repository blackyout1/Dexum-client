package wtf.dexum.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.client.modules.api.setting.Setting;

public class StringSetting extends Setting {
    private String value;
    private final int maxLength;

    public StringSetting(String name, String defaultValue) {
        this(name, defaultValue, 32, () -> true);
    }

    public StringSetting(String name, String defaultValue, Supplier<Boolean> visible) {
        this(name, defaultValue, 32, visible);
    }

    public StringSetting(String name, String defaultValue, int maxLength, Supplier<Boolean> visible) {
        super(name);
        this.value = defaultValue == null ? "" : defaultValue;
        this.maxLength = maxLength;
        this.setVisible(visible);
    }

    public void setValue(String value) {
        if (value == null) {
            this.value = "";
            return;
        }
        this.value = value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(this.name, this.value);
    }

    @Override
    public void load(JsonObject propertiesObject) {
        if (propertiesObject.has(this.name) && !propertiesObject.get(this.name).isJsonNull()) {
            this.setValue(propertiesObject.get(this.name).getAsString());
        }
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public int getMaxLength() {
        return this.maxLength;
    }
}