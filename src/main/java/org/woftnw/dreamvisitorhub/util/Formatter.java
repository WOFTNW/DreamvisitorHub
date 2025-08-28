package org.woftnw.dreamvisitorhub.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

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
}
