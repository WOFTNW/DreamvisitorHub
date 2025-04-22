package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
// import org.woftnw.DreamvisitorHub.data.storage.StorageManager;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.pb.PocketBase;

import java.awt.*;
import java.util.Optional;
import java.util.logging.Logger;

public class DCmdBalance implements DiscordCommand {
  private static final Logger logger = Logger.getLogger(DCmdBalance.class.toString());
  private static final String DEFAULT_CURRENCY_SYMBOL = "$";

  private UserRepository getUserRepository() {
    // Get PocketBase instance from App
    PocketBase pocketBase = App.getPb();
    return new PocketBaseUserRepository(pocketBase);
  }

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("balance", "Get your current balance.")
        .addOption(OptionType.USER, "member", "[Optional] The member whose balance to fetch", false, false);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    // Defer reply to give time for database query
    event.deferReply().queue();

    String message;
    UserRepository userRepository = getUserRepository();

    // Get the currency symbol from StorageManager or use default
    // String currencySymbol = StorageManager.getCurrencyIcon();

    // if (currencySymbol == null || currencySymbol.isEmpty()) {
    // currencySymbol = DEFAULT_CURRENCY_SYMBOL;
    // }

    User targetUser = event.getOption("member", OptionMapping::getAsUser);
    if (targetUser == null) {
      // If no target provided, use the command issuer
      targetUser = event.getUser();
    }

    logger.info("Fetching balance for user: " + targetUser.getId());

    // Find user in PocketBase
    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(targetUser.getIdLong());

    if (userOpt.isPresent()) {
      DVUser user = userOpt.get();
      double balance = user.getBalance() != null ? user.getBalance() : 0.0;

      if (targetUser.equals(event.getUser())) {
        message = "You have " + DEFAULT_CURRENCY_SYMBOL + String.format("%.2f", balance) + ".";
      } else {
        message = targetUser.getAsMention() + " has " + DEFAULT_CURRENCY_SYMBOL + String.format("%.2f", balance) + ".";
      }
    } else {
      // User not found in database
      if (targetUser.equals(event.getUser())) {
        message = "You don't have an account yet.";
      } else {
        message = targetUser.getAsMention() + " doesn't have an account yet.";
      }
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setColor(Color.YELLOW);
    embed.setDescription(message);

    event.getHook().sendMessageEmbeds(embed.build()).queue();
  }
}
