package org.woftnw.dreamvisitorhub.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.App;
import org.woftnw.dreamvisitorhub.Bot;
import org.woftnw.dreamvisitorhub.commands.framework.ExecutableSlashCommand;
import org.woftnw.dreamvisitorhub.config.Config;
import org.woftnw.dreamvisitorhub.config.ConfigKey;
import org.woftnw.dreamvisitorhub.data.type.DVUser;
import org.woftnw.dreamvisitorhub.functions.Economy;
import org.woftnw.dreamvisitorhub.util.Formatter;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

public class CommandDaily extends ExecutableSlashCommand {

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("daily", "Claim your daily streak allowance.");
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteraction event) {
        User sender = event.getUser();
        Optional<DVUser> optionalUser = App.getUserRepository().findByDiscordId(sender.getId());
        DVUser user;

        // Initialize user if they do not exist
        if (optionalUser.isEmpty()) {
            user = new DVUser();
            user.setDiscordId(sender.getId());
            user.setDiscordUsername(sender.getName());
            user.setDiscordImg(sender.getAvatarUrl());
        } else user = optionalUser.get();

        EmbedBuilder embedBuilder = new EmbedBuilder();

        double reward;

        try {
            reward = Economy.claimDaily(user);
        } catch (Economy.CoolDownException e) {
            Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT));
            embedBuilder.setColor(Color.red).setTitle("Reward already claimed").setDescription("You have already claimed your daily reward for today. You cannot claim your daily reward for " + Formatter.chooseCountForm(duration.toHoursPart(), "hour", "hours") + " and " + Formatter.chooseCountForm(duration.toMinutesPart(), "minute", "minutes") + ".");
            event.replyEmbeds(embedBuilder.build()).queue();
            return;
        }
        App.getUserRepository().save(user);

        embedBuilder.setTitle("Claimed daily reward");
        embedBuilder.setDescription("You earned " + Config.get(ConfigKey.CURRENCY_ICON) + reward + " today.\nCome back tomorrow for your next reward.")
                .setFooter("Your new balance is " + user.getBalance() + "\nThis brings your streak to " + Formatter.chooseCountForm(user.getDailyStreak(), "day", "days") + ".")
                .setColor(Color.GREEN);
        event.replyEmbeds(embedBuilder.build()).queue();
    }

    @Override
    protected void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        noAutoComplete();
    }
}
