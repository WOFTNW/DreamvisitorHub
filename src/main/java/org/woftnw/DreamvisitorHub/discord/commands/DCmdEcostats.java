package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DCmdEcostats implements DiscordCommand {

  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("ecostats", "Get your economic stats.");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    Long discordId = event.getUser().getIdLong();
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(discordId);

    if (!userOptional.isPresent()) {
      event.reply("You don't have a profile yet. Please connect your Minecraft account first.").setEphemeral(true)
          .queue();
      return;
    }

    DVUser user = userOptional.get();
    EmbedBuilder embed = new EmbedBuilder();

    embed.setTitle("Economic Stats")
        .setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl())
        .setDescription(
            "Here are your stats as of " + Bot.createTimestamp(LocalDateTime.now(), TimeFormat.DATE_TIME_SHORT) + ", "
                + event.getUser().getAsMention() + ".")
        .addField("Balance", Bot.CURRENCY_SYMBOL + " " + (user.getBalance() != null ? user.getBalance() : "0"), true)
        .addField("Daily Streak", (user.getDaily_streak() != null ? user.getDaily_streak() : "0") + " days", true);

    int workCoolDown = Integer.parseInt((String) App.getConfig().get("work_cooldown"));
    // Format and add last work time
    String lastWork = formatTime(user.getLast_work());
    // Calculate when the user can work again
    String timeUntilNextWork;

    if (user.getLast_work() != null) {
      OffsetDateTime nextWorkTime = user.getLast_work().plusMinutes(workCoolDown);
      OffsetDateTime now = OffsetDateTime.now();

      if (now.isAfter(nextWorkTime)) {
        timeUntilNextWork = "Available now";
      } else {
        // Calculate remaining time in minutes and hours
        Duration remainingTime = Duration.between(now, nextWorkTime);
        long hours = remainingTime.toHours();
        long minutes = remainingTime.toMinutes() % 60;

        if (hours > 0) {
          timeUntilNextWork = hours + " hour" + (hours > 1 ? "s" : "") +
              (minutes > 0 ? " " + minutes + " minute" + (minutes > 1 ? "s" : "") : "");
        } else {
          timeUntilNextWork = minutes + " minute" + (minutes > 1 ? "s" : "");
        }
      }
    } else {
      timeUntilNextWork = "Available now";
    }

    embed.addField("Time until next work", timeUntilNextWork, false);

    event.replyEmbeds(embed.build()).queue();
  }

  private String formatTime(OffsetDateTime time) {
    if (time == null) {
      return "Never";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return time.format(formatter);
  }
}
