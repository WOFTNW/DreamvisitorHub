package org.woftnw.dreamvisitorhub.pb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.woftnw.dreamvisitorhub.data.repository.PocketBaseChatMessageRepository;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for PocketBase operations
 */
public class PocketBaseUtils {
    private static final Logger LOGGER = Logger.getLogger(PocketBaseUtils.class.getName());
    private static final Gson gson = new Gson();

    /**
     * Converts a JsonObject to a specific class instance
     *
     * @param json  The JsonObject to convert
     * @param clazz The target class
     * @param <T>   Target type
     * @return An instance of the target class
     */
    public static <T> T jsonToClass(@NotNull JsonObject json, @NotNull Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    /**
     * Converts a JsonObject to an instance of the specified type
     *
     * @param json The JsonObject to convert
     * @param type The target type
     * @param <T>  Target type
     * @return An instance of the target type
     */
    public static <T> T jsonToType(@NotNull JsonObject json, @NotNull Type type) {
        return gson.fromJson(json, type);
    }

    /**
     * Converts a list of JsonObjects to a list of class instances
     *
     * @param jsonList The list of JsonObjects
     * @param clazz    The target class
     * @param <T>      Target type
     * @return A list of instances of the target class
     */
    public static <T> List<T> jsonListToClass(@NotNull List<JsonObject> jsonList, @NotNull Class<T> clazz) {
        return jsonList.stream()
                .map(json -> jsonToClass(json, clazz))
                .collect(Collectors.toList());
    }

    /**
     * Gets a string value from JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return String value or null if not found or not a string
     */
    public static String getString(JsonObject json, String key) {
        if (json == null || !json.has(key))
            return null;
        JsonElement element = json.get(key);
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString()
                : null;
    }

    /**
     * Gets an integer value from JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return Integer value or null if not found or not a number
     */
    public static Integer getInteger(JsonObject json, String key) {
        if (json == null || !json.has(key))
            return null;
        JsonElement element = json.get(key);
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt()
                : null;
    }

    /**
     * Gets a double value from JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return Double value or null if not found or not a number
     */
    public static Double getDouble(JsonObject json, String key) {
        if (json == null || !json.has(key))
            return null;
        JsonElement element = json.get(key);
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble()
                : null;
    }

    /**
     * Gets a boolean value from JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return Boolean value or null if not found or not a boolean
     */
    public static Boolean getBoolean(JsonObject json, String key) {
        if (json == null || !json.has(key))
            return null;
        JsonElement element = json.get(key);
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : null;
    }

    /**
     * Gets a nested JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return JsonObject or null if not found or not an object
     */
    public static JsonObject getJsonObject(JsonObject json, String key) {
        if (json == null || !json.has(key))
            return null;
        JsonElement element = json.get(key);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * Gets a OffsetDateTime value from JsonObject safely
     *
     * @param json JsonObject to get value from
     * @param key  Key to lookup
     * @return OffsetDateTime value or null if not found or not a OffsetDateTime
     */
    @Nullable
    public static OffsetDateTime getOffsetDateTime(@NotNull JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                String dateStr = json.get(key).getAsString();

                // Check if the string is empty or blank
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    return null;
                }

                // Handle PocketBase date format "yyyy-MM-dd HH:mm:ss.SSSZ"
                if (dateStr.contains(" ") && !dateStr.contains("T")) {
                    // Replace space with 'T' to make it ISO-8601 compatible
                    dateStr = dateStr.replace(" ", "T");
                }

                return OffsetDateTime.parse(dateStr);
            } catch (Exception e) {
                LOGGER.warning("Failed to parse date: " + json.get(key).getAsString() + " - " + e.getMessage());

                // Try alternative parsing with explicit formatter
                try {
                    String dateStr = json.get(key).getAsString();

                    // Check if the string is empty or blank
                    if (dateStr == null || dateStr.trim().isEmpty()) {
                        return null;
                    }

                    // Convert to ISO format for parsing
                    if (dateStr.endsWith("Z")) {
                        dateStr = dateStr.substring(0, dateStr.length() - 1) + "+0000";
                    }

                    return OffsetDateTime.parse(dateStr.replace(" ", "T"));
                } catch (Exception ex) {
                    LOGGER.warning("Alternative date parsing also failed: " + ex.getMessage());
                    return null;
                }
            }
        }
        return null;
    }
}
