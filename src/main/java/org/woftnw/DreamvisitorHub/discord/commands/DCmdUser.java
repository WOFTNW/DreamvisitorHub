package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.discord.Bot;
import org.woftnw.DreamvisitorHub.pb.PocketBase;
import org.woftnw.mc_renderer.MCRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class DCmdUser implements DiscordCommand {
  private static final Logger logger = Logger.getLogger(DCmdUser.class.toString());
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private final UserRepository userRepository = App.getUserRepository();

  @Override
  public @NotNull SlashCommandData getCommandData() {
    return Commands.slash("user", "Get the details of a user.")
        .addOption(OptionType.USER, "user", "The user to search for.", true)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    logger.info("Command requested. /user");
    User targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
    logger.info("Target user: " + targetUser.getId());
    byte[] skinImageBytes = null;
    // Defer reply to give us time to fetch data
    event.deferReply().queue();

    // Find user by Discord ID (snowflake)
    logger.info("Searching for user with snowflake ID: " + targetUser.getIdLong());
    Optional<DVUser> userData = userRepository
        .findBySnowflakeId(targetUser.getIdLong());

    if (!userData.isPresent()) {
      // Try with string ID as fallback
      logger.info("User not found by snowflake, trying with string ID: " + targetUser.getId());
      userData = userRepository.findByDiscordId(targetUser.getId());
    }

    EmbedBuilder builder = new EmbedBuilder();
    builder.setColor(Color.BLUE);
    builder.setAuthor(targetUser.getName(), targetUser.getAvatarUrl(), targetUser.getAvatarUrl());
    builder.addField("Discord ID", targetUser.getId(), false);

    if (userData.isPresent()) {
      // User found in database
      DVUser user = userData.get();
      logger.info("User found: " + user.getDiscord_id());

      // Minecraft Information
      String mcUsername = user.getMcUsername() != null ? user.getMcUsername() : "N/A";
      builder.addField("Minecraft Username", mcUsername, false);
      // Try to fetch player skin if we have MC username
      if (user.getMc_uuid() != null && !user.getMc_uuid().toString().isEmpty()) {
        try {
          String skinUrl = fetchPlayerSkinUrl(user.getMc_uuid().toString());
          if (skinUrl != null && !skinUrl.isEmpty()) {
            logger.info("Found skin URL: " + skinUrl);
            BufferedImage skin = MCRenderer.renderModelToBufferFromUrl(skinUrl);

            // Convert BufferedImage to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(skin, "png", outputStream);
            skinImageBytes = outputStream.toByteArray();
          }
        } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to fetch player skin", e);
          logger.log(Level.WARNING, "Exception details:", e);
        }
      }

      // Economic Info
      builder.addField("Balance",
          user.getBalance() != null ? String.format("%.2f", user.getBalance()) + Bot.CURRENCY_SYMBOL
              : "0.00 " + Bot.CURRENCY_SYMBOL,
          true);

      builder.addField("Daily Streak",
          user.getDaily_streak() != null ? user.getDaily_streak().toString() : "0",
          true);

      // Home and Claim Info
      builder.addField("Claims",
          user.getClaims() != null ? String.valueOf(user.getClaims().size()) : "0",
          true);

      builder.addField("Claim Limit",
          user.getClaim_limit() != null ? user.getClaim_limit().toString() : "N/A",
          true);

      builder.addField("Homes",
          user.getUsers_home() != null ? String.valueOf(user.getUsers_home().size()) : "0",
          true);

      // Status Info
      if (user.getIs_banned() != null && user.getIs_banned()) {
        builder.addField("Account Status", "BANNED", false);
      } else if (user.getIs_suspended() != null && user.getIs_suspended()) {
        builder.addField("Account Status", "SUSPENDED", false);
      }

      // Time Info
      if (user.getPlay_time() != null) {
        long totalSeconds = user.getPlay_time() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        builder.addField("Play Time", String.format("%d hours, %d minutes", hours, minutes), false);
      }

      if (user.getLast_work() != null) {
        builder.addField("Last Work", user.getLast_work().format(DATE_FORMATTER), true);
      }

      if (user.getLast_daily() != null) {
        builder.addField("Last Daily", user.getLast_daily().format(DATE_FORMATTER), true);
      }
      if (user.getLast_played() != null) {
        builder.addField("Last Played", user.getLast_played().format(DATE_FORMATTER), true);
      }

      if (user.getCreated() != null) {
        builder.addField("Joined", user.getCreated().format(DATE_FORMATTER), false);
      }

      // Add info about infractions if any
      if (user.getInfractions() != null && !user.getInfractions().isEmpty()) {
        builder.addField("Infractions", String.valueOf(user.getInfractions().size()), false);
      }

    } else {
      // User not found in database
      builder.setDescription("User not found in database");
      builder.addField("Minecraft Username", "N/A", false);
    }

    logger.info("sending Message");
    if (skinImageBytes != null) {
      event.getHook().sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(skinImageBytes, "skin.png"))
        .addEmbeds(builder.build())
        .queue();
    } else {
      event.getHook().sendMessageEmbeds(builder.build()).queue();
    }
  }

  /**
   * Fetches a player's skin URL from the Mojang API
   *
   * @param uuid Minecraft player UUID
   * @return URL to the player's skin texture, or null if not found
   */
  private String fetchPlayerSkinUrl(String uuid) {
    // Remove dashes from UUID if present
    uuid = uuid.replace("-", "");

    try {
      URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);

      int status = connection.getResponseCode();
      if (status != 200) {
        logger.warning("Mojang API returned status code: " + status);
        return null;
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();

      // Parse JSON response
      JSONObject jsonResponse = new JSONObject(response.toString());
      JSONArray properties = jsonResponse.getJSONArray("properties");

      for (int i = 0; i < properties.length(); i++) {
        JSONObject property = properties.getJSONObject(i);
        if ("textures".equals(property.getString("name"))) {
          String textureBase64 = property.getString("value");
          String decodedTexture = new String(Base64.getDecoder().decode(textureBase64));

          // Parse the decoded texture JSON
          JSONObject textureJson = new JSONObject(decodedTexture);
          JSONObject textures = textureJson.getJSONObject("textures");

          if (textures.has("SKIN")) {
            return textures.getJSONObject("SKIN").getString("url");
          }
        }
      }
    } catch (IOException | JSONException e) {
      logger.log(Level.WARNING, "Error fetching player skin", e);
    }

    return null;
  }
}
