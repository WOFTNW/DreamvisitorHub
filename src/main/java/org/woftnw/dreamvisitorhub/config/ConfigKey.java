package org.woftnw.dreamvisitorhub.config;

/**
 * An enum that holds all the relevant configuration keys. Each key has a default value and a type.
 */
public enum ConfigKey {

    DEBUG("debug", true, Boolean.class), // This is intentionally set to true because these default should typically only be used when no database is connected.
    AUTO_RESTART("auto_restart", false, Boolean.class),
    PAUSE_CHAT("pause_chat", false, Boolean.class),
    RESOURCE_PACK_REPO("resource_pack_repo", "WOFTNW/Dragonspeak", String.class),
    LOG_CONSOLE("log_console", false, Boolean.class),
    ENABLE_LOG_CONSOLE_COMMANDS("enable_log_console_commands", false, Boolean.class), // Technically this only need to be read by DreamvisitorHub, but it's a good idea to have it here for redundancy.
    WEB_WHITELIST_ENABLED("web_whitelist_enabled", true, Boolean.class),
    WEBSITE_URL("website_url", "http://127.0.0.1", String.class),
    GAME_CHAT_CHANNEL("game_chat_channel", "1008590641091072020", String.class),
    WHITELIST_CHANNEL("whitelist_channel", 1020730055539839116L, Long.class),
    GAME_LOG_CHANNEL("game_log_channel", 1019409712527200266L, Long.class),
    INFRACTION_EXPIRE_TIME_DAYS("infraction_expire_time_days", 90, Integer.class),
    INFRACTIONS_CATEGORY("infractions_category", 1387827882554032000L, Long.class),
    SHOP_NAME("shop_name", "DV Shop", String.class),
    CURRENCY_ICON("currency_icon", "$", String.class),
    DAILY_BASE_AMOUNT("daily_base_amount", 50.00, Double.class),
    DAILY_STREAK_MULTIPLIER("daily_streak_multiplier", 2.00, Double.class),
    WORK_REWARD("work_reward", 100.00, Double.class),
    WORK_COOLDOWN_MINUTES("work_cooldown", 60, Integer.class),
    WHITELIST_PORT("whitelist_port", 10826, Integer.class);

    private final String key;
    private final Object defaultValue;
    private final Class<?> type;

    ConfigKey(String key, Object defaultValue, Class<?> type) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getType() {
        return type;
    }

}
