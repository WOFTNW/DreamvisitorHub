package org.woftnw.dreamvisitorhub.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.pb.PocketBase;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Config {
    private static Map<String, Object> config;
    private static String configId;
    private static PocketBase pocketBaseClient;
    private static final String COLLECTION_NAME = "dreamvisitor_config";

    public static void init(PocketBase pocketBase, String configId) throws IOException {

        pocketBaseClient = pocketBase;
        Config.configId = configId;

        // Initial config load
        loadConfig();
    }

    public static void loadConfig() throws IOException {
        if (pocketBaseClient == null) {
            System.out.println("PocketBase client not initialized, cannot load config");
            return;
        }

        // Get record using PocketBase client
        System.out.println("Getting record " + configId + " from collection " + COLLECTION_NAME);
        JsonObject record = pocketBaseClient.getRecord(COLLECTION_NAME, configId, null, null);
        config = new Gson().fromJson(record, Map.class);

        System.out.println("Loaded PocketBase configuration: " + config);

        // Apply config values to the system
        applyConfig();
    }

    public static void updateLocalConfig(JsonObject newConfigData) {
        // Update our local config with new data
        if (config != null) {
            // Merge the new data into our existing config
            for (String key : newConfigData.keySet()) {
                config.put(key, newConfigData.get(key));
            }

            // Apply the updated config
            applyConfig();
        }
    }

    private static void applyConfig() {
        // Apply configuration values to the relevant systems
//        if (config != null) {
//            // Add more configuration handlers here as needed
//        }
    }

    @NotNull
    @Contract("_, _ -> new")
    public static <T> CompletableFuture<Void> set(ConfigKey key, T value) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (pocketBaseClient == null) {
                    System.out.println("PocketBase client not initialized, cannot update config");
                    return;
                }

                if (value != null && !key.getType().isInstance(value)) {
                    throw new IllegalArgumentException("Value for config key '" + key.getKey() +
                            "' is not of expected type: " + key.getType().getSimpleName());
                }

                // Create update data object
                JsonObject updateData = new JsonObject();

                // Handle manual conversion for supported types
                if (value instanceof Boolean) {
                    updateData.addProperty(key.getKey(), (Boolean) value);
                } else if (value instanceof Number) {
                    updateData.addProperty(key.getKey(), (Number) value);
                } else if (value instanceof String) {
                    updateData.addProperty(key.getKey(), (String) value);
                } else {
                    // More types can be added if needed
                    throw new IllegalArgumentException("Unsupported config value type for key '" + key.getKey() + "'");
                }

                // Update the record
                pocketBaseClient.updateRecord(COLLECTION_NAME, configId, updateData, null, null);

                System.out.println("Updated PocketBase configuration field " + key.getKey() + " to " + value);

                // Update local storage
                if (value instanceof Boolean) {
                    config.put(key.getKey(), new Gson().toJsonTree(value, Boolean.class));
                } else if (value instanceof Number) {
                    config.put(key.getKey(), new Gson().toJsonTree(value, Number.class));
                } else {
                    config.put(key.getKey(), new Gson().toJsonTree(value, String.class));
                }

                // If not using realtime updates, we need to reload config manually
                loadConfig();

            } catch (IOException e) {
                System.out.println("Error updating PocketBase config: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(@NotNull ConfigKey configKey) {
        Object value = config.get(configKey.getKey());

        if (value == null) {
            value = configKey.getDefaultValue();
        }

        Class<?> expectedType = configKey.getType();

        // Handle numeric coercion
        if (expectedType == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (expectedType == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (expectedType == Float.class && value instanceof Number) {
            return (T) Float.valueOf(((Number) value).floatValue());
        } else if (expectedType == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (expectedType.isInstance(value)) {
            return (T) value;
        }

        throw new IllegalStateException("Config value for key '" + configKey.getKey() +
                "' is type " + value.getClass().getName() + ", not of expected type: " + configKey.getType().getSimpleName());
    }

}
