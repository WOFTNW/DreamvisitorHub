package org.woftnw;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {

    public static JDA jda;

    private static CommandManager commandManager;

    public static void startBot(String token) throws InterruptedException {

        // Note: It is important to register your ReadyListener before building
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT) // enables explicit access to message.getContentDisplay()
                .build();

        // optionally block until JDA is ready
        jda.awaitReady();

        commandManager = new CommandManager(jda);
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }
}
