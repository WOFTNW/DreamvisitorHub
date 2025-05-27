package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
// import org.woftnw.mc_renderer.MCRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DCmdSeen implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());
  private static final Logger logger = Logger.getLogger(DCmdSeen.class.toString());

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("seen", "See when someone was last on the Minecraft server.")
        .addOption(OptionType.USER, "user", "The user to search for.", true)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    if (user == null) {
      event.reply("User cannot be null!").setEphemeral(true).queue();
      return;
    }

    // Defer reply to give us time to process
    event.deferReply().queue();

    // Find the user in PocketBase
    Optional<DVUser> dvUserOptional = userRepository.findBySnowflakeId(user.getIdLong());

    if (!dvUserOptional.isPresent()) {
      event.getHook().sendMessage(user.getAsMention() + " does not have a linked Minecraft account.").setEphemeral(true)
          .queue();
      return;
    }

    DVUser dvUser = dvUserOptional.get();

    // Check if the user has ever played (has last_played data)
    if (dvUser.getLast_played() == null) {
      event.getHook().sendMessage(user.getAsMention() + " has never logged into the Minecraft server.")
          .setEphemeral(true).queue();
      return;
    }

    // Calculate time difference from last played time to now
    OffsetDateTime lastPlayed = dvUser.getLast_played();
    OffsetDateTime now = OffsetDateTime.now();
    Duration duration = Duration.between(lastPlayed, now);

    // Format duration in days, hours, minutes, seconds
    long days = duration.toDays();
    long hours = duration.toHours() % 24;
    long minutes = duration.toMinutes() % 60;
    long seconds = duration.getSeconds() % 60;

    // Currently we can only check if they're offline based on DB data
    String status = "offline";
    String mcUsername = dvUser.getMcUsername() != null ? dvUser.getMcUsername() : "Unknown";

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Last Seen: " + mcUsername);
    embed.setDescription(user.getAsMention() + " has been " + status + " since " +
        days + " days, " +
        hours + " hours, " +
        minutes + " minutes, and " +
        seconds + " seconds ago.");
    embed.setColor(Color.BLUE);
    embed.setTimestamp(Instant.now());

    // // Try to fetch and render player skin
    // byte[] skinImageBytes = null;
    // if (dvUser.getMc_uuid() != null) {
    // try {
    // String skinUrl = fetchPlayerSkinUrl(dvUser.getMc_uuid().toString());
    // if (skinUrl != null && !skinUrl.isEmpty()) {
    // logger.info("Found skin URL: " + skinUrl);
    // BufferedImage skin = MCRenderer.renderModelToBufferFromUrl(skinUrl);

    // // Convert BufferedImage to byte array
    // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // ImageIO.write(skin, "png", outputStream);
    // skinImageBytes = outputStream.toByteArray();
    // }
    // } catch (Exception e) {
    // logger.log(Level.WARNING, "Failed to fetch player skin", e);
    // }
    // }

    // // Send response with or without skin
    // if (skinImageBytes != null) {
    // // Set the thumbnail to show as part of the embed
    // embed.setThumbnail("attachment://skin.png");

    // // Send the embed with the file attached
    // event.getHook().sendMessageEmbeds(embed.build())
    // .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(skinImageBytes,
    // "skin.png"))
    // .queue();
    // } else {
    // event.getHook().sendMessageEmbeds(embed.build()).queue();
    // }
    event.getHook().sendMessageEmbeds(embed.build()).queue();
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
