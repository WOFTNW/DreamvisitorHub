package org.woftnw.dreamvisitorhub.data.repository;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.data.type.ChatMessage;
import org.woftnw.dreamvisitorhub.pb.PocketBase;
import org.woftnw.dreamvisitorhub.pb.PocketBaseUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * PocketBase implementation of the ChatMessagesRepository interface
 */
public record PocketBaseChatMessageRepository(PocketBase pocketBase) implements ChatMessagesRepository {
    private static final Logger LOGGER = Logger.getLogger(PocketBaseChatMessageRepository.class.getName());
    private static final String COLLECTION_NAME = "chat_messages";

    /**
     * Constructor for PocketBaseChatMessageRepository
     *
     * @param pocketBase The PocketBase client to use
     */
    public PocketBaseChatMessageRepository {
    }

    @Override
    public Optional<ChatMessage> findById(String id) {
        try {
            JsonObject record = pocketBase.getRecord(COLLECTION_NAME, id, null, null);
            return Optional.of(mapToChatMessage(record));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error finding user by ID: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<ChatMessage> findAll() {
        try {
            List<JsonObject> records = pocketBase.getFullList(COLLECTION_NAME, 500, null, null, null, null);
            return records.stream()
                    .map(this::mapToChatMessage)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error retrieving all users", e);
            return Collections.emptyList();
        }
    }

    @NotNull
    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        try {
            JsonObject chatMessageData = mapToJsonObject(chatMessage);

            if (chatMessage.getId() != null && !chatMessage.getId().isEmpty()) {
                // Update existing chatMessage
                JsonObject updatedRecord = pocketBase.updateRecord(COLLECTION_NAME, chatMessage.getId(), chatMessageData, null, null);
                return mapToChatMessage(updatedRecord);
            } else {
                // Create new chatMessage
                JsonObject newRecord = pocketBase.createRecord(COLLECTION_NAME, chatMessageData, null, null);
                return mapToChatMessage(newRecord);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving chatMessage: " + chatMessage.getMessage(), e);
            throw new RuntimeException("Failed to save chatMessage", e);
        }
    }

    @Override
    public void delete(@NotNull ChatMessage chatMessage) {
        if (chatMessage.getId() != null) {
            deleteById(chatMessage.getId());
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            pocketBase.deleteRecord(COLLECTION_NAME, id);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting chat message with ID: " + id, e);
            throw new RuntimeException("Failed to delete chat message", e);
        }
    }

    /**
     * Convert a JsonObject from PocketBase to a User object
     *
     * @param json JsonObject from PocketBase API
     * @return Mapped User object
     */
    @NotNull
    private ChatMessage mapToChatMessage(JsonObject json) {
        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setId(PocketBaseUtils.getString(json, "id"));
        chatMessage.setCollectionId(PocketBaseUtils.getString(json, "collectionId"));
        chatMessage.setCollectionName(PocketBaseUtils.getString(json, "collectionName"));

        chatMessage.setMessageId(PocketBaseUtils.getString(json, "message_id"));
        chatMessage.setSenderUsername(PocketBaseUtils.getString(json, "sender_username"));
        chatMessage.setSenderDisplayname(PocketBaseUtils.getString(json, "sender_displayname"));
        chatMessage.setMessage(PocketBaseUtils.getString(json, "message"));
        chatMessage.setAction(PocketBaseUtils.getBoolean(json, "action"));
        chatMessage.setSource(ChatMessage.Source.valueOf(PocketBaseUtils.getString(json, "source").toUpperCase()));

        // Parse datetime fields
        chatMessage.setCreated(PocketBaseUtils.getOffsetDateTime(json, "created"));
        chatMessage.setUpdated(PocketBaseUtils.getOffsetDateTime(json, "updated"));

        return chatMessage;
    }

    /**
     * Convert a User object to a JsonObject for PocketBase
     *
     * @param user User object to convert
     * @return JsonObject for PocketBase API
     */
    @NotNull
    private JsonObject mapToJsonObject(@NotNull ChatMessage user) {
        JsonObject json = new JsonObject();

        // Only include fields that PocketBase expects for updates/creates
        if (user.getMessageId() != null)
            json.addProperty("message_id", user.getMessageId());
        if (user.getSenderUsername() != null)
            json.addProperty("sender_username", user.getSenderUsername());
        if (user.getSenderDisplayname()  != null)
            json.addProperty("sender_displayname", user.getSenderDisplayname());
        if (user.getMessage() != null)
            json.addProperty("message", user.getMessage());
        if (user.getMessage() != null)
            json.addProperty("message", user.getMessage());
        if (user.getAction() != null)
            json.addProperty("action", user.getAction());
        if (user.getSource() != null)
            json.addProperty("source", user.getSource().toString().toLowerCase());

        return json;
    }

    @Override
    public List<ChatMessage> getAllWhere(String filter) {
        try {
            List<JsonObject> records = pocketBase.getFullList(COLLECTION_NAME, 500, filter, null, null, null);
            return records.stream()
                    .map(this::mapToChatMessage)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error retrieving users with filter: " + filter, e);
            return Collections.emptyList();
        }
    }
}
