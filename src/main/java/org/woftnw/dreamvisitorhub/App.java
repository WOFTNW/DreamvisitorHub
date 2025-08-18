package org.woftnw.dreamvisitorhub;

import org.woftnw.dreamvisitorhub.config.Config;
import org.woftnw.dreamvisitorhub.data.repository.*;
import org.woftnw.dreamvisitorhub.pb.PocketBase;
import org.woftnw.dreamvisitorhub.util.ConfigLoader;
import org.woftnw.dreamvisitorhub.util.PBConfigLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class App {
    private static final Logger logger = Logger.getLogger("DreamvisitorHub");
    private static final String CONFIG_PATH = "config.yml";

    private static PocketBase pb;
    private static Map<String, Object> config;
    private static UserRepository userRepository;
    private static ItemRepository itemRepository;
    private static UserInventoryRepository userInventoryRepository;
    private static InfractionRepository infractionRepository;
    private static AltRepository altRepository;
    private static ChatMessagesRepository chatMessagesRepository;

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info("Starting DreamvisitorHub...");

        // First load minimal config to get PocketBase connection info
        Map<String, Object> initialConfig = ConfigLoader.loadConfig(CONFIG_PATH);

        // Initialize PocketBase with the minimal configuration
        pb = PocketBase.fromConfig(initialConfig);

        // Initialize repositories
        userRepository = new PocketBaseUserRepository(pb);
        itemRepository = new PocketBaseItemRepository(pb);
        userInventoryRepository = new PocketBaseUserInventoryRepository(pb, userRepository, itemRepository);
        infractionRepository = new PocketBaseInfractionRepository(pb, userRepository);
        chatMessagesRepository = new PocketBaseChatMessageRepository(pb);

        try {
            // Try to load configuration from PocketBase
            Map<String, Object> pbConfig = PBConfigLoader.loadConfig(pb);
            logger.info("Using configuration from PocketBase");

            // If PocketBase config is empty, fall back to file config
            if (pbConfig.isEmpty()) {
                logger.warning("PocketBase configuration is empty, falling back to file configuration");
                config = initialConfig;
            } else {
                // Merge configurations: start with file config and override with PocketBase
                // values
                // This way, we keep file config values for fields that are null in PocketBase
                config = new HashMap<>(initialConfig);
                config.putAll(pbConfig);
                logger.info("Merged PocketBase configuration with file configuration");
            }
        } catch (Exception e) {
            // If loading from PocketBase fails, fall back to the file config
            logger.warning("Failed to load configuration from PocketBase: " + e.getMessage());
            logger.warning("Falling back to file-based configuration");
            config = initialConfig;
        }

        Config.init(pb, (String) config.get("pocketbaseConfigId"));

        // Start the bot with the configuration
        Bot.startBot(config);
    }

    public static String getConfigPath() {
        return CONFIG_PATH;
    }

    public static PocketBase getPb() {
        return pb;
    }

    public static Map<String, Object> getConfig() {
        return config;
    }

    public static UserRepository getUserRepository() {
        return userRepository;
    }

    public static ItemRepository getItemRepository() {
        return itemRepository;
    }

    public static UserInventoryRepository getUserInventoryRepository() {
        return userInventoryRepository;
    }

    public static InfractionRepository getInfractionRepository() {
        return infractionRepository;
    }

    public static AltRepository getAltRepository() {
        if (altRepository == null) {
            altRepository = new PocketBaseAltRepository(pb, getUserRepository());
        }
        return altRepository;
    }

    public static ChatMessagesRepository getChatMessageRepository() {
        return chatMessagesRepository;
    }
}
