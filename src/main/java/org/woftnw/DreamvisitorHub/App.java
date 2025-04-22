package org.woftnw.DreamvisitorHub;

// import org.woftnw.DreamvisitorHub.data.storage.StorageManager;
import org.woftnw.DreamvisitorHub.discord.Bot;
import org.woftnw.DreamvisitorHub.pb.PocketBase;
import org.woftnw.DreamvisitorHub.util.ConfigLoader;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class App {
  private static final Logger logger = Logger.getLogger("DreamvisitorHub");
  private static final String CONFIG_PATH = "config.yml";

  public static void main(String[] args) throws IOException {
    logger.info("Starting DreamvisitorHub...");

    // Load configuration from YAML file
    Map<String, Object> config = ConfigLoader.loadConfig(CONFIG_PATH);
    // StorageManager.loadFromFile("config");
    // Start the bot with the configuration
    Bot.startBot(config);
    PocketBase pb = PocketBase.fromConfig(config);
    List<JsonObject> res = pb.getFullList("users", 1, null, null, "claims", null);
    for (JsonObject jsonObject : res) {
      System.out.println(jsonObject.toString());
    }
  }
}
