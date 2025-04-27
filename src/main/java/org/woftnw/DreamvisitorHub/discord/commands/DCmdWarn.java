package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.InfractionRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Infraction;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdWarn implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdWarn.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final InfractionRepository infractionRepository = App.getInfractionRepository();

  // Define role IDs for ban/temp ban roles
  private long BANNED_ROLE_ID = 1121851264741933138L;
  private long TEMP_BAN_ROLE_ID = 1121851513564835900L;

  // Warning threshold for automatic ban
  private static final int BAN_THRESHOLD = 10;
  private static final int TEMP_BAN_THRESHOLD = 5;

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("warn", "Warn a user for breaking rules.")
        .addOption(OptionType.USER, "user", "The user to warn.", true)
        .addOption(OptionType.INTEGER, "value", "The severity of the warning.", true)
        .addOption(OptionType.STRING, "reason", "The reason for the warning.", true)
        .addOption(OptionType.BOOLEAN, "send-warning", "Whether to send a DM to the user. Defaults to true.", false)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    // Try to parse role IDs from config with error checking
    try {
      BANNED_ROLE_ID = (long) App.getConfig().getOrDefault("bannedRole", BANNED_ROLE_ID);
      TEMP_BAN_ROLE_ID = (long) App.getConfig().getOrDefault("tempBanRole", TEMP_BAN_ROLE_ID);
      LOGGER.info("Using ban role ID: " + BANNED_ROLE_ID + ", temp ban role ID: " + TEMP_BAN_ROLE_ID);
    } catch (Exception e) {
      LOGGER.warning("Failed to get role IDs from config: " + e.getMessage());
    }

    // Verify bot has permission to assign roles
    if (!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
      event.reply("I don't have permission to assign roles. Please grant the bot the 'Manage Roles' permission.")
          .setEphemeral(true).queue();
      return;
    }

    // Get command options
    User targetUser = event.getOption("user", OptionMapping::getAsUser);
    Integer value = event.getOption("value", OptionMapping::getAsInt);
    String reason = event.getOption("reason", OptionMapping::getAsString);
    boolean sendWarning = event.getOption("send-warning", true, OptionMapping::getAsBoolean);

    if (targetUser == null || value == null || reason == null) {
      event.reply("Missing required options.").setEphemeral(true).queue();
      return;
    }

    // Validate input
    if (value <= 0) {
      event.reply("The warning value must be positive.").setEphemeral(true).queue();
      return;
    }

    // Find user in database
    Optional<DVUser> targetUserOpt = userRepository.findBySnowflakeId(targetUser.getIdLong());

    if (!targetUserOpt.isPresent()) {
      event.reply("That user does not have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = targetUserOpt.get();

    // Create new infraction
    Infraction infraction = new Infraction();
    infraction.setReason(reason);
    infraction.setValue(value);
    infraction.setSend_warning(sendWarning);
    infraction.setExpired(false);
    infraction.setUser(dvUser.getId());

    try {
      // Save infraction to database
      infractionRepository.save(infraction);
      LOGGER.info("Saved infraction for user " + targetUser.getId() + " with value " + value);

      // Get total active infractions value
      List<Infraction> activeInfractions = infractionRepository.findActiveByUser(dvUser.getId());
      int totalInfractionValue = 0;

      for (Infraction activeInfraction : activeInfractions) {
        if (activeInfraction.getValue() != null) {
          totalInfractionValue += activeInfraction.getValue();
        }
      }
      LOGGER.info("User " + targetUser.getId() + " has " + totalInfractionValue + " total active infraction points");

      // Check if thresholds are exceeded and update ban status
      boolean wasBanned = dvUser.getIs_banned() != null && dvUser.getIs_banned();
      boolean wasTempBanned = dvUser.getIs_suspended() != null && dvUser.getIs_suspended();

      boolean shouldBeBanned = totalInfractionValue >= BAN_THRESHOLD;
      boolean shouldBeTempBanned = totalInfractionValue >= TEMP_BAN_THRESHOLD;

      StringBuilder specialActionMsg = new StringBuilder();

      // Get member with a more robust method that attempts REST retrieval
      Member targetMember = null;
      try {
        // First try to get from cache
        targetMember = event.getGuild().getMember(targetUser);
        LOGGER.info("Attempt to get member from cache: " + (targetMember != null ? "SUCCESS" : "FAILED"));

        // If not in cache, try REST API
        if (targetMember == null) {
          LOGGER.info("Member not found in cache for " + targetUser.getName() + ", trying REST API...");
          try {
            targetMember = event.getGuild().retrieveMemberById(targetUser.getId()).complete();
            LOGGER.info("REST API member retrieval result: " + (targetMember != null ? "SUCCESS" : "FAILED"));
          } catch (Exception restEx) {
            LOGGER.severe("REST API retrieval exception: " + restEx.getMessage());
            restEx.printStackTrace();
          }
        }
      } catch (Exception e) {
        LOGGER.severe("Failed to retrieve member for user " + targetUser.getName() + ": " + e.getMessage());
        e.printStackTrace();
      }

      // Log detailed information about the member if found
      if (targetMember != null) {
        LOGGER.info("Found member: " + targetMember.getEffectiveName() +
            ", ID: " + targetMember.getId() +
            ", Guild: " + targetMember.getGuild().getName() +
            ", Roles count: " + targetMember.getRoles().size());
      } else {
        LOGGER.warning("Member is NULL after both cache and REST attempts for user " + targetUser.getName());
        specialActionMsg.append("⚠️ Could not find this user in the server. They may have left. ");
      }

      // Update user's ban status if needed
      if (shouldBeBanned && !wasBanned) {
        dvUser.setIs_banned(true);
        dvUser.setIs_suspended(false);
        userRepository.save(dvUser);

        if (targetMember != null) {
          try {
            // Get role objects with proper error checking
            LOGGER.info("User " + targetUser.getName() + " should be FULLY banned - attempting to add role");
            Role bannedRole = null;
            Role tempBanRole = null;

            if (BANNED_ROLE_ID != 0) {
              bannedRole = event.getGuild().getRoleById(BANNED_ROLE_ID);
              if (bannedRole == null) {
                LOGGER.severe("BANNED ROLE IS NULL for ID: " + BANNED_ROLE_ID);
                specialActionMsg.append("\n⚠️ Could not find banned role with ID ").append(BANNED_ROLE_ID);
              } else {
                LOGGER.info("Found banned role: " + bannedRole.getName() + " (ID: " + bannedRole.getId() + ")");
                if (!event.getGuild().getSelfMember().canInteract(bannedRole)) {
                  LOGGER.severe("BOT CANNOT INTERACT WITH BANNED ROLE - role hierarchy issue");
                  specialActionMsg.append("\n⚠️ Cannot assign banned role due to role hierarchy");
                }
              }
            } else {
              LOGGER.severe("BANNED ROLE ID IS ZERO");
              specialActionMsg.append("\n⚠️ Banned role ID is not configured properly");
            }

            if (TEMP_BAN_ROLE_ID != 0) {
              tempBanRole = event.getGuild().getRoleById(TEMP_BAN_ROLE_ID);
              if (tempBanRole == null) {
                LOGGER.warning("Temp ban role with ID " + TEMP_BAN_ROLE_ID + " not found in guild");
              } else {
                LOGGER.info("Found temp ban role: " + tempBanRole.getName() + " (ID: " + tempBanRole.getId() + ")");
              }
            }

            // Check guild, member and bot permissions
            LOGGER.info("Guild has " + event.getGuild().getRoles().size() + " roles");
            LOGGER.info("Bot has manage roles permission: "
                + event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES));
            LOGGER.info("Target member has " + targetMember.getRoles().size() + " roles");

            // Check if the member already has the role
            boolean hasRole = bannedRole != null && targetMember.getRoles().contains(bannedRole);
            LOGGER.info("Member already has banned role: " + hasRole);

            // Add banned role if not already present - use complete() for synchronous role
            // management
            if (bannedRole != null && !targetMember.getRoles().contains(bannedRole) &&
                event.getGuild().getSelfMember().canInteract(bannedRole)) {

              LOGGER.info("Attempting to add banned role " + bannedRole.getName() + " to " + targetUser.getName());
              try {
                // Try synchronous role assignment for easier debugging
                event.getGuild().addRoleToMember(targetMember, bannedRole).complete();
                LOGGER.info("Successfully added banned role to " + targetUser.getName());
              } catch (Exception roleEx) {
                LOGGER.severe("ROLE ADDITION FAILED: " + roleEx.getMessage());
                specialActionMsg.append("\n⚠️ Failed to add banned role: ").append(roleEx.getMessage());
              }
            }

            // Remove temp ban role if present
            if (tempBanRole != null && targetMember.getRoles().contains(tempBanRole) &&
                event.getGuild().getSelfMember().canInteract(tempBanRole)) {
              try {
                event.getGuild().removeRoleFromMember(targetMember, tempBanRole).complete();
                LOGGER.info("Successfully removed temp ban role from " + targetUser.getName());
              } catch (Exception roleEx) {
                LOGGER.severe("TEMP ROLE REMOVAL FAILED: " + roleEx.getMessage());
              }
            }

            specialActionMsg.append("User now has ").append(totalInfractionValue)
                .append(" active infraction points and has been **banned**.");
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error managing roles for ban", e);
            specialActionMsg.append("User now has ").append(totalInfractionValue)
                .append(" active infraction points and should be **banned**, but role assignment failed.");
          }
        } else {
          LOGGER.info("User " + targetUser.getName() + " is not in the guild, can't assign banned role");
          specialActionMsg.append("User now has ").append(totalInfractionValue)
              .append(" active infraction points and should be **banned**, but they are not in the server.");
        }
      } else if (shouldBeTempBanned && !wasTempBanned && !shouldBeBanned) {
        dvUser.setIs_banned(false);
        dvUser.setIs_suspended(true);
        userRepository.save(dvUser);

        if (targetMember != null) {
          try {
            // Get role objects with proper error checking
            LOGGER.info("User " + targetUser.getName() + " should be TEMP banned - attempting to add role");
            Role bannedRole = null;
            Role tempBanRole = null;

            if (BANNED_ROLE_ID != 0) {
              bannedRole = event.getGuild().getRoleById(BANNED_ROLE_ID);
              if (bannedRole == null) {
                LOGGER.warning("Banned role with ID " + BANNED_ROLE_ID + " not found in guild");
              }
            }

            if (TEMP_BAN_ROLE_ID != 0) {
              tempBanRole = event.getGuild().getRoleById(TEMP_BAN_ROLE_ID);
              if (tempBanRole == null) {
                LOGGER.severe("TEMP BAN ROLE IS NULL for ID: " + TEMP_BAN_ROLE_ID);
                specialActionMsg.append("\n⚠️ Could not find temp ban role with ID ").append(TEMP_BAN_ROLE_ID);
              } else {
                LOGGER.info("Found temp ban role: " + tempBanRole.getName() + " (ID: " + tempBanRole.getId() + ")");
                if (!event.getGuild().getSelfMember().canInteract(tempBanRole)) {
                  LOGGER.severe("BOT CANNOT INTERACT WITH TEMP BAN ROLE - role hierarchy issue");
                  specialActionMsg.append("\n⚠️ Cannot assign temp ban role due to role hierarchy");
                }
              }
            } else {
              LOGGER.severe("TEMP BAN ROLE ID IS ZERO");
              specialActionMsg.append("\n⚠️ Temp ban role ID is not configured properly");
            }

            // Add temp ban role if not already present - use complete() for synchronous
            // role management
            if (tempBanRole != null && !targetMember.getRoles().contains(tempBanRole) &&
                event.getGuild().getSelfMember().canInteract(tempBanRole)) {

              LOGGER.info("Attempting to add temp ban role " + tempBanRole.getName() + " to " + targetUser.getName());
              try {
                // Try synchronous role assignment for easier debugging
                event.getGuild().addRoleToMember(targetMember, tempBanRole).complete();
                LOGGER.info("Successfully added temp ban role to " + targetUser.getName());
              } catch (Exception roleEx) {
                LOGGER.severe("TEMP ROLE ADDITION FAILED: " + roleEx.getMessage());
                specialActionMsg.append("\n⚠️ Failed to add temp ban role: ").append(roleEx.getMessage());
              }
            }

            specialActionMsg.append("User now has ").append(totalInfractionValue)
                .append(" active infraction points and has been **temporarily banned**.");
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error managing roles for temp ban", e);
            specialActionMsg.append("User now has ").append(totalInfractionValue)
                .append(" active infraction points and should be **temporarily banned**, but role assignment failed.");
          }
        } else {
          LOGGER.info("User " + targetUser.getName() + " is not in the guild, can't assign temp ban role");
          specialActionMsg.append("User now has ").append(totalInfractionValue)
              .append(
                  " active infraction points and should be **temporarily banned**, but they are not in the server.");
        }
      } else {
        specialActionMsg.append("User now has ").append(totalInfractionValue).append(" active infraction points.");
      }

      // Inform moderator
      EmbedBuilder embed = new EmbedBuilder()
          .setTitle("Warning Issued")
          .setDescription("User " + targetUser.getAsMention() + " has been warned.")
          .addField("Reason", reason, false)
          .addField("Value", value.toString(), true)
          .addField("Sent DM", sendWarning ? "Yes" : "No", true)
          .addField("Status", specialActionMsg.toString(), false)
          .setFooter("Warning issued by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
          .setTimestamp(Instant.now())
          .setColor(Color.ORANGE);

      event.replyEmbeds(embed.build()).queue();

      // Send warning DM if enabled
      if (sendWarning) {
        sendWarningDM(targetUser, reason, value, totalInfractionValue, shouldBeTempBanned, shouldBeBanned);
      }

      // Log warning to server logs
      logWarning(event.getUser(), targetUser, reason, value, totalInfractionValue);

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error issuing warning", e);
      event.reply("An error occurred while processing the warning. Please try again later.").setEphemeral(true).queue();
    }
  }

  /**
   * Sends a warning DM to the user
   */
  private void sendWarningDM(User targetUser, String reason, int value, int totalValue, boolean isTempBanned,
      boolean isBanned) {
    EmbedBuilder dmEmbed = new EmbedBuilder()
        .setTitle("Warning Received")
        .setDescription("You have received a warning on the DreamvisitorHub server.")
        .addField("Reason", reason, false)
        .addField("Value", Integer.toString(value), true)
        .addField("Total Active Points", Integer.toString(totalValue), true)
        .setColor(Color.RED)
        .setTimestamp(Instant.now());

    if (isBanned) {
      dmEmbed.addField("Status", "You have been banned from the server due to accumulating too many warning points.",
          false);
    } else if (isTempBanned) {
      dmEmbed.addField("Status", "You have been temporarily banned from the server due to accumulating warning points.",
          false);
    } else {
      dmEmbed.addField("Thresholds",
          "Temporary ban: " + TEMP_BAN_THRESHOLD + " points\nPermanent ban: " + BAN_THRESHOLD + " points", false);
    }

    targetUser.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(dmEmbed.build()).queue(
        success -> {
        },
        error -> LOGGER.log(Level.WARNING, "Could not send warning DM to user " + targetUser.getId(), error)));
  }

  /**
   * Logs the warning to the server log channel
   */
  private void logWarning(User moderator, User targetUser, String reason, int value, int totalValue) {
    EmbedBuilder logEmbed = new EmbedBuilder()
        .setTitle("User Warned")
        .setDescription(moderator.getAsMention() + " warned " + targetUser.getAsMention())
        .addField("Reason", reason, false)
        .addField("Value", Integer.toString(value), true)
        .addField("Total Active Points", Integer.toString(totalValue), true)
        .setFooter("User ID: " + targetUser.getId())
        .setTimestamp(Instant.now())
        .setColor(Color.ORANGE);

    // Send to log channel if exists
    if (Bot.getGameLogChannel() != null) {
      Bot.getGameLogChannel().sendMessageEmbeds(logEmbed.build()).queue();
    }
  }

  /**
   * Notifies server admin of role-related issues
   */
  private void notifyAdminOfRoleIssue(Guild guild, String action, String errorMessage) {
    try {
      User owner = guild.getOwner() != null ? guild.getOwner().getUser() : guild.retrieveOwner().complete().getUser();

      if (owner != null) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Role Permission Issue")
            .setDescription("The bot couldn't " + action + " in " + guild.getName() + ".")
            .addField("Error", errorMessage, false)
            .addField("Fix",
                "Please ensure the bot's role is above the roles it needs to manage and that it has the 'Manage Roles' permission.",
                false)
            .setColor(Color.RED);

        owner.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(embed.build()).queue(
            success -> LOGGER.info("Sent role issue notification to server owner"),
            error -> LOGGER.warning("Could not DM server owner about role issue")));
      }
    } catch (Exception e) {
      LOGGER.warning("Failed to notify admin of role issue: " + e.getMessage());
    }
  }
}
