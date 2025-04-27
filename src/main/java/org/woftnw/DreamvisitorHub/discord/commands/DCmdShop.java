package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
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
  private static String SHOP_NAME = "Dreamvisitor Shop";
  private static final int ITEMS_PER_PAGE = 4;

  // Store the current page each user is viewing
  private final Map<Long, Integer> userPages = new HashMap<>();

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("shop", "Access the shop.");
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    SHOP_NAME = (String) App.getConfig().get("shopName");
    // Reset page to 1 when shop command is executed
    userPages.put(event.getUser().getIdLong(), 1);

    // Display the first page
    displayShopPage(event, 1);
  }

  /**
   * Displays a specific page of the shop
   *
   * @param event The interaction event
   * @param page  The page number to display
   */
  private void displayShopPage(SlashCommandInteractionEvent event, int page) {
    // Get all enabled items
    List<Item> allItems = itemRepository.findAllEnabled();

    // Handle empty shop
    if (allItems.isEmpty()) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle(SHOP_NAME);
      embed.setColor(Color.YELLOW);
      embed.setDescription("There are no items currently for sale.");
      event.replyEmbeds(embed.build()).queue();
      return;
    }

    // Calculate pagination
    int totalItems = allItems.size();
    int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

    // Ensure page is within valid range
    page = Math.max(1, Math.min(page, totalPages));

    // Update stored page
    userPages.put(event.getUser().getIdLong(), page);

    // Get items for the current page
    int startIndex = (page - 1) * ITEMS_PER_PAGE;
    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
    List<Item> pageItems = allItems.subList(startIndex, endIndex);

    // Create embed for the page
    EmbedBuilder embed = createShopEmbed(pageItems, page, totalPages);

    // Create purchase menu for the page
    StringSelectMenu.Builder purchaseMenu = createPurchaseMenu(pageItems);

    // Create navigation buttons
    Button prevButton = Button.primary("shop_prev_page", "⬅️ Previous")
        .withDisabled(page <= 1);
    Button nextButton = Button.primary("shop_next_page", "Next ➡️")
        .withDisabled(page >= totalPages);

    // Send the message with navigation and item selection
    event.replyEmbeds(embed.build())
        .addActionRow(purchaseMenu.build())
        .addActionRow(prevButton, nextButton)
        .queue();
  }

  /**
   * Updates a shop page display after button interaction
   *
   * @param event The button interaction event
   * @param page  The page to display
   */
  private void updateShopPage(ButtonInteractionEvent event, int page) {
    // Get all enabled items
    List<Item> allItems = itemRepository.findAllEnabled();

    // Handle empty shop
    if (allItems.isEmpty()) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle(SHOP_NAME);
      embed.setColor(Color.YELLOW);
      embed.setDescription("There are no items currently for sale.");
      event.editMessageEmbeds(embed.build())
          .setComponents()
          .queue();
      return;
    }

    // Calculate pagination
    int totalItems = allItems.size();
    int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

    // Ensure page is within valid range
    page = Math.max(1, Math.min(page, totalPages));

    // Update stored page
    userPages.put(event.getUser().getIdLong(), page);

    // Get items for the current page
    int startIndex = (page - 1) * ITEMS_PER_PAGE;
    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
    List<Item> pageItems = allItems.subList(startIndex, endIndex);

    // Create embed for the page
    EmbedBuilder embed = createShopEmbed(pageItems, page, totalPages);

    // Create purchase menu for the page
    StringSelectMenu.Builder purchaseMenu = createPurchaseMenu(pageItems);

    // Create navigation buttons
    Button prevButton = Button.primary("shop_prev_page", "⬅️ Previous")
        .withDisabled(page <= 1);
    Button nextButton = Button.primary("shop_next_page", "Next ➡️")
        .withDisabled(page >= totalPages);

    // Update the message with new data - FIX: Use clearComponents() and
    // addActionRow() to ensure all components are visible
    event.editMessageEmbeds(embed.build())
        .setComponents(
            ActionRow.of(purchaseMenu.build()),
            ActionRow.of(prevButton, nextButton))
        .queue();
  }

  /**
   * Creates the shop embed for a specific page
   */
  private EmbedBuilder createShopEmbed(List<Item> items, int currentPage, int totalPages) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(SHOP_NAME + " (Page " + currentPage + "/" + totalPages + ")");
    embed.setColor(Color.YELLOW);

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
    }

    return embed;
  }

  /**
   * Creates a purchase menu for the given items
   */
  private StringSelectMenu.Builder createPurchaseMenu(List<Item> items) {
    StringSelectMenu.Builder purchaseMenu = StringSelectMenu.create("purchase")
        .setPlaceholder("Select an item to purchase");

    for (Item item : items) {
      if (item.getName() != null && item.getPrice() != null) {
        double truePrice = calculateTruePrice(item);
        purchaseMenu.addOption(
            item.getName(),
            item.getId(),
            item.getId() + " - " + Bot.formatCurrency(truePrice) + " " + Bot.CURRENCY_SYMBOL);
      }
    }

    return purchaseMenu;
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String componentId = event.getComponentId();

    if (componentId.startsWith("purchase-")) {
      handlePurchaseButton(event);
    } else if (componentId.equals("shop_prev_page")) {
      handleNavigationButton(event, -1);
    } else if (componentId.equals("shop_next_page")) {
      handleNavigationButton(event, 1);
    }
  }

  /**
   * Handles navigation button clicks
   */
  private void handleNavigationButton(ButtonInteractionEvent event, int pageDelta) {
    long userId = event.getUser().getIdLong();
    int currentPage = userPages.getOrDefault(userId, 1);
    int newPage = currentPage + pageDelta;

    // Update the shop display with the new page
    updateShopPage(event, newPage);
  }

  /**
   * Handles purchase button clicks
   */
  private void handlePurchaseButton(ButtonInteractionEvent event) {
    String itemId = event.getComponentId().substring("purchase-".length());

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
      if (item.getQuantity() != null && item.getQuantity() == 0) {
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
        LOGGER.info("Item set to use on purchase, but this feature isn't implemented yet");
      }

      // Get the current page number
      int currentPage = userPages.getOrDefault(discordId, 1);

      // Get updated shop items for the current page
      List<Item> allItems = itemRepository.findAllEnabled();
      int totalItems = allItems.size();
      int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
      currentPage = Math.max(1, Math.min(currentPage, totalPages));

      int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
      int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
      List<Item> pageItems = allItems.subList(startIndex, endIndex);

      // Create updated shop embed
      EmbedBuilder shopEmbed = createShopEmbed(pageItems, currentPage, totalPages);

      // Send confirmation with updated shop info
      EmbedBuilder purchaseEmbed = new EmbedBuilder();
      purchaseEmbed.setTitle("Purchase successful!");
      purchaseEmbed.setDescription("Purchased " + item.getName() + " for " + Bot.CURRENCY_SYMBOL + " " +
          Bot.formatCurrency(price) + ".");

      int newQuantity = getCurrentItemQuantity(user.getId(), itemId);

      purchaseEmbed.setFooter("You now have " + newQuantity + " of this item.\nYour new balance is " +
          Bot.formatCurrency(user.getBalance()) + " " + Bot.CURRENCY_SYMBOL);
      purchaseEmbed.setColor(Color.GREEN);

      // Create updated purchase menu
      StringSelectMenu.Builder purchaseMenu = createPurchaseMenu(pageItems);

      // Create navigation buttons
      Button prevButton = Button.primary("shop_prev_page", "⬅️ Previous")
          .withDisabled(currentPage <= 1);
      Button nextButton = Button.primary("shop_next_page", "Next ➡️")
          .withDisabled(currentPage >= totalPages);

      // Send purchase confirmation as ephemeral message and update the shop display
      event.replyEmbeds(purchaseEmbed.build())
          .setEphemeral(true)
          .queue();

      // Edit the original message to update the shop display
      event.getMessage().editMessageEmbeds(shopEmbed.build())
          .setComponents(
              ActionRow.of(purchaseMenu.build()),
              ActionRow.of(prevButton, nextButton))
          .queue();

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
