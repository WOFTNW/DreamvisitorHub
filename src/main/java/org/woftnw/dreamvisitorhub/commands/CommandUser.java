package org.woftnw.dreamvisitorhub.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.woftnw.dreamvisitorhub.App;
import org.woftnw.dreamvisitorhub.commands.framework.ExecutableSlashCommand;
import org.woftnw.dreamvisitorhub.data.type.DVUser;
import org.woftnw.dreamvisitorhub.util.Mojang;
import org.woftnw.mc_renderer.TextureLoader;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class CommandUser extends ExecutableSlashCommand {

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("user", "Get the details of a user.")
                .addSubcommands(
                        new SubcommandData("discord", "Search by Discord user")
                                .addOption(OptionType.USER, "user", "The user to search for.", true),
                        new SubcommandData("minecraft", "Search by Minecraft username")
                                .addOption(OptionType.STRING, "username", "The username to search for.", true)
                )
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    protected void onCommand(@NotNull SlashCommandInteraction event) {

        String subcommand = event.getSubcommandName();

        if (subcommand == null) {
            EmbedBuilder embed = new EmbedBuilder().setColor(Color.red).setDescription("You need to specify subcommand \"discord\" or \"minecraft\".");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "discord" -> {
                User targetUser = event.getOption("user", OptionMapping::getAsUser);

                if (targetUser == null) {
                    EmbedBuilder embed = new EmbedBuilder().setColor(Color.red).setDescription("You must provide a user.");
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    return;
                }

                Logger.getLogger("DreamvisitorHub").info("Target user ID: " + targetUser.getId());

                Optional<DVUser> user = App.getUserRepository().findByDiscordId(targetUser.getId());

                if (user.isEmpty()) {
                    EmbedBuilder embed = new EmbedBuilder().setColor(Color.yellow).setTitle("User not found.").setDescription(targetUser.getAsMention() + " does not exist in the database. Would you like to add them?");
                    // TODO: Make button "userinit-discord-<discord_id>" add user
                    ActionRow buttons = ActionRow.of(Button.of(ButtonStyle.PRIMARY, "userinit-discord-" + targetUser.getId(), "Add to user database", Emoji.fromFormatted("\uD83D\uDCDD")));
                    event.replyEmbeds(embed.build()).addComponents(buttons).setEphemeral(true).queue();
                    return;
                }

                // TODO: Eventually include more info here

                buildEmbed(event, user.get(), targetUser, null);

            }
            case "minecraft" -> {

                String username = event.getOption("username", OptionMapping::getAsString);

                if (username == null) {
                    EmbedBuilder embed = new EmbedBuilder().setColor(Color.red).setDescription("You must provide a username.");
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    return;
                }

                Optional<DVUser> user = App.getUserRepository().findByMinecraftUsername(username);

                if (user.isEmpty()) {
                    EmbedBuilder embed = new EmbedBuilder().setColor(Color.yellow).setDescription("No user with that username could be found. Would you like to add them?");
                    // TODO: Make button "userinit-minecraftusername-<minecraft_username>" add user
                    ActionRow buttons = ActionRow.of(Button.of(ButtonStyle.PRIMARY, "userinit-minecraftusername-" + username, "Add to user database", Emoji.fromFormatted("\uD83D\uDCDD")));
                    event.replyEmbeds(embed.build()).addComponents(buttons).setEphemeral(true).queue();
                    return;
                }

                // TODO: Eventually include more info here

                buildEmbed(event, user.get(), null, username);

            }
            default -> {
                EmbedBuilder embed = new EmbedBuilder().setColor(Color.red).setDescription(subcommand + " is not a valid subcommand. You need to specify subcommand \"discord\" or \"minecraft\".");
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        }
    }

    private void buildEmbed(SlashCommandInteraction event, @NotNull DVUser user, @Nullable User discordUser, @Nullable String searchedMinecraftUsername) {
        UUID uuid = user.getMinecraftUuid();
        String stringUuid = "N/A";
        String username = user.getMinecraftUsername();
        // fallback to searched Minecraft username if used
        if (username == null) {
            username = searchedMinecraftUsername;
        }

        if (uuid != null) {
            stringUuid = uuid.toString();
        }

        OffsetDateTime lastPlayed = user.getLastPlayed();
        String lastPlayedString = "Never";
        if (lastPlayed != null) lastPlayedString = TimeFormat.DEFAULT.format(lastPlayed.toInstant());

        // Send data
        EmbedBuilder embed = new EmbedBuilder();

        embed.setColor(Color.BLUE);

        String discordId;
        String discordUsername;
        String discordAvatar;

        if (discordUser == null) {
            discordId = user.getDiscordId();
            discordUsername = user.getDiscordUsername();
            discordAvatar = user.getDiscordAvatarUrl();
        } else {
            discordId = discordUser.getId();
            discordUsername = discordUser.getName();
            discordAvatar = discordUser.getAvatarUrl();
        }

        embed.setAuthor(discordUsername, discordAvatar, discordAvatar);

        embed.addField("Discord ID", valueOrNaIfNull(discordId), false);
        embed.addField("Minecraft Username", valueOrNaIfNull(username), false);
        embed.addField("Minecraft UUID", valueOrNaIfNull(stringUuid), false);
        embed.addField("Last Played", valueOrNaIfNull(lastPlayedString), false);

        if (uuid != null) {
            // If the UUID exists, try to get the skin and add it to the embed
            try {
                ByteBuffer byteBuffer = TextureLoader.extractAndScaleMinecraftHead(TextureLoader.loadTextureFromUrl(Mojang.getSkinUrl(String.valueOf(uuid))), 512);
                byte[] byteArray = new byte[byteBuffer.remaining()];
                byteBuffer.get(byteArray);
                embed.setThumbnail("attachment://face.png");

                // This requires the image to be uploaded as an attachment (it will not appear as one though)
                event.replyFiles(FileUpload.fromData(byteArray, "face.png")).setEmbeds(embed.build()).setEphemeral(true).queue();
            } catch (IOException ignore) {
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        } else {
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }

    @NotNull
    @Contract(value = "!null -> param1", pure = true)
    private String valueOrNaIfNull(String input) {
        if (input == null) return "N/A";
        return input;
    }

    @Override
    protected void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        noAutoComplete();
    }
}
