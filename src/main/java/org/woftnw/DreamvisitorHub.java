package org.woftnw;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DreamvisitorHub {

    public static final Logger logger = LoggerFactory.getLogger(DreamvisitorHub.class);
    public static LocalConfig localConfig;

    public static void main(String[] args) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        File config = new File("config.json");
        if (!Files.exists(config.toPath())) {
            logger.error("config.json does not exist.");
            return;
        }
        try {
            localConfig = mapper.readValue(config, new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Bot.startBot(localConfig.botToken());

    }

    public static Logger getLogger() {
        return logger;
    }
}