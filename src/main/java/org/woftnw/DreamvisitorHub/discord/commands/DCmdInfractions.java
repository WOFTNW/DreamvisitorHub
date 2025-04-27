package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
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
import org.woftnw.DreamvisitorHub.data.repository.AltRepository;
import org.woftnw.DreamvisitorHub.data.repository.InfractionRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.Alt;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Infraction;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdInfractions extends ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdInfractions.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final InfractionRepository infractionRepository = App.getInfractionRepository();
  private final AltRepository altRepository = App.getAltRepository();
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

    // Defer reply immediately to avoid timeout and double reply issues
    event.deferReply().queue();

    long userId = user.getIdLong();

    // First check if this user is an alt account, before looking for their profile
    Optional<Alt> altOpt = altRepository.findBySnowflakeId(userId);
    boolean isAlt = false;
    String parentInfo = null;
    DVUser parentUser = null;
    User parentDiscordUser = null;

    if (altOpt.isPresent()) {
      // This user is an alt - get their parent
      Alt alt = altOpt.get();
      String parentId = alt.getParent();

      if (parentId != null) {
        // Get the parent user
        Optional<DVUser> parentUserOpt = userRepository.findById(parentId);
        if (parentUserOpt.isPresent()) {
          parentUser = parentUserOpt.get();
          isAlt = true;

          // Get parent user's Discord info if available
          String parentName = parentUser.getDiscord_username();

          if (parentUser.getSnowflakeId() != null) {
            try {
              parentDiscordUser = Bot.getJda().retrieveUserById(parentUser.getSnowflakeId()).complete();
              if (parentDiscordUser != null) {
                parentName = parentDiscordUser.getName();
              }
            } catch (Exception e) {
              LOGGER.log(Level.WARNING, "Could not retrieve parent user", e);
            }
          }

          parentInfo = "This is an alt account of **" + parentName + "**. Showing main account infractions.";
        }
      }
    }

    // If we're dealing with an alt, use parent user's data
    if (isAlt && parentUser != null) {
      User displayUser = parentDiscordUser != null ? parentDiscordUser : user;
      handleInfractions(event, parentUser, displayUser, true, parentInfo);
      return;
    }

    // If not an alt, find the user in the database
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
    if (!userOpt.isPresent()) {
      event.getHook().sendMessage("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Handle a regular user's infractions
    handleInfractions(event, dvUser, user, false, null);
  }

  // Extract the infraction handling logic to a separate method
  private void handleInfractions(@NotNull SlashCommandInteractionEvent event, DVUser dvUser, User discordUser,
      boolean isAltParent, String altInfo) {
    long userId = discordUser.getIdLong();

    // Get user's infractions
    List<Infraction> infractions = infractionRepository.findByUser(dvUser.getId());

    if (infractions.isEmpty()) {
      event.getHook().sendMessage(discordUser.getName() + " has no recorded infractions.").queue();
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
        // String shortId = infraction.getId().length() > 8 ? infraction.getId().substring(0, 8) : infraction.getId();
        String reason = infraction.getReason() != null ? infraction.getReason() : "No reason provided";
        String shortReason = reason.length() > 30 ? reason.substring(0, 27) + "..." : reason;

        expireMenu.addOption(
            "Value: " + (infraction.getValue() != null ? infraction.getValue() : "0"),
            infraction.getId(),
            shortReason);
      }

      // Add to remove dropdown (all infractions)
      // String shortId = infraction.getId().length() > 8 ? infraction.getId().substring(0, 8) : infraction.getId();
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

    // Add title and description based on whether this is an alt parent
    String title = isAltParent ? "Main Account Infractions" : "Infractions";
    String description = "Infractions of " + discordUser.getAsMention() + ":";

    // Add alt info if provided
    if (altInfo != null) {
      description += "\n\n" + altInfo;
    }

    embed
        .setTitle(title)
        .setDescription(description)
        .setAuthor(discordUser.getName(), null, discordUser.getEffectiveAvatarUrl())
        .setFooter("The total value of valid infractions is " + totalActive + ".\n" +
            "The total value of all infractions is " + totalAll + ".");

    // Build response with appropriate components
    boolean hasActiveInfractions = totalActive > 0;

    // Use event.getHook() instead of event.reply since we already deferred the
    // reply
    if (hasActiveInfractions) {
      event.getHook().sendMessageEmbeds(embed.build())
          .addActionRow(expireMenu.build())
          .addActionRow(removeMenu.build())
          .addActionRow(primary, danger)
          .addActionRow(noBan, tempBan, fullBan)
          .queue();
    } else {
      // Only add remove dropdown since no active infractions
      event.getHook().sendMessageEmbeds(embed.build())
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

    DVUser targetUser = userOpt.get();
    event.deferReply().queue(); // Defer the reply as this might take a while

    // Collect all accounts to update (main account + all alts)
    Set<DVUser> accountsToUpdate = new HashSet<>();
    Set<Long> discordIdsToUpdate = new HashSet<>();
    boolean isAlt = false;
    DVUser mainAccount = targetUser;

    // Check if the target user is an alt account
    Optional<Alt> asAltOpt = altRepository.findBySnowflakeId(userId);
    if (asAltOpt.isPresent()) {
      // This user is an alt - get their parent
      Alt altRecord = asAltOpt.get();
      String parentId = altRecord.getParent();
      isAlt = true;

      if (parentId != null) {
        Optional<DVUser> parentOpt = userRepository.findById(parentId);
        if (parentOpt.isPresent()) {
          mainAccount = parentOpt.get(); // Set the main account
          LOGGER.info("User is an alt account, found parent: " + parentId);
        } else {
          LOGGER.warning("Alt's parent account not found: " + parentId);
        }
      }
    }

    // Add the main account to the update list
    accountsToUpdate.add(mainAccount);
    if (mainAccount.getSnowflakeId() != null) {
      discordIdsToUpdate.add(mainAccount.getSnowflakeId());
    }

    // Find all alt accounts and add them to the update list
    List<Alt> altAccounts = altRepository.findByParentId(mainAccount.getId());
    LOGGER.info("Found " + altAccounts.size() + " alt accounts for main account: " + mainAccount.getId());

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

    // Update ban status for all accounts
    for (DVUser user : accountsToUpdate) {
      switch (banType) {
        case "none":
          user.setIs_banned(false);
          user.setIs_suspended(false);
          break;
        case "temp":
          user.setIs_banned(false);
          user.setIs_suspended(true);
          break;
        case "full":
          user.setIs_banned(true);
          user.setIs_suspended(false);
          break;
        default:
          event.getHook().sendMessage("Invalid ban type.").setEphemeral(true).queue();
          return;
      }

      try {
        userRepository.save(user);
        LOGGER.info("Updated ban status for user: " + user.getId());
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error updating ban status for " + user.getId(), e);
      }
    }

    // Apply roles to all accounts that are in the guild
    applyRolesToAccounts(event, discordIdsToUpdate, banType);

    // Prepare status message
    String statusAction;
    if (banType.equals("none")) {
      statusAction = "unbanned";
    } else if (banType.equals("temp")) {
      statusAction = "temporarily banned";
    } else {
      statusAction = "permanently banned";
    }

    String statusMessage;
    if (isAlt) {
      statusMessage = "The main account and all linked alt accounts (" + discordIdsToUpdate.size() +
          " total accounts) have been " + statusAction + ".";
    } else {
      statusMessage = "This account and all its alts (" + discordIdsToUpdate.size() +
          " total accounts) have been " + statusAction + ".";
    }

    event.getHook().sendMessage(statusMessage).queue();
  }

  private void applyRolesToAccounts(ButtonInteractionEvent event, Set<Long> discordIds, String banType) {
    Guild guild = event.getGuild();
    if (guild == null) {
      LOGGER.warning("Guild is null, cannot apply roles");
      return;
    }

    // Get role objects
    Role bannedRoleObj = null;
    Role tempBanRoleObj = null;

    if (bannedRole != 0) {
      bannedRoleObj = guild.getRoleById(bannedRole);
      if (bannedRoleObj == null) {
        LOGGER.warning("Banned role with ID " + bannedRole + " not found in guild");
      }
    }

    if (tempBanRole != 0) {
      tempBanRoleObj = guild.getRoleById(tempBanRole);
      if (tempBanRoleObj == null) {
        LOGGER.warning("Temp ban role with ID " + tempBanRole + " not found in guild");
      }
    }

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

        switch (banType) {
          case "none":
            if (bannedRoleObj != null && member.getRoles().contains(bannedRoleObj)) {
              guild.removeRoleFromMember(member, bannedRoleObj).queue(
                  success -> LOGGER.info("Removed banned role from " + username),
                  error -> LOGGER.warning("Failed to remove banned role: " + error.getMessage()));
            }

            if (tempBanRoleObj != null && member.getRoles().contains(tempBanRoleObj)) {
              guild.removeRoleFromMember(member, tempBanRoleObj).queue(
                  success -> LOGGER.info("Removed temp ban role from " + username),
                  error -> LOGGER.warning("Failed to remove temp ban role: " + error.getMessage()));
            }
            break;

          case "temp":
            if (tempBanRoleObj != null && !member.getRoles().contains(tempBanRoleObj)) {
              guild.addRoleToMember(member, tempBanRoleObj).queue(
                  success -> LOGGER.info("Added temp ban role to " + username),
                  error -> LOGGER.warning("Failed to add temp ban role: " + error.getMessage()));
            }

            if (bannedRoleObj != null && member.getRoles().contains(bannedRoleObj)) {
              guild.removeRoleFromMember(member, bannedRoleObj).queue(
                  success -> LOGGER.info("Removed banned role from " + username),
                  error -> LOGGER.warning("Failed to remove banned role: " + error.getMessage()));
            }
            break;

          case "full":
            if (bannedRoleObj != null && !member.getRoles().contains(bannedRoleObj)) {
              guild.addRoleToMember(member, bannedRoleObj).queue(
                  success -> LOGGER.info("Added banned role to " + username),
                  error -> LOGGER.warning("Failed to add banned role: " + error.getMessage()));
            }

            if (tempBanRoleObj != null && member.getRoles().contains(tempBanRoleObj)) {
              guild.removeRoleFromMember(member, tempBanRoleObj).queue(
                  success -> LOGGER.info("Removed temp ban role from " + username),
                  error -> LOGGER.warning("Failed to remove temp ban role: " + error.getMessage()));
            }
            break;
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error processing roles for Discord ID: " + discordId, e);
      }
    }
  }

  private String formatDate(OffsetDateTime dateTime) {
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
  }
}
