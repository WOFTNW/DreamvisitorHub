package org.woftnw.DreamvisitorHub.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.discord.commands.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiscCommandsManager extends ListenerAdapter {

  static final List<DiscordCommand> commands = new ArrayList<>();

  // Get channels and roles from config
  @SuppressWarnings({ "null" })
  public static void init() {

    System.out.println("Initializing commands...");

    List<DiscordCommand> addList = new ArrayList<>();

    addList.add(new DCmdActivity());
    // addList.add(new DCmdBroadcast());
    addList.add(new DCmdLink());
    // addList.add(new DCmdList());
    // addList.add(new DCmdMsg());
    // addList.add(new DCmdPanic());
    // addList.add(new DCmdResourcepackupdate());
    // addList.add(new DCmdSchedulerestart());
    addList.add(new DCmdSetgamechat());
    addList.add(new DCmdSetlogchat());
    // addList.add(new DCmdSetrole());
    addList.add(new DCmdSetwhitelist());
    // addList.add(new DCmdToggleweb());
    // addList.add(new DCmdUnwhitelist());
    addList.add(new DCmdUser());
    // addList.add(new DCmdWarn());
    // addList.add(new DCmdAlts());
    // addList.add(new DCmdInfractions());
    addList.add(new DCmdBalance());
    // addList.add(new DCmdInventory());
    // addList.add(new DCmdShop());
    // addList.add(new DCmdEconomy());
    addList.add(new DCmdEcostats());
    addList.add(new DCmdDaily());
    addList.add(new DCmdWork());
    addList.add(new DCmdBaltop());
    addList.add(new DCmdSeen());
    // addList.add(new DCmdYear());
    System.out.println("Ready to add to guild.");

    addCommands(addList);

  }

  @Override
  @SuppressWarnings({ "null" })
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

    for (DiscordCommand command : commands) {
      if (event.getName().equals(command.getName())) {
        System.out.println("Found the Command");
        command.onCommand(event);
        return;
      }
    }
    EmbedBuilder noMatchEmbed = new EmbedBuilder();
    noMatchEmbed.setColor(Color.RED).setTitle("No commands match your request.").setDescription(
        "This is a fatal error and should not be possible. The command has been scheduled for deletion to prevent further exceptions.");
    event.reply("Great, everything is broken. I'm going to have to bother one of my superiors to fix this.")
        .addEmbeds(noMatchEmbed.build()).queue();

    String commandId = event.getCommandId();
    event.getJDA().deleteCommandById(commandId).queue();
  }

  public static void addCommands(@NotNull List<DiscordCommand> commands) {

    System.out.println("Request to add " + commands.size() + " commands.");

    if (commands.isEmpty())
      return;

    JDA jda = Bot.getJda();

    try {
      jda.awaitReady();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    List<SlashCommandData> commandData = new ArrayList<>();
    for (DiscordCommand command : commands) {
      if (command != null) {
        commandData.add(command.getCommandData());
        try {
          jda.addEventListener(command);
        } catch (IllegalArgumentException ignored) {
        }
        System.out.println("Added command " + command.getName());
      }
    }

    for (Guild guild : jda.getGuilds()) {
      // register commands
      for (SlashCommandData commandDatum : commandData) {
        guild.upsertCommand(commandDatum).queue();
      }
    }

    System.out.println("Updated commands for " + jda.getGuilds().size() + " guild(s).");

    commands.removeIf(Objects::isNull);
    DiscCommandsManager.commands.addAll(commands);

  }

}
