package org.woftnw.DreamvisitorHub.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.Timestamp;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.util.ConfigLoader;
import org.woftnw.DreamvisitorHub.util.PBConfigLoader;
import org.woftnw.DreamvisitorHub.pb.PocketBase;

import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Bot {

  public static final String[] TRIBE_NAMES = { "HiveWing", "IceWing", "LeafWing", "MudWing", "NightWing", "RainWing",
      "SandWing", "SeaWing", "SilkWing", "SkyWing" };
  private static TextChannel gameChatChannel;
  private static TextChannel gameLogChannel;
  private static TextChannel whitelistChannel;
  public static final List<Role> tribeRole = new ArrayList<>();
  static JDA jda;
  private static final Logger logger = Logger.getLogger("DreamvisitorHub");

  public static boolean botFailed;
  private static Map<String, Object> config;

  private Bot() {
    throw new IllegalStateException("Utility class.");
  }

  public static void startBot(@NotNull Map<String, Object> conf) {
    config = conf;
    // Build JDA
    String token = (String) config.get("bot-token");
    // Try to create a bot
    System.out.println("Attempting to create a bot...");
    try {
      jda = JDABuilder
          .createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
          .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
          .build();
      System.out.println("Bot created.");
      botFailed = false;

    } catch (InvalidTokenException e) {

      logger.severe(
          "BOT LOGIN FAILED: You need a valid bot token in dreamvisitor/config.yml. Dreamvisitor will not work properly unless there is a valid bot token.");
      botFailed = true;

    } catch (ErrorResponseException exception) {

      if (exception.getErrorCode() == -1) {
        logger.severe(
            "BOT LOGIN FAILED: Dreamvisitor is unable to connect to the Discord server. Dreamvisitor functionality will not work properly.");
        botFailed = true;
      }

    }

    if (!botFailed) {
      jda.addEventListener(new DiscCommandsManager());

      // Wait for bot ready
      try {
        jda.awaitReady();
        System.out.println("Bot is ready.");

        long chatChannelID = getChannelIdFromConfig("chatChannelID");
        long logChannelID = getChannelIdFromConfig("logChannelID");
        long whitelistChannelID = getChannelIdFromConfig("whitelistChannelID");

        System.out.println(String.valueOf(chatChannelID));
        System.out.println(String.valueOf(logChannelID));
        System.out.println(String.valueOf(whitelistChannelID));

        Bot.gameChatChannel = jda.getTextChannelById(chatChannelID);
        Bot.gameLogChannel = jda.getTextChannelById(logChannelID);
        Bot.whitelistChannel = jda.getTextChannelById(whitelistChannelID);

        if (Bot.gameChatChannel == null)
          logger.warning("The game chat channel with ID " + chatChannelID + " does not exist!");
        if (Bot.gameLogChannel == null)
          logger.warning("The game log channel with ID " + logChannelID + " does not exist!");
        if (Bot.whitelistChannel == null)
          logger.warning("The whitelist channel with ID " + whitelistChannelID + " does not exist!");

        // Handle tribe roles
        List<Number> tribeRoleIds = (List<Number>) config.getOrDefault("tribeRoles", new ArrayList<>());
        for (int i = 0; i < Math.min(10, tribeRoleIds.size()); i++) {
          Bot.tribeRole.add(jda.getRoleById(tribeRoleIds.get(i).longValue()));
        }

        // Initialize commands after JDA is ready
        DiscCommandsManager.init();

      } catch (InterruptedException exception) {
        throw new RuntimeException();
      }
    }
  }

  public static TextChannel getGameChatChannel() {
    if (gameChatChannel == null) {
      long channelId = getChannelIdFromConfig("chatChannelID");
      if (channelId != 0L) {
        gameChatChannel = jda.getTextChannelById(channelId);
      }
    }
    return gameChatChannel;
  }

  public static void setGameChatChannel(TextChannel channel) {
    gameChatChannel = channel;
    config.put("chatChannelID", String.valueOf(gameChatChannel.getIdLong()));
    try {
      saveConfig();
      // After saving config, reload channels to ensure consistency
      reloadChannels();
    } catch (FileNotFoundException e) {
      System.out.println("Error saving config: " + e.getMessage());
    }
  }

  public static TextChannel getWhitelistChannel() {
    if (whitelistChannel == null) {
      long channelId = getChannelIdFromConfig("whitelistChannelID");
      if (channelId != 0L) {
        whitelistChannel = jda.getTextChannelById(channelId);
      }
    }
    return whitelistChannel;
  }

  public static void setWhitelistChannel(TextChannel channel) {
    whitelistChannel = channel;
    config.put("whitelistChannelID", String.valueOf(whitelistChannel.getIdLong()));
    try {
      saveConfig();
      // After saving config, reload channels to ensure consistency
      reloadChannels();
    } catch (FileNotFoundException e) {
      System.out.println("Error saving config: " + e.getMessage());
    }
  }

  public static TextChannel getGameLogChannel() {
    if (gameLogChannel == null) {
      long channelId = getChannelIdFromConfig("logChannelID");
      if (channelId != 0L) {
        gameLogChannel = jda.getTextChannelById(channelId);
      }
    }
    return gameLogChannel;
  }

  public static void setGameLogChannel(TextChannel channel) {
    gameLogChannel = channel;
    config.put("logChannelID", String.valueOf(gameLogChannel.getIdLong()));
    try {
      saveConfig();
      // After saving config, reload channels to ensure consistency
      reloadChannels();
    } catch (FileNotFoundException e) {
      System.out.println("Error saving config: " + e.getMessage());
    }
  }

  public static JDA getJda() {
    return jda;
  }

  public static void sendLog(@NotNull String message) {
    try {
      if (!botFailed)
        Bot.getGameLogChannel().sendMessage(message).queue();
      else
        gameLogChannel.sendMessage(message).queue();
    } catch (InsufficientPermissionException e) {
      logger.warning("Dreamvisitor bot does not have sufficient permissions to send messages in game log channel!");
    } catch (IllegalArgumentException e) {
      if (Boolean.TRUE.equals(config.getOrDefault("debug", false)))
        System.out.println("Attempted to send an invalid message.");
    }
  }

  /**
   * Escapes all Discord markdown elements in a {@link String}.
   *
   * @param string the {@link String} to format.
   * @return the formatted {@link String}.
   */
  public static @NotNull String escapeMarkdownFormatting(@NotNull String string) {
    return string.isEmpty() ? string
        : string.replaceAll("_", "\\\\_").replaceAll("\\*", "\\\\*").replaceAll("\\|", "\\\\|");
  }

  @NotNull
  @Contract("_, _ -> fail")
  public static Timestamp createTimestamp(@NotNull LocalDateTime dateTime, @NotNull TimeFormat format) {
    return format.atInstant(dateTime.toInstant(OffsetDateTime.now().getOffset()));
  }

  /**
   * Helper method to safely get a channel ID from config regardless of its type
   *
   * @param configKey The configuration key
   * @return The channel ID as a long, or 0 if not found or invalid
   */
  private static long getChannelIdFromConfig(String configKey) {
    Object value = config.getOrDefault(configKey, "0");

    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else if (value instanceof String) {
      try {
        return Long.parseLong((String) value);
      } catch (NumberFormatException e) {
        logger.warning("Invalid channel ID format for " + configKey + ": " + value);
        return 0L;
      }
    }

    return 0L;
  }

  /**
   * Reloads all channel references from the configuration
   */
  public static void reloadChannels() {
    if (jda == null || botFailed) {
      return;
    }

    try {
      // Load channels from config using the safe helper method
      long chatChannelID = getChannelIdFromConfig("chatChannelID");
      long logChannelID = getChannelIdFromConfig("logChannelID");
      long whitelistChannelID = getChannelIdFromConfig("whitelistChannelID");

      // Update channel references
      if (chatChannelID != 0L) {
        gameChatChannel = jda.getTextChannelById(chatChannelID);
      }

      if (logChannelID != 0L) {
        gameLogChannel = jda.getTextChannelById(logChannelID);
      }

      if (whitelistChannelID != 0L) {
        whitelistChannel = jda.getTextChannelById(whitelistChannelID);
      }

      logger.info("Reloaded channel references from configuration");
    } catch (Exception e) {
      logger.warning("Failed to reload channels: " + e.getMessage());
      e.printStackTrace(); // Print stack trace for debugging
    }
  }

  /**
   * Saves the configuration to PocketBase and falls back to file if needed
   */
  private static void saveConfig() throws FileNotFoundException {
    try {
      // Try to update config in PocketBase
      JsonObject configData = new JsonObject();

      // Convert config map to JsonObject with appropriate field names
      for (Map.Entry<String, Object> entry : config.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        // Skip null values
        if (value == null) {
          continue;
        }

        // Map field names back to PocketBase format
        switch (key) {
          case "whitelistChannelID":
            key = "whitelist_channel";
            break;
          case "chatChannelID":
            key = "game_chat_channel";
            break;
          case "logChannelID":
            key = "game_log_channel";
            break;
          case "resourcePackRepo":
            key = "resource_pack_repo";
            break;
          // Add other cases as needed
        }

        if (value instanceof String) {
          configData.addProperty(key, (String) value);
        } else if (value instanceof Number) {
          configData.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
          configData.addProperty(key, (Boolean) value);
        }
      }

      // Update the record in PocketBase
      App.getPb().updateRecord("dreamvisitor_config", "45q1at367581q3a", configData, null, null);
      logger.info("Configuration saved to PocketBase");
    } catch (Exception e) {
      logger.warning("Failed to save configuration to PocketBase: " + e.getMessage());
      logger.warning("Falling back to file-based configuration storage");

      // Fall back to saving to file
      if (!ConfigLoader.saveConfig("config.yml", config)) {
        throw new FileNotFoundException("Failed to save config to both PocketBase and file");
      }
    }
  }
}
