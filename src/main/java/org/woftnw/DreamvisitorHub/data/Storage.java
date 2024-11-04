package org.woftnw.DreamvisitorHub.data;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class Storage {

    private static Map<String, Object> getYamlFileMap(String fileName) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(fileName);
        return yaml.load(inputStream);
    }
    private static void saveYamlFileMap(String fileName, Map<String, Object> data) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        PrintWriter writer = new PrintWriter(fileName);
        yaml.dump(data, writer);
        writer.close();
    }

    public static class Config {

        private static final String fileName = "config.yml";

        private static String botToken = null;

        private static Map<String, Object> getConfigMap() throws FileNotFoundException {
            return getYamlFileMap(fileName);
        }
        private static void saveConfig(Map<String, Object> data) throws FileNotFoundException {
            saveYamlFileMap(fileName, data);
        }

        /**
         * Get the Discord bot token.
         * @return the Discord bot token.
         * @throws FileNotFoundException if the file does not exist.
         * @throws NullPointerException if the value does not exist.
         */
        public static @NotNull String getBotToken() throws FileNotFoundException, NullPointerException {
            if (botToken == null) {
                botToken = (String) getConfigMap().get("botToken");
                if (botToken == null) throw new NullPointerException("botToken is not defined in " + fileName + "!");
            }
            return botToken;
        }

        /**
         * Set the Discord bot token
         * @param botToken the token to set.
         * @throws FileNotFoundException if the file does not exist.
         */
        public static void setBotToken(String botToken) throws FileNotFoundException {
            Map<String, Object> configMap = getConfigMap();
            configMap.put("botToken", botToken);
            saveConfig(configMap);
        }
    }
}
