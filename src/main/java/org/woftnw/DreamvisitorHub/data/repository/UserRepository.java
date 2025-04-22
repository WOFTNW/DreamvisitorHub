package org.woftnw.DreamvisitorHub.data.repository;

import org.woftnw.DreamvisitorHub.data.type.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User data operations
 */
public interface UserRepository {
  /**
   * Find a user by their PocketBase ID
   *
   * @param id PocketBase record ID
   * @return Optional containing the user if found
   */
  Optional<User> findById(String id);

  /**
   * Find a user by their Minecraft UUID
   *
   * @param uuid Minecraft UUID
   * @return Optional containing the user if found
   */
  Optional<User> findByUuid(UUID uuid);

  /**
   * Find a user by their Discord ID
   *
   * @param discordId Discord ID
   * @return Optional containing the user if found
   */
  Optional<User> findByDiscordId(String discordId);

  /**
   * Find a user by their Discord Snowflake ID
   *
   * @param snowflakeId Discord Snowflake ID
   * @return Optional containing the user if found
   */
  Optional<User> findBySnowflakeId(Long snowflakeId);

  /**
   * Find a user by their Minecraft username
   *
   * @param mcUsername Minecraft username
   * @return Optional containing the user if found
   */
  Optional<User> findByMcUsername(String mcUsername);

  /**
   * Get all users
   *
   * @return List of all users
   */
  List<User> findAll();

  /**
   * Save a user (create or update)
   *
   * @param user User to save
   * @return Saved user
   */
  User save(User user);

  /**
   * Delete a user
   *
   * @param user User to delete
   */
  void delete(User user);

  /**
   * Delete a user by ID
   *
   * @param id PocketBase ID of user to delete
   */
  void deleteById(String id);
}
