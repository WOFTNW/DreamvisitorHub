package org.woftnw.dreamvisitorhub;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.commands.framework.CommandManager;
import org.woftnw.dreamvisitorhub.commands.framework.ExecutableSlashCommand;
import org.woftnw.dreamvisitorhub.commands.CommandActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bot is the entrypoint for all bot-related operations.
 */
public class Bot {

    private static CommandManager commandManager;
    private static Guild guild;
    private static JDA bot;

    public static void startBot(@NotNull Map<String, Object> config) throws InterruptedException {
        String token;
        try {
            token = (String) config.get("botToken");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        bot = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT)
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

        try {
            guild = bot.getGuildById((String) config.get("guildId"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<ExecutableSlashCommand> commands = new ArrayList<>();
        commands.add(new CommandActivity());

        commandManager = new CommandManager(guild, commands);
        bot.addEventListener(commandManager);

    }

    /**
     * Get the configured guild.
     * @return the {@link Guild}
     */
    public static Guild getGuild() {
        return guild;
    }

    /**
     * Get the JDA instance.
     * @return the {@link JDA} instance.
     */
    public static JDA getJda() {
        if (bot == null) throw new NullPointerException("Bot has not been initialized.");
        return bot;
    }
}
