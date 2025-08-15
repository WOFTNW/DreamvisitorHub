package org.woftnw.dreamvisitorhub.commands.framework;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The CommandManager is responsible for handling all Discord slash commands. It registers commands to the guild and
 * handles command autocomplete and execution by invoking the appropriate {@link ExecutableSlashCommand} methods.
 */
public class CommandManager extends ListenerAdapter {

    /**
     * We use guild commands instead of global commands because they are faster to update and Dreamvisitor is only
     * intended to work in one guild anyway.
     */
    private final Guild guild;
    private final List<ExecutableSlashCommand> commands = new ArrayList<>();

    public CommandManager(Guild guild, List<ExecutableSlashCommand> commands) {
        this.guild = guild;
        this.commands.addAll(commands);
        registerAllCommands();
    }

    /**
     * An internal method to register commands to the guild.
     */
    private void registerAllCommands() {
        commands.forEach(command -> guild.upsertCommand(command.getCommandData()).queue());
    }

    /**
     * Sends slash command events to the correct {@link ExecutableSlashCommand}. This event should be registered by JDA.
     * @param event the {@link SlashCommandInteractionEvent} context.
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        for (ExecutableSlashCommand command : commands) {
            if (Objects.equals(command.getCommandData().getName(), event.getName())) {
                command.execute(event);
                return;
            }
        }

        // If we get to this point, there are no commands that match. This can happen if a command is removed from the code, but not from Discord's side.
        // This is not dangerous, but we should delete the command.
        EmbedBuilder noMatchEmbed = new EmbedBuilder();
        noMatchEmbed.setColor(Color.RED).setTitle("No commands match your request.").setDescription("This command was removed, but not deleted. It will be deleted shortly.");
        event.reply("That command doesn't exist anymore.").addEmbeds(noMatchEmbed.build()).queue();

        // Delete the command
        String commandId = event.getCommandId();
        guild.deleteCommandById(commandId).queue();
    }

    /**
     * Sends autocomplete command events to the correct {@link ExecutableSlashCommand}. This event should be registered by JDA.
     * @param event the {@link CommandAutoCompleteInteractionEvent} context.
     */
    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        for (ExecutableSlashCommand command : commands) {
            if (Objects.equals(command.getCommandData().getName(), event.getName())) {
                command.autoComplete(event);
                return;
            }
        }
    }
}
