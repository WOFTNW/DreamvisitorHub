// package org.woftnw.DreamvisitorHub.data.storage;

// // import org.bukkit.Location;
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
// // import org.woftnw.DreamvisitorHub.data.functions.Infraction;
// import org.yaml.snakeyaml.Yaml;

// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.InputStream;
// import java.io.PrintWriter;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// // TODO: add the Location Class to this
// public class StorageManager {

//     private static boolean debug = false;
//     private static boolean pauseChat = false;
//     private static boolean softWhitelist = false;
//     private static boolean disablePvP = false;
//     private static int playerLimit = -1;
//     private static @Nullable String resourcePackRepo = "WOFTNW/Dragonspeak";
//     // private static @Nullable Location hubLocation = null;
//     private static @Nullable String botToken = null;
//     private static @Nullable Long whitelistChannel = null;
//     private static @Nullable Long gameChatChannel = null;
//     private static @Nullable Long gameLogChannel = null;
//     private static boolean enableLogConsoleCommands = true;
//     private static boolean webWhitelistEnabled = true;
//     private static String websiteUrl = "https://woftnw.org";
//     private static int infractionExpireTimeDays = 90;
//     private static @Nullable Long infractionsCategory = null;
//     private static @NotNull String shopName = "Shop";
//     private static @NotNull String currencyIcon = "$";
//     private static double dailyStreakMultiplier = 5.00;
//     private static double workReward = 20.00;
//     private static double mailDeliveryLocationSelectionDistanceWeightMultiplier = 1.00;
//     private static double mailDistanceToRewardMultiplier = 0.05;
//     private static int consoleSize = 512;

//     private static final String CONFIG = "config";
//     private static final String INFRACTIONS = "infractions";

//     public static void loadFromFile(@NotNull String storageName) throws FileNotFoundException {

//         Yaml yaml = new Yaml();
//         InputStream inputStream = new FileInputStream(storageName + ".yml");
//         Map<String, Object> fileData = yaml.load(inputStream);

//         if (storageName.equals(CONFIG)) {
//             debug = (boolean) fileData.get("debug");
//             pauseChat = (boolean) fileData.get("pauseChat");
//             softWhitelist = (boolean) fileData.get("softWhitelist");
//             disablePvP = (boolean) fileData.get("disablePvP");
//             playerLimit = (int) fileData.get("playerLimit");
//             resourcePackRepo = (String) fileData.get("resourcePackRepo");
//             // hubLocation = (Location) fileData.get("hubLocation");
//             botToken = (String) fileData.get("botToken");
//             whitelistChannel = (Long) fileData.get("whitelistChannel");
//             gameChatChannel = (Long) fileData.get("gameChatChannel");
//             gameLogChannel = (Long) fileData.get("gameLogChannel");
//             enableLogConsoleCommands = (boolean) fileData.get("enableLogConsoleCommands");
//             webWhitelistEnabled = (boolean) fileData.get("webWhitelistEnabled");
//             websiteUrl = (String) fileData.get("websiteUrl");
//             infractionExpireTimeDays = (int) fileData.get("infractionExpireTimeDays");
//             infractionsCategory = (Long) fileData.get("infractionsCategory");
//             shopName = (String) fileData.get("shopName");
//             currencyIcon = (String) fileData.get("currencyIcon");
//             dailyStreakMultiplier = (double) fileData.get("dailyStreakMultiplier");
//             workReward = (double) fileData.get("workReward");
//             mailDeliveryLocationSelectionDistanceWeightMultiplier = (double) fileData.get("mailDeliveryLocationSelectionDistanceWeightMultiplier");
//             mailDistanceToRewardMultiplier = (double) fileData.get("mailDistanceToRewardMultiplier");
//             consoleSize = (int) fileData.get("consoleSize");
//         } else if (storageName.equals(INFRACTIONS)) {
//             List<Map<String, Object>> yamlInfractions = (List<Map<String, Object>>) fileData.get("infractions");
//             for (Map<String, Object> yamlInfraction : yamlInfractions) {
//                 // Infraction infraction = Infraction.deserialize(yamlInfraction);
//             }
//         }

//     }

//     public static void saveFile() throws FileNotFoundException {

//         Map<String, Object> data = new HashMap<>();

//         data.put("debug", debug);
//         data.put("pauseChat", pauseChat);
//         data.put("softWhitelist", softWhitelist);
//         data.put("disablePvP", disablePvP);
//         data.put("playerLimit", playerLimit);
//         data.put("resourcePackRepo", resourcePackRepo);
//         // data.put("hubLocation", hubLocation);
//         data.put("botToken", botToken);
//         data.put("whitelistChannel", whitelistChannel);
//         data.put("gameChatChannel", gameChatChannel);
//         data.put("gameLogChannel", gameLogChannel);
//         data.put("enableLogConsoleCommands", enableLogConsoleCommands);
//         data.put("webWhitelistEnabled", webWhitelistEnabled);
//         data.put("websiteUrl", websiteUrl);
//         data.put("infractionExpireTimeDays", infractionExpireTimeDays);
//         data.put("infractionsCategory", infractionsCategory);
//         data.put("shopName", shopName);
//         data.put("currencyIcon", currencyIcon);
//         data.put("dailyStreakMultiplier", dailyStreakMultiplier);
//         data.put("workReward", workReward);
//         data.put("mailDeliveryLocationSelectionDistanceWeightMultiplier", mailDeliveryLocationSelectionDistanceWeightMultiplier);
//         data.put("mailDistanceToRewardMultiplier", mailDistanceToRewardMultiplier);
//         data.put("consoleSize", consoleSize);

//         Yaml yaml = new Yaml();
//         PrintWriter writer = new PrintWriter("config.yml");
//         yaml.dump(data, writer);
//         writer.close();
//     }

//     @Nullable
//     public static String getBotToken() {
//         return botToken;
//     }

//     public static double getDailyStreakMultiplier() {
//         return dailyStreakMultiplier;
//     }

//     public static double getMailDeliveryLocationSelectionDistanceWeightMultiplier() {
//         return mailDeliveryLocationSelectionDistanceWeightMultiplier;
//     }

//     public static double getMailDistanceToRewardMultiplier() {
//         return mailDistanceToRewardMultiplier;
//     }

//     public static double getWorkReward() {
//         return workReward;
//     }

//     public static int getInfractionExpireTimeDays() {
//         return infractionExpireTimeDays;
//     }

//     public static int getPlayerLimit() {
//         return playerLimit;
//     }

//     // @Nullable
//     // public static Location getHubLocation() {
//     //     return hubLocation;
//     // }

//     @Nullable
//     public static Long getGameChatChannel() {
//         return gameChatChannel;
//     }

//     @Nullable
//     public static Long getGameLogChannel() {
//         return gameLogChannel;
//     }

//     @Nullable
//     public static Long getInfractionsCategory() {
//         return infractionsCategory;
//     }

//     @Nullable
//     public static Long getWhitelistChannel() {
//         return whitelistChannel;
//     }

//     @NotNull
//     public static String getCurrencyIcon() {
//         return currencyIcon;
//     }

//     @Nullable
//     public static String getResourcePackRepo() {
//         return resourcePackRepo;
//     }

//     @NotNull
//     public static String getShopName() {
//         return shopName;
//     }

//     public static String getWebsiteUrl() {
//         return websiteUrl;
//     }

//     public static boolean isDebug() {
//         return debug;
//     }

//     public static boolean isDisablePvP() {
//         return disablePvP;
//     }

//     public static boolean isEnableLogConsoleCommands() {
//         return enableLogConsoleCommands;
//     }

//     public static boolean isPauseChat() {
//         return pauseChat;
//     }

//     public static boolean isSoftWhitelist() {
//         return softWhitelist;
//     }

//     public static boolean isWebWhitelistEnabled() {
//         return webWhitelistEnabled;
//     }

//     public static int getConsoleSize() {
//         return consoleSize;
//     }

//     public static void setBotToken(@Nullable String botToken) {
//         StorageManager.botToken = botToken;
//     }

//     public static void setCurrencyIcon(@NotNull String currencyIcon) {
//         StorageManager.currencyIcon = currencyIcon;
//     }

//     public static void setDailyStreakMultiplier(double dailyStreakMultiplier) {
//         StorageManager.dailyStreakMultiplier = dailyStreakMultiplier;
//     }

//     public static void setDebug(boolean debug) {
//         StorageManager.debug = debug;
//     }

//     public static void setDisablePvP(boolean disablePvP) {
//         StorageManager.disablePvP = disablePvP;
//     }

//     public static void setEnableLogConsoleCommands(boolean enableLogConsoleCommands) {
//         StorageManager.enableLogConsoleCommands = enableLogConsoleCommands;
//     }

//     public static void setGameChatChannel(@Nullable Long gameChatChannel) {
//         StorageManager.gameChatChannel = gameChatChannel;
//     }

//     public static void setGameLogChannel(@Nullable Long gameLogChannel) {
//         StorageManager.gameLogChannel = gameLogChannel;
//     }

//     // public static void setHubLocation(@Nullable Location hubLocation) {
//     //     StorageManager.hubLocation = hubLocation;
//     // }

//     public static void setInfractionExpireTimeDays(int infractionExpireTimeDays) {
//         StorageManager.infractionExpireTimeDays = infractionExpireTimeDays;
//     }

//     public static void setInfractionsCategory(@Nullable Long infractionsCategory) {
//         StorageManager.infractionsCategory = infractionsCategory;
//     }

//     public static void setMailDeliveryLocationSelectionDistanceWeightMultiplier(double mailDeliveryLocationSelectionDistanceWeightMultiplier) {
//         StorageManager.mailDeliveryLocationSelectionDistanceWeightMultiplier = mailDeliveryLocationSelectionDistanceWeightMultiplier;
//     }

//     public static void setMailDistanceToRewardMultiplier(double mailDistanceToRewardMultiplier) {
//         StorageManager.mailDistanceToRewardMultiplier = mailDistanceToRewardMultiplier;
//     }

//     public static void setPauseChat(boolean pauseChat) {
//         StorageManager.pauseChat = pauseChat;
//     }

//     public static void setPlayerLimit(int playerLimit) {
//         StorageManager.playerLimit = playerLimit;
//     }

//     public static void setResourcePackRepo(@Nullable String resourcePackRepo) {
//         StorageManager.resourcePackRepo = resourcePackRepo;
//     }

//     public static void setShopName(@NotNull String shopName) {
//         StorageManager.shopName = shopName;
//     }

//     public static void setSoftWhitelist(boolean softWhitelist) {
//         StorageManager.softWhitelist = softWhitelist;
//     }

//     public static void setWebsiteUrl(String websiteUrl) {
//         StorageManager.websiteUrl = websiteUrl;
//     }

//     public static void setWebWhitelistEnabled(boolean webWhitelistEnabled) {
//         StorageManager.webWhitelistEnabled = webWhitelistEnabled;
//     }

//     public static void setWhitelistChannel(@Nullable Long whitelistChannel) {
//         StorageManager.whitelistChannel = whitelistChannel;
//     }

//     public static void setWorkReward(double workReward) {
//         StorageManager.workReward = workReward;
//     }

//     public static void setConsoleSize(int consoleSize) {
//         StorageManager.consoleSize = consoleSize;
//     }
// }
