package org.woftnw.dreamvisitorhub.commands.framework;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This is the abstract representation for a slash command.
 *
 * <p>
 *     Each slash command has its own class to keep things organized, and each extends this class so that the
 *     {@link CommandManager} can dynamically select the correct class to send command events to.
 * </p>
 * <p>
 *     If you want to create a command, take a look at {@link org.woftnw.dreamvisitorhub.commands.CommandActivity}.
 *     Start by copying and pasting the file as a template.
 * </p>
 */
public abstract class ExecutableSlashCommand {

    /**
     * This represents the Discord slash command data.
     */
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

    /**
     * This can be used within onAutoComplete for commands which do not have any autocomplete arguments.
     */
    protected void noAutoComplete() {
        throw new IllegalStateException("This command does not have autocomplete, but an autocompletion was called.");
    }

}
