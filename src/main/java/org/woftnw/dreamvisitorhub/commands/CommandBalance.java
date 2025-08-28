package org.woftnw.dreamvisitorhub.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.App;
import org.woftnw.dreamvisitorhub.commands.framework.ExecutableSlashCommand;
import org.woftnw.dreamvisitorhub.config.Config;
import org.woftnw.dreamvisitorhub.config.ConfigKey;
import org.woftnw.dreamvisitorhub.data.type.DVUser;
import org.woftnw.dreamvisitorhub.util.Formatter;

import java.awt.*;
import java.util.Optional;

public class CommandBalance extends ExecutableSlashCommand {

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("balance", "Get your current balance.")
                .addOption(OptionType.USER, "member", "[Optional] The member whose balance to fetch", false, false);
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteraction event) {
        String title;
        String description;
        boolean error = false;

        String currencyIcon = Config.get(ConfigKey.CURRENCY_ICON);

        User targetUser = event.getOption("member", OptionMapping::getAsUser);
        if (targetUser != null) {
            Optional<DVUser> user = App.getUserRepository().findByDiscordId(targetUser.getId());
            if (user.isEmpty()) {
                title = "User not found";
                description = targetUser.getAsMention() + " could not be found in the data base. Contact a staff member.";
                error = true;
            } else {
                double balance;
                balance = user.get().getBalance();
                title = "User balance";
                description = targetUser.getAsMention() + " has " + currencyIcon + Formatter.formatMoney(balance) + ".";
            }
        } else {
            Optional<DVUser> user = App.getUserRepository().findByDiscordId(event.getUser().getId());
            if (user.isEmpty()) {
                title = "User not found";
                description = event.getUser().getAsMention() + " could not be found in the data base. Contact a staff member.";
                error = true;
            } else {
                double balance;
                balance = user.get().getBalance();
                title = "User balance";
                description = "You have " + currencyIcon + Formatter.formatMoney(balance) + ".";
            }
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title).setDescription(description);
        if (error) embed.setColor(Color.red);

        event.replyEmbeds(embed.build()).queue();
    }

    @Override
    protected void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        noAutoComplete();
    }
}
