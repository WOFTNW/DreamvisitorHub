package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.InfractionRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Infraction;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdInfractions extends ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdInfractions.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final InfractionRepository infractionRepository = App.getInfractionRepository();
  long bannedRole = 0;
  long tempBanRole = 0;

  @Override
  public @NotNull SlashCommandData getCommandData() {
    return Commands.slash("infractions", "Get the infractions of a user.")
        .addOption(OptionType.USER, "user", "The user whose infractions to get.", true)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    bannedRole = (long) App.getConfig().get("bannedRole");
    tempBanRole = (long) App.getConfig().get("tempBanRole");
    if (user == null) {
      event.reply("Null option! Invalid!").queue();
      return;
    }

    long userId = user.getIdLong();

    // Find the user in the database
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Get user's infractions
    List<Infraction> infractions = infractionRepository.findByUser(dvUser.getId());

    if (infractions.isEmpty()) {
      event.reply(user.getName() + " has no recorded infractions.").queue();
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();

    // Update button IDs to avoid conflicts with select menus
    Button primary = Button.primary("infraction-expire-btn-" + userId, "Expire a warn");
    Button danger = Button.danger("infraction-remove-btn-" + userId, "Remove a warn");

    Button noBan = Button.secondary("setban-" + userId + "-none", "Set No Ban");
    Button tempBan = Button.secondary("setban-" + userId + "-temp", "Set Temp-Banned");
    Button fullBan = Button.secondary("setban-" + userId + "-full", "Set Banned");

    // Disable the button that matches the current ban status
    boolean isBanned = dvUser.getIs_banned() != null && dvUser.getIs_banned();
    boolean isSuspended = dvUser.getIs_suspended() != null && dvUser.getIs_suspended();

    if (isBanned) {
      fullBan = fullBan.asDisabled();
    } else if (isSuspended) {
      tempBan = tempBan.asDisabled();
    } else {
      noBan = noBan.asDisabled();
    }

    // Create dropdown for selecting an infraction to expire
    StringSelectMenu.Builder expireMenu = StringSelectMenu.create("infraction-expire-" + userId);
    expireMenu.setPlaceholder("Select an infraction to expire");

    // Create dropdown for selecting an infraction to remove
    StringSelectMenu.Builder removeMenu = StringSelectMenu.create("infraction-remove-" + userId);
    removeMenu.setPlaceholder("Select an infraction to remove");

    // Calculate total infraction value
    int totalActive = 0;
    int totalAll = 0;

    for (Infraction infraction : infractions) {
      String expire = "Valid";
      if (infraction.getExpired() != null && infraction.getExpired()) {
        expire = "Expired";
      } else {
        // Count active infractions value
        if (infraction.getValue() != null) {
          totalActive += infraction.getValue();
        }

        // Add to expire dropdown if not expired
        String shortId = infraction.getId().length() > 8 ? infraction.getId().substring(0, 8) : infraction.getId();
        String reason = infraction.getReason() != null ? infraction.getReason() : "No reason provided";
        String shortReason = reason.length() > 30 ? reason.substring(0, 27) + "..." : reason;

        expireMenu.addOption(
            "Value: " + (infraction.getValue() != null ? infraction.getValue() : "0"),
            infraction.getId(),
            shortReason);
      }

      // Add to remove dropdown (all infractions)
      String shortId = infraction.getId().length() > 8 ? infraction.getId().substring(0, 8) : infraction.getId();
      String reason = infraction.getReason() != null ? infraction.getReason() : "No reason provided";
      String shortReason = reason.length() > 30 ? reason.substring(0, 27) + "..." : reason;

      removeMenu.addOption(
          (infraction.getExpired() != null && infraction.getExpired() ? "[EXPIRED] " : "") +
              "Value: " + (infraction.getValue() != null ? infraction.getValue() : "0"),
          infraction.getId(),
          shortReason);

      // Count all infractions value
      if (infraction.getValue() != null) {
        totalAll += infraction.getValue();
      }

      // Format the created date
      String createdDate = "Unknown";
      if (infraction.getCreated() != null) {
        createdDate = formatDate(infraction.getCreated());
      }

      embed.addField(
          createdDate,
          "*Value: " + (infraction.getValue() != null ? infraction.getValue() : "0") +
              ", " + expire + "\n**Reason:** " +
              (infraction.getReason() != null ? infraction.getReason() : "No reason provided"),
          false);
    }

    embed
        .setTitle("Infractions")
        .setDescription("Infractions of " + user.getAsMention() + ":")
        .setAuthor(user.getName(), null, user.getAvatarUrl())
        .setFooter("The total value of valid infractions is " + totalActive + ".\n" +
            "The total value of all infractions is " + totalAll + ".");

    // Build response with appropriate components
    boolean hasActiveInfractions = totalActive > 0;

    // Only add expiration dropdown if there are valid infractions to expire
    if (hasActiveInfractions) {
      event.replyEmbeds(embed.build())
          .addActionRow(expireMenu.build())
          .addActionRow(removeMenu.build())
          .addActionRow(primary, danger)
          .addActionRow(noBan, tempBan, fullBan)
          .queue();
    } else {
      // Only add remove dropdown since no active infractions
      event.replyEmbeds(embed.build())
          .addActionRow(removeMenu.build())
          .addActionRow(danger)
          .addActionRow(noBan, tempBan, fullBan)
          .queue();
    }
  }

  @Override
  public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
    String componentId = event.getComponentId();

    // Handle both original and modal select menus
    if (componentId.startsWith("infraction-expire-modal-")) {
      handleExpireInfractionSelect(event, true);
    } else if (componentId.startsWith("infraction-remove-modal-")) {
      handleRemoveInfractionSelect(event, true);
    } else if (componentId.startsWith("infraction-expire-")) {
      handleExpireInfractionSelect(event, false);
    } else if (componentId.startsWith("infraction-remove-")) {
      handleRemoveInfractionSelect(event, false);
    }
  }

  private void handleExpireInfractionSelect(StringSelectInteractionEvent event, boolean isModal) {
    if (event.getValues().isEmpty()) {
      event.reply("No infraction selected.").setEphemeral(true).queue();
      return;
    }

    String infractionId = event.getValues().get(0);
    String componentId = event.getComponentId();

    // Extract userId based on whether this is from a modal or regular select menu
    long userId;
    if (isModal) {
      userId = Long.parseLong(componentId.substring("infraction-expire-modal-".length()));
    } else {
      userId = Long.parseLong(componentId.substring("infraction-expire-".length()));
    }

    try {
      // Find the infraction
      Optional<Infraction> infractionOpt = infractionRepository.findById(infractionId);
      if (!infractionOpt.isPresent()) {
        event.reply("That infraction no longer exists.").setEphemeral(true).queue();
        return;
      }

      Infraction infraction = infractionOpt.get();

      // Check if already expired
      if (infraction.getExpired() != null && infraction.getExpired()) {
        event.reply("This infraction is already expired.").setEphemeral(true).queue();
        return;
      }

      // Expire the infraction
      infraction.setExpired(true);
      infractionRepository.save(infraction);

      // Update the message
      event.reply("Infraction expired successfully.").queue();

      // Disable the component that was used
      event.getMessage().editMessageComponents(event.getMessage().getActionRows().get(0).asDisabled()).queue();

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error expiring infraction", e);
      event.reply("An error occurred while expiring the infraction: " + e.getMessage()).setEphemeral(true).queue();
    }
  }

  private void handleRemoveInfractionSelect(StringSelectInteractionEvent event, boolean isModal) {
    if (event.getValues().isEmpty()) {
      event.reply("No infraction selected.").setEphemeral(true).queue();
      return;
    }

    String infractionId = event.getValues().get(0);
    String componentId = event.getComponentId();

    // Extract userId based on whether this is from a modal or regular select menu
    long userId;
    if (isModal) {
      userId = Long.parseLong(componentId.substring("infraction-remove-modal-".length()));
    } else {
      userId = Long.parseLong(componentId.substring("infraction-remove-".length()));
    }

    try {
      // Find the infraction
      Optional<Infraction> infractionOpt = infractionRepository.findById(infractionId);
      if (!infractionOpt.isPresent()) {
        event.reply("That infraction no longer exists.").setEphemeral(true).queue();
        return;
      }

      // Delete the infraction
      infractionRepository.deleteById(infractionId);

      // Update the message
      event.reply("Infraction removed successfully.").queue();

      // Disable the component that was used
      event.getMessage().editMessageComponents(event.getMessage().getActionRows().get(0).asDisabled()).queue();

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error removing infraction", e);
      event.reply("An error occurred while removing the infraction: " + e.getMessage()).setEphemeral(true).queue();
    }
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String componentId = event.getComponentId();
    // TODO:uncommet this later
    // Ensure user has permission
    // if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
    // event.reply("You do not have permission to use this
    // feature.").setEphemeral(true).queue();
    // return;
    // }

    if (componentId.startsWith("setban-")) {
      String[] parts = componentId.split("-");
      if (parts.length >= 3) {
        long userId = Long.parseLong(parts[1]);
        String banType = parts[2];

        handleBanStatusChange(event, userId, banType);
      }
    } else if (componentId.startsWith("infraction-expire-btn-")) {
      // Update to match new button ID pattern
      long userId = Long.parseLong(componentId.substring("infraction-expire-btn-".length()));
      handleExpireWarningButton(event, userId);
    } else if (componentId.startsWith("infraction-remove-btn-")) {
      // Update to match new button ID pattern
      long userId = Long.parseLong(componentId.substring("infraction-remove-btn-".length()));
      handleRemoveWarningButton(event, userId);
    }
  }

  private void handleExpireWarningButton(ButtonInteractionEvent event, long userId) {
    // Find the user in the database
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Get active infractions
    List<Infraction> activeInfractions = infractionRepository.findActiveByUser(dvUser.getId());
    if (activeInfractions.isEmpty()) {
      event.reply("This user has no active infractions to expire.").setEphemeral(true).queue();
      return;
    }

    // Create selection menu for active infractions - use a different ID to avoid
    // conflicts
    StringSelectMenu.Builder menu = StringSelectMenu.create("infraction-expire-modal-" + userId);
    menu.setPlaceholder("Select an infraction to expire");

    for (Infraction infraction : activeInfractions) {
      String reason = infraction.getReason() != null ? infraction.getReason() : "No reason";
      String shortReason = reason.length() > 30 ? reason.substring(0, 27) + "..." : reason;
      int value = infraction.getValue() != null ? infraction.getValue() : 0;

      menu.addOption(
          "Value: " + value,
          infraction.getId(),
          shortReason);
    }

    event.reply("Select an infraction to expire:")
        .addActionRow(menu.build())
        .setEphemeral(true)
        .queue();
  }

  private void handleRemoveWarningButton(ButtonInteractionEvent event, long userId) {
    // Find the user in the database
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Get all infractions
    List<Infraction> infractions = infractionRepository.findByUser(dvUser.getId());
    if (infractions.isEmpty()) {
      event.reply("This user has no infractions to remove.").setEphemeral(true).queue();
      return;
    }

    // Create selection menu for all infractions - use a different ID to avoid
    // conflicts
    StringSelectMenu.Builder menu = StringSelectMenu.create("infraction-remove-modal-" + userId);
    menu.setPlaceholder("Select an infraction to remove");

    for (Infraction infraction : infractions) {
      String reason = infraction.getReason() != null ? infraction.getReason() : "No reason";
      String shortReason = reason.length() > 30 ? reason.substring(0, 27) + "..." : reason;
      int value = infraction.getValue() != null ? infraction.getValue() : 0;
      boolean expired = infraction.getExpired() != null && infraction.getExpired();

      menu.addOption(
          (expired ? "[EXPIRED] " : "") + "Value: " + value,
          infraction.getId(),
          shortReason);
    }

    event.reply("Select an infraction to remove:")
        .addActionRow(menu.build())
        .setEphemeral(true)
        .queue();
  }

  private void handleBanStatusChange(ButtonInteractionEvent event, long userId, String banType) {
    // Find the user
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Update ban status
    switch (banType) {
      case "none":
        dvUser.setIs_banned(false);
        dvUser.setIs_suspended(false);
        break;
      case "temp":
        dvUser.setIs_banned(false);
        dvUser.setIs_suspended(true);
        break;
      case "full":
        dvUser.setIs_banned(true);
        dvUser.setIs_suspended(false);
        break;
      default:
        event.reply("Invalid ban type.").setEphemeral(true).queue();
        return;
    }

    // Save user and Add Role
    try {
      userRepository.save(dvUser);

      // Get Discord user info for the response
      net.dv8tion.jda.api.entities.User discordUser = event.getJDA().retrieveUserById(userId).complete();
      String username = discordUser != null ? discordUser.getName() : "Unknown User";

      // Add or remove roles based on ban type
      net.dv8tion.jda.api.entities.Member member = event.getGuild().retrieveMemberById(userId).complete();
      if (member != null) {
        try {
          // Get role objects with proper error checking
          Role bannedRoleObj = null;
          Role tempBanRoleObj = null;

          if (bannedRole != 0) {
            bannedRoleObj = event.getGuild().getRoleById(bannedRole);
            if (bannedRoleObj == null) {
              LOGGER.warning("Banned role with ID " + bannedRole + " not found in guild");
            }
          }

          if (tempBanRole != 0) {
            tempBanRoleObj = event.getGuild().getRoleById(tempBanRole);
            if (tempBanRoleObj == null) {
              LOGGER.warning("Temp ban role with ID " + tempBanRole + " not found in guild");
            }
          }

          switch (banType) {
            case "none":
              if (bannedRoleObj != null && member.getRoles().contains(bannedRoleObj)) {
                event.getGuild().removeRoleFromMember(member, bannedRoleObj).queue(
                    success -> LOGGER.info("Removed banned role from " + username),
                    error -> LOGGER.warning("Failed to remove banned role: " + error.getMessage()));
              }

              if (tempBanRoleObj != null && member.getRoles().contains(tempBanRoleObj)) {
                event.getGuild().removeRoleFromMember(member, tempBanRoleObj).queue(
                    success -> LOGGER.info("Removed temp ban role from " + username),
                    error -> LOGGER.warning("Failed to remove temp ban role: " + error.getMessage()));
              }
              break;

            case "temp":
              if (tempBanRoleObj != null && !member.getRoles().contains(tempBanRoleObj)) {
                event.getGuild().addRoleToMember(member, tempBanRoleObj).queue(
                    success -> LOGGER.info("Added temp ban role to " + username),
                    error -> LOGGER.warning("Failed to add temp ban role: " + error.getMessage()));
              }

              if (bannedRoleObj != null && member.getRoles().contains(bannedRoleObj)) {
                event.getGuild().removeRoleFromMember(member, bannedRoleObj).queue(
                    success -> LOGGER.info("Removed banned role from " + username),
                    error -> LOGGER.warning("Failed to remove banned role: " + error.getMessage()));
              }
              break;

            case "full":
              if (bannedRoleObj != null && !member.getRoles().contains(bannedRoleObj)) {
                event.getGuild().addRoleToMember(member, bannedRoleObj).queue(
                    success -> LOGGER.info("Added banned role to " + username),
                    error -> LOGGER.warning("Failed to add banned role: " + error.getMessage()));
              }

              if (tempBanRoleObj != null && member.getRoles().contains(tempBanRoleObj)) {
                event.getGuild().removeRoleFromMember(member, tempBanRoleObj).queue(
                    success -> LOGGER.info("Removed temp ban role from " + username),
                    error -> LOGGER.warning("Failed to remove temp ban role: " + error.getMessage()));
              }
              break;
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error managing roles", e);
        }
      } else {
        LOGGER.warning("Could not find member with ID " + userId + " in guild");
      }

      // Prepare status message
      String statusMessage;
      if (banType.equals("none")) {
        statusMessage = username + " is now unbanned.";
      } else if (banType.equals("temp")) {
        statusMessage = username + " is now temporarily banned.";
      } else {
        statusMessage = username + " is now permanently banned.";
      }

      event.reply(statusMessage).queue();

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error updating ban status", e);
      event.reply("An error occurred while updating ban status: " + e.getMessage()).setEphemeral(true).queue();
    }
  }

  private String formatDate(OffsetDateTime dateTime) {
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
  }
}
