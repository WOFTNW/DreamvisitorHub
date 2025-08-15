package org.woftnw.dreamvisitorhub;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Bot {

    public static void startBot(@NotNull Map<String, Object> config) throws InterruptedException {
        String token;
        try {
            token = (String) config.get("botToken");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JDA bot = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .disableCache(
                        CacheFlag.VOICE_STATE,
                        CacheFlag.STICKER,
                        CacheFlag.SCHEDULED_EVENTS,
                        CacheFlag.ROLE_TAGS,
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.MEMBER_OVERRIDES,
                        CacheFlag.FORUM_TAGS,
                        CacheFlag.EMOJI
                ).build();

        bot.awaitReady();

    }

}
