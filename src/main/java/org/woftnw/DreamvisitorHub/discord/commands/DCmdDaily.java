package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.awt.*;
import java.time.*;
import java.util.Optional;

public class DCmdDaily implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("daily", "Claim your daily streak allowance.");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    User user = event.getUser();
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(user.getIdLong());

    if (!userOptional.isPresent()) {
      event.reply("You don't have a profile yet. Please connect your Minecraft account first.").setEphemeral(true)
          .queue();
      return;
    }

    DVUser dvUser = userOptional.get();
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle("Daily Reward");

    // Get current time and midnight reference
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime todayMidnight = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
    OffsetDateTime tomorrowMidnight = todayMidnight.plusDays(1);

    // Check if user has already claimed today
    OffsetDateTime lastDaily = dvUser.getLast_daily();
    if (lastDaily != null && lastDaily.isAfter(todayMidnight)) {
      Duration timeUntilNextClaim = Duration.between(now, tomorrowMidnight);
      embedBuilder.setColor(Color.red)
          .setDescription(
              "You have already claimed your daily reward for today. You cannot claim your daily reward for "
                  + (timeUntilNextClaim.toHours() % 24) + " hour(s), "
                  + (timeUntilNextClaim.toMinutes() % 60) + " minute(s).");
      event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
      return;
    }

    // Calculate streak
    int streak = dvUser.getDaily_streak() != null ? dvUser.getDaily_streak() : 0;

    // Check if streak continues
    if (lastDaily != null) {
      // If last claim was yesterday, maintain streak
      if (lastDaily.isAfter(todayMidnight.minusDays(1))) {
        streak++;
      } else {
        // Streak broken
        streak = 1;
      }
    } else {
      // First time claiming
      streak = 1;
    }

    // Calculate reward
    double baseAmount = (Double) App.getConfig().get("dailyBaseAmount");
    double streakMultiplier = (Double) App.getConfig().get("dailyStreakMultiplier");
    double reward = baseAmount + (streak * streakMultiplier);
    reward = Math.round(reward * 100.0) / 100.0; // Round to 2 decimal places

    // Update user data
    double currentBalance = dvUser.getBalance() != null ? dvUser.getBalance() : 0.0;
    dvUser.setBalance(currentBalance + reward);
    dvUser.setDaily_streak(streak);
    dvUser.setLast_daily(now);
    userRepository.save(dvUser);

    // Send success message
    embedBuilder
        .setDescription(
            "You earned " + Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(reward)
                + " today.\nCome back tomorrow for your next reward.")
        .setFooter("Your new balance is " + Bot.formatCurrency(dvUser.getBalance()) +
            "\nThis brings your streak to " + streak + " day(s).")
        .setColor(Color.GREEN);
    event.replyEmbeds(embedBuilder.build()).queue();
  }
}
