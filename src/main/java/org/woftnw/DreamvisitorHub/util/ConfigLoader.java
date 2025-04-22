package org.woftnw.DreamvisitorHub.util;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility class for loading YAML configuration files
 */
public class ConfigLoader {
  private static final Logger logger = Logger.getLogger("DreamvisitorHub");

  private ConfigLoader() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Loads a YAML configuration file and returns it as a Map
   *
   * @param filePath path to the YAML file
   * @return Map containing the configuration, or empty map if loading fails
   */
  public static Map<String, Object> loadConfig(String filePath) {
    try {
      File configFile = new File(filePath);
      if (!configFile.exists()) {
        logger.warning("Config file not found at: " + filePath);
        return new HashMap<>();
      }

      Yaml yaml = new Yaml();
      InputStream inputStream = new FileInputStream(configFile);
      Map<String, Object> config = yaml.load(inputStream);
      logger.info("Successfully loaded config from: " + filePath);
      return config != null ? config : new HashMap<>();
    } catch (FileNotFoundException e) {
      logger.severe("Failed to load config file: " + e.getMessage());
      return new HashMap<>();
    } catch (Exception e) {
      logger.severe("Error parsing config file: " + e.getMessage());
      return new HashMap<>();
    }
  }
}
