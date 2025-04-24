package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.awt.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

public class DCmdWork extends ListenerAdapter implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("work", "Work for a paycheck!");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    int workCoolDown = Integer.parseInt((String) App.getConfig().get("work_cooldown"));
    double workReward = (Double) App.getConfig().get("workReward");
    User user = event.getUser();
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(user.getIdLong());

    if (!userOptional.isPresent()) {
      event.reply("You don't have a profile yet. Please connect your Minecraft account first.").setEphemeral(true)
          .queue();
      return;
    }

    DVUser dvUser = userOptional.get();
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Work");

    // Check if user is on cooldown
    OffsetDateTime lastWork = dvUser.getLast_work();
    OffsetDateTime now = OffsetDateTime.now();

    if (lastWork != null) {
      Duration sinceLastWork = Duration.between(lastWork, now);
      if (sinceLastWork.toMinutes() < workCoolDown) {
        Duration timeLeft = Duration.ofMinutes(workCoolDown).minus(sinceLastWork);
        embed.setColor(Color.red)
            .setDescription("You cannot work for " + timeLeft.toMinutes() + " minute(s).");
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        return;
      }
    }

    // Update user data
    double currentBalance = dvUser.getBalance() != null ? dvUser.getBalance() : 0.0;
    dvUser.setBalance(currentBalance + workReward);
    dvUser.setLast_work(now);
    userRepository.save(dvUser);

    embed
        .setDescription(
            "You earned " + Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(workReward) + " by working.\nCome back in "
                + workCoolDown + " minutes to work again.")
        .setFooter("Your new balance is " + Bot.formatCurrency(dvUser.getBalance()))
        .setColor(Color.GREEN);
    event.replyEmbeds(embed.build()).queue();
  }
}
