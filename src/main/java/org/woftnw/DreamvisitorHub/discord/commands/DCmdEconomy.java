package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.ItemRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserInventoryRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.data.type.Item;
import org.woftnw.DreamvisitorHub.data.type.UserInventory;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class DCmdEconomy extends ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdEconomy.class.getName());
  private final ItemRepository itemRepository = App.getItemRepository();
  private final UserRepository userRepository = App.getUserRepository();
  private final UserInventoryRepository userInventoryRepository = App.getUserInventoryRepository();
  private final Map<String, Object> config = App.getConfig();

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("economy", "Manage the Discord economy.")
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        .addSubcommandGroups(
            new SubcommandGroupData("shop", "Manage the shop.").addSubcommands(
                new SubcommandData("name", "Get or set the name of the shop.")
                    .addOption(OptionType.STRING, "new-name", "The name to set.", false, false),
                new SubcommandData("currency-symbol", "Get or set the currency symbol.")
                    .addOption(OptionType.STRING, "new-symbol", "The symbol to set.", false, false)),
            new SubcommandGroupData("items", "Manage items.").addSubcommands(
                new SubcommandData("list", "List all items."),
                new SubcommandData("edit", "Edit an item.")
                    .addOption(OptionType.STRING, "id", "The ID of the item to edit.", true),
                new SubcommandData("add", "Create a new item.")
                    .addOption(OptionType.STRING, "name", "The name of the item.", true)
                    .addOption(OptionType.STRING, "description", "The description of this item.", true),
                new SubcommandData("remove", "Permanently remove an item.")
                    .addOption(OptionType.STRING, "id", "The ID of the item to remove.", true)),
            new SubcommandGroupData("users", "Manage users.").addSubcommands(
                new SubcommandData("balance", "Get or set the balance of a user.")
                    .addOption(OptionType.USER, "user", "The user whose balance to get.", true)
                    .addOption(OptionType.NUMBER, "new-balance", "The balance to set.", false),
                new SubcommandData("get-items", "Manage the items of a user.")
                    .addOption(OptionType.USER, "user", "The user whose items to get.", true),
                new SubcommandData("set-items", "Set the quantity of an item held by a user.")
                    .addOption(OptionType.USER, "user", "The user whose items to get.", true)
                    .addOption(OptionType.STRING, "id", "The ID of the item to modify the quantity of.", true)
                    .addOption(OptionType.INTEGER, "new-quantity", "The number of this item the user should have.",
                        true),
                new SubcommandData("daily-streak", "Get or set the daily streak of a user.")
                    .addOption(OptionType.USER, "user", "The user whose streak to get.", true)
                    .addOption(OptionType.INTEGER, "new-value", "The streak to set for this player.", false)));
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    String subcommandGroup = event.getSubcommandGroup();
    String subcommand = event.getSubcommandName();

    if (subcommandGroup == null || subcommand == null) {
      event.reply("Subcommand Group or Subcommand is null.").setEphemeral(true).queue();
      return;
    }

    switch (subcommandGroup) {
      case "shop": {
        if (subcommand.equals("name")) {
          String name = event.getOption("new-name", OptionMapping::getAsString);
          if (name == null) {
            String shopName = (String) config.getOrDefault("shopName", "Dreamvisitor Shop");
            event.reply("The current shop name is " + shopName).setEphemeral(true).queue();
          } else {
            String oldName = (String) config.getOrDefault("shopName", "Dreamvisitor Shop");
            config.put("shopName", name);

            // Update the config in PocketBase
            try {
              com.google.gson.JsonObject configData = new com.google.gson.JsonObject();
              configData.addProperty("shopName", name);
              App.getPb().updateRecord("dreamvisitor_config", "45q1at367581q3a", configData, null, null);
              event.reply("Changed shop name from " + oldName + " to " + name + ".").queue();
            } catch (Exception e) {
              LOGGER.warning("Failed to update shop name in config: " + e.getMessage());
              event.reply("Error updating shop name in database.").setEphemeral(true).queue();
            }
          }
        } else if (subcommand.equals("currency-symbol")) {
          String symbol = event.getOption("new-symbol", OptionMapping::getAsString);
          if (symbol == null) {
            event.reply("The current currency symbol is " + Bot.CURRENCY_SYMBOL).setEphemeral(true).queue();
          } else {
            String oldSymbol = Bot.CURRENCY_SYMBOL;

            // This is a static field in Bot class, so we'd need to update it globally
            // For demo purposes, we'll just show the change response
            Bot.CURRENCY_SYMBOL = symbol;
            event.reply("Changed currency symbol from " + oldSymbol + " to " + symbol + ".").queue();
          }
        } else {
          event.reply("Subcommand not found.").setEphemeral(true).queue();
        }
        break;
      }
      case "items": {
        switch (subcommand) {
          case "list":
            handleItemsList(event);
            break;
          case "edit":
            handleItemsEdit(event);
            break;
          case "add":
            handleItemsAdd(event);
            break;
          case "remove":
            handleItemsRemove(event);
            break;
          default:
            event.reply("Subcommand not found.").setEphemeral(true).queue();
            break;
        }
        break;
      }
      case "users": {
        switch (subcommand) {
          case "balance":
            handleUsersBalance(event);
            break;
          case "get-items":
            handleUsersGetItems(event);
            break;
          case "set-items":
            handleUsersSetItems(event);
            break;
          case "daily-streak":
            handleUsersDailyStreak(event);
            break;
          default:
            event.reply("Subcommand not found.").setEphemeral(true).queue();
            break;
        }
        break;
      }
      default:
        event.reply("Subcommand group not found.").setEphemeral(true).queue();
        break;
    }
  }

  private void handleItemsList(SlashCommandInteractionEvent event) {
    EmbedBuilder embed = new EmbedBuilder();

    List<Item> items = itemRepository.findAll();
    if (items.isEmpty()) {
      embed.setDescription("There are currently no items.");
      event.replyEmbeds(embed.build()).queue();
      return;
    }

    for (Item item : items) {
      StringBuilder description = new StringBuilder();

      if (item.getDescription() != null) {
        description.append(item.getDescription()).append("\n\n");
      }

      // Add price information
      description.append("Price: ").append(Bot.CURRENCY_SYMBOL).append(" ")
          .append(item.getPrice() != null ? Bot.formatCurrency(item.getPrice()) : "0.00");

      // Add sale percentage
      description.append("\nSale Percent: ").append(item.getSale_percent() != null ? item.getSale_percent() : "0")
          .append("%");

      // Add quantity
      description.append("\nQuantity: ");
      if (item.getQuantity() == null || item.getQuantity() < 0) {
        description.append("Infinite");
      } else {
        description.append(item.getQuantity());
      }

      // Add other properties
      description.append("\nEnabled: ").append(item.getEnabled() != null ? item.getEnabled() : false);
      description.append("\nGifting: ").append(item.getGifting_enabled() != null ? item.getGifting_enabled() : false);
      description.append("\nUsable: ").append(item.getUse_disabled() != null ? !item.getUse_disabled() : true);
      description.append("\nUse on Purchase: ")
          .append(item.getUse_on_purchase() != null ? item.getUse_on_purchase() : false);

      // Add max allowed
      description.append("\nMax Allowed: ");
      if (item.getMax_allowed() == null || item.getMax_allowed() < 0) {
        description.append("Infinite");
      } else {
        description.append(item.getMax_allowed());
      }

      // Handle roles to add on use
      description.append("\nRoles Add on Use: ");
      String rolesAdd = formatRolesString(item.getOn_use_roles_add(), event.getGuild());
      description.append(rolesAdd.isEmpty() ? "None" : rolesAdd);

      // Handle roles to remove on use
      description.append("\nRoles Remove on Use: ");
      String rolesRemove = formatRolesString(item.getOn_use_roles_remove(), event.getGuild());
      description.append(rolesRemove.isEmpty() ? "None" : rolesRemove);

      // Handle groups to add on use
      description.append("\nGroups Parent on Use: ");
      String groupsAdd = formatGroupsString(item.getOn_use_groups_add());
      description.append(groupsAdd.isEmpty() ? "None" : groupsAdd);

      // Handle groups to remove on use
      description.append("\nGroups Unparent on Use: ");
      String groupsRemove = formatGroupsString(item.getOn_use_groups_remove());
      description.append(groupsRemove.isEmpty() ? "None" : groupsRemove);

      // Handle console commands on use
      description.append("\nCommands on Use: ");
      String commands = formatCommandsString(item.getOn_use_console_commands());
      description.append(commands.isEmpty() ? "None" : commands);

      embed.addField(item.getName() + " - `" + item.getId() + "`", description.toString(), false);
    }

    event.replyEmbeds(embed.build()).queue();
  }

  private void handleItemsEdit(SlashCommandInteractionEvent event) {
    String itemId = event.getOption("id", OptionMapping::getAsString);
    if (itemId == null) {
      event.reply("ID cannot be null!").setEphemeral(true).queue();
      return;
    }

    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item does not exist.").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();
    EmbedBuilder embed = getEditEmbed(item, Objects.requireNonNull(event.getGuild()));

    event.replyEmbeds(embed.build()).setEphemeral(true)
        .addActionRow(getEditDropdown(itemId).build()).queue();
  }

  private void handleItemsAdd(SlashCommandInteractionEvent event) {
    String name = event.getOption("name", OptionMapping::getAsString);
    if (name == null) {
      event.reply("Name cannot be null!").setEphemeral(true).queue();
      return;
    }

    String description = event.getOption("description", OptionMapping::getAsString);
    if (description == null) {
      event.reply("Description cannot be null!").setEphemeral(true).queue();
      return;
    }

    // Create new item
    Item newItem = new Item();
    newItem.setName(name);
    newItem.setDescription(description);

    // Make sure price is explicitly set to avoid validation errors
    newItem.setPrice(1.0); // Required by PocketBase

    // Optional fields with defaults
    newItem.setSale_percent(0.0);
    newItem.setQuantity(-1); // Infinite
    newItem.setEnabled(false); // Disabled by default
    newItem.setGifting_enabled(false);
    newItem.setUse_disabled(false);
    newItem.setUse_on_purchase(false);
    newItem.setMax_allowed(-1); // Infinite

    try {
      // Save item to get an ID
      Item savedItem = itemRepository.save(newItem);

      // Create edit embed
      EmbedBuilder embed = getEditEmbed(savedItem, Objects.requireNonNull(event.getGuild()));

      event.reply("The item has been created, but not enabled. Edit it below and set it to enabled when ready.")
          .addEmbeds(embed.build())
          .setEphemeral(true)
          .addActionRow(getEditDropdown(savedItem.getId()).build())
          .queue();
    } catch (Exception e) {
      LOGGER.warning("Failed to create item: " + e.getMessage());
      event.reply("Error creating item: " + e.getMessage()).setEphemeral(true).queue();
    }
  }

  private void handleItemsRemove(SlashCommandInteractionEvent event) {
    String itemId = event.getOption("id", OptionMapping::getAsString);
    if (itemId == null) {
      event.reply("ID cannot be null!").setEphemeral(true).queue();
      return;
    }

    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item could not be found.").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();

    // Create confirmation button
    Button confirm = Button.danger("item-" + itemId + "-delete", "Yes, permanently delete.");

    // Create description
    StringBuilder description = new StringBuilder();
    if (item.getDescription() != null) {
      description.append(item.getDescription()).append("\n\n");
    }

    // Add item details
    description.append("Price: ").append(Bot.CURRENCY_SYMBOL).append(" ")
        .append(item.getPrice() != null ? Bot.formatCurrency(item.getPrice()) : "0.00");
    description.append("\nSale Percent: ").append(item.getSale_percent() != null ? item.getSale_percent() : "0")
        .append("%");
    description.append("\nQuantity: ");
    if (item.getQuantity() == null || item.getQuantity() < 0) {
      description.append("Infinite");
    } else {
      description.append(item.getQuantity());
    }
    description.append("\nEnabled: ").append(item.getEnabled() != null ? item.getEnabled() : false);
    // Add remaining properties similar to list command

    // Create confirmation embed
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Are you sure you want to delete this item?")
        .setDescription(description)
        .setFooter(
            "If you delete this item, it will be permanently removed from the shop and be cleared from all users. If you instead just want to disable this item, use /economy items edit. This action cannot be undone.");

    event.replyEmbeds(embed.build()).addActionRow(confirm).setEphemeral(true).queue();
  }

  private void handleUsersBalance(SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    if (user == null) {
      event.reply("That user could not be found.").setEphemeral(true).queue();
      return;
    }

    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(user.getIdLong());
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();
    double balance = dvUser.getBalance() != null ? dvUser.getBalance() : 0.0;

    try {
      double newBalance = event.getOption("new-balance", OptionMapping::getAsDouble);

      // Update user balance
      dvUser.setBalance(newBalance);
      userRepository.save(dvUser);

      EmbedBuilder embed = new EmbedBuilder();
      embed.setAuthor(user.getName(), null, user.getAvatarUrl());
      embed.setDescription("Changed " + user.getAsMention() + "'s balance from " +
          Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(balance) +
          " to " + Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(newBalance));

      event.replyEmbeds(embed.build()).queue();
    } catch (NullPointerException e) {
      // Just display current balance
      EmbedBuilder embed = new EmbedBuilder();
      embed.setAuthor(user.getName(), null, user.getAvatarUrl());
      embed.setDescription(user.getAsMention() + "'s current balance is " +
          Bot.CURRENCY_SYMBOL + " " + Bot.formatCurrency(balance));

      event.replyEmbeds(embed.build()).queue();
    }
  }

  private void handleUsersGetItems(SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    if (user == null) {
      event.reply("That user could not be found.").setEphemeral(true).queue();
      return;
    }

    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(user.getIdLong());
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Get user's inventory
    List<UserInventory> inventoryItems = userInventoryRepository.findByUser(dvUser.getId());

    // Load related items
    userInventoryRepository.loadRelatedItems(inventoryItems);

    EmbedBuilder embed = new EmbedBuilder();
    embed.setAuthor(user.getName(), null, user.getAvatarUrl());
    embed.setTitle(user.getEffectiveName() + "'s Inventory");

    if (inventoryItems.isEmpty()) {
      embed.setDescription("This user has no items in their inventory.");
      event.replyEmbeds(embed.build()).queue();
      return;
    }

    for (UserInventory inventory : inventoryItems) {
      Item item = inventory.getCachedItem();
      if (item != null && inventory.getQuantity() != null && inventory.getQuantity() > 0) {
        embed.addField(inventory.getQuantity() + " " + item.getName(),
            "`" + item.getId() + "`", true);
      }
    }

    event.replyEmbeds(embed.build()).queue();
  }

  private void handleUsersSetItems(SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    String itemId = event.getOption("id", OptionMapping::getAsString);
    Integer quantity = event.getOption("new-quantity", OptionMapping::getAsInt);

    if (user == null) {
      event.reply("That user could not be found.").setEphemeral(true).queue();
      return;
    }

    if (itemId == null || quantity == null) {
      event.reply("Item ID and quantity cannot be null!").setEphemeral(true).queue();
      return;
    }

    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item does not exist!").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();

    if (quantity < 0) {
      event.reply("Quantity must be a positive number!").setEphemeral(true).queue();
      return;
    }

    // Check max allowed
    if (item.getMax_allowed() != null && item.getMax_allowed() > 0 && quantity > item.getMax_allowed()) {
      event.reply("You cannot set the quantity to above the max allowed!").setEphemeral(true).queue();
      return;
    }

    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(user.getIdLong());
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();

    // Find or create inventory entry
    Optional<UserInventory> inventoryOpt = userInventoryRepository.findByUserAndItem(dvUser.getId(), itemId);
    UserInventory inventory;

    if (inventoryOpt.isPresent()) {
      inventory = inventoryOpt.get();
      inventory.setQuantity(quantity);
    } else {
      inventory = new UserInventory();
      inventory.setUser(dvUser.getId());
      inventory.setItem(itemId);
      inventory.setQuantity(quantity);
    }

    // Save inventory
    if (quantity == 0) {
      // Remove the entry if quantity is 0
      if (inventoryOpt.isPresent()) {
        userInventoryRepository.delete(inventory);
      }
    } else {
      userInventoryRepository.save(inventory);
    }

    event.reply("Set quantity of " + item.getName() + " owned by " +
        user.getAsMention() + " to " + quantity + ".").queue();
  }

  private void handleUsersDailyStreak(SlashCommandInteractionEvent event) {
    User user = event.getOption("user", OptionMapping::getAsUser);
    if (user == null) {
      event.reply("User cannot be null!").setEphemeral(true).queue();
      return;
    }

    Optional<DVUser> userOpt = userRepository.findBySnowflakeId(user.getIdLong());
    if (!userOpt.isPresent()) {
      event.reply("That user doesn't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOpt.get();
    int currentStreak = dvUser.getDaily_streak() != null ? dvUser.getDaily_streak() : 0;

    Integer newValue = event.getOption("new-value", OptionMapping::getAsInt);
    if (newValue == null) {
      // Just show current streak
      EmbedBuilder embed = new EmbedBuilder();
      embed.setDescription(user.getAsMention() + " has a daily streak of " + currentStreak);
      event.replyEmbeds(embed.build()).queue();
      return;
    }

    // Update streak and reset cooldown
    dvUser.setDaily_streak(newValue);
    dvUser.setLast_daily(OffsetDateTime.now().minusHours(24)); // Reset cooldown
    userRepository.save(dvUser);

    EmbedBuilder embed = new EmbedBuilder();
    embed.setDescription(user.getAsMention() + "'s streak has been set to " +
        newValue + " and their cooldown has been reset.");

    event.replyEmbeds(embed.build()).queue();
  }

  @NotNull
  private EmbedBuilder getEditEmbed(@NotNull Item item, @NotNull Guild guild) {
    EmbedBuilder embed = new EmbedBuilder();

    StringBuilder description = new StringBuilder();

    if (item.getDescription() != null) {
      description.append(item.getDescription()).append("\n\n");
    }

    // Add item properties
    description.append("Price: ").append(Bot.CURRENCY_SYMBOL).append(" ")
        .append(item.getPrice() != null ? Bot.formatCurrency(item.getPrice()) : "0.00");
    description.append("\nSale Percent: ").append(item.getSale_percent() != null ? item.getSale_percent() : "0")
        .append("%");
    description.append("\nQuantity: ");
    if (item.getQuantity() == null || item.getQuantity() < 0) {
      description.append("Infinite");
    } else {
      description.append(item.getQuantity());
    }
    description.append("\nEnabled: ").append(item.getEnabled() != null ? item.getEnabled() : false);
    description.append("\nGifting: ").append(item.getGifting_enabled() != null ? item.getGifting_enabled() : false);
    description.append("\nUsable: ").append(item.getUse_disabled() != null ? !item.getUse_disabled() : true);
    description.append("\nUse on Purchase: ")
        .append(item.getUse_on_purchase() != null ? item.getUse_on_purchase() : false);
    description.append("\nMax Allowed: ");
    if (item.getMax_allowed() == null || item.getMax_allowed() < 0) {
      description.append("Infinite");
    } else {
      description.append(item.getMax_allowed());
    }

    // Add roles/groups/commands
    description.append("\nRoles Add on Use: ").append(formatRolesString(item.getOn_use_roles_add(), guild));
    description.append("\nRoles Remove on Use: ").append(formatRolesString(item.getOn_use_roles_remove(), guild));
    description.append("\nGroups Parent on Use: ").append(formatGroupsString(item.getOn_use_groups_add()));
    description.append("\nGroups Unparent on Use: ").append(formatGroupsString(item.getOn_use_groups_remove()));
    description.append("\nCommands on Use: ").append(formatCommandsString(item.getOn_use_console_commands()));

    embed.setTitle(item.getName() + " - " + item.getId());
    embed.setDescription(description.toString());
    embed.setFooter("Choose a property from the select menu to edit.");

    return embed;
  }

  @NotNull
  private StringSelectMenu.Builder getEditDropdown(String itemId) {
    StringSelectMenu.Builder selectMenu = StringSelectMenu.create("item-" + itemId + "-edit-select");
    return selectMenu
        .addOption("Name", "name", "The name of the item.")
        .addOption("Description", "description", "The description of the item.")
        .addOption("Price", "price", "The regular price of the item.")
        .addOption("Sale Percent", "salePercent", "The percent to remove from the regular price.")
        .addOption("Quantity", "quantity", "The quantity of this item available in the shop.")
        .addOption("Max Allowed", "maxAllowed", "The maximum quantity of this item a user can have at a time.")
        .addOption("Enabled", "enabled", "Whether the item can be bought. Disabled items can still be used.")
        .addOption("Gifting Enabled", "giftingEnabled", "Where the item can be transferred to another user.")
        .addOption("Use Disabled", "useDisabled", "Whether the item's use is disabled. Useful for vanity items.")
        .addOption("Use On Purchase", "useOnPurchase", "Whether the item is auto-used upon purchase.")
        .addOption("Roles to Add", "onUseRolesAdd", "The roles to add upon use.")
        .addOption("Roles to Remove", "onUseRolesRemove", "The roles to remove upon use.")
        .addOption("Groups to Add", "onUseGroupsAdd", "The permission groups to add upon use.")
        .addOption("Groups to Remove", "onUseGroupsRemove", "The permission groups to remove upon use.")
        .addOption("Console Commands", "onUseConsoleCommands", "The commands to execute upon use.");
  }

  @Override
  public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
    String id = event.getComponentId();
    String[] splitId = id.split("-");

    if (event.getSelectedOptions().isEmpty())
      return;

    String type = splitId[0];
    if (!type.equals("item"))
      return;

    String itemId = splitId[1];
    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item could not be found.").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();
    String action = splitId[2];

    if (action.equals("edit")) {
      String subAction = splitId[3];
      if (subAction.equals("select")) {
        String value = event.getSelectedOptions().get(0).getValue();

        if (value.equals("enabled") || value.equals("giftingEnabled") ||
            value.equals("useDisabled") || value.equals("useOnPurchase")) {

          StringSelectMenu.Builder toggleMenu = StringSelectMenu.create("item-" + itemId + "-edit-toggle-" + value);
          toggleMenu.addOption("True", "true").addOption("False", "false");
          toggleMenu.setPlaceholder("Set to true or false");

          event.editComponents(event.getMessage().getActionRows().get(0)).queue();
          List<net.dv8tion.jda.api.interactions.components.LayoutComponent> components = new ArrayList<>(
              event.getMessage().getComponents());
          components.add(ActionRow.of(toggleMenu.build()));
          event.getHook().editOriginalComponents(components).queue();

        } else if (value.equals("onUseRolesAdd") || value.equals("onUseRolesRemove")) {

          EntitySelectMenu.Builder selectMenu;

          if (value.equals("onUseRolesAdd")) {
            selectMenu = EntitySelectMenu.create("item-" + itemId + "-edit-roles-add",
                EntitySelectMenu.SelectTarget.ROLE);
          } else {
            selectMenu = EntitySelectMenu.create("item-" + itemId + "-edit-roles-remove",
                EntitySelectMenu.SelectTarget.ROLE);
          }

          selectMenu.setPlaceholder("Select roles to include");

          event.editComponents(event.getMessage().getActionRows().get(0)).queue();
          List<net.dv8tion.jda.api.interactions.components.LayoutComponent> components = new ArrayList<>(
              event.getMessage().getComponents());
          components.add(ActionRow.of(selectMenu.build()));
          event.getHook().editOriginalComponents(components).queue();

        } else {
          Modal.Builder modal = Modal.create("item-" + itemId + "-edit-" + value,
              "Change " + value + " of item " + itemId);
          modal.addActionRow(TextInput.create("new" + value, "New " + value, TextInputStyle.PARAGRAPH).build());
          event.replyModal(modal.build()).queue();
          event.getHook().editOriginalComponents(event.getMessage().getActionRows().get(0)).queue();
        }
      } else if (subAction.equals("toggle")) {
        String value = splitId[4];
        boolean bool = Boolean.parseBoolean(event.getSelectedOptions().get(0).getValue());

        switch (value) {
          case "enabled":
            item.setEnabled(bool);
            break;
          case "giftingEnabled":
            item.setGifting_enabled(bool);
            break;
          case "useDisabled":
            item.setUse_disabled(bool);
            break;
          case "useOnPurchase":
            item.setUse_on_purchase(bool);
            break;
          default: {
            event.reply("Did not expect this property to be set this way!").setEphemeral(true).queue();
            return;
          }
        }

        itemRepository.save(item);
        event.editMessageEmbeds(getEditEmbed(item, Objects.requireNonNull(event.getGuild())).build()).queue();
        Objects.requireNonNull(event.getMessage())
            .reply("Toggled " + value + " of item `" + item.getId() + "` to " + bool + ".").queue();
      }
    }
  }

  @Override
  public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
    String id = event.getComponentId();
    String[] splitId = id.split("-");

    String type = splitId[0];
    if (!type.equals("item"))
      return;

    String itemId = splitId[1];
    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item could not be found.").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();
    String action = splitId[2];

    if (action.equals("edit")) {
      String subAction = splitId[3];
      if (subAction.equals("roles")) {
        String roleAction = splitId[4];

        // Convert roles to JSON array
        JSONArray rolesArray = new JSONArray();
        for (Role role : event.getMentions().getRoles()) {
          rolesArray.put(role.getId());
        }

        if (roleAction.equals("add")) {
          item.setOn_use_roles_add(rolesArray.toString());
        } else if (roleAction.equals("remove")) {
          item.setOn_use_roles_remove(rolesArray.toString());
        } else {
          event.reply("Unexpected request").queue();
          return;
        }

        itemRepository.save(item);
        event.editMessageEmbeds(getEditEmbed(item, Objects.requireNonNull(event.getGuild())).build()).queue();

        String fieldName = roleAction.equals("add") ? "onUseRolesAdd" : "onUseRolesRemove";
        Objects.requireNonNull(event.getMessage()).reply("Set " + fieldName + " of item `" + item.getId() +
            "` to " + event.getMentions().getRoles().size() + " role(s).").queue();
      }
    }
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String id = event.getButton().getId();
    if (id == null)
      return;

    String[] splitId = id.split("-");

    String type = splitId[0];
    if (!type.equals("item"))
      return;

    String itemId = splitId[1];
    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item could not be found.").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();
    String action = splitId[2];

    if (action.equals("delete")) {
      // Only Do this if the Item has no cascade delete enabled
      // Find all inventory entries for this item
      // List<UserInventory> inventoryEntries =
      // userInventoryRepository.getAllWhere("item = '" + itemId + "'");

      // // Delete all inventory entries
      // for (UserInventory entry : inventoryEntries) {
      // userInventoryRepository.delete(entry);
      // }

      // Delete the item
      itemRepository.delete(item);

      event.editMessage("Deleted item `" + item.getId() + "`.").queue();
      event.getHook().editOriginalEmbeds(new ArrayList<>()).queue();
      event.editButton(null).queue();
    }
  }

  @Override
  public void onModalInteraction(@NotNull ModalInteractionEvent event) {
    String id = event.getModalId();
    String[] splitId = id.split("-");

    String type = splitId[0];
    if (!type.equals("item"))
      return;

    String itemId = splitId[1];
    Optional<Item> itemOpt = itemRepository.findById(itemId);
    if (!itemOpt.isPresent()) {
      event.reply("That item does not exist!").setEphemeral(true).queue();
      return;
    }

    Item item = itemOpt.get();
    String action = splitId[2];

    if (action.equals("edit")) {
      String editValue = splitId[3];
      String modalResponse = event.getValues().get(0).getAsString();

      try {
        switch (editValue) {
          case "name": {
            item.setName(modalResponse);
            break;
          }
          case "description":
            item.setDescription(modalResponse);
            break;
          case "price":
            item.setPrice(Double.parseDouble(modalResponse));
            break;
          case "salePercent":
            item.setSale_percent(Double.parseDouble(modalResponse));
            break;
          case "quantity":
            item.setQuantity(Integer.parseInt(modalResponse));
            break;
          case "maxAllowed":
            item.setMax_allowed(Integer.parseInt(modalResponse));
            break;
          case "onUseGroupsAdd": {
            // Convert comma-separated values to JSON array
            JSONArray groupsArray = new JSONArray();
            for (String s : modalResponse.split(",")) {
              groupsArray.put(s.trim());
            }
            item.setOn_use_groups_add(groupsArray.toString());
          }
            break;
          case "onUseGroupsRemove": {
            JSONArray groupsArray = new JSONArray();
            for (String s : modalResponse.split(",")) {
              groupsArray.put(s.trim());
            }
            item.setOn_use_groups_remove(groupsArray.toString());
          }
            break;
          case "onUseConsoleCommands": {
            JSONArray commandsArray = new JSONArray();
            for (String s : modalResponse.split(",")) {
              commandsArray.put(s.trim());
            }
            item.setOn_use_console_commands(commandsArray.toString());
          }
            break;
          default: {
            event.reply("Did not expect this property to be set this way!").setEphemeral(true).queue();
            return;
          }
        }
      } catch (NumberFormatException e) {
        event.reply("Could not parse response as a number!").setEphemeral(true).queue();
        return;
      }

      itemRepository.save(item);
      event.editMessageEmbeds(getEditEmbed(item, Objects.requireNonNull(event.getGuild())).build()).queue();
      Objects.requireNonNull(event.getMessage())
          .reply("Changed " + editValue + " of item `" + itemId + "` to " + modalResponse).queue();
    }
  }

  // Helper methods to format strings for display
  private String formatRolesString(String rolesJson, Guild guild) {
    if (rolesJson == null || rolesJson.trim().isEmpty()) {
      return "None";
    }

    StringBuilder result = new StringBuilder();

    try {
      JSONArray rolesArray = new JSONArray(rolesJson);
      for (int i = 0; i < rolesArray.length(); i++) {
        String roleId;
        try {
          roleId = rolesArray.getString(i);
        } catch (JSONException e) {
          roleId = String.valueOf(rolesArray.getLong(i));
        }

        Role role = guild.getRoleById(roleId);
        if (role != null) {
          if (i > 0)
            result.append(", ");
          result.append(role.getAsMention());
        }
      }
    } catch (JSONException e) {
      LOGGER.warning("Error parsing roles JSON: " + e.getMessage());
      return "Error parsing roles";
    }

    return result.length() == 0 ? "None" : result.toString();
  }

  private String formatGroupsString(String groupsJson) {
    if (groupsJson == null || groupsJson.trim().isEmpty()) {
      return "None";
    }

    StringBuilder result = new StringBuilder();

    try {
      JSONArray groupsArray = new JSONArray(groupsJson);
      for (int i = 0; i < groupsArray.length(); i++) {
        if (i > 0)
          result.append(", ");
        result.append(groupsArray.getString(i));
      }
    } catch (JSONException e) {
      LOGGER.warning("Error parsing groups JSON: " + e.getMessage());
      return "Error parsing groups";
    }

    return result.length() == 0 ? "None" : result.toString();
  }

  private String formatCommandsString(String commandsJson) {
    if (commandsJson == null || commandsJson.trim().isEmpty()) {
      return "None";
    }

    StringBuilder result = new StringBuilder();

    try {
      JSONArray commandsArray = new JSONArray(commandsJson);
      for (int i = 0; i < commandsArray.length(); i++) {
        if (i > 0)
          result.append(", ");
        result.append(commandsArray.getString(i));
      }
    } catch (JSONException e) {
      LOGGER.warning("Error parsing commands JSON: " + e.getMessage());
      return "Error parsing commands";
    }

    return result.length() == 0 ? "None" : result.toString();
  }
}
