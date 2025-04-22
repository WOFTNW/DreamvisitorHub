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
import org.woftnw.DreamvisitorHub.pb.PocketBase;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class DCmdUser implements DiscordCommand {
  private static final Logger logger = Logger.getLogger(DCmdUser.class.toString());
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private UserRepository getUserRepository() {
    // Get PocketBase instance from App configuration
    PocketBase pocketBase = (App.getPb());
    return new PocketBaseUserRepository(pocketBase);
  }

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

    // Defer reply to give us time to fetch data
    event.deferReply().queue();

    // Get the user repository
    UserRepository userRepository = getUserRepository();

    // Find user by Discord ID (snowflake)
    Optional<org.woftnw.DreamvisitorHub.data.type.DVUser> userData = userRepository
        .findBySnowflakeId(targetUser.getIdLong());

    EmbedBuilder builder = new EmbedBuilder();
    builder.setColor(Color.BLUE);
    builder.setAuthor(targetUser.getName(), targetUser.getAvatarUrl(), targetUser.getAvatarUrl());
    builder.addField("Discord ID", targetUser.getId(), false);

    if (userData.isPresent()) {
      // User found in database
      org.woftnw.DreamvisitorHub.data.type.DVUser user = userData.get();

      // Minecraft Information
      builder.addField("Minecraft Username",
          user.getMcUsername() != null ? user.getMcUsername() : "N/A",
          false);

      // Economic Info
      builder.addField("Balance",
          user.getBalance() != null ? String.format("%.2f", user.getBalance()) : "0.00",
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
        int hours = user.getPlay_time() / 3600;
        int minutes = (user.getPlay_time() % 3600) / 60;
        builder.addField("Play Time", String.format("%d hours, %d minutes", hours, minutes), false);
      }

      if (user.getLast_work() != null) {
        builder.addField("Last Work", user.getLast_work().format(DATE_FORMATTER), true);
      }

      if (user.getLast_daily() != null) {
        builder.addField("Last Daily", user.getLast_daily().format(DATE_FORMATTER), true);
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

    event.getHook().sendMessageEmbeds(builder.build()).queue();
  }
}
