package org.woftnw.dreamvisitorhub.data.repository;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.data.type.DVUser;
import org.woftnw.dreamvisitorhub.pb.PocketBase;
import org.woftnw.dreamvisitorhub.util.Formatter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * PocketBase implementation of the UserRepository interface
 */
public class PocketBaseUserRepository implements UserRepository {
    private static final Logger LOGGER = Logger.getLogger(PocketBaseUserRepository.class.getName());
    private static final String COLLECTION_NAME = "users";
    private final PocketBase pocketBase;
    private final Gson gson;

    /**
     * Constructor for PocketBaseUserRepository
     *
     * @param pocketBase The PocketBase client to use
     */
    public PocketBaseUserRepository(PocketBase pocketBase) {
        this.pocketBase = pocketBase;
        this.gson = new Gson();
    }

    @Override
    public Optional<DVUser> findById(String id) {
        try {
            JsonObject record = pocketBase.getRecord(COLLECTION_NAME, id, null, null);
            return Optional.of(mapToUser(record));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error finding user by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DVUser> findByUuid(UUID mc_uuid) {
        try {
            String filter = "mc_uuid = '" + mc_uuid.toString() + "'";
            JsonObject record = pocketBase.getFirstListItem(COLLECTION_NAME, filter, null, null, null);
            return Optional.of(mapToUser(record));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "No user found with UUID: " + mc_uuid);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DVUser> findByDiscordId(String discordId) {
        try {
            String filter = "discord_id = '" + discordId + "'";
            JsonObject record = pocketBase.getFirstListItem(COLLECTION_NAME, filter, null, null, null);
            return Optional.of(mapToUser(record));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "No user found with Discord ID: " + discordId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DVUser> findBySnowflakeId(Long snowflakeId) {
        try {
            // First try with the direct snowflake ID
            String filter = "discord_id = '" + snowflakeId.toString() + "'";
            LOGGER.info("Searching for user with filter: " + filter);

            try {
                JsonObject record = pocketBase.getFirstListItem(COLLECTION_NAME, filter, null, null, null);
                LOGGER.info("User found with snowflake ID: " + snowflakeId);
                DVUser user = mapToUser(record);
                return Optional.of(user);
            } catch (IOException e) {
                // First search failed, try with just string comparison if numeric search fails
                LOGGER.info("No exact match found, trying alternative search...");
                filter = "discord_id ~ '" + snowflakeId + "'";
                try {
                    JsonObject record = pocketBase.getFirstListItem(COLLECTION_NAME, filter, null, null, null);
                    LOGGER.info("User found with partial match: " + record.toString());
                    DVUser user = mapToUser(record);
                    return Optional.of(user);
                } catch (IOException e2) {
                    LOGGER.log(Level.INFO, "No user found with Snowflake ID (partial match): " + snowflakeId);
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error searching for user with Snowflake ID: " + snowflakeId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DVUser> findByMinecraftUsername(String mcUsername) {
        try {
            String filter = "mc_username = '" + mcUsername + "'";
            JsonObject record = pocketBase.getFirstListItem(COLLECTION_NAME, filter, null, null, null);
            return Optional.of(mapToUser(record));
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "No user found with MC username: " + mcUsername);
            return Optional.empty();
        }
    }

    @Override
    public List<DVUser> findAll() {
        try {
            List<JsonObject> records = pocketBase.getFullList(COLLECTION_NAME, 500, null, null, null, null);
            return records.stream()
                    .map(this::mapToUser)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error retrieving all users", e);
            return Collections.emptyList();
        }
    }

    @Override
    public DVUser save(DVUser user) {
        try {
            JsonObject userData = mapToJsonObject(user);

            if (user.getId() != null && !user.getId().isEmpty()) {
                // Update existing user
                JsonObject updatedRecord = pocketBase.updateRecord(COLLECTION_NAME, user.getId(), userData, null, null);
                return mapToUser(updatedRecord);
            } else {
                // Create new user
                JsonObject newRecord = pocketBase.createRecord(COLLECTION_NAME, userData, null, null);
                return mapToUser(newRecord);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving user: " + user.getMinecraftUsername(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    public void delete(DVUser user) {
        if (user.getId() != null) {
            deleteById(user.getId());
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            pocketBase.deleteRecord(COLLECTION_NAME, id);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user with ID: " + id, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    /**
     * Convert a JsonObject from PocketBase to a User object
     *
     * @param json JsonObject from PocketBase API
     * @return Mapped User object
     */
    private DVUser mapToUser(JsonObject json) {
        DVUser user = new DVUser();

        user.setId(getStringOrNull(json, "id"));
        user.setCollectionId(getStringOrNull(json, "collectionId"));
        user.setCollectionName(getStringOrNull(json, "collectionName"));

        user.setDiscordId(getStringOrNull(json, "discord_id"));
        user.setDiscordUsername(getStringOrNull(json, "discord_username"));
        user.setDiscordImg(getStringOrNull(json, "discord_img"));
        user.setMcUsername(getStringOrNull(json, "mc_username"));

        if (json.has("mc_uuid") && !json.get("mc_uuid").isJsonNull()) {
            try {
                user.setMcUuid(UUID.fromString(Formatter.formatUuid(json.get("mc_uuid").getAsString())));
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid UUID format: " + json.get("mc_uuid").getAsString());
            }
        }

        if (user.getDiscordId() != null) {
            try {
                user.setSnowflakeId(Long.parseLong(user.getDiscordId()));
            } catch (NumberFormatException e) {
                LOGGER.warning("Discord ID is not a valid snowflake: " + user.getDiscordId());
            }
        }

        // Parse relation lists
        user.setInfractions(getStringListOrEmpty(json, "infractions"));
        user.setUsers_home(getStringListOrEmpty(json, "users_home"));
        user.setInventory_items(getStringListOrEmpty(json, "inventory_items"));
        user.setClaims(getStringListOrEmpty(json, "claims"));

        // Parse numeric fields
        user.setClaim_limit(getIntOrNull(json, "claim_limit"));
        user.setPlay_time(getIntOrNull(json, "play_time"));
        user.setBalance(getDoubleOrNull(json, "balance"));
        user.setDaily_streak(getIntOrNull(json, "daily_streak"));

        // Parse boolean fields
        user.setIs_suspended(getBooleanOrNull(json, "is_suspended"));
        user.setIs_banned(getBooleanOrNull(json, "is_banned"));

        // Parse datetime fields
        user.setLast_work(getOffsetDateTimeOrNull(json, "last_work"));
        user.setLast_Played(getOffsetDateTimeOrNull(json, "last_played"));
        user.setLast_daily(getOffsetDateTimeOrNull(json, "last_daily"));
        user.setCreated(getOffsetDateTimeOrNull(json, "created"));
        user.setUpdated(getOffsetDateTimeOrNull(json, "updated"));

        return user;
    }

    /**
     * Convert a User object to a JsonObject for PocketBase
     *
     * @param user User object to convert
     * @return JsonObject for PocketBase API
     */
    private JsonObject mapToJsonObject(DVUser user) {
        JsonObject json = new JsonObject();

        // Only include fields that PocketBase expects for updates/creates
        if (user.getDiscordId() != null)
            json.addProperty("discord_id", user.getDiscordId());
        if (user.getDiscordUsername() != null)
            json.addProperty("discord_username", user.getDiscordUsername());
        if (user.getDiscordAvatarUrl() != null)
            json.addProperty("discord_img", user.getDiscordAvatarUrl());
        if (user.getMinecraftUsername() != null)
            json.addProperty("mc_username", user.getMinecraftUsername());
        if (user.getMinecraftUuid() != null)
            json.addProperty("mc_uuid", user.getMinecraftUuid().toString());

        // Add numeric fields
        if (user.getClaim_limit() != null)
            json.addProperty("claim_limit", user.getClaim_limit());
        if (user.getPlay_time() != null)
            json.addProperty("play_time", user.getPlay_time());
        if (user.getBalance() != null)
            json.addProperty("balance", user.getBalance());
        if (user.getDaily_streak() != null)
            json.addProperty("daily_streak", user.getDaily_streak());

        // Add boolean fields
        if (user.getIs_suspended() != null)
            json.addProperty("is_suspended", user.getIs_suspended());
        if (user.getIs_banned() != null)
            json.addProperty("is_banned", user.getIs_banned());

        // Format and add datetime fields
        if (user.getLast_work() != null)
            json.addProperty("last_work", formatDateTime(user.getLast_work()));
        if (user.getLast_daily() != null)
            json.addProperty("last_daily", formatDateTime(user.getLast_daily()));

        // Add relation fields (these need to be handled separately based on
        // PocketBase's expectations)
        // For now, we'll just leave them out as they typically require special handling
        // TODO:Add relation fields
        return json;
    }

    private String getStringOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    private Integer getIntOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsInt() : null;
    }

    private Double getDoubleOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsDouble() : null;
    }

    private Boolean getBooleanOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsBoolean() : null;
    }

    private OffsetDateTime getOffsetDateTimeOrNull(JsonObject json, String key) {
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

    @NotNull
    private List<String> getStringListOrEmpty(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull() && json.get(key).isJsonArray()) {
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            return gson.fromJson(json.get(key), listType);
        }
        return new ArrayList<>();
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public List<DVUser> getAllWhere(String filter) {
        try {
            List<JsonObject> records = pocketBase.getFullList(COLLECTION_NAME, 500, filter, null, null, null);
            return records.stream()
                    .map(this::mapToUser)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error retrieving users with filter: " + filter, e);
            return Collections.emptyList();
        }
    }
}
