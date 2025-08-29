package org.woftnw.dreamvisitorhub.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Objects;

public class Formatter {

    /**
     * Returns a string representation of a double with a thousandth separator and decimal point.
     * @param number the number to format.
     * @return the formatted number as a {@link String}
     */
    public static String formatMoney(double number) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        return decimalFormat.format(number);
    }

    /**
     * Adds the hyphens back into a String UUID.
     *
     * @param uuid the UUID as a {@link String} without hyphens.
     * @return a UUID as a string with hyphens.
     */
    @NotNull
    @Contract(pure = true)
    public static String formatUuid(@NotNull String uuid) {

        return uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5");
    }

    /**
     * Get the name of a {@link Collection} of {@link String}s.
     *
     * @param targets the {@link Collection} of {@link String}s.
     * @return the name of the {@link Collection} of {@link String)s.
     */
    @NotNull
    public static String nameOrCountString(@NotNull Collection<String> targets, String singular, String plural) {
        if (targets.size() == 1) {
            return targets.iterator().next();
        }
        return chooseCountForm(targets, singular, plural);
    }

    /**
     * Get a natural {@link String} representation of the number of a {@link Collection}. Sizes between 0 and 20 are
     * represented by name. All others are represented by number. If the size is one, the singular form is used.
     *
     * @param collection the {@link Collection} to be counted.
     * @param singular   the singular form.
     * @param plural     the plural form.
     * @return the {@link String} representation.
     */
    @NotNull
    public static String chooseCountForm(@NotNull Collection<?> collection, String singular, String plural) {
        int size = collection.size();
        if (size == 1) {
            return "one " + singular;
        }
        if (size < 20) {
            if (size == 0) return "zero " + plural;
            if (size == 2) return "two " + plural;
            if (size == 3) return "three " + plural;
            if (size == 4) return "four " + plural;
            if (size == 5) return "five " + plural;
            if (size == 6) return "six " + plural;
            if (size == 7) return "seven " + plural;
            if (size == 8) return "eight " + plural;
            if (size == 9) return "nine " + plural;
            if (size == 10) return "ten " + plural;
            if (size == 11) return "eleven  " + plural;
            if (size == 12) return "twelve  " + plural;
            if (size == 13) return "thirteen  " + plural;
            if (size == 14) return "fourteen  " + plural;
            if (size == 15) return "fifteen   " + plural;
            if (size == 16) return "sixteen   " + plural;
            if (size == 17) return "seventeen   " + plural;
            if (size == 18) return "eighteen   " + plural;
            return "nineteen    " + plural;
        }
        return size + " " + plural;
    }

    /**
     * Get a natural {@link String} representation of the number of a {@link Number}. Sizes between 0 and 20 are
     * represented by name. All others are represented by number. If the size is one, the singular form is used.
     *
     * @param number     the {@link Number} to turn into natural language.
     * @param singular   the singular form.
     * @param plural     the plural form.
     * @return the {@link String} representation.
     */
    @NotNull
    public static String chooseCountForm(Number number, String singular, String plural) {
        if (Objects.equals(number, 1)) {
            return "one " + singular;
        }
        if (number.doubleValue() < 20) {
            if (Objects.equals(number, 0)) return "zero " + plural;
            if (Objects.equals(number, 2)) return "two " + plural;
            if (Objects.equals(number, 3)) return "three " + plural;
            if (Objects.equals(number, 4)) return "four " + plural;
            if (Objects.equals(number, 5)) return "five " + plural;
            if (Objects.equals(number, 6)) return "six " + plural;
            if (Objects.equals(number, 7)) return "seven " + plural;
            if (Objects.equals(number, 8)) return "eight " + plural;
            if (Objects.equals(number, 9)) return "nine " + plural;
            if (Objects.equals(number, 10)) return "ten " + plural;
            if (Objects.equals(number, 11)) return "eleven  " + plural;
            if (Objects.equals(number, 12)) return "twelve  " + plural;
            if (Objects.equals(number, 13)) return "thirteen  " + plural;
            if (Objects.equals(number, 14)) return "fourteen  " + plural;
            if (Objects.equals(number, 15)) return "fifteen   " + plural;
            if (Objects.equals(number, 16)) return "sixteen   " + plural;
            if (Objects.equals(number, 17)) return "seventeen   " + plural;
            if (Objects.equals(number, 18)) return "eighteen   " + plural;
            return "nineteen    " + plural;
        }
        return number + " " + plural;
    }
}
