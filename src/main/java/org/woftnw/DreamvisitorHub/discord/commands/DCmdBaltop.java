package org.woftnw.DreamvisitorHub.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.woftnw.DreamvisitorHub.App;
import org.woftnw.DreamvisitorHub.data.repository.PocketBaseUserRepository;
import org.woftnw.DreamvisitorHub.data.repository.UserRepository;
import org.woftnw.DreamvisitorHub.data.type.DVUser;
import org.woftnw.DreamvisitorHub.discord.Bot;

import java.util.*;

public class DCmdBaltop implements DiscordCommand {
  private final UserRepository userRepository = new PocketBaseUserRepository(App.getPb());

  @NotNull
  @Override
  public SlashCommandData getCommandData() {
    return Commands.slash("baltop", "Get the balances of the richest members.")
        .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
  }

  @Override
  public void onCommand(@NotNull SlashCommandInteractionEvent event) {
    // Get all users from repository
    List<DVUser> allUsers = userRepository.getAllWhere("-balance");

    // Filter users with non-null balance and snowflake ID
    List<DVUser> usersWithBalance = allUsers;

    // // Sort by balance in descending order
    // usersWithBalance.sort((user1, user2) -> {
    // Double balance1 = user1.getBalance();
    // Double balance2 = user2.getBalance();
    // if (balance1 == null && balance2 == null)
    // return 0;
    // if (balance1 == null)
    // return 1; // null values go last
    // if (balance2 == null)
    // return -1;
    // return Double.compare(balance2, balance1); // descending order
    // });

    // Find the requesting user
    long requesterId = event.getUser().getIdLong();
    Optional<DVUser> requesterOpt = userRepository.findBySnowflakeId(requesterId);

    if (!requesterOpt.isPresent()) {
      event.reply("You don't have a profile yet. Please connect your Minecraft account first.")
          .setEphemeral(true)
          .queue();
      return;
    }

    DVUser requester = requesterOpt.get();

    // Find the requester's position
    int requesterIndex = -1;
    for (int i = 0; i < usersWithBalance.size(); i++) {
      if (usersWithBalance.get(i).getSnowflakeId() != null &&
          usersWithBalance.get(i).getSnowflakeId() == requesterId) {
        requesterIndex = i;
        break;
      }
    }

    // Get users above and below requester
    DVUser aboveRequester = null;
    if (requesterIndex > 0) {
      aboveRequester = usersWithBalance.get(requesterIndex - 1);
    }

    DVUser belowRequester = null;
    if (requesterIndex >= 0 && requesterIndex < usersWithBalance.size() - 1) {
      belowRequester = usersWithBalance.get(requesterIndex + 1);
    }

    // Create the embed
    final EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Top Balances");

    // Determine how many users to show
    int numberShown = Math.min(10, usersWithBalance.size());

    // Get IDs to retrieve members
    List<Long> retrieveIds = new ArrayList<>();
    for (int i = 0; i < numberShown; i++) {
      if (usersWithBalance.get(i).getSnowflakeId() != null) {
        retrieveIds.add(usersWithBalance.get(i).getSnowflakeId());
      }
    }

    // Add users above and below requester to the list if not already in top 10
    if (aboveRequester != null && aboveRequester.getSnowflakeId() != null) {
      retrieveIds.add(aboveRequester.getSnowflakeId());
    }

    if (belowRequester != null && belowRequester.getSnowflakeId() != null) {
      retrieveIds.add(belowRequester.getSnowflakeId());
    }

    // Final variables for use in lambda
    final int finalRequesterIndex = requesterIndex;
    final DVUser finalAboveRequester = aboveRequester;
    final DVUser finalBelowRequester = belowRequester;
    final List<DVUser> finalUsersWithBalance = usersWithBalance;
    final int finalNumberShown = numberShown;

    // Retrieve members and build the response
    Objects.requireNonNull(event.getGuild()).retrieveMembersByIds(retrieveIds).onSuccess(members -> {
      // Create member map for quick lookup
      Map<Long, Member> memberMap = new HashMap<>();
      for (Member member : members) {
        memberMap.put(member.getIdLong(), member);
      }

      // Build the balance list
      StringBuilder balanceList = new StringBuilder();
      for (int i = 0; i < finalNumberShown; i++) {
        DVUser user = finalUsersWithBalance.get(i);
        if (user.getSnowflakeId() == null)
          continue;

        balanceList.append(i + 1).append(". ")
            .append(Bot.CURRENCY_SYMBOL)
            .append(" ").append(Bot.formatCurrency(user.getBalance()))
            .append(": ");

        Member member = memberMap.get(user.getSnowflakeId());
        if (member != null) {
          balanceList.append(member.getAsMention());
        } else {
          balanceList.append("<@").append(user.getSnowflakeId()).append(">");
        }
        balanceList.append("\n");
      }

      if (finalNumberShown == 0) {
        balanceList.append("No one has any ").append(Bot.CURRENCY_SYMBOL).append(" yet!\n");
      }

      // Add requester info if they have a balance
      if (requester.getBalance() != null && requester.getBalance() > 0) {
        balanceList.append("\nYour current rank is ").append(finalRequesterIndex + 1).append(",\n");

        if (finalAboveRequester != null && finalAboveRequester.getSnowflakeId() != null) {
          Member aboveMember = memberMap.get(finalAboveRequester.getSnowflakeId());
          if (aboveMember != null) {
            balanceList.append("behind ")
                .append(aboveMember.getAsMention())
                .append(" (")
                .append(Bot.CURRENCY_SYMBOL)
                .append(" ").append(Bot.formatCurrency(finalAboveRequester.getBalance()))
                .append(")")
                .append(" by ")
                .append(Bot.formatCurrency(finalAboveRequester.getBalance() - requester.getBalance()) + " "
                    + Bot.CURRENCY_SYMBOL);
          }
        }

        if (finalBelowRequester != null && finalBelowRequester.getSnowflakeId() != null) {
          Member belowMember = memberMap.get(finalBelowRequester.getSnowflakeId());
          if (belowMember != null) {
            balanceList.append(",\n ahead of ")
                .append(belowMember.getAsMention())
                .append(" (")
                .append(Bot.CURRENCY_SYMBOL)
                .append(" ").append(Bot.formatCurrency(finalBelowRequester.getBalance()))
                .append(") ")
                .append("by ")
                .append(Bot.formatCurrency(requester.getBalance() - finalBelowRequester.getBalance()) + " "
                    + Bot.CURRENCY_SYMBOL);

          }
        }

        balanceList.append(".");
      }

      embed.setDescription(balanceList)
          .setFooter("Your current balance is " +
              (requester.getBalance() != null ? Bot.formatCurrency(requester.getBalance()) : "0.00"));
      event.replyEmbeds(embed.build()).queue();
    });
  }
}
