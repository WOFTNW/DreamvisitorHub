package org.woftnw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandManager extends ListenerAdapter {

    private final JDA jda;

    public CommandManager(@NotNull JDA jda) {
        this.jda = jda;
        jda.addEventListener(this);
    }

    private final List<SlashCommandData> commands = new ArrayList<>();

    public void addCommand(SlashCommandData command) {
        jda.upsertCommand(command).queue();
        commands.add(command);
    }

}
