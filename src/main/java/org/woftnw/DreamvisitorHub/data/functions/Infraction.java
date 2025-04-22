// package org.woftnw.DreamvisitorHub.data.functions;

// import net.dv8tion.jda.api.EmbedBuilder;
// import net.dv8tion.jda.api.Permission;
// import net.dv8tion.jda.api.entities.Member;
// import net.dv8tion.jda.api.entities.channel.concrete.Category;
// import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
// import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
// import net.dv8tion.jda.api.interactions.components.buttons.Button;
// import org.jetbrains.annotations.Contract;
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
// import org.yaml.snakeyaml.Yaml;

// import java.awt.*;
// import java.io.*;
// import java.time.LocalDateTime;
// import java.time.OffsetDateTime;
// import java.time.ZoneId;
// import java.util.*;
// import java.util.List;
// import java.util.concurrent.TimeUnit;
// import java.util.logging.Logger;

// public class Infraction {

//   private static final Logger logger = Logger.getLogger("DreamvisitorHub");
//   private static final String INFRACTIONS_FILE = "infractions.yml";

//   public static final String actionBan = "ban"; // regualar MC temp-ban or ban & notify
//   public static final String actionUserBan = "user_ban"; // notify only
//   public static final String actionNoBan = "no_ban"; // no ban, no notify
//   public static final String actionAllBan = "all_ban"; // ban from all
//   public static final int BAN_POINT = 3;
//   private final byte value;
//   @NotNull
//   private final String reason;
//   @NotNull
//   private final LocalDateTime time;
//   private boolean expired = false;
//   @Nullable
//   private Long warnChannelID = null;

//   public Infraction(byte infractionValue, @NotNull String infractionReason, @NotNull LocalDateTime dateTime) {
//     this.value = infractionValue;
//     this.reason = infractionReason;
//     this.time = dateTime;
//   }

//   public Infraction(byte infractionValue, @NotNull String infractionReason, @NotNull LocalDateTime dateTime,
//       boolean expired, @Nullable Long warnChannelID) {
//     this.value = infractionValue;
//     this.reason = infractionReason;
//     this.time = dateTime;
//     this.expired = expired;
//     this.warnChannelID = warnChannelID;
//   }

//   /**
//    * Loads the infractions YAML file from disk.
//    *
//    * @return a map containing the infractions data
//    */
//   private static Map<String, Object> loadInfractions() {
//     try {
//       File file = new File(INFRACTIONS_FILE);
//       if (!file.exists()) {
//         return new HashMap<>();
//       }

//       Yaml yaml = new Yaml();
//       InputStream inputStream = new FileInputStream(file);
//       Map<String, Object> data = yaml.load(inputStream);
//       return data != null ? data : new HashMap<>();
//     } catch (FileNotFoundException e) {
//       logger.warning("Failed to load infractions file: " + e.getMessage());
//       return new HashMap<>();
//     }
//   }

//   /**
//    * Saves infractions data to disk.
//    *
//    * @param data the map containing the infractions data
//    */
//   private static void saveInfractions(Map<String, Object> data) {
//     try {
//       Yaml yaml = new Yaml();
//       PrintWriter writer = new PrintWriter(INFRACTIONS_FILE);
//       yaml.dump(data, writer);
//       writer.close();
//     } catch (FileNotFoundException e) {
//       logger.severe("Failed to save infractions file: " + e.getMessage());
//     }
//   }

//   /**
//    * Fetch the infractions of a member from disk.
//    *
//    * @param memberId the Discord Snowflake ID of the member whose infractions to
//    *                 fetch.
//    * @return a non-null {@link List<Infraction>}
//    */
//   @SuppressWarnings("unchecked")
//   public static @NotNull List<Infraction> getInfractions(long memberId) {
//     Map<String, Object> data = loadInfractions();
//     Map<String, Object> memberData = (Map<String, Object>) data.get(String.valueOf(memberId));

//     if (memberData == null || !memberData.containsKey("infractions")) {
//       return new ArrayList<>();
//     }

//     List<Map<String, Object>> infractionsMap = (List<Map<String, Object>>) memberData.get("infractions");
//     List<Infraction> infractions = new ArrayList<>();

//     if (infractionsMap != null) {
//       for (Map<String, Object> map : infractionsMap) {
//         infractions.add(deserialize(map));
//       }
//     }

//     return infractions;
//   }

//   public static @NotNull Map<Long, List<Infraction>> getAllInfractions() {
//     Map<String, Object> data = loadInfractions();
//     Map<Long, List<Infraction>> infractionList = new HashMap<>();

//     for (String key : data.keySet()) {
//       try {
//         long memberId = Long.parseLong(key);
//         infractionList.put(memberId, getInfractions(memberId));
//       } catch (NumberFormatException e) {
//         logger.warning("Invalid member ID in infractions file: " + key);
//       }
//     }

//     return infractionList;
//   }

//   /**
//    * Get the value of all of a member's infractions.
//    *
//    * @param infractions  the infractions to count.
//    * @param countExpired whether to count expired infractions.
//    * @return the total value as a {@code byte}.
//    */
//   @Contract(pure = true)
//   public static byte getInfractionCount(@NotNull List<Infraction> infractions, boolean countExpired) {
//     byte count = 0;
//     for (Infraction infraction : infractions)
//       if (countExpired || !infraction.isExpired())
//         count += infraction.value;
//     return count;
//   }

//   /**
//    * Overwrite a member's infraction list with a new list and saves to disk.
//    *
//    * @param infractions the {@link List<Infraction>} to write.
//    * @param memberId    the Discord Snowflake ID of the member to write to.
//    */
//   public static void setInfractions(@NotNull List<Infraction> infractions, long memberId) {
//     Map<String, Object> data = loadInfractions();

//     // Create member data if it doesn't exist
//     Map<String, Object> memberData = (Map<String, Object>) data.getOrDefault(String.valueOf(memberId), new HashMap<>());

//     // Convert infractions to serializable maps
//     List<Map<String, Object>> serializedInfractions = new ArrayList<>();
//     for (Infraction infraction : infractions) {
//       serializedInfractions.add(infraction.serialize());
//     }

//     // Update the infractions list
//     memberData.put("infractions", serializedInfractions);
//     data.put(String.valueOf(memberId), memberData);

//     // Save to disk
//     saveInfractions(data);
//   }

//   public static void setTempban(long memberId, boolean state) {
//     Map<String, Object> data = loadInfractions();

//     // Create member data if it doesn't exist
//     Map<String, Object> memberData = (Map<String, Object>) data.getOrDefault(String.valueOf(memberId), new HashMap<>());

//     // Update tempban state
//     memberData.put("tempban", state);
//     data.put(String.valueOf(memberId), memberData);

//     // Save to disk
//     saveInfractions(data);
//   }

//   public static void setBan(long memberId, boolean state) {
//     Map<String, Object> data = loadInfractions();

//     // Create member data if it doesn't exist
//     Map<String, Object> memberData = (Map<String, Object>) data.getOrDefault(String.valueOf(memberId), new HashMap<>());

//     // Update ban state
//     memberData.put("ban", state);
//     data.put(String.valueOf(memberId), memberData);

//     // Save to disk
//     saveInfractions(data);
//   }

//   public static boolean hasTempban(long memberId) {
//     Map<String, Object> data = loadInfractions();
//     Map<String, Object> memberData = (Map<String, Object>) data.get(String.valueOf(memberId));

//     if (memberData == null || !memberData.containsKey("tempban")) {
//       return false;
//     }

//     return (boolean) memberData.get("tempban");
//   }

//   public static boolean hasBan(long memberId) {
//     Map<String, Object> data = loadInfractions();
//     Map<String, Object> memberData = (Map<String, Object>) data.get(String.valueOf(memberId));

//     if (memberData == null || !memberData.containsKey("ban")) {
//       return false;
//     }

//     return (boolean) memberData.get("ban");
//   }

//   public static byte getInfractionsUntilBan(long memberId) {
//     return (byte) (BAN_POINT - getInfractionCount(getInfractions(memberId), false));
//   }

//   public static void execute(@NotNull Infraction infraction, @NotNull Member member, boolean silent,
//       @NotNull String actionId) throws InsufficientPermissionException, InvalidObjectException {
//     // Existing code remains unchanged
//   }

//   /**
//    * Save an infraction to a member and write to disk.
//    *
//    * @param memberId the Discord Snowflake ID of the member.
//    */
//   private void save(long memberId) {
//     Map<String, Object> data = loadInfractions();

//     // Create member data if it doesn't exist
//     Map<String, Object> memberData = (Map<String, Object>) data.getOrDefault(String.valueOf(memberId), new HashMap<>());

//     // Get existing infractions or create new list
//     List<Map<String, Object>> infractions;
//     if (memberData.containsKey("infractions")) {
//       infractions = (List<Map<String, Object>>) memberData.get("infractions");
//     } else {
//       infractions = new ArrayList<>();
//     }

//     // Add the new infraction
//     infractions.add(serialize());

//     // Update the infractions list
//     memberData.put("infractions", infractions);
//     data.put(String.valueOf(memberId), memberData);

//     // Save to disk
//     saveInfractions(data);
//   }

//   public byte getValue() {
//     return value;
//   }

//   public @NotNull String getReason() {
//     return reason;
//   }

//   public @NotNull LocalDateTime getTime() {
//     return time;
//   }

//   public boolean isExpired() {
//     expireCheck();
//     return expired;
//   }

//   public void expire() {
//     expired = true;
//   }

//   private void expireCheck() {
//     int expireTimeDays = Dreamvisitor.getPlugin().getConfig().getInt("infraction-expire-time-days");
//     if (time.plusDays(expireTimeDays).isBefore(LocalDateTime.now()))
//       expired = true;
//   }

//   @Nullable
//   public TextChannel getWarnChannel() {
//     if (warnChannelID == null)
//       return null;
//     return Bot.getJda().getTextChannelById(warnChannelID);
//   }

//   public void remind(long user) {
//     Dreamvisitor.debug("Remind warn. warnChannelId: " + warnChannelID);
//     if (warnChannelID == null)
//       return;
//     TextChannel warnChannel = getWarnChannel();
//     if (warnChannel == null)
//       return;

//     Dreamvisitor.debug("Attempting to retrieve last message.");
//     warnChannel.retrieveMessageById(warnChannel.getLatestMessageId()).queue(message -> {
//       Dreamvisitor.debug("Retrieved last message.");
//       Dreamvisitor.debug("Message author is bot? " + message.getAuthor().equals(Bot.getJda().getSelfUser()));
//       Dreamvisitor.debug("Time is passed? " + message.getTimeCreated().plusDays(1).isBefore(OffsetDateTime.now()));
//       if (message.getAuthor().equals(Bot.getJda().getSelfUser())
//           && message.getTimeCreated().plusDays(1).isBefore(OffsetDateTime.now())) {
//         warnChannel.getGuild().retrieveMemberById(user).queue(member -> warnChannel.sendMessage(member.getAsMention()
//             + ", you have not yet responded to this thread. On the first message in this thread, press **I understand** to close the thread or **I'm confused** if you're confused.")
//             .queue());
//       }
//     });
//   }

//   @NotNull
//   public Map<String, Object> serialize() {
//     Map<String, Object> objectMap = new HashMap<>();
//     objectMap.put("value", value);
//     objectMap.put("reason", reason);
//     objectMap.put("time", time.toString());
//     objectMap.put("expired", expired);
//     objectMap.put("warnChannelID", warnChannelID);

//     return objectMap;
//   }

//   @Contract("_ -> new")
//   public static @NotNull Infraction deserialize(@NotNull Map<String, Object> map) {
//     Infraction infraction = new Infraction(Byte.parseByte(String.valueOf((int) map.get("value"))),
//         (String) map.get("reason"), LocalDateTime.parse((CharSequence) map.get("time")));
//     if (map.get("expired") != null && (boolean) map.get("expired"))
//       infraction.expire();
//     infraction.warnChannelID = (Long) map.get("warnChannelID");
//     return infraction;
//   }
// }
