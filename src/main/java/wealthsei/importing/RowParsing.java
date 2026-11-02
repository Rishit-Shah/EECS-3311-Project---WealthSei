package wealthsei.importing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import wealthsei.domain.Money;

/**
 * Turns the text of CSV cells into dates, amounts and merchant names. Every failure throws
 * {@link InvalidRowException} so the adapter can reject just that row.
 */
public final class RowParsing {

    /** Accepted date layouts: 2026-10-05, 2026/10/05 and 10/05/2026 (month first). */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("uuuu/M/d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT));

    private static final Pattern PLAIN_NUMBER = Pattern.compile("\\d+(\\.\\d+)?|\\.\\d+");
    private static final Pattern THOUSANDS_NUMBER = Pattern.compile("\\d{1,3}(,\\d{3})+(\\.\\d+)?");

    private RowParsing() {
    }

    public static LocalDate parseDate(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new InvalidRowException("missing date");
        }
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // try the next layout
            }
        }
        throw new InvalidRowException("invalid date '" + t + "'");
    }

    /**
     * Parses an amount, keeping its sign. Accepts {@code 12.50}, {@code -12.50},
     * {@code $1,234.50} and accounting-style {@code (12.50)} for negatives. A comma is only
     * accepted as a thousands separator, so {@code 12,50} is rejected rather than misread.
     */
    public static Money parseSignedAmount(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            throw new InvalidRowException("missing amount");
        }
        boolean negative = false;
        String s = t.replace("$", "").replace(" ", "");
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true;
            s = s.substring(1, s.length() - 1);
        }
        if (s.startsWith("-")) {
            negative = !negative;
            s = s.substring(1);
        } else if (s.startsWith("+")) {
            s = s.substring(1);
        }
        if (THOUSANDS_NUMBER.matcher(s).matches()) {
            s = s.replace(",", "");
        } else if (!PLAIN_NUMBER.matcher(s).matches()) {
            throw new InvalidRowException("invalid amount '" + t + "'");
        }
        Money value = Money.of(new BigDecimal(s));
        return negative ? value.negate() : value;
    }

    /**
     * A tidy merchant name from a raw bank description: store numbers ("#1234") and bare
     * numbers are dropped, spacing is collapsed, and words are Title Cased.
     * "LOBLAWS #1234 TORONTO" becomes "Loblaws Toronto".
     */
    public static String normaliseMerchant(String description) {
        String raw = description == null ? "" : description.trim().replaceAll("\\s+", " ");
        List<String> kept = new ArrayList<>();
        for (String token : raw.split(" ")) {
            if (token.isEmpty() || token.startsWith("#") || token.matches("\\d+")) {
                continue;
            }
            kept.add(token.substring(0, 1).toUpperCase() + token.substring(1).toLowerCase());
        }
        return kept.isEmpty() ? raw : String.join(" ", kept);
    }
}
