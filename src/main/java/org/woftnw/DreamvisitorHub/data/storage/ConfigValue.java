package org.woftnw.DreamvisitorHub.data.storage;

public class ConfigValue {

    private String name;
    private String displayName;
    private String description;
    private Object defaultValue;
    private Type type;

    public enum Type {
        BOOLEAN,
        INT,
        DOUBLE,
        FLOAT,
        LONG,
        STRING,
        CHAR,
        LOCATION,
        CHANNEL,
        ROLE
    }

    public ConfigValue(String name, String displayName, String description, Object defaultValue, Type type) {
        this.name = name;
        this.description = description;
        this.displayName = displayName;
        this.defaultValue = defaultValue;
        this.type = type;
    }
}
