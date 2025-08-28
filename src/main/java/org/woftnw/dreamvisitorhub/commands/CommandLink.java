package org.woftnw.dreamvisitorhub.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.woftnw.dreamvisitorhub.App;
import org.woftnw.dreamvisitorhub.commands.framework.ExecutableSlashCommand;
import org.woftnw.dreamvisitorhub.data.repository.UserRepository;
import org.woftnw.dreamvisitorhub.data.type.DVUser;
import org.woftnw.dreamvisitorhub.util.Mojang;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandLink extends ExecutableSlashCommand {
    @Override
    public @NotNull SlashCommandData getCommandData() {
        return Commands.slash("link", "Link a Discord account to a Minecraft account.")
                .addOption(OptionType.USER, "user", "The Discord user to register.", true)
                .addOption(OptionType.STRING, "username", "The Minecraft account to connect", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteraction event) {
        final User targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
        final String username = Objects.requireNonNull(event.getOption("username")).getAsString();

        UUID uuid = null;
        try {
            uuid = Mojang.getUuidOfUsername(username);
        } catch (IOException | IllegalArgumentException ignored) {
        }

        UserRepository userRepository = App.getUserRepository();

        final Optional<DVUser> optionalTargetDvUserFromDiscord = userRepository.findByDiscordId(targetUser.getId());
        Optional<DVUser> optionalTargetDvUserFromMinecraft;
        if (uuid != null) {
            optionalTargetDvUserFromMinecraft = userRepository.findByUuid(uuid);
        } else {
            optionalTargetDvUserFromMinecraft = userRepository.findByMinecraftUsername(username);
        }

        if (optionalTargetDvUserFromDiscord.isPresent() && optionalTargetDvUserFromMinecraft.isPresent()) {
            final DVUser targetDvUserFromDiscord = optionalTargetDvUserFromDiscord.get();
            final DVUser targetDvUserFromMinecraft = optionalTargetDvUserFromMinecraft.get();
            // Check if the Discord user and Minecraft user refer to different accounts
            if (!Objects.equals(targetDvUserFromDiscord.getId(), targetDvUserFromMinecraft.getId())) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Two different records exist").setDescription("Two different records exist for the Discord account and Minecraft account you are trying to connect. Delete one or both and try again.").setColor(Color.red)
                        .addField("Discord Target",
                                "`" + targetDvUserFromDiscord.getMinecraftUsername() + "`" +
                                        "\n<@" + targetDvUserFromDiscord.getDiscordId() + ">",
                                true
                        )
                        .addField(
                                "Minecraft Target",
                                "`" + targetDvUserFromMinecraft.getMinecraftUsername() + "`" +
                                        "\n<@" + targetDvUserFromMinecraft.getDiscordId() + ">",
                                true
                        );
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                return;
            }
            // Both already refer to the same user
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("No changes made").setDescription("Those accounts are already linked to a user.").setColor(Color.BLUE);
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        DVUser computeUser = null;
        boolean created = false;

        if (optionalTargetDvUserFromDiscord.isPresent()) computeUser = optionalTargetDvUserFromDiscord.get();
        else if (optionalTargetDvUserFromMinecraft.isPresent()) computeUser = optionalTargetDvUserFromMinecraft.get();
        if (computeUser == null) {
            computeUser = new DVUser();
            created = true;
        }

        @Nullable String oldDiscordId = computeUser.getDiscordId();
        @Nullable String oldMcName = computeUser.getMinecraftUsername();

        computeUser.setDiscordId(targetUser.getId());
        computeUser.setDiscordUsername(targetUser.getName());
        computeUser.setDiscordImg(targetUser.getAvatarUrl());

        computeUser.setMcUuid(uuid);
        computeUser.setMcUsername(username);

        userRepository.save(computeUser);

        StringBuilder description = new StringBuilder(targetUser.getAsMention() + "** is now linked to **`" + username + "`**.**");
        if (oldDiscordId != null && !Objects.equals(oldDiscordId, targetUser.getId()))
            description.append("\nThe account was unattached from the previous Discord account <@").append(oldDiscordId).append(">.");
        if (oldMcName != null && !Objects.equals(oldMcName.toLowerCase(), username.toLowerCase()))
            description.append("\nThe account was unattached from the previous Minecraft account `").append(oldMcName).append("`.");
        if (uuid == null)
            description.append("\nNo UUID could be found with that username, so only the Minecraft username has been recorded.");
        if (created)
            description.append("\nNo record previously existed for the Discord or Minecraft account, so this record has been added as a new user.");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Accounts linked");
        embed.setDescription(description.toString().strip());
        embed.setColor(Color.GREEN);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    @Override
    protected void onAutoComplete(CommandAutoCompleteInteractionEvent event) {

    }
}
