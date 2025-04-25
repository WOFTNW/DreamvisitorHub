package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.ArrayList;

public class DCmdInventory extends ListenerAdapter implements DiscordCommand {
  private static final Logger LOGGER = Logger.getLogger(DCmdInventory.class.getName());
  private final UserRepository userRepository = App.getUserRepository();
  private final ItemRepository itemRepository = App.getItemRepository();
  private final UserInventoryRepository userInventoryRepository = App.getUserInventoryRepository();

  // Store temporary gift selection state
  private final Map<Long, GiftProcess> giftProcesses = new HashMap<>();

  // Store temporary use item state
  private final Map<Long, String> useItemSelections = new HashMap<>();

  // Class to store gift selection state
  private static class GiftProcess {
    String itemId;
    String recipientId;
    int quantity = 1;
  }

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
      Button useButton = Button.primary("use_item", "Use Item");
      Button giftButton = Button.secondary("gift_item", "Gift Item");

      // Send embed with buttons
      event.getHook().sendMessageEmbeds(embed.build())
          .addActionRow(useButton, giftButton)
          .queue();
    }
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    String componentId = event.getComponentId();

    switch (componentId) {
      case "gift_item":
        handleGiftItemButtonClick(event);
        break;
      case "confirm_gift":
        handleConfirmGiftButtonClick(event);
        break;
      case "cancel_gift":
        handleCancelGiftButtonClick(event);
        break;
      case "set_quantity":
        handleSetQuantityButtonClick(event);
        break;
      case "use_item":
        handleUseItemButtonClick(event);
        break;
      case "confirm_use":
        handleConfirmUseButtonClick(event);
        break;
      case "cancel_use":
        handleCancelUseButtonClick(event);
        break;
    }
  }

  @Override
  public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
    String componentId = event.getComponentId();

    switch (componentId) {
      case "gift_item_selection":
        handleItemSelection(event);
        break;
      case "gift_recipient_selection":
        handleRecipientSelection(event);
        break;
      case "use_item_selection":
        handleUseItemSelection(event);
        break;
    }
  }

  @Override
  public void onModalInteraction(@NotNull ModalInteractionEvent event) {
    if (event.getModalId().equals("gift_quantity_modal")) {
      handleQuantityModalSubmit(event);
    }
  }

  private void handleItemSelection(StringSelectInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    String selectedItemId = event.getValues().get(0);

    // Get or create gift process for this user
    GiftProcess process = giftProcesses.getOrDefault(userId, new GiftProcess());
    process.itemId = selectedItemId;
    giftProcesses.put(userId, process);

    // Get item details to validate
    Optional<Item> itemOpt = itemRepository.findById(selectedItemId);
    if (!itemOpt.isPresent()) {
      event.reply("Error: Item not found.").setEphemeral(true).queue();
      return;
    }

    // No acknowledgment message needed - silently update the state
    event.deferEdit().queue();
  }

  private void handleRecipientSelection(StringSelectInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    String selectedRecipientId = event.getValues().get(0);

    // Get or create gift process for this user
    GiftProcess process = giftProcesses.getOrDefault(userId, new GiftProcess());
    process.recipientId = selectedRecipientId;
    giftProcesses.put(userId, process);

    // Get recipient details to validate
    Optional<DVUser> recipientOpt = userRepository.findById(selectedRecipientId);
    if (!recipientOpt.isPresent()) {
      event.reply("Error: Recipient not found.").setEphemeral(true).queue();
      return;
    }

    // No acknowledgment message needed - silently update the state
    event.deferEdit().queue();
  }

  private void handleSetQuantityButtonClick(ButtonInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    GiftProcess process = giftProcesses.get(userId);

    if (process == null) {
      process = new GiftProcess();
      giftProcesses.put(userId, process);
    }

    // Create the quantity input with current value if any
    TextInput quantityInput = TextInput.create("gift_quantity", "Quantity to Gift", TextInputStyle.SHORT)
        .setPlaceholder("Enter the quantity (number)")
        .setRequiredRange(1, 5)
        .setValue(String.valueOf(process.quantity))
        .build();

    // Create the modal
    Modal quantityModal = Modal.create("gift_quantity_modal", "Set Gift Quantity")
        .addActionRow(quantityInput)
        .build();

    // Show the modal
    event.replyModal(quantityModal).queue();
  }

  private void handleQuantityModalSubmit(ModalInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    String quantityStr = event.getValue("gift_quantity").getAsString();

    try {
      int quantity = Integer.parseInt(quantityStr);

      // Validate quantity (should be positive)
      if (quantity <= 0) {
        event.reply("Quantity must be a positive number.").setEphemeral(true).queue();
        return;
      }

      // Always preserve existing itemId and recipientId when updating quantity
      GiftProcess process = giftProcesses.getOrDefault(userId, new GiftProcess());

      // Log current state for debugging
      LOGGER.info("Updating quantity for user " + userId +
          " from " + process.quantity + " to " + quantity +
          ", itemId: " + process.itemId +
          ", recipientId: " + process.recipientId);

      // Only update the quantity, keeping other selections intact
      process.quantity = quantity;
      giftProcesses.put(userId, process);

      // Instead of a reply, just acknowledge that the modal was received
      event.deferEdit().queue();
    } catch (NumberFormatException e) {
      event.reply("Invalid quantity. Please enter a valid number.").setEphemeral(true).queue();
    }
  }

  private void handleConfirmGiftButtonClick(ButtonInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    GiftProcess process = giftProcesses.get(userId);

    if (process == null || process.itemId == null || process.recipientId == null) {
      LOGGER.warning("Gift process state missing for user " + userId +
          ", itemId: " + (process != null ? process.itemId : "null") +
          ", recipientId: " + (process != null ? process.recipientId : "null"));
      event.reply("Please select both an item and a recipient before confirming.").setEphemeral(true).queue();
      return;
    }

    // Defer reply to allow time for database operations
    event.deferReply(true).queue();

    try {
      // Get sender user from database
      Optional<DVUser> senderOpt = userRepository.findBySnowflakeId(userId);
      if (!senderOpt.isPresent()) {
        event.getHook().sendMessage("Error: Your user profile could not be found.").queue();
        return;
      }
      DVUser sender = senderOpt.get();

      // Get recipient from database
      Optional<DVUser> recipientOpt = userRepository.findById(process.recipientId);
      if (!recipientOpt.isPresent()) {
        event.getHook().sendMessage("Error: Recipient user could not be found.").queue();
        return;
      }
      DVUser recipient = recipientOpt.get();

      // Get item details
      Optional<Item> itemOpt = itemRepository.findById(process.itemId);
      if (!itemOpt.isPresent()) {
        event.getHook().sendMessage("Error: Item not found.").queue();
        return;
      }
      Item item = itemOpt.get();

      // Check if item allows gifting
      if (item.getGifting_enabled() != null && !item.getGifting_enabled()) {
        event.getHook().sendMessage("This item cannot be gifted.").queue();
        return;
      }

      // Find sender's inventory entry for this item
      Optional<UserInventory> senderInventoryOpt = userInventoryRepository.findByUserAndItem(sender.getId(),
          process.itemId);
      if (!senderInventoryOpt.isPresent()) {
        event.getHook().sendMessage("You don't have this item in your inventory.").queue();
        return;
      }
      UserInventory senderInventory = senderInventoryOpt.get();

      // Check if sender has enough of the item
      int senderQuantity = senderInventory.getQuantity() != null ? senderInventory.getQuantity() : 0;
      if (senderQuantity < process.quantity) {
        // Important: Don't remove the gift process when validation fails
        event.getHook().sendMessage("You don't have enough of this item. You have " + senderQuantity +
            " but are trying to gift " + process.quantity + ".\n" +
            "Please use the 'Set Quantity' button to set a valid amount.").queue();
        return;
      }

      // Find or create recipient's inventory entry
      Optional<UserInventory> recipientInventoryOpt = userInventoryRepository.findByUserAndItem(
          recipient.getId(), process.itemId);
      UserInventory recipientInventory;

      if (recipientInventoryOpt.isPresent()) {
        // Update existing inventory entry
        recipientInventory = recipientInventoryOpt.get();
        int currentQuantity = recipientInventory.getQuantity() != null ? recipientInventory.getQuantity() : 0;
        recipientInventory.setQuantity(currentQuantity + process.quantity);
      } else {
        // Create new inventory entry for recipient
        recipientInventory = new UserInventory();
        recipientInventory.setUser(recipient.getId());
        recipientInventory.setItem(process.itemId);
        recipientInventory.setQuantity(process.quantity);
      }

      // Update sender's inventory
      senderInventory.setQuantity(senderQuantity - process.quantity);

      // Perform database operations
      if (senderInventory.getQuantity() <= 0) {
        // Remove item from inventory if quantity is 0
        userInventoryRepository.delete(senderInventory);
      } else {
        // Update with new quantity
        userInventoryRepository.save(senderInventory);
      }

      // Save recipient's inventory
      userInventoryRepository.save(recipientInventory);

      // Get recipient's actual Discord user if possible for mentioning
      String recipientName = recipient.getDiscord_username() != null ? recipient.getDiscord_username()
          : (recipient.getMcUsername() != null ? recipient.getMcUsername() : "Unknown User");

      String recipientMention = "";
      User recipientUser = null;
      if (recipient.getSnowflakeId() != null) {
        try {
          recipientUser = Bot.getJda().retrieveUserById(recipient.getSnowflakeId()).complete();
          if (recipientUser != null) {
            recipientMention = recipientUser.getAsMention();
            recipientName = recipientUser.getName();
          }
        } catch (Exception e) {
          LOGGER.warning("Failed to retrieve recipient user: " + e.getMessage());
          // Continue with just the name if user retrieval fails
        }
      }

      // Send confirmation messages
      EmbedBuilder embed = new EmbedBuilder()
          .setTitle("Gift Sent!")
          .setDescription("You've successfully gifted **" + process.quantity + "x " + item.getName() +
              "** to **" + recipientName + "**.")
          .setColor(Color.GREEN)
          .setTimestamp(Instant.now());

      event.getHook().sendMessageEmbeds(embed.build()).queue();

      // Notify recipient if they have a Discord account linked
      if (!recipientMention.isEmpty()) {
        EmbedBuilder recipientEmbed = new EmbedBuilder()
            .setTitle("Gift Received!")
            .setDescription("You've received **" + process.quantity + "x " + item.getName() +
                "** from **" + event.getUser().getName() + "**.")
            .setColor(Color.GREEN)
            .setTimestamp(Instant.now());

        // Try to send a DM to the recipient
        try {
          recipientUser.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(recipientEmbed.build()).queue(
              success -> {
              },
              error -> LOGGER.warning("Failed to send DM to recipient: " + error.getMessage())));
        } catch (Exception e) {
          LOGGER.warning("Failed to open private channel with recipient: " + e.getMessage());
        }
      }

      // Only remove the gift process if the gift was successful
      giftProcesses.remove(userId);

    } catch (Exception e) {
      LOGGER.severe("Error processing gift: " + e.getMessage());
      e.printStackTrace();
      event.getHook().sendMessage("An error occurred while processing your gift. Please try again later.").queue();
      // Don't remove the gift process on error so the user can try again
    }
  }

  private void handleCancelGiftButtonClick(ButtonInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    giftProcesses.remove(userId);

    // Keep this reply as it's important feedback that the action was canceled
    event.reply("Gift process canceled.").setEphemeral(true).queue();
  }

  private void handleGiftItemButtonClick(ButtonInteractionEvent event) {
    // Get the user's inventory items
    long discordId = event.getUser().getIdLong();
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(discordId);

    if (!userOptional.isPresent()) {
      event.reply("You don't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOptional.get();
    List<UserInventory> inventoryItems = userInventoryRepository.findByUser(dvUser.getId());

    if (inventoryItems.isEmpty()) {
      event.reply("You don't have any items to gift.").setEphemeral(true).queue();
      return;
    }

    // Load related items for inventory
    userInventoryRepository.loadRelatedItems(inventoryItems);

    // Create item selection menu
    StringSelectMenu.Builder itemSelectBuilder = StringSelectMenu.create("gift_item_selection")
        .setPlaceholder("Select an item to gift")
        .setRequiredRange(1, 1);

    // Add inventory items to the selection menu
    for (UserInventory inventoryItem : inventoryItems) {
      Item item = inventoryItem.getCachedItem();
      if (item != null && inventoryItem.getQuantity() != null && inventoryItem.getQuantity() > 0) {
        // Only add items that the user has and that are enabled for gifting
        if (item.getGifting_enabled() == null || item.getGifting_enabled()) {
          String itemName = item.getName() != null ? item.getName() : "Unknown Item";
          String itemId = item.getId();
          String description = "Quantity: " + inventoryItem.getQuantity();

          itemSelectBuilder.addOption(
              itemName,
              itemId,
              description);
        }
      }
    }

    // Get a list of all users that have profiles
    List<DVUser> allUsers = userRepository.findAll();

    // Create user selection menu
    StringSelectMenu.Builder userSelectBuilder = StringSelectMenu.create("gift_recipient_selection")
        .setPlaceholder("Select a user to gift to")
        .setRequiredRange(1, 1);

    // Add users to the selection menu (exclude self)
    for (DVUser otherUser : allUsers) {
      if (otherUser.getSnowflakeId() != null && otherUser.getSnowflakeId() != discordId) {
        String displayName = otherUser.getDiscord_username() != null ? otherUser.getDiscord_username()
            : (otherUser.getMcUsername() != null ? otherUser.getMcUsername() : "Unknown User");

        userSelectBuilder.addOption(
            displayName,
            otherUser.getId(),
            "User ID: " + otherUser.getId());
      }
    }

    // Create the gift form embed with explanation
    EmbedBuilder embed = new EmbedBuilder()
        .setTitle("Gift an Item")
        .setDescription("Please select an item to gift, a recipient, and set the quantity.")
        .setColor(Color.BLUE)
        .addField("Step 1", "Select an item from your inventory below.", false)
        .addField("Step 2", "Select a user to gift to.", false)
        .addField("Step 3", "Click the 'Set Quantity' button to specify how many items to gift.", false)
        .addField("Note", "You will be asked to confirm before the gift is sent.", false);

    // Create buttons for quantity and confirmation
    Button setQuantityButton = Button.primary("set_quantity", "Set Quantity");
    Button confirmButton = Button.success("confirm_gift", "Confirm Gift");
    Button cancelButton = Button.danger("cancel_gift", "Cancel");

    // Send the ephemeral message with all components
    event.replyEmbeds(embed.build())
        .addActionRow(itemSelectBuilder.build())
        .addActionRow(userSelectBuilder.build())
        .addActionRow(setQuantityButton, confirmButton, cancelButton)
        .setEphemeral(true) // Make the message only visible to the user
        .queue();
  }

  private void handleUseItemButtonClick(ButtonInteractionEvent event) {
    // Get the user's inventory items
    long discordId = event.getUser().getIdLong();
    Optional<DVUser> userOptional = userRepository.findBySnowflakeId(discordId);

    if (!userOptional.isPresent()) {
      event.reply("You don't have a profile yet.").setEphemeral(true).queue();
      return;
    }

    DVUser dvUser = userOptional.get();
    List<UserInventory> inventoryItems = userInventoryRepository.findByUser(dvUser.getId());

    if (inventoryItems.isEmpty()) {
      event.reply("You don't have any items to use.").setEphemeral(true).queue();
      return;
    }

    // Load related items for inventory
    userInventoryRepository.loadRelatedItems(inventoryItems);

    // Create item selection menu
    StringSelectMenu.Builder itemSelectBuilder = StringSelectMenu.create("use_item_selection")
        .setPlaceholder("Select an item to use")
        .setRequiredRange(1, 1);

    // Add inventory items to the selection menu (only those that can be used)
    boolean hasUsableItems = false;
    for (UserInventory inventoryItem : inventoryItems) {
      Item item = inventoryItem.getCachedItem();
      if (item != null && inventoryItem.getQuantity() != null && inventoryItem.getQuantity() > 0) {
        // Skip items that have use disabled
        if (item.getUse_disabled() != null && item.getUse_disabled()) {
          continue;
        }

        String itemName = item.getName() != null ? item.getName() : "Unknown Item";
        String itemId = item.getId();
        String description = "Quantity: " + inventoryItem.getQuantity();

        itemSelectBuilder.addOption(
            itemName,
            itemId,
            description);
        hasUsableItems = true;
      }
    }

    if (!hasUsableItems) {
      event.reply("You don't have any items that can be used.").setEphemeral(true).queue();
      return;
    }

    // Create the use item embed
    EmbedBuilder embed = new EmbedBuilder()
        .setTitle("Use an Item")
        .setDescription("Please select an item to use from your inventory.")
        .setColor(Color.BLUE)
        .addField("Note", "Using an item will consume it and apply its effects.", false);

    // Create confirmation and cancel buttons
    Button confirmButton = Button.success("confirm_use", "Use Item");
    Button cancelButton = Button.danger("cancel_use", "Cancel");

    // Send the ephemeral message with all components
    event.replyEmbeds(embed.build())
        .addActionRow(itemSelectBuilder.build())
        .addActionRow(confirmButton, cancelButton)
        .setEphemeral(true)
        .queue();
  }

  private void handleUseItemSelection(StringSelectInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    String selectedItemId = event.getValues().get(0);

    // Store the selected item
    useItemSelections.put(userId, selectedItemId);

    // Get item details to validate
    Optional<Item> itemOpt = itemRepository.findById(selectedItemId);
    if (!itemOpt.isPresent()) {
      event.reply("Error: Item not found.").setEphemeral(true).queue();
      return;
    }

    // Silently acknowledge the selection
    event.deferEdit().queue();
  }

  private void handleConfirmUseButtonClick(ButtonInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    String itemId = useItemSelections.get(userId);

    if (itemId == null) {
      event.reply("Please select an item to use first.").setEphemeral(true).queue();
      return;
    }

    // Defer reply to allow time for processing
    event.deferReply(true).queue();

    try {
      // Get user from database
      Optional<DVUser> userOpt = userRepository.findBySnowflakeId(userId);
      if (!userOpt.isPresent()) {
        event.getHook().sendMessage("Error: Your user profile could not be found.").queue();
        return;
      }
      DVUser user = userOpt.get();

      // Get item details
      Optional<Item> itemOpt = itemRepository.findById(itemId);
      if (!itemOpt.isPresent()) {
        event.getHook().sendMessage("Error: Item not found.").queue();
        return;
      }
      Item item = itemOpt.get();

      // Check if item can be used
      if (item.getUse_disabled() != null && item.getUse_disabled()) {
        event.getHook().sendMessage("This item cannot be used.").queue();
        return;
      }

      // Find user's inventory entry for this item
      Optional<UserInventory> inventoryOpt = userInventoryRepository.findByUserAndItem(user.getId(), itemId);
      if (!inventoryOpt.isPresent()) {
        event.getHook().sendMessage("You don't have this item in your inventory.").queue();
        return;
      }
      UserInventory inventory = inventoryOpt.get();

      // Check quantity
      int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
      if (quantity <= 0) {
        event.getHook().sendMessage("You don't have any of this item left.").queue();
        return;
      }

      // Process the item usage effects
      StringBuilder effectsDescription = new StringBuilder();
      boolean hadEffects = false;

      // Add roles if specified
      if (item.getOn_use_roles_add() != null && !item.getOn_use_roles_add().isEmpty()) {
        try {
          // Clean up and validate the JSON string
          String rolesJson = item.getOn_use_roles_add().trim();
          LOGGER.info("Processing roles_add JSON: " + rolesJson);

          // Ensure the JSON starts with [
          if (!rolesJson.startsWith("[")) {
            rolesJson = "[" + rolesJson;
          }

          // Ensure the JSON ends with ]
          if (!rolesJson.endsWith("]")) {
            rolesJson = rolesJson + "]";
          }

          // Parse the JSON array
          org.json.JSONArray rolesArray = new org.json.JSONArray(rolesJson);

          // Process each role
          for (int i = 0; i < rolesArray.length(); i++) {
            // Handle both string and numeric role IDs
            String roleId;
            try {
              // First try to get as string
              roleId = rolesArray.getString(i);
            } catch (org.json.JSONException e) {
              // If not a string, try to get as number and convert to string
              try {
                roleId = String.valueOf(rolesArray.getLong(i));
                LOGGER.info("Converted numeric role ID to string: " + roleId);
              } catch (Exception e2) {
                LOGGER.warning("Role ID at index " + i + " is neither a string nor a number. Skipping.");
                continue;
              }
            }

            try {
              // Get the Discord role
              net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
              if (role != null) {
                // Add the role to the user
                event.getGuild().addRoleToMember(event.getMember(), role).queue();
                effectsDescription.append("- Added role: **").append(role.getName()).append("**\n");
                hadEffects = true;
              } else {
                LOGGER.warning("Role not found with ID: " + roleId);
              }
            } catch (Exception e) {
              LOGGER.warning("Failed to add role " + roleId + ": " + e.getMessage());
            }
          }
        } catch (Exception e) {
          LOGGER
              .warning("Error parsing roles_add JSON: " + e.getMessage() + " for JSON: " + item.getOn_use_roles_add());
        }
      }

      // Remove roles if specified - use same robust parsing approach
      if (item.getOn_use_roles_remove() != null && !item.getOn_use_roles_remove().isEmpty()) {
        try {
          // Clean up and validate the JSON string
          String rolesJson = item.getOn_use_roles_remove().trim();
          LOGGER.info("Processing roles_remove JSON: " + rolesJson);

          // Ensure the JSON starts with [
          if (!rolesJson.startsWith("[")) {
            rolesJson = "[" + rolesJson;
          }

          // Ensure the JSON ends with ]
          if (!rolesJson.endsWith("]")) {
            rolesJson = rolesJson + "]";
          }

          // Parse the JSON array
          org.json.JSONArray rolesArray = new org.json.JSONArray(rolesJson);

          // Process each role
          for (int i = 0; i < rolesArray.length(); i++) {
            // Handle both string and numeric role IDs
            String roleId;
            try {
              // First try to get as string
              roleId = rolesArray.getString(i);
            } catch (org.json.JSONException e) {
              // If not a string, try to get as number and convert to string
              try {
                roleId = String.valueOf(rolesArray.getLong(i));
                LOGGER.info("Converted numeric role ID to string: " + roleId);
              } catch (Exception e2) {
                LOGGER.warning("Role ID at index " + i + " is neither a string nor a number. Skipping.");
                continue;
              }
            }

            try {
              // Get the Discord role
              net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
              if (role != null) {
                // Remove the role from the user
                event.getGuild().removeRoleFromMember(event.getMember(), role).queue();
                effectsDescription.append("- Removed role: **").append(role.getName()).append("**\n");
                hadEffects = true;
              } else {
                LOGGER.warning("Role not found with ID: " + roleId);
              }
            } catch (Exception e) {
              LOGGER.warning("Failed to remove role " + roleId + ": " + e.getMessage());
            }
          }
        } catch (Exception e) {
          LOGGER.warning(
              "Error parsing roles_remove JSON: " + e.getMessage() + " for JSON: " + item.getOn_use_roles_remove());
        }
      }

      // Reduce inventory quantity
      inventory.setQuantity(quantity - 1);

      // Update inventory in database
      if (inventory.getQuantity() <= 0) {
        // Remove item from inventory if quantity is 0
        userInventoryRepository.delete(inventory);
      } else {
        // Update with new quantity
        userInventoryRepository.save(inventory);
      }

      // Create response embed
      EmbedBuilder embed = new EmbedBuilder()
          .setTitle("Item Used")
          .setDescription("You used **" + item.getName() + "**")
          .setColor(Color.GREEN)
          .setTimestamp(Instant.now());

      if (hadEffects) {
        embed.addField("Effects Applied", effectsDescription.toString(), false);
      } else {
        embed.addField("Note", "This item had no visible effects.", false);
      }

      // Add remaining quantity info
      if (inventory.getQuantity() > 0) {
        embed.setFooter("You have " + inventory.getQuantity() + " remaining.");
      } else {
        embed.setFooter("You have no more of this item.");
      }

      // Send response
      event.getHook().sendMessageEmbeds(embed.build()).queue();

      // Clean up
      useItemSelections.remove(userId);

    } catch (Exception e) {
      LOGGER.severe("Error using item: " + e.getMessage());
      e.printStackTrace();
      event.getHook().sendMessage("An error occurred while using the item. Please try again later.").queue();
    }
  }

  private void handleCancelUseButtonClick(ButtonInteractionEvent event) {
    long userId = event.getUser().getIdLong();
    useItemSelections.remove(userId);
    event.reply("Item usage canceled.").setEphemeral(true).queue();
  }

  /**
   * Safely parses a string that might contain a JSON array of role IDs
   * Can handle role IDs stored as numbers or strings in the JSON
   *
   * @param jsonString The string to parse
   * @return A list of role ID strings or empty list if parsing fails
   */
  private List<String> safeParseRoleIds(String jsonString) {
    List<String> roleIds = new ArrayList<>();

    if (jsonString == null || jsonString.trim().isEmpty()) {
      return roleIds;
    }

    try {
      // Clean up the string
      String cleaned = jsonString.trim();

      // Ensure it has proper JSON array format
      if (!cleaned.startsWith("[")) {
        cleaned = "[" + cleaned;
      }
      if (!cleaned.endsWith("]")) {
        cleaned = cleaned + "]";
      }

      org.json.JSONArray array = new org.json.JSONArray(cleaned);

      // Extract all values, converting numbers to strings as needed
      for (int i = 0; i < array.length(); i++) {
        try {
          // Try to get as string first
          roleIds.add(array.getString(i));
        } catch (org.json.JSONException e) {
          // If that fails, try as a number and convert to string
          try {
            roleIds.add(String.valueOf(array.getLong(i)));
          } catch (Exception e2) {
            LOGGER.warning("Could not parse role ID at index " + i);
          }
        }
      }

      return roleIds;
    } catch (Exception e) {
      LOGGER.severe("Failed to parse role IDs: " + e.getMessage());
      return roleIds;
    }
  }
}
