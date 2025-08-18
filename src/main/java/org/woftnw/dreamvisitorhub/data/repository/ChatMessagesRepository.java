package org.woftnw.dreamvisitorhub.data.repository;

import org.woftnw.dreamvisitorhub.data.type.ChatMessage;
import org.woftnw.dreamvisitorhub.data.type.DVUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User data operations
 */
public interface ChatMessagesRepository {
    /**
     * Find a chat message by its PocketBase ID
     *
     * @param id PocketBase record ID
     * @return Optional containing the chat message if found
     */
    Optional<ChatMessage> findById(String id);

    /**
     * Get all chat messages
     *
     * @return List of all chat messages
     */
    List<ChatMessage> findAll();

    /**
     * Save a chat message (create or update)
     *
     * @param chatMessage Chat message to save
     * @return Saved chatMessage
     */
    ChatMessage save(ChatMessage chatMessage);

    /**
     * Delete a chat message
     *
     * @param chatMessage Chat message to delete
     */
    void delete(ChatMessage chatMessage);

    /**
     * Delete a chat message by ID
     *
     * @param id PocketBase ID of chat message to delete
     */
    void deleteById(String id);

    /**
     * Get all chat messages that match a given condition
     *
     * @param filter Condition to filter chat messages
     * @return List of chat messages matching the condition
     */
    List<ChatMessage> getAllWhere(String filter);
}
