package org.woftnw.dreamvisitorhub.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

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
