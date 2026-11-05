package wealthsei.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;

/**
 * Picks a category by looking for known words in the merchant name or description. Keywords
 * match whole words only ("metro" does not match "metropolis"), ignoring case, and are
 * checked in the order they were added (so "uber eats" can be listed before "uber").
 * Any income transaction is categorized as Income.
 */
public class KeywordRuleStrategy implements CategorizationStrategy {

    private final Map<Pattern, Category> rules = new LinkedHashMap<>();

    public KeywordRuleStrategy(Map<String, Category> keywords) {
        Objects.requireNonNull(keywords, "keywords");
        keywords.forEach((word, category) -> rules.put(
                Pattern.compile("\\b" + Pattern.quote(word.toLowerCase()) + "\\b"), category));
    }

    /** A starter list of common Canadian merchants. */
    public static KeywordRuleStrategy withDefaults() {
        Map<String, Category> k = new LinkedHashMap<>();
        put(k, Categories.GROCERIES, "loblaws", "sobeys", "metro", "loblaw", "no frills", "freshco", "farm boy", "t&t", "food basics",
                "rabba", "circle k", "variety", "marche", "market");
        put(k, Categories.DINING, "uber eats", "doordash", "tim hortons", "starbucks", "mcdonald's", "mcdonalds",
                "subway", "pizza", "restaurant", "a&w", "harvey's", "coffee", "sushi",
                "chipotle", "fresh choice", "yogen fruz", "chartwells", "indian", "bakery");
        put(k, Categories.TRANSPORT, "ttc", "presto", "uber", "lyft", "petro-canada", "esso", "shell", "parking", "go transit");
        put(k, Categories.RENT, "rent", "landlord");
        put(k, Categories.UTILITIES, "hydro", "enbridge", "rogers", "bell", "telus", "fido", "koodo");
        put(k, Categories.SUBSCRIPTIONS, "netflix", "spotify", "claude", "neo build", "disney plus", "youtube premium", "amazon prime", "apple.com/bill");
        put(k, Categories.SHOPPING, "amazon", "best buy", "ikea", "h&m", "zara", "uniqlo", "sephora");
        put(k, Categories.ENTERTAINMENT, "cineplex", "famous player", "famous players", "tfc", "steam", "ticketmaster");
        put(k, Categories.HEALTH, "shoppers drug mart", "rexall", "pharmacy", "dentist", "clinic", "supplements", "nutrition house");
        return new KeywordRuleStrategy(k);
    }

    private static void put(Map<String, Category> map, Category category, String... words) {
        for (String w : words) {
            map.put(w, category);
        }
    }

    @Override
    public Optional<Category> categorize(Transaction tx) {
        if (tx.income()) {
            return Optional.of(Categories.INCOME);
        }
        String text = (tx.merchant() + " " + tx.description()).toLowerCase();
        for (Map.Entry<Pattern, Category> rule : rules.entrySet()) {
            if (rule.getKey().matcher(text).find()) {
                return Optional.of(rule.getValue());
            }
        }
        return Optional.empty();
    }
}
