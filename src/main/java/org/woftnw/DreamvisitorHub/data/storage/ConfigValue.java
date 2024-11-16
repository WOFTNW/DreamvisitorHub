package org.woftnw.DreamvisitorHub.data.storage;

public class ConfigValue<E> {

    private final String name;
    private final String displayName;
    private final String description;
    private final E defaultValue;
    private E value;

    public ConfigValue(String name, String displayName, String description, E defaultValue) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public E getDefaultValue() {
        return defaultValue;
    }

    public E getValue() {
        return value;
    }

    public void setValue(E value) {
        this.value = value;
    }
}
