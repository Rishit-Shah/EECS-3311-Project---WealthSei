package wealthsei.importing;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns the raw description of a credit-card statement line into a readable merchant name.
 *
 * <p>Card statements use fixed-width text such as
 * {@code "FRESH CHOICE INDIAN SU TORONTO       CAN"}: the merchant, then the city, then a
 * country code, often separated by runs of spaces. Payment processors also add prefixes
 * ({@code SQ *}, {@code TST-}) and reference codes ({@code GOOG/SW6LPBCLHX}).
 */
public final class CardMerchantCleaner {

    private static final Pattern COUNTRY = Pattern.compile("\\s+(CAN|USA|GBR|IRL|FRA|MEX|CHN|IND|AUS|NLD)\\s*$");
    private static final Pattern GAP = Pattern.compile("\\s{2,}");
    private static final Pattern PROCESSOR_PREFIX = Pattern.compile("^(SQ\\s*\\*|TST[-*]|PP\\*|SP\\s*\\*)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern MASKED_NUMBER = Pattern.compile("\\*+\\d+");
    private static final Pattern REFERENCE_CODE = Pattern.compile("[A-Za-z]+/[A-Za-z0-9]{6,}");

    private static final List<String> CITIES = List.of(
            "downtown toro", "north york", "richmond hill", "san francisco", "new york",
            "toronto", "etobicoke", "markham", "vancouver", "mississauga", "scarborough",
            "brampton", "ottawa", "montreal", "calgary", "edmonton", "waterloo", "vaughan",
            "oakville", "burlington", "hamilton");

    /** Well-known merchants whose statement text is messy; first match wins. */
    private static final List<String[]> ALIASES = List.of(
            new String[] {"UBEREATS", "Uber Eats"},
            new String[] {"DOORDASH", "DoorDash"},
            new String[] {"ANTHROPIC", "Anthropic Claude"});

    private CardMerchantCleaner() {
    }

    public static String clean(String rawDescription) {
        String raw = rawDescription == null ? "" : rawDescription.trim();
        String s = COUNTRY.matcher(raw).replaceFirst("");
        String[] chunks = GAP.split(s.trim());
        boolean hadGap = chunks.length > 1;
        s = chunks[0];
        if (!hadGap) {
            s = stripCity(s);
        }
        s = PROCESSOR_PREFIX.matcher(s).replaceFirst("");
        String upper = s.toUpperCase(Locale.ROOT);
        for (String[] alias : ALIASES) {
            if (upper.contains(alias[0])) {
                return alias[1];
            }
        }
        StringBuilder out = new StringBuilder();
        for (String token : s.replace('*', ' ').split("\\s+")) {
            if (token.isEmpty() || token.equals("-") || MASKED_NUMBER.matcher(token).matches()
                    || REFERENCE_CODE.matcher(token).matches()) {
                continue;
            }
            out.append(token).append(' ');
        }
        String cleaned = RowParsing.normaliseMerchant(out.toString().trim());
        return cleaned.isBlank() ? RowParsing.normaliseMerchant(raw) : cleaned;
    }

    private static String stripCity(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        for (String city : CITIES) {
            if (lower.endsWith(" " + city)) {
                return s.substring(0, s.length() - city.length() - 1).trim();
            }
        }
        return s;
    }
}
