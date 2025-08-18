package org.woftnw.dreamvisitorhub.functions;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.woftnw.dreamvisitorhub.App;
import org.woftnw.dreamvisitorhub.Bot;
import org.woftnw.dreamvisitorhub.config.Config;
import org.woftnw.dreamvisitorhub.config.ConfigKey;
import org.woftnw.dreamvisitorhub.data.type.ChatMessage;

import java.util.List;

public class ChatBridge {

    private static boolean isRunning = false;
    private static Thread thread;

    public static void start() {
        isRunning = true;
        thread = new Thread(ChatBridge::loop);
        thread.start();
    }

    public static  void stop() {
        isRunning = false;
        thread.interrupt();
    }

    public static  boolean isRunning() {
        return isRunning;
    }

    private static void loop() {
        while (isRunning) {
            List<ChatMessage> messages = App.getChatMessageRepository().findAll();
            String chatChannelId = Config.get(ConfigKey.GAME_CHAT_CHANNEL);

            TextChannel chatChannel = Bot.getJda().getTextChannelById(chatChannelId);
            if (chatChannel == null) return;
            for (ChatMessage message : messages) {
                if (message.getSource().equals(ChatMessage.Source.DISCORD)) continue;
                chatChannel.sendMessage("**" + message.getSenderUsername() + ":** " + message.getMessage()).queue();
                App.getChatMessageRepository().delete(message);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
