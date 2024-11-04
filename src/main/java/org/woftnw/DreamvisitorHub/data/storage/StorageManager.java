package org.woftnw.DreamvisitorHub.data.storage;

import java.util.Arrays;
import java.util.List;

public class StorageManager {

    private static final List<UnifiedStorage> unifiedStorages = Arrays.asList(
            new UnifiedStorage("config",
                    new ConfigValue(
                            "debug",
                            "Debug Mode",
                            "Whether to enable debug messages. This will send additional messages to help debug Dreamvisitor. Disabled by default.",
                            false,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "pauseChat",
                            "Pause Chat",
                            "Whether chat is paused or not. This can be toggled in Minecraft with /pausechat. Disabled by default.",
                            false,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "softWhitelist",
                            "Soft Whitelist",
                            "Whether the soft whitelist is enabled or not. This can be set in Minecraft with /softwhitelist [on|off]. Disabled by default.",
                            false,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "disablePvp",
                            "Disable PvP",
                            "Whether to globally disable pvp or not. This can be toggled in Minecraft with /togglepvp.",
                            false,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "playerLimit",
                            "Player Limit Override",
                            "Player limit override. This will override the player limit, both over and under. This can be set in Minecraft with /playerlimit <int>",
                            -1,
                            ConfigValue.Type.INT
                    ),
                    new ConfigValue(
                            "resourcePackRepo",
                            "Resource Pack Repository",
                            "The repository path of the server resource pack. Dreamvisitor will pull the first artifact from the latest release on pack update.",
                            "WOFTNW/Dragonspeak",
                            ConfigValue.Type.INT
                    ),
                    new ConfigValue(
                            "hubLocation",
                            "Hub Location",
                            "The location of the hub.",
                            null,
                            ConfigValue.Type.LOCATION
                    ),
                    new ConfigValue(
                            "botToken",
                            "Bot Token",
                            "The Dreamvisitor bot token. DO NOT SHARE THIS. Dreamvisitor will not work properly unless this is a valid bot token. Ask Bog for a bot token if Dreamvisitor reports a login error on startup.",
                            "",
                            ConfigValue.Type.STRING
                    ),
                    new ConfigValue(
                            "whitelistChannel",
                            "Whitelist Channel",
                            "The channel of the whitelist chat. This can be set on Discord with /setwhitelist.",
                            null,
                            ConfigValue.Type.CHANNEL
                    ),
                    new ConfigValue(
                            "gameChatChannel",
                            "Game Chat Channel",
                            "The channel of the game chat. This can be set on Discord with /setgamechat.",
                            null,
                            ConfigValue.Type.CHANNEL
                    ),
                    new ConfigValue(
                            "logChannel",
                            "Log Channel",
                            "The channel of the log chat. This can be set on Discord with /setlogchat.",
                            null,
                            ConfigValue.Type.CHANNEL
                    ),
                    new ConfigValue(
                            "enableLogConsoleCommands",
                            "Enable Log Console Commands",
                            "Whether to pass messages in the log channel as console commands. If log-console is enabled, this will take messages sent by users with the Discord administrator permission and pass them as console commands.",
                            true,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "webWhitelistEnabled",
                            "Web Whitelist",
                            "Whether web whitelisting is enabled or not. This can be set with the /toggleweb Discord command. Enabled by default.",
                            true,
                            ConfigValue.Type.BOOLEAN
                    ),
                    new ConfigValue(
                            "websiteUrl",
                            "Website URL",
                            "The URL for the whitelisting website. Used to restrict requests not from the specified website to prevent abuse.",
                            "https://woftnw.org",
                            ConfigValue.Type.STRING
                    ),
                    new ConfigValue(
                            "infractionExpireTimeDays",
                            "Infraction Expire Time",
                            "The amount of time in days (as an integer) that infractions take to expire. Expired infractions are not deleted, but they do not count toward a total infraction count.",
                            90,
                            ConfigValue.Type.INT
                    ),
                    new ConfigValue(
                            "infractionsCategory",
                            "Infractions Category",
                            "The ID of the category to create infractions channels. They will accumulate here.",
                            null,
                            ConfigValue.Type.CATEGORY
                    ),
                    new ConfigValue(
                            "shopName",
                            "Shop Name",
                            "The name of the Discord shop. This will appear at the top of the embed.",
                            null,
                            ConfigValue.Type.STRING
                    ),
                    new ConfigValue(
                            "currencyIcon",
                            "Currency Icon",
                            "The icon used for currency in the Discord economy system. This can be any string, including symbols, letters, emojis, and Discord custom emoji.",
                            "$",
                            ConfigValue.Type.STRING
                    ),
                    new ConfigValue(
                            "currencyIcon",
                            "Currency Icon",
                            "The icon used for currency in the Discord economy system. This can be any string, including symbols, letters, emojis, and Discord custom emoji.",
                            "$",
                            ConfigValue.Type.STRING
                    )
                    
            )
    );

}
