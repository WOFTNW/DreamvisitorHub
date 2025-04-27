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
import org.woftnw.DreamvisitorHub.data.repository.AltRepository;
import org.woftnw.DreamvisitorHub.data.repository.InfractionRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.Alt;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Infraction;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdWarn implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdWarn.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final InfractionRepository infractionRepository = App.getInfractionRepository();
  private final AltRepository altRepository = App.getAltRepository(); // Add AltRepository

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

    // Defer reply as this might take some time
    event.deferReply().queue();

    // Check if the user is an alt and get the main account if they are
    long userId = targetUser.getIdLong();
    User effectiveUser = targetUser;
    DVUser mainUser = null;
    boolean isAlt = false;
    String altInfo = null;

    // Check if the user is an alt account
    Optional<Alt> altOpt = altRepository.findBySnowflakeId(userId);
    if (altOpt.isPresent()) {
      // This user is an alt - get their parent
      Alt alt = altOpt.get();
      String parentId = alt.getParent();
      isAlt = true;

      if (parentId != null) {
        // Get the parent user
        Optional<DVUser> parentUserOpt = userRepository.findById(parentId);
        if (parentUserOpt.isPresent()) {
          mainUser = parentUserOpt.get();
          LOGGER.info("Found parent account: " + parentId + " for alt user: " + userId);

          // Get parent's Discord user info if available
          if (mainUser.getSnowflakeId() != null) {
            try {
              User parentDiscordUser = Bot.getJda().retrieveUserById(mainUser.getSnowflakeId()).complete();
              if (parentDiscordUser != null) {
                effectiveUser = parentDiscordUser;
                altInfo = "Warning applied to main account **" + effectiveUser.getName() +
                    "** instead of alt account **" + targetUser.getName() + "**";
                LOGGER.info("Using parent account for warning: " + effectiveUser.getName());
              }
            } catch (Exception e) {
              LOGGER.log(Level.WARNING, "Could not retrieve parent user", e);
            }
          }
        }
      }
    }

    // If user isn't an alt or we couldn't find their parent, find or create their
    // profile
    if (mainUser == null) {
      // Find user in database or create if not exists
      Optional<DVUser> targetUserOpt = userRepository.findBySnowflakeId(userId);
      if (!targetUserOpt.isPresent()) {
        // Create a new user profile
        mainUser = new DVUser();
        mainUser.setDiscord_id(targetUser.getId());
        mainUser.setDiscord_username(targetUser.getName());
        mainUser.setSnowflakeId(targetUser.getIdLong());
        mainUser.setDiscord_img(targetUser.getEffectiveAvatarUrl());
        mainUser = userRepository.save(mainUser);
        LOGGER.info("Created new user profile for: " + targetUser.getName());
      } else {
        mainUser = targetUserOpt.get();
      }
    }

    // Create new infraction
    Infraction infraction = new Infraction();
    infraction.setReason(reason + (isAlt ? "\n[Warning issued to alt account: " + targetUser.getName() + "]" : ""));
    infraction.setValue(value);
    infraction.setSend_warning(sendWarning);
    infraction.setExpired(false);
    infraction.setUser(mainUser.getId());

    try {
      // Save infraction to database
      infractionRepository.save(infraction);
      LOGGER.info("Saved infraction for user " + effectiveUser.getId() + " with value " + value);

      // Get total active infractions value
      List<Infraction> activeInfractions = infractionRepository.findActiveByUser(mainUser.getId());
      int totalInfractionValue = 0;

      for (Infraction activeInfraction : activeInfractions) {
        if (activeInfraction.getValue() != null) {
          totalInfractionValue += activeInfraction.getValue();
        }
      }
      LOGGER.info("User " + effectiveUser.getId() + " has " + totalInfractionValue + " total active infraction warns");

      // Check if thresholds are exceeded and update ban status
      boolean wasBanned = mainUser.getIs_banned() != null && mainUser.getIs_banned();
      boolean wasTempBanned = mainUser.getIs_suspended() != null && mainUser.getIs_suspended();

      boolean shouldBeBanned = totalInfractionValue >= BAN_THRESHOLD;
      boolean shouldBeTempBanned = totalInfractionValue >= TEMP_BAN_THRESHOLD;

      StringBuilder specialActionMsg = new StringBuilder();

      // Process ban status changes and apply to all related accounts
      if ((shouldBeBanned && !wasBanned) || (shouldBeTempBanned && !wasTempBanned && !shouldBeBanned)) {
        // Get all related accounts (main + alts)
        Set<DVUser> accountsToUpdate = new HashSet<>();
        Set<Long> discordIdsToUpdate = new HashSet<>();

        // Add the main account
        accountsToUpdate.add(mainUser);
        if (mainUser.getSnowflakeId() != null) {
          discordIdsToUpdate.add(mainUser.getSnowflakeId());
        }

        // Find all alts and add them
        List<Alt> altAccounts = altRepository.findByParentId(mainUser.getId());
        LOGGER.info("Found " + altAccounts.size() + " alt accounts for main account: " + mainUser.getId());

        for (Alt alt : altAccounts) {
          if (alt.getSnowflakeId() != null) {
            discordIdsToUpdate.add(alt.getSnowflakeId());

            // If the alt has a profile, add it to accounts to update
            Optional<DVUser> altUserOpt = userRepository.findBySnowflakeId(alt.getSnowflakeId());
            altUserOpt.ifPresent(accountsToUpdate::add);
          }
        }

        LOGGER.info("Will update ban status for " + accountsToUpdate.size() + " database accounts and " +
            discordIdsToUpdate.size() + " Discord accounts");

        // Update all accounts with appropriate ban status
        if (shouldBeBanned) {
          // Full ban for all accounts
          for (DVUser user : accountsToUpdate) {
            user.setIs_banned(true);
            user.setIs_suspended(false);
            userRepository.save(user);
          }

          // Apply roles to all accounts in the guild
          applyRolesToAccounts(event.getGuild(), discordIdsToUpdate, "full", specialActionMsg);

          specialActionMsg.append("User now has ").append(totalInfractionValue)
              .append(" active infraction warns and ").append(accountsToUpdate.size())
              .append(" accounts have been **banned**.");
        } else if (shouldBeTempBanned) {
          // Temp ban for all accounts
          for (DVUser user : accountsToUpdate) {
            user.setIs_banned(false);
            user.setIs_suspended(true);
            userRepository.save(user);
          }

          // Apply roles to all accounts in the guild
          applyRolesToAccounts(event.getGuild(), discordIdsToUpdate, "temp", specialActionMsg);

          specialActionMsg.append("User now has ").append(totalInfractionValue)
              .append(" active infraction warns and ").append(accountsToUpdate.size())
              .append(" accounts have been **temporarily banned**.");
        }
      } else {
        specialActionMsg.append("User now has ").append(totalInfractionValue).append(" active infraction warns.");
      }

      // Inform moderator
      EmbedBuilder embed = new EmbedBuilder()
          .setTitle("Warning Issued")
          .setDescription("User " + effectiveUser.getAsMention() + " has been warned.")
          .addField("Reason", reason, false)
          .addField("Value", value.toString(), true)
          .addField("Sent DM", sendWarning ? "Yes" : "No", true)
          .addField("Status", specialActionMsg.toString(), false)
          .setFooter("Warning issued by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
          .setTimestamp(Instant.now())
          .setColor(Color.ORANGE);

      // Add note if this was an alt account
      if (isAlt && altInfo != null) {
        embed.addField("Alt Account", altInfo, false);
      }

      event.getHook().sendMessageEmbeds(embed.build()).queue();

      // Send warning DM if enabled
      if (sendWarning) {
        sendWarningDM(effectiveUser, reason, value, totalInfractionValue, shouldBeTempBanned, shouldBeBanned);
      }

      // Log warning to server logs
      logWarning(event.getUser(), effectiveUser, reason, value, totalInfractionValue, isAlt ? targetUser : null);

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error issuing warning", e);
      event.getHook().sendMessage("An error occurred while processing the warning: " + e.getMessage()).queue();
    }
  }

  /**
   * Apply roles to multiple Discord accounts
   */
  private void applyRolesToAccounts(Guild guild, Set<Long> discordIds, String banType, StringBuilder statusMsg) {
    if (guild == null) {
      LOGGER.warning("Guild is null, cannot apply roles");
      statusMsg.append("\n⚠️ Guild is null, cannot apply roles");
      return;
    }

    // Get role objects
    Role bannedRoleObj = null;
    Role tempBanRoleObj = null;

    if (BANNED_ROLE_ID != 0) {
      bannedRoleObj = guild.getRoleById(BANNED_ROLE_ID);
      if (bannedRoleObj == null) {
        LOGGER.warning("Banned role with ID " + BANNED_ROLE_ID + " not found in guild");
        statusMsg.append("\n⚠️ Could not find banned role with ID ").append(BANNED_ROLE_ID);
      }
    }

    if (TEMP_BAN_ROLE_ID != 0) {
      tempBanRoleObj = guild.getRoleById(TEMP_BAN_ROLE_ID);
      if (tempBanRoleObj == null) {
        LOGGER.warning("Temp ban role with ID " + TEMP_BAN_ROLE_ID + " not found in guild");
        statusMsg.append("\n⚠️ Could not find temp ban role with ID ").append(TEMP_BAN_ROLE_ID);
      }
    }

    int successCount = 0;
    int failCount = 0;

    // Process each Discord ID
    for (Long discordId : discordIds) {
      try {
        // Retrieve the member
        Member member = guild.retrieveMemberById(discordId).complete();
        if (member == null) {
          LOGGER.info("Member with ID " + discordId + " not found in guild");
          continue;
        }

        String username = member.getUser().getName();
        LOGGER.info("Processing roles for member: " + username);

        boolean success = false;

        switch (banType) {
          case "full":
            if (bannedRoleObj != null && !member.getRoles().contains(bannedRoleObj) &&
                guild.getSelfMember().canInteract(bannedRoleObj)) {
              try {
                guild.addRoleToMember(member, bannedRoleObj).complete();
                LOGGER.info("Added banned role to " + username);
                success = true;
              } catch (Exception e) {
                LOGGER.severe("Failed to add banned role: " + e.getMessage());
                failCount++;
              }
            }

            if (tempBanRoleObj != null && member.getRoles().contains(tempBanRoleObj) &&
                guild.getSelfMember().canInteract(tempBanRoleObj)) {
              try {
                guild.removeRoleFromMember(member, tempBanRoleObj).complete();
                LOGGER.info("Removed temp ban role from " + username);
              } catch (Exception e) {
                LOGGER.warning("Failed to remove temp ban role: " + e.getMessage());
              }
            }
            break;

          case "temp":
            if (tempBanRoleObj != null && !member.getRoles().contains(tempBanRoleObj) &&
                guild.getSelfMember().canInteract(tempBanRoleObj)) {
              try {
                guild.addRoleToMember(member, tempBanRoleObj).complete();
                LOGGER.info("Added temp ban role to " + username);
                success = true;
              } catch (Exception e) {
                LOGGER.severe("Failed to add temp ban role: " + e.getMessage());
                failCount++;
              }
            }

            if (bannedRoleObj != null && member.getRoles().contains(bannedRoleObj) &&
                guild.getSelfMember().canInteract(bannedRoleObj)) {
              try {
                guild.removeRoleFromMember(member, bannedRoleObj).complete();
                LOGGER.info("Removed banned role from " + username);
              } catch (Exception e) {
                LOGGER.warning("Failed to remove banned role: " + e.getMessage());
              }
            }
            break;
        }

        if (success) {
          successCount++;
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error processing roles for Discord ID: " + discordId, e);
        failCount++;
      }
    }

    // Add summary to status message
    if (successCount > 0) {
      statusMsg.append("\nSuccessfully applied roles to ").append(successCount).append(" accounts.");
    }
    if (failCount > 0) {
      statusMsg.append("\n⚠️ Failed to apply roles to ").append(failCount).append(" accounts.");
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
        .addField("Total Active Warns", Integer.toString(totalValue), true)
        .setColor(Color.RED)
        .setTimestamp(Instant.now());

    if (isBanned) {
      dmEmbed.addField("Status", "You have been banned from the server due to accumulating too many warns.",
          false);
    } else if (isTempBanned) {
      dmEmbed.addField("Status", "You have been temporarily banned from the server due to accumulating warns.",
          false);
    } else {
      dmEmbed.addField("Thresholds",
          "Temporary ban: " + TEMP_BAN_THRESHOLD + " warns\nPermanent ban: " + BAN_THRESHOLD + " warns", false);
    }

    targetUser.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(dmEmbed.build()).queue(
        success -> {
        },
        error -> LOGGER.log(Level.WARNING, "Could not send warning DM to user " + targetUser.getId(), error)));
  }

  /**
   * Logs the warning to the server log channel
   */
  private void logWarning(User moderator, User targetUser, String reason, int value, int totalValue, User altUser) {
    EmbedBuilder logEmbed = new EmbedBuilder()
        .setTitle("User Warned")
        .setDescription(moderator.getAsMention() + " warned " + targetUser.getAsMention())
        .addField("Reason", reason, false)
        .addField("Value", Integer.toString(value), true)
        .addField("Total Active Warns", Integer.toString(totalValue), true)
        .setFooter("User ID: " + targetUser.getId())
        .setTimestamp(Instant.now())
        .setColor(Color.ORANGE);

    // Add note if this was redirected from an alt
    if (altUser != null) {
      logEmbed.addField("Alt Account", "Warning was issued to " + altUser.getAsMention() +
          " but applied to main account", false);
    }

    // Send to log channel if exists
    if (Bot.getGameLogChannel() != null) {
      Bot.getGameLogChannel().sendMessageEmbeds(logEmbed.build()).queue();
    }
  }
}
