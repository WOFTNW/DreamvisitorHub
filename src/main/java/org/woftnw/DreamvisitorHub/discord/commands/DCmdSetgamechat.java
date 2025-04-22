package org.woftnw.DreamvisitorHub.discord.commands;

import org.woftnw.DreamvisitorHub.discord.Bot;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdSetgamechat implements DiscordCommand {
  private static final Logger logger = Logger.getLogger("DreamvisitorHub");

  @Override
  public @NotNull SlashCommandData getCommandData() {
    return Commands.slash("setgamechat", "Set the channel that game chat occurs in.")
        .addOption(OptionType.CHANNEL, "channel", "The channel to set.", true, false)
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    try {
      // Get channel from args
      TextChannel channel = (TextChannel) event.getOption("channel", event.getChannel(), OptionMapping::getAsChannel);

      // Set the game chat channel - this will update both PocketBase and the file
      // config
      Bot.setGameChatChannel(channel);

      // Reply with success message
      event.reply("Game chat channel set to " + Bot.getGameChatChannel().getAsMention() +
          ". Configuration has been updated in both PocketBase and the config file.").queue();

      logger.info("Game chat channel set to " + channel.getName() + " (" + channel.getId() + ")");
      Bot.sendLog("Game chat channel set to " + channel.getName() + " (" + channel.getId() + ")");
    } catch (Exception e) {
      // Handle any exceptions
      logger.log(Level.SEVERE, "Error setting game chat channel", e);
      event.reply("An error occurred while setting the game chat channel: " + e.getMessage()).setEphemeral(true)
          .queue();
    }
  }
}
