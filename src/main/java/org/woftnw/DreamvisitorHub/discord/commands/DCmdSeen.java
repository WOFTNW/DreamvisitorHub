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

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

public class DCmdSeen implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

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

    // Find the user in PocketBase
    Optional<DVUser> dvUserOptional = userRepository.findBySnowflakeId(user.getIdLong());

    if (!dvUserOptional.isPresent()) {
      event.reply(user.getAsMention() + " does not have a linked Minecraft account.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = dvUserOptional.get();

    // Check if the user has ever played (has last_played data)
    if (dvUser.getLast_played() == null) {
      event.reply(user.getAsMention() + " has never logged into the Minecraft server.").setEphemeral(true).queue();
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

    if (dvUser.getMc_uuid() != null) {
      embed.setThumbnail("https://crafatar.com/avatars/" + dvUser.getMc_uuid().toString() + "?overlay=true");
    }

    event.replyEmbeds(embed.build()).queue();
  }
}
