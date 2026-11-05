package wealthsei.analytics;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.llm.ToolArgs;
import wealthsei.service.ValidationException;

/**
 * Turns the four plain-text fields a what-if is described with into {@link ScenarioChange}s:
 * <ul>
 *   <li>{@code cancel}: merchants or bills to stop, comma separated ("Spotify,Netflix")</li>
 *   <li>{@code caps}: monthly category caps ("dining=150,shopping=100")</li>
 *   <li>{@code income_increase}: extra income per month ("200")</li>
 *   <li>{@code one_time_expenses}: single expenses with dates ("600@2026-12-01")</li>
 * </ul>
 */
public final class ScenarioParser {

    private ScenarioParser() {
    }

    /** @throws ValidationException for a bad category, number or date, a value that is not above zero, or no changes at all */
    public static List<ScenarioChange> parse(ToolArgs args) {
        List<ScenarioChange> changes = new ArrayList<>();
        for (String merchant : split(args, "cancel")) {
            changes.add(new CancelRecurringChange(merchant));
        }
        for (String pair : split(args, "caps")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                throw new ValidationException("A cap must look like dining=150, not '" + pair + "'");
            }
            changes.add(new CapCategoryChange(category(pair.substring(0, eq).trim()), money(pair.substring(eq + 1), "cap")));
        }
        if (args.has("income_increase")) {
            changes.add(new IncomeChange(money(args.requireText("income_increase"), "income increase")));
        }
        for (String item : split(args, "one_time_expenses")) {
            int at = item.indexOf('@');
            if (at <= 0) {
                throw new ValidationException("A one-time expense must look like 600@2026-12-01, not '" + item + "'");
            }
            changes.add(new OneTimeExpenseChange(money(item.substring(0, at), "expense"), date(item.substring(at + 1).trim())));
        }
        if (changes.isEmpty()) {
            throw new ValidationException("No changes were given. Describe at least one: cancel, caps, income_increase or one_time_expenses");
        }
        if (changes.size() > Scenario.MAX_CHANGES) {
            throw new ValidationException("That is " + changes.size() + " changes; at most " + Scenario.MAX_CHANGES
                    + " are allowed at once. Please split it into smaller scenarios.");
        }
        return changes;
    }

    private static List<String> split(ToolArgs args, String name) {
        List<String> parts = new ArrayList<>();
        if (args.has(name)) {
            for (String part : args.requireText(name).split(",")) {
                if (!part.isBlank()) {
                    parts.add(part.trim());
                }
            }
        }
        return parts;
    }

    private static Category category(String text) {
        return Categories.find(text.toLowerCase()).orElseGet(() -> Categories.defaults().stream()
                .filter(c -> c.name().equalsIgnoreCase(text)).findFirst()
                .orElseThrow(() -> new ValidationException("Unknown category '" + text + "'")));
    }

    private static Money money(String text, String what) {
        try {
            return Money.of(text.trim().replace("$", ""));
        } catch (RuntimeException e) {
            throw new ValidationException("The " + what + " '" + text.trim() + "' is not a number");
        }
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException("The date '" + text + "' must look like 2026-12-01");
        }
    }
}
