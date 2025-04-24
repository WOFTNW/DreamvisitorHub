package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.ItemRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserInventoryRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Item;
import org.woftnw.DreamvisitorHub.data.type.UserInventory;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DCmdInventory implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdInventory.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final ItemRepository itemRepository = App.getItemRepository();
  private final UserInventoryRepository userInventoryRepository = App.getUserInventoryRepository();

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("inventory", "View and manage your inventory");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    handleViewInventory(event);
  }

  private void handleViewInventory(SlashCommandInteractionEvent event) {
    // Defer reply since this might take some time
    event.deferReply().queue();

    // Get target user (default to command user if not specified)
    User targetUser = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
    long discordId = targetUser.getIdLong();

    // Find user in database
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(discordId);

    if (!userOptional.isPresent()) {
      event.getHook().sendMessage(targetUser.getAsMention() + " doesn't have a profile yet.")
          .setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOptional.get();

    // Get user's inventory items
    List<UserInventory> inventoryItems = userInventoryRepository.findByUser(dvUser.getId());
    LOGGER.info("user inventory:" + inventoryItems);
    // Load related items
    userInventoryRepository.loadRelatedItems(inventoryItems);

    // Create embed for inventory display
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(targetUser.getName() + "'s Inventory");
    embed.setThumbnail(targetUser.getEffectiveAvatarUrl());
    embed.setColor(Color.BLUE);

    if (inventoryItems.isEmpty()) {
      embed.setDescription("No items in inventory.");
      // If no items, just send without buttons
      event.getHook().sendMessageEmbeds(embed.build()).queue();
    } else {
      StringBuilder description = new StringBuilder();
      description.append("Items in inventory: ").append(inventoryItems.size()).append("\n\n");

      for (UserInventory inventoryItem : inventoryItems) {
        Item item = inventoryItem.getCachedItem();
        if (item != null) {
          String itemName = item.getName() != null ? item.getName() : "Unknown Item";
          int quantity = inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0;

          description.append("**").append(itemName).append("** x").append(quantity);

          // Add price if available
          if (item.getPrice() != null) {
            description.append(" (").append(Bot.CURRENCY_SYMBOL).append(" ")
                .append(Bot.formatCurrency(item.getPrice())).append(")");
          }

          String id = item.getId();
          description.append("\n").append("• ").append(id);

          description.append("\n\n");
        }
      }

      embed.setDescription(description.toString().trim());

      // Create buttons for use and gift actions
      // TODO: handle interactions with the buttons
      Button useButton = Button.primary("use_item", "Use Item");
      Button giftButton = Button.secondary("gift_item", "Gift Item");

      // Send embed with buttons
      event.getHook().sendMessageEmbeds(embed.build())
          .addActionRow(useButton, giftButton)
          .queue();
    }
  }
}
