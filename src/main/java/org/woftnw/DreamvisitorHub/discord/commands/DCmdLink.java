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
import org.woftnw.DreamvisitorHub.util.UUIDFromater;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DCmdLink implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

  @Override
  public @NotNull SlashCommandData getCommandData() {
    return Commands.slash("link", "Link a Discord account to a Minecraft account.")
        .addOption(OptionType.USER, "user", "The Discord user to register.", true)
        .addOption(OptionType.STRING, "username", "The Minecraft account to connect", true)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    User targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
    String username = Objects.requireNonNull(event.getOption("username")).getAsString();
    String mc_uuid = App.getMojang().getUUIDOfUsername(username);
    if (mc_uuid == null) {
      event.reply("`" + username + "` could not be found!").queue();
      return;
    }
    UUID uuid = UUID.fromString(UUIDFromater.formatUuid(mc_uuid));

    if (uuid == null) {
      event.reply("`" + username + "` could not be found!").queue();
      return;
    }

    // Check if Minecraft user already exists
    Optional<DVUser> existingMcUser = userRepository.findByUuid(uuid);

    // Check if Discord user already exists
    Optional<DVUser> existingDiscordUser = userRepository.findBySnowflakeId(targetUser.getIdLong());

    DVUser user;

    if (existingMcUser.isPresent()) {
      // Update existing Minecraft user with Discord info
      user = existingMcUser.get();

      // If this MC account is already linked to a different Discord account
      if (user.getSnowflakeId() != null && !user.getSnowflakeId().equals(targetUser.getIdLong())) {
        event.reply("This Minecraft account is already linked to a different Discord user!").queue();
        return;
      }

      user.setSnowflakeId(targetUser.getIdLong());
      user.setDiscord_id(targetUser.getId());
      user.setDcUsername(targetUser.getName());
      user.setDiscord_img(targetUser.getEffectiveAvatarUrl());
    } else if (existingDiscordUser.isPresent()) {
      // Update existing Discord user with Minecraft info
      user = existingDiscordUser.get();

      // If this Discord account is already linked to a different MC account
      if (user.getMc_uuid() != null && !user.getMc_uuid().equals(uuid)) {
        event.reply("This Discord account is already linked to a different Minecraft user!").queue();
        return;
      }

      user.setMc_uuid(uuid);
      user.setMcUsername(username);
    } else {
      // Create new user with both Discord and Minecraft info
      user = new DVUser();
      user.setMc_uuid(uuid);
      user.setMcUsername(username);
      user.setSnowflakeId(targetUser.getIdLong());
      user.setDiscord_id(targetUser.getId());
      user.setDcUsername(targetUser.getName());
      user.setDiscord_img(targetUser.getEffectiveAvatarUrl());
    }

    // Save user to repository
    userRepository.save(user);

    EmbedBuilder embed = new EmbedBuilder();
    embed.setDescription(targetUser.getAsMention() + " is now linked to `" + username + "`!");
    embed.setColor(Color.GREEN);

    event.replyEmbeds(embed.build()).queue();
  }
}
