package org.woftnw.dreamvisitorhub.functions;

import org.jetbrains.annotations.NotNull;
import org.woftnw.dreamvisitorhub.config.Config;
import org.woftnw.dreamvisitorhub.config.ConfigKey;
import org.woftnw.dreamvisitorhub.data.type.DVUser;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Economy {

    /**
     * Claim the daily reward. This method will refresh and update the streak, then add the reward to the balance.
     *
     * @return the amount that was rewarded.
     * @throws CoolDownException if this consumer cannot yet claim their
     */
    public static double claimDaily(DVUser user) throws CoolDownException {
        updateStreak(user);
        // The OffsetDateTime are converted to string without time to compare by date only.
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        if (user.getLastDaily() != null && Objects.equals(user.getLastDaily().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate(), LocalDate.now())) throw new CoolDownException();
        // Calculate reward
        double dailyBaseAmount = Economy.getDailyBaseAmount();
        double reward = dailyBaseAmount + (user.getDailyStreak() * getDailyStreakMultiplier());
        // Set values
        user.setBalance(user.getBalance() + reward);
        user.setDailyStreak(user.getDailyStreak());
        user.setLastDaily(OffsetDateTime.now());
        return reward;
    }

    public static void updateStreak(@NotNull DVUser user) {
        if (user.getLastDaily() == null || LocalDate.from(user.getLastDaily()).plusDays(1).isBefore(LocalDate.now())) user.setDailyStreak(0);
    }

    private static double getDailyBaseAmount() {
        return Config.get(ConfigKey.DAILY_BASE_AMOUNT);
    }

    public static double getDailyStreakMultiplier() {
        return Config.get(ConfigKey.DAILY_STREAK_MULTIPLIER);
    }

    /**
     * Thrown if the attempted action cannot be completed because the user is on cooldown.
     */
    public static class CoolDownException extends Exception {
        public CoolDownException() {
            super();
        }
    }

}
