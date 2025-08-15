package org.woftnw.dreamvisitorhub.commands.framework;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ExecutableSlashCommand {

    public abstract SlashCommandData getCommandData();

    // These abstract methods are overridden by each command class, but they are protected so that the CommandManager
    // uses the autoComplete() and execute() below instead, both of which do a preliminary check to make sure that the
    // command inside the event and the command being executed match. This should happen anyway in the CommandManager,
    // but it acts as a failsafe.
    protected abstract void onCommand(SlashCommandInteraction event);
    protected abstract void onAutoComplete(CommandAutoCompleteInteractionEvent event);

    /**
     * Run the autocomplete for this command.
     * @param event the {@link CommandAutoCompleteInteractionEvent} context
     */
    public void autoComplete(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (Objects.equals(event.getName(), getCommandData().getName())) {
            onAutoComplete(event);
        } else {
            throw new IllegalArgumentException("This command doesn't match the given CommandAutoCompleteInteractionEvent.");
        }
    }

    /**
     * Run the execution for this command.
     * @param event the {@link SlashCommandInteraction} context
     */
    public void execute(@NotNull SlashCommandInteraction event) {
        if (Objects.equals(event.getName(), getCommandData().getName())) {
            onCommand(event);
        } else {
            throw new IllegalArgumentException("This command doesn't match the given SlashCommandInteraction.");
        }
    }

    protected void noAutoComplete() {
        throw new IllegalStateException("This command does not have autocomplete, but an autocompletion was called.");
    }

}
