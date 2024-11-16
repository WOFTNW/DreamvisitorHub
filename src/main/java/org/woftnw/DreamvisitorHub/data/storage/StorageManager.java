package org.woftnw.DreamvisitorHub.data.storage;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class StorageManager {

//    private static final List<UnifiedStorage> unifiedStorages = Arrays.asList(
//            new UnifiedStorage("config",
//                    new ConfigValue<>(
//                            "debug",
//                            "Debug Mode",
//                            "Whether to enable debug messages. This will send additional messages to help debug Dreamvisitor. Disabled by default.",
//                            false
//                    ),
//                    new ConfigValue<>(
//                            "pauseChat",
//                            "Pause Chat",
//                            "Whether chat is paused or not. This can be toggled in Minecraft with /pausechat. Disabled by default.",
//                            false
//                    ),
//                    new ConfigValue<>(
//                            "softWhitelist",
//                            "Soft Whitelist",
//                            "Whether the soft whitelist is enabled or not. This can be set in Minecraft with /softwhitelist [on|off]. Disabled by default.",
//                            false
//                    ),
//                    new ConfigValue<>(
//                            "disablePvp",
//                            "Disable PvP",
//                            "Whether to globally disable pvp or not. This can be toggled in Minecraft with /togglepvp.",
//                            false
//                    ),
//                    new ConfigValue<>(
//                            "playerLimit",
//                            "Player Limit Override",
//                            "Player limit override. This will override the player limit, both over and under. This can be set in Minecraft with /playerlimit <int>",
//                            -1
//                    ),
//                    new ConfigValue<>(
//                            "resourcePackRepo",
//                            "Resource Pack Repository",
//                            "The repository path of the server resource pack. Dreamvisitor will pull the first artifact from the latest release on pack update.",
//                            "WOFTNW/Dragonspeak"
//                    ),
//                    new ConfigValue<Location>(
//                            "hubLocation",
//                            "Hub Location",
//                            "The location of the hub.",
//                            null
//                    ),
//                    new ConfigValue<String>(
//                            "botToken",
//                            "Bot Token",
//                            "The Dreamvisitor bot token. DO NOT SHARE THIS. Dreamvisitor will not work properly unless this is a valid bot token. Ask Bog for a bot token if Dreamvisitor reports a login error on startup.",
//                            null
//                    ),
//                    new ConfigValue<Long>(
//                            "whitelistChannel",
//                            "Whitelist Channel",
//                            "The channel of the whitelist chat. This can be set on Discord with /setwhitelist.",
//                            null
//                    ),
//                    new ConfigValue<Long>(
//                            "gameChatChannel",
//                            "Game Chat Channel",
//                            "The channel of the game chat. This can be set on Discord with /setgamechat.",
//                            null
//                    ),
//                    new ConfigValue<Long>(
//                            "logChannel",
//                            "Log Channel",
//                            "The channel of the log chat. This can be set on Discord with /setlogchat.",
//                            null
//                    ),
//                    new ConfigValue<>(
//                            "enableLogConsoleCommands",
//                            "Enable Log Console Commands",
//                            "Whether to pass messages in the log channel as console commands. If log-console is enabled, this will take messages sent by users with the Discord administrator permission and pass them as console commands.",
//                            true
//                    ),
//                    new ConfigValue<>(
//                            "webWhitelistEnabled",
//                            "Web Whitelist",
//                            "Whether web whitelisting is enabled or not. This can be set with the /toggleweb Discord command. Enabled by default.",
//                            true
//                    ),
//                    new ConfigValue<>(
//                            "websiteUrl",
//                            "Website URL",
//                            "The URL for the whitelisting website. Used to restrict requests not from the specified website to prevent abuse.",
//                            "https://woftnw.org"
//                    ),
//                    new ConfigValue<>(
//                            "infractionExpireTimeDays",
//                            "Infraction Expire Time",
//                            "The amount of time in days (as an integer) that infractions take to expire. Expired infractions are not deleted, but they do not count toward a total infraction count.",
//                            90
//                    ),
//                    new ConfigValue<Long>(
//                            "infractionsCategory",
//                            "Infractions Category",
//                            "The ID of the category to create infractions channels. They will accumulate here.",
//                            null
//                    ),
//                    new ConfigValue<>(
//                            "shopName",
//                            "Shop Name",
//                            "The name of the Discord shop. This will appear at the top of the embed.",
//                            "Shop"
//                    ),
//                    new ConfigValue<>(
//                            "currencyIcon",
//                            "Currency Icon",
//                            "The icon used for currency in the Discord economy system. This can be any string, including symbols, letters, emojis, and Discord custom emoji.",
//                            "$"
//                    ),
//                    new ConfigValue<>(
//                            "dailyBaseAmount",
//                            "Daily Base Amount",
//                            "The base amount given by the /daily Discord command. This is the default amount before adding the streak bonus. The total amount is decided by dailyBaseAmount + (user streak * this).",
//                            10.00
//                    ),
//                    new ConfigValue<>(
//                            "dailyStreakMultiplier",
//                            "Daily Streak Multiplier",
//                            "The multiplier of the streak bonus given by the /daily command. This is multiplied by the streak and added to the base amount. The total amount is decided by dailyBaseAmount + (users streak * this).",
//                            5.00
//                    ),
//                    new ConfigValue<>(
//                            "workReward",
//                            "Work Reward",
//                            "The amount gained from the /work command. /work can only be run every hour.",
//                            20.00
//                    ),
//                    new ConfigValue<>(
//                            "mailDeliveryLocationSelectionDistanceWeightMultiplier",
//                            "Mail Delivery Location Selection Distance Weight Multiplier",
//                            "The multiplier of the distance weight when choosing mail delivery locations. Takes the ratio (between 0 and 1) of the distance to the maximum distance between locations, multiplies it by this value, and adds it to the mail location weight. This weight is used to randomly choose a mail location to deliver to provide a realistic relationship between delivery locations. At 0, distance has no effect on location selection. At 1, the weight will have a slight effect on the location selection. At 10, the weight will have a significant effect on the location selection. The weight is applied inversely, making closer distances worth more than further distances.",
//                            1.00
//                    ),
//                    new ConfigValue<>(
//                            "mailDistanceToRewardMultiplier",
//                            "Mail Distance To Reward Multiplier",
//                            "Mail delivery reward is calculated by multiplying the distance by this number. The result is then rounded to the nearest ten. At 0, the reward given is 0. At 1, the reward given will be the distance in blocks.",
//                            0.05
//                    )
//
//            )
//    );

    private static boolean debug = false;
    private static boolean pauseChat = false;
    private static boolean softWhitelist = false;
    private static boolean disablePvP = false;
    private static int playerLimit = -1;
    private static @Nullable String resourcePackRepo = "WOFTNW/Dragonspeak";
    private static @Nullable Location hubLocation = null;
    private static @Nullable String botToken = null;
    private static @Nullable Long whitelistChannel = null;
    private static @Nullable Long gameChatChannel = null;
    private static @Nullable Long gameLogChannel = null;
    private static boolean enableLogConsoleCommands = true;
    private static boolean webWhitelistEnabled = true;
    private static String websiteUrl = "https://woftnw.org";
    private static int infractionExpireTimeDays = 90;
    private static @Nullable Long infractionsCategory = null;
    private static @NotNull String shopName = "Shop";
    private static @NotNull String currencyIcon = "$";
    private static double dailyStreakMultiplier = 5.00;
    private static double workReward = 20.00;
    private static double mailDeliveryLocationSelectionDistanceWeightMultiplier = 1.00;
    private static double mailDistanceToRewardMultiplier = 0.05;


    public static void loadFromFile(String storageName) throws FileNotFoundException {

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream("config.yml");
        Map<String, Object> fileData = yaml.load(inputStream);

        debug = (boolean) fileData.get("debug");
        pauseChat = (boolean) fileData.get("pauseChat");
        softWhitelist = (boolean) fileData.get("softWhitelist");
        disablePvP = (boolean) fileData.get("disablePvP");
        playerLimit = (int) fileData.get("playerLimit");
        resourcePackRepo = (String) fileData.get("resourcePackRepo");
        hubLocation = (Location) fileData.get("hubLocation");
        botToken = (String) fileData.get("botToken");
        whitelistChannel = (Long) fileData.get("whitelistChannel");
        gameChatChannel = (Long) fileData.get("gameChatChannel");
        gameLogChannel = (Long) fileData.get("gameLogChannel");
        enableLogConsoleCommands = (boolean) fileData.get("enableLogConsoleCommands");
        webWhitelistEnabled = (boolean) fileData.get("webWhitelistEnabled");
        websiteUrl = (String) fileData.get("websiteUrl");
        infractionExpireTimeDays = (int) fileData.get("infractionExpireTimeDays");
        infractionsCategory = (Long) fileData.get("infractionsCategory");
        shopName = (String) fileData.get("shopName");
        currencyIcon = (String) fileData.get("currencyIcon");
        dailyStreakMultiplier = (double) fileData.get("dailyStreakMultiplier");
        workReward = (double) fileData.get("workReward");
        mailDeliveryLocationSelectionDistanceWeightMultiplier = (double) fileData.get("mailDeliveryLocationSelectionDistanceWeightMultiplier");
        mailDistanceToRewardMultiplier = (double) fileData.get("mailDistanceToRewardMultiplier");

    }

    public static void saveFile() throws FileNotFoundException {

        Map<String, Object> data = new HashMap<>();

        data.put("debug", debug);
        data.put("pauseChat", pauseChat);
        data.put("softWhitelist", softWhitelist);
        data.put("disablePvP", disablePvP);
        data.put("playerLimit", playerLimit);
        data.put("resourcePackRepo", resourcePackRepo);
        data.put("hubLocation", hubLocation);
        data.put("botToken", botToken);
        data.put("whitelistChannel", whitelistChannel);
        data.put("gameChatChannel", gameChatChannel);
        data.put("gameLogChannel", gameLogChannel);
        data.put("enableLogConsoleCommands", enableLogConsoleCommands);
        data.put("webWhitelistEnabled", webWhitelistEnabled);
        data.put("websiteUrl", websiteUrl);
        data.put("infractionExpireTimeDays", infractionExpireTimeDays);
        data.put("infractionsCategory", infractionsCategory);
        data.put("shopName", shopName);
        data.put("currencyIcon", currencyIcon);
        data.put("dailyStreakMultiplier", dailyStreakMultiplier);
        data.put("workReward", workReward);
        data.put("mailDeliveryLocationSelectionDistanceWeightMultiplier", mailDeliveryLocationSelectionDistanceWeightMultiplier);
        data.put("mailDistanceToRewardMultiplier", mailDistanceToRewardMultiplier);

        Yaml yaml = new Yaml();
        PrintWriter writer = new PrintWriter("config.yml");
        yaml.dump(data, writer);
        writer.close();
    }

}
