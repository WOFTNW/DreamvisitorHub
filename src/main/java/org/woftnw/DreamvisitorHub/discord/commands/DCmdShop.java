package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
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
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCmdShop extends ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdShop.class.getName());
  private final ItemRepository itemRepository = App.getItemRepository();
  private final UserRepository userRepository = App.getUserRepository();
  private final UserInventoryRepository userInventoryRepository = App.getUserInventoryRepository();
  private static final String SHOP_NAME = "Dreamvisitor Shop";

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("shop", "Access the shop.");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(SHOP_NAME);
    embed.setColor(Color.YELLOW);

    // Fetch all enabled items
    List<Item> items = itemRepository.findAllEnabled();

    // Create selection menu for purchase
    StringSelectMenu.Builder purchaseMenu = StringSelectMenu.create("purchase");

    if (items.isEmpty()) {
      embed.setDescription("There are no items currently for sale.");
      event.replyEmbeds(embed.build()).queue();
      return;
    }

    for (Item item : items) {
      // Skip items with null properties
      if (item.getName() == null || item.getPrice() == null) {
        continue;
      }

      String priceString = Bot.formatCurrency(item.getPrice());
      double truePrice = calculateTruePrice(item);

      if (item.getSale_percent() != null && item.getSale_percent() > 0) {
        priceString = "~~" + priceString + "~~ " + Bot.formatCurrency(truePrice) +
            " (" + item.getSale_percent() + "% off)";
      }

      String header = (item.getName() + " - " + Bot.CURRENCY_SYMBOL + " " + priceString);
      StringBuilder body = new StringBuilder();
      body.append("`").append(item.getId()).append("`");

      if (item.getDescription() != null) {
        body.append("\n**").append(item.getDescription()).append("**");
      }

      if (item.getQuantity() != null && item.getQuantity() > 0) {
        body.append("\n").append(item.getQuantity()).append(" of this item remain(s).");
      } else if (item.getQuantity() == null || item.getQuantity() < 0) {
        body.append("\nUnlimited quantity available.");
      }

      body.append("\n")
          .append(item.getGifting_enabled() != null && item.getGifting_enabled() ? "This item can be gifted."
              : "This item cannot be gifted.");

      embed.addField(header, body.toString(), false);

      // Add to selection menu
      purchaseMenu.addOption(
          item.getName(),
          item.getId(),
          item.getId() + " - " + truePrice);
    }

    event.reply("Here is what is currently available in the shop.")
        .addEmbeds(embed.build())
        .addActionRow(purchaseMenu.build())
        .queue();
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String componentId = event.getComponentId();

    if (!componentId.startsWith("purchase-")) {
      return;
    }

    String itemId = componentId.substring("purchase-".length());

    try {
      // Get user
      long discordId = event.getUser().getIdLong();
      Optional<DVUser> userOpt = userRepository.findBySnowflakeId(discordId);

      if (!userOpt.isPresent()) {
        event.reply("You don't have a profile yet. Please connect your Minecraft account first.")
            .setEphemeral(true).queue();
        return;
      }

      DVUser user = userOpt.get();

      // Get item
      Optional<Item> itemOpt = itemRepository.findById(itemId);
      if (!itemOpt.isPresent()) {
        event.reply("That item does not exist.").setEphemeral(true).queue();
        return;
      }

      Item item = itemOpt.get();

      // Validate item is enabled
      if (item.getEnabled() == null || !item.getEnabled()) {
        event.reply("This item is not currently available for purchase.").setEphemeral(true).queue();
        return;
      }

      // Check if item is in stock
      if (item.getQuantity() != null && item.getQuantity() <= 0) {
        event.reply("This item is out of stock.").setEphemeral(true).queue();
        return;
      }

      // Calculate true price with sale
      double price = calculateTruePrice(item);

      // Check if user has enough balance
      double balance = user.getBalance() != null ? user.getBalance() : 0.0;
      if (balance < price) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setDescription("You do not have sufficient funds to purchase " + item.getName() +
            ".\nYour balance: " + Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(balance) +
            "\nItem cost: " + Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(price));
        embed.setColor(Color.RED);
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        return;
      }

      // Check max allowed quantity
      if (item.getMax_allowed() != null && item.getMax_allowed() > 0) {
        int currentQuantity = getCurrentItemQuantity(user.getId(), itemId);
        if (currentQuantity >= item.getMax_allowed()) {
          EmbedBuilder embed = new EmbedBuilder();
          embed.setDescription("You already have " + item.getMax_allowed() +
              " of this item, which is as many as you can have at one time.");
          embed.setColor(Color.RED);
          event.replyEmbeds(embed.build()).setEphemeral(true).queue();
          return;
        }
      }

      // Process purchase
      // 1. Update user balance
      user.setBalance(balance - price);
      userRepository.save(user);

      // 2. Update item stock if needed
      if (item.getQuantity() != null && item.getQuantity() > 0) {
        item.setQuantity(item.getQuantity() - 1);
        itemRepository.save(item);
      }

      // 3. Add to user inventory
      addItemToUserInventory(user.getId(), itemId);

      // 4. Handle item use if needed
      if (item.getUse_on_purchase() != null && item.getUse_on_purchase()) {
        // Implementation would go here - could reuse code from DCmdInventory
        LOGGER.info("Item set to use on purchase, but this feature isn't implemented yet");
      }

      // Send confirmation
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Purchase successful!");
      embed.setDescription("Purchased " + item.getName() + " for " + Bot.CURRENCY_SYMBOL + " " +
          Bot.formatCurrency(price) + ".");

      int newQuantity = getCurrentItemQuantity(user.getId(), itemId);

      embed.setFooter("You now have " + newQuantity + " of this item.\nYour new balance is " +
          Bot.formatCurrency(user.getBalance()) + " " + Bot.CURRENCY_SYMBOL);
      embed.setColor(Color.GREEN);

      event.replyEmbeds(embed.build()).queue();

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error processing purchase", e);
      event.reply("An error occurred while processing your purchase. Please try again later.")
          .setEphemeral(true).queue();
    }
  }

  @Override
  public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
    if (event.getComponentId().equals("purchase")) {
      String itemId = event.getValues().get(0);

      try {
        // Get item
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if (!itemOpt.isPresent()) {
          event.reply("That item does not exist.").setEphemeral(true).queue();
          return;
        }

        Item item = itemOpt.get();

        // Get user
        long discordId = event.getUser().getIdLong();
        Optional<DVUser> userOpt = userRepository.findBySnowflakeId(discordId);

        if (!userOpt.isPresent()) {
          event.reply("You don't have a profile yet. Please connect your Minecraft account first.")
              .setEphemeral(true).queue();
          return;
        }

        DVUser user = userOpt.get();
        double balance = user.getBalance() != null ? user.getBalance() : 0.0;
        double price = calculateTruePrice(item);

        // Create embed with item details
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(item.getName());

        StringBuilder description = new StringBuilder();
        if (item.getDescription() != null) {
          description.append(item.getDescription());
        }

        if (item.getSale_percent() == null || item.getSale_percent() == 0) {
          description.append("\n\nThis item costs ").append(Bot.CURRENCY_SYMBOL)
              .append(" ").append(Bot.formatCurrency(item.getPrice()));
        } else {
          description.append("\n\nThis item regularly costs ").append(Bot.CURRENCY_SYMBOL)
              .append(" ").append(Bot.formatCurrency(item.getPrice())).append(".")
              .append("\nIt is currently **").append(item.getSale_percent())
              .append("% off**, bringing the total to **").append(Bot.CURRENCY_SYMBOL)
              .append(" ").append(Bot.formatCurrency(price)).append("**.");
        }

        if (item.getMax_allowed() != null && item.getMax_allowed() > 0) {
          description.append("\nYou can carry up to **").append(item.getMax_allowed())
              .append("** of this item at a time.");
        }

        if (item.getQuantity() != null && item.getQuantity() > 0) {
          description.append("\n**").append(item.getQuantity())
              .append("** of this item remain.");
        }

        embed.setDescription(description);

        // Show balance impact
        embed.setFooter("Your current balance is " + Bot.formatCurrency(balance) +
            ". After purchasing this item, it would be " +
            Bot.formatCurrency(balance - price) + ".");

        // Create purchase button
        Button buyButton = Button.success("purchase-" + itemId,
            "Purchase for " + Bot.formatCurrency(price) + " " + Bot.CURRENCY_SYMBOL);

        event.replyEmbeds(embed.build()).addActionRow(buyButton).setEphemeral(true).queue();

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error displaying item details", e);
        event.reply("An error occurred while fetching item details. Please try again later.")
            .setEphemeral(true).queue();
      }
    }
  }

  /**
   * Calculates the true price of an item after applying any sale percentage
   */
  private double calculateTruePrice(Item item) {
    double basePrice = item.getPrice() != null ? item.getPrice() : 0.0;
    double salePercent = item.getSale_percent() != null ? item.getSale_percent() : 0.0;

    if (salePercent <= 0) {
      return basePrice;
    }

    return basePrice * (1 - (salePercent / 100.0));
  }

  /**
   * Gets the current quantity of an item in a user's inventory
   */
  private int getCurrentItemQuantity(String userId, String itemId) {
    Optional<UserInventory> inventoryOpt = userInventoryRepository.findByUserAndItem(userId, itemId);
    if (!inventoryOpt.isPresent()) {
      return 0;
    }

    UserInventory inventory = inventoryOpt.get();
    return inventory.getQuantity() != null ? inventory.getQuantity() : 0;
  }

  /**
   * Adds an item to a user's inventory, or increments quantity if already present
   */
  private void addItemToUserInventory(String userId, String itemId) {
    Optional<UserInventory> inventoryOpt = userInventoryRepository.findByUserAndItem(userId, itemId);

    if (inventoryOpt.isPresent()) {
      // Update existing inventory entry
      UserInventory inventory = inventoryOpt.get();
      int currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
      inventory.setQuantity(currentQuantity + 1);
      userInventoryRepository.save(inventory);
    } else {
      // Create new inventory entry
      UserInventory inventory = new UserInventory();
      inventory.setUser(userId);
      inventory.setItem(itemId);
      inventory.setQuantity(1);
      userInventoryRepository.save(inventory);
    }
  }
}
