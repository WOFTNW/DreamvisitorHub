package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DCmdAlts extends net.dv8tion.jda.api.hooks.ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdAlts.class.getName());
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final UserRepository userRepository = App.getUserRepository();
  private final AltRepository altRepository;
  private final InfractionRepository infractionRepository = App.getInfractionRepository();

  public DCmdAlts() {
    // Initialize the alt repository
    this.altRepository = new org.woftnw.DreamvisitorHub.data.repository.PocketBaseAltRepository(
        App.getPb(), userRepository);
  }

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("alts", "Manage alternate accounts.")
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        .addSubcommands(
            new SubcommandData("get", "Get alt accounts for a user")
                .addOption(OptionType.USER, "user", "The user to get alts for", true),
            new SubcommandData("link", "Link an alt account to a main account")
                .addOption(OptionType.USER, "parent", "The main (parent) account", true)
                .addOption(OptionType.USER, "child", "The alt (child) account", true),
            new SubcommandData("unlink", "Unlink an alt account")
                .addOption(OptionType.USER, "child", "The alt (child) account to unlink", true));
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    String subcommand = event.getSubcommandName();

    if (subcommand == null) {
      event.reply("Invalid command usage.").setEphemeral(true).queue();
      return;
    }

    switch (subcommand) {
      case "get":
        handleGetAlts(event);
        break;
      case "link":
        handleLinkAlts(event);
        break;
      case "unlink":
        handleUnlinkAlt(event);
        break;
      default:
        event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        break;
    }
  }

  private void handleGetAlts(SlashCommandInteractionEvent event) {
    User targetUser = event.getOption("user", OptionMapping::getAsUser);
    if (targetUser == null) {
      event.reply("You must specify a user.").setEphemeral(true).queue();
      return;
    }

    // Defer the reply since this might take some time
    event.deferReply().queue();

    // First check if the user exists in the database
    Optional<DVUser> targetUserOpt = userRepository.findBySnowflakeId(targetUser.getIdLong());
    if (!targetUserOpt.isPresent()) {
      event.getHook().sendMessage("That user doesn't have a profile yet.").queue();
      return;
    }

    DVUser targetDVUser = targetUserOpt.get();

    // Now try to determine if this user is a parent or a child in an alt family
    Optional<Alt> asChildOpt = altRepository.findBySnowflakeId(targetUser.getIdLong());

    if (asChildOpt.isPresent()) {
      // This user is a child, get the parent
      Alt childAlt = asChildOpt.get();
      String parentId = childAlt.getParent();

      if (parentId != null) {
        Optional<DVUser> parentUserOpt = userRepository.findById(parentId);
        if (parentUserOpt.isPresent()) {
          DVUser parentUser = parentUserOpt.get();
          showAltFamily(event, parentUser, "This user is an alt account of a main account.");
          return;
        }
      }
    }

    // If we get here, either the user is a parent or not part of an alt family
    // Check if they have any alts
    showAltFamily(event, targetDVUser, null);
  }

  private void showAltFamily(SlashCommandInteractionEvent event, DVUser parentUser, String note) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Alt Account Family");

    // Get Discord user for parent if possible
    String parentName = parentUser.getDiscord_username();
    String parentAvatarUrl = null;

    if (parentUser.getSnowflakeId() != null) {
      try {
        User discordParent = Bot.getJda().retrieveUserById(parentUser.getSnowflakeId()).complete();
        if (discordParent != null) {
          parentName = discordParent.getName();
          parentAvatarUrl = discordParent.getEffectiveAvatarUrl();
          embed.setAuthor(parentName, null, parentAvatarUrl);
        }
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Could not retrieve parent user", e);
      }
    }

    embed.setDescription("Main account: **" + parentName + "**");

    if (note != null) {
      embed.appendDescription("\n\n" + note);
    }

    // Get all alts linked to this parent
    List<Alt> alts = new ArrayList<>();
    if (parentUser.getAlts() != null && !parentUser.getAlts().isEmpty()) {
      // Load alt records
      for (String altId : parentUser.getAlts()) {
        altRepository.findById(altId).ifPresent(alts::add);
      }
    } else {
      // Try direct query
      alts = altRepository.findByParentId(parentUser.getId());
    }

    if (alts.isEmpty()) {
      embed.addField("Alternate Accounts", "No alt accounts found.", false);
    } else {
      Guild guild = event.getGuild();
      StringBuilder altsText = new StringBuilder();

      for (Alt alt : alts) {
        String altName = alt.getDiscord_name();
        boolean inGuild = false;

        // Check if the alt is in the current guild
        if (guild != null && alt.getSnowflakeId() != null) {
          try {
            Member member = guild.retrieveMemberById(alt.getSnowflakeId()).complete();
            if (member != null) {
              altName = member.getUser().getName();
              inGuild = true;
            }
          } catch (Exception e) {
            // User not in guild
          }
        }

        altsText.append("• **").append(altName).append("**");
        if (!inGuild) {
          altsText.append(" (not in server)");
        }
        altsText.append("\n");
      }

      embed.addField("Alternate Accounts (" + alts.size() + ")", altsText.toString(), false);
    }

    // Add infractions count if available
    if (parentUser.getInfractions() != null) {
      int infractionCount = parentUser.getInfractions().size();
      if (infractionCount > 0) {
        embed.addField("Infractions", "This user has " + infractionCount + " infraction(s).", false);
      }
    }

    // Send the embed
    event.getHook().sendMessageEmbeds(embed.build()).queue();
  }

  private void handleLinkAlts(SlashCommandInteractionEvent event) {
    // Get the parent (main) and child (alt) users
    User parentUser = event.getOption("parent", OptionMapping::getAsUser);
    User childUser = event.getOption("child", OptionMapping::getAsUser);

    if (parentUser == null || childUser == null) {
      event.reply("You must specify both a parent and child user.").setEphemeral(true).queue();
      return;
    }

    // Check that they're not the same user
    if (parentUser.getIdLong() == childUser.getIdLong()) {
      event.reply("You can't link a user to itself.").setEphemeral(true).queue();
      return;
    }

    // Defer reply since this might take time
    event.deferReply().queue();

    // First check if the child user is already an alt of ANY user
    Optional<Alt> existingChildAltOpt = altRepository.findBySnowflakeId(childUser.getIdLong());
    if (existingChildAltOpt.isPresent()) {
      Alt existingAlt = existingChildAltOpt.get();
      String currentParentId = existingAlt.getParent();

      if (currentParentId != null) {
        Optional<DVUser> currentParentOpt = userRepository.findById(currentParentId);
        String currentParentName = currentParentOpt.map(DVUser::getDiscord_username).orElse("Unknown");

        // If the alt is already linked to the same parent, just inform the user and
        // exit
        if (currentParentId.equals(userRepository.findBySnowflakeId(parentUser.getIdLong())
            .map(DVUser::getId).orElse(null))) {
          event.getHook().sendMessage("This user is already linked as an alt of " + currentParentName + ".").queue();
          return;
        }

        event.getHook().sendMessage("This user is already linked as an alt to " + currentParentName +
            ". Please unlink it first.").queue();
        return;
      }
    }

    // Check if parent is an alt of someone else
    Optional<Alt> parentAsAltOpt = altRepository.findBySnowflakeId(parentUser.getIdLong());
    if (parentAsAltOpt.isPresent()) {
      Alt parentAsAlt = parentAsAltOpt.get();
      String currentParentId = parentAsAlt.getParent();

      if (currentParentId != null) {
        Optional<DVUser> currentParentOpt = userRepository.findById(currentParentId);
        String currentParentName = currentParentOpt.map(DVUser::getDiscord_username).orElse("Unknown");

        event.getHook().sendMessage("The user you're trying to set as a parent is itself an alt account of " +
            currentParentName + ". Please unlink it first.").queue();
        return;
      }
    }

    // Get DVUser objects for both users
    Optional<DVUser> parentDVUserOpt = userRepository.findBySnowflakeId(parentUser.getIdLong());
    Optional<DVUser> childDVUserOpt = userRepository.findBySnowflakeId(childUser.getIdLong());

    // Create users if they don't exist
    DVUser parentDVUser;
    DVUser childDVUser;

    try {
      if (!parentDVUserOpt.isPresent()) {
        parentDVUser = createNewUser(parentUser);
      } else {
        parentDVUser = parentDVUserOpt.get();
      }

      if (!childDVUserOpt.isPresent()) {
        childDVUser = createNewUser(childUser);
      } else {
        childDVUser = childDVUserOpt.get();
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error creating user profile", e);
      event.getHook().sendMessage("Failed to create user profile: " + e.getMessage() +
          "\nPlease make sure both users have valid profiles before linking.").queue();
      return;
    }

    // Create or update the alt record
    Alt childAlt;
    if (existingChildAltOpt.isPresent()) {
      childAlt = existingChildAltOpt.get();
    } else {
      childAlt = new Alt();
      childAlt.setDiscord_id(childUser.getId());
      childAlt.setDiscord_name(childUser.getName());
    }

    childAlt.setParent(parentDVUser.getId());

    try {
      Alt savedAlt = altRepository.save(childAlt);

      // Update the parent's alts list
      List<String> parentAlts = parentDVUser.getAlts();
      if (parentAlts == null) {
        parentAlts = new ArrayList<>();
      }

      if (!parentAlts.contains(savedAlt.getId())) {
        parentAlts.add(savedAlt.getId());
        parentDVUser.setAlts(parentAlts);
        userRepository.save(parentDVUser);
      }

      // Transfer any infractions from child to parent
      List<Infraction> childInfractions = infractionRepository.findByUser(childDVUser.getId());
      if (!childInfractions.isEmpty()) {
        for (Infraction infraction : childInfractions) {
          // Update the user reference to the parent
          infraction.setUser(parentDVUser.getId());
          // Add a note that this was transferred from an alt
          String currentReason = infraction.getReason() != null ? infraction.getReason() : "";
          infraction.setReason(currentReason + "\n[Transferred from alt account: " + childUser.getName() + "]");
          // Save the updated infraction
          infractionRepository.save(infraction);
        }

        // Clear the child's infractions list
        if (childDVUser.getInfractions() != null) {
          childDVUser.setInfractions(new ArrayList<>());
          userRepository.save(childDVUser);
        }
      }

      // Send success message
      event.getHook().sendMessage("Successfully linked " + childUser.getAsMention() +
          " as an alt account of " + parentUser.getAsMention() + ".").queue();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error linking alt account", e);
      event.getHook().sendMessage("Failed to link alt account: " + e.getMessage()).queue();
    }
  }

  private void handleUnlinkAlt(SlashCommandInteractionEvent event) {
    User childUser = event.getOption("child", OptionMapping::getAsUser);

    if (childUser == null) {
      event.reply("You must specify a child user to unlink.").setEphemeral(true).queue();
      return;
    }

    // Defer reply
    event.deferReply().queue();

    // Check if the user is actually an alt
    Optional<Alt> altOpt = altRepository.findBySnowflakeId(childUser.getIdLong());

    if (!altOpt.isPresent()) {
      event.getHook().sendMessage("This user is not linked as an alt account.").queue();
      return;
    }

    Alt alt = altOpt.get();
    String parentId = alt.getParent();

    if (parentId == null) {
      event.getHook().sendMessage("This alt is not linked to any parent account.").queue();
      return;
    }

    // Get the parent user
    Optional<DVUser> parentUserOpt = userRepository.findById(parentId);

    // Remove the alt record
    altRepository.delete(alt);

    // Update the parent's alts list if parent exists
    if (parentUserOpt.isPresent()) {
      DVUser parentUser = parentUserOpt.get();
      List<String> parentAlts = parentUser.getAlts();

      if (parentAlts != null && parentAlts.contains(alt.getId())) {
        parentAlts.remove(alt.getId());
        parentUser.setAlts(parentAlts);
        userRepository.save(parentUser);
      }

      // Get parent's Discord username for the response
      String parentName = parentUser.getDiscord_username();
      if (parentUser.getSnowflakeId() != null) {
        try {
          User discordParent = Bot.getJda().retrieveUserById(parentUser.getSnowflakeId()).complete();
          if (discordParent != null) {
            parentName = discordParent.getName();
          }
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Could not retrieve parent user", e);
        }
      }

      event.getHook().sendMessage("Successfully unlinked " + childUser.getAsMention() +
          " from parent account " + parentName + ".").queue();
    } else {
      event.getHook().sendMessage("Successfully unlinked " + childUser.getAsMention() +
          " from parent account (parent not found in database).").queue();
    }
  }

  private DVUser createNewUser(User discordUser) {
    try {
      DVUser newUser = new DVUser();
      newUser.setDiscord_id(discordUser.getId());
      newUser.setDiscord_username(discordUser.getName());
      newUser.setSnowflakeId(discordUser.getIdLong());
      newUser.setDiscord_img(discordUser.getEffectiveAvatarUrl());

      // Set a placeholder for required fields
      newUser.setMcUsername("pending_" + discordUser.getId()); // Ensures mc_username isn't blank

      return userRepository.save(newUser);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create user profile for " + discordUser.getName(), e);
      throw new RuntimeException("Failed to create user profile: " + e.getMessage(), e);
    }
  }
}
