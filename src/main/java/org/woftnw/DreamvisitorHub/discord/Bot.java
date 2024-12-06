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
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.data.storage.StorageManager;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bot {

    public static final String[] TRIBE_NAMES = {"HiveWing", "IceWing", "LeafWing", "MudWing", "NightWing", "RainWing", "SandWing", "SeaWing", "SilkWing", "SkyWing"};
    private static TextChannel gameChatChannel;
    private static TextChannel gameLogChannel;
    private static TextChannel whitelistChannel;
    public static final List<Role> tribeRole = new ArrayList<>();
    static JDA jda;

    public static boolean botFailed;

    private Bot() {
        throw new IllegalStateException("Utility class.");
    }

    public static void startBot(@NotNull FileConfiguration config) {

        // Build JDA
        String token = StorageManager.getBotToken();
        // Try to create a bot
        System.out.println("Attempting to create a bot...");
        try {
            jda = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                    .build();
            System.out.println("Bot created.");
            botFailed = false;

        } catch (InvalidTokenException e) {

            Bukkit.getLogger().severe(
                    "BOT LOGIN FAILED: You need a valid bot token in dreamvisitor/config.yml. Dreamvisitor will not work properly unless there is a valid bot token.");
            botFailed = true;

        } catch (ErrorResponseException exception) {

            if (exception.getErrorCode() == -1) {
                Bukkit.getLogger().severe("BOT LOGIN FAILED: Dreamvisitor is unable to connect to the Discord server. Dreamvisitor functionality will not work properly.");
                botFailed = true;
            }

        }

        if (!botFailed) {
            jda.addEventListener(new DiscCommandsManager());
            jda.addEventListener(new DiscEventListener());

            // Wait for bot ready
            try {
                jda.awaitReady();
                System.out.println("Bot is ready.");

                long chatChannelID = config.getLong("chatChannelID");
                long logChannelID = config.getLong("logChannelID");
                long whitelistChannelID = config.getLong("whitelistChannelID");

                System.out.println(String.valueOf(chatChannelID));
                System.out.println(String.valueOf(logChannelID));
                System.out.println(String.valueOf(whitelistChannelID));

                Bot.gameChatChannel = jda.getTextChannelById(chatChannelID);
                Bot.gameLogChannel = jda.getTextChannelById(logChannelID);
                Bot.whitelistChannel = jda.getTextChannelById(whitelistChannelID);

                if (Bot.gameChatChannel == null) Bukkit.getLogger().warning("The game chat channel with ID " + chatChannelID + " does not exist!");
                if (Bot.gameLogChannel == null) Bukkit.getLogger().warning("The game log channel with ID " + logChannelID + " does not exist!");
                if (Bot.whitelistChannel == null) Bukkit.getLogger().warning("The whitelist channel with ID " + whitelistChannelID + " does not exist!");

                for (int i = 0; i < 10; i++) Bot.tribeRole.add(jda.getRoleById(config.getLongList("tribeRoles").get(i)));

            } catch (InterruptedException exception) {
                throw new RuntimeException();
            }
        }
    }

    public static TextChannel getGameChatChannel() {
        if (gameChatChannel == null) {
            Long channelId = StorageManager.getGameChatChannel();
            if (channelId != null) gameChatChannel = jda.getTextChannelById(channelId);
        }
        return gameChatChannel;
    }

    public static void setGameChatChannel(TextChannel channel) {
        gameChatChannel = channel;
        StorageManager.setGameChatChannel(gameChatChannel.getIdLong());
        try {
            StorageManager.saveFile();
        } catch (FileNotFoundException e) {
            System.out.println("Error saving config: " + e.getMessage());
        }
    }

    public static TextChannel getWhitelistChannel() {
        if (whitelistChannel == null) {
            Long channelId = StorageManager.getWhitelistChannel();
            if (channelId != null) whitelistChannel = jda.getTextChannelById(channelId);
        }
        return whitelistChannel;
    }

    public static void setWhitelistChannel(TextChannel channel) {
        whitelistChannel = channel;
        StorageManager.setWhitelistChannel(whitelistChannel.getIdLong());
        try {
            StorageManager.saveFile();
        } catch (FileNotFoundException e) {
            System.out.println("Error saving config: " + e.getMessage());
        }
    }

    public static TextChannel getGameLogChannel() {
        if (gameLogChannel == null) {
            Long channelId = StorageManager.getGameLogChannel();
            if (channelId != null) gameLogChannel = jda.getTextChannelById(channelId);
        }
        return gameLogChannel;
    }

    public static void setGameLogChannel(TextChannel channel) {
        gameLogChannel = channel;
        StorageManager.setGameLogChannel(gameLogChannel.getIdLong());
        try {
            StorageManager.saveFile();
        } catch (FileNotFoundException e) {
            System.out.println("Error saving config: " + e.getMessage());
        }
    }

    public static JDA getJda() {
        return jda;
    }

    public static void sendLog(@NotNull String message) {
        try {
            if (!botFailed) Bot.getGameLogChannel().sendMessage(message).queue();
            else gameLogChannel.sendMessage(message).queue();
        } catch (InsufficientPermissionException e) {
            Bukkit.getLogger().warning("Dreamvisitor bot does not have sufficient permissions to send messages in game log channel!");
        } catch (IllegalArgumentException e) {
            if (StorageManager.isDebug()) System.out.println("Attempted to send an invalid message.");
        }
    }


    /**
     * Escapes all Discord markdown elements in a {@link String}.
     * @param string the {@link String} to format.
     * @return the formatted {@link String}.
     */
    public static @NotNull String escapeMarkdownFormatting(@NotNull String string) {
        return string.isEmpty() ? string : string.replaceAll("_","\\\\_").replaceAll("\\*","\\\\*").replaceAll("\\|","\\\\|");
    }

    @NotNull
    @Contract("_, _ -> fail")
    public static Timestamp createTimestamp(@NotNull LocalDateTime dateTime, @NotNull TimeFormat format) {
        return format.atInstant(dateTime.toInstant(OffsetDateTime.now().getOffset()));
    }
}