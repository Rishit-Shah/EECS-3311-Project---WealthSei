package wealthsei.domain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The built-in spending categories. {@link Category#UNCATEGORIZED} is separate on purpose. */
public final class Categories {

    public static final Category GROCERIES = new Category("groceries", "Groceries", true);
    public static final Category DINING = new Category("dining", "Dining out", false);
    public static final Category TRANSPORT = new Category("transport", "Transport", true);
    public static final Category RENT = new Category("rent", "Rent", true);
    public static final Category UTILITIES = new Category("utilities", "Utilities", true);
    public static final Category SUBSCRIPTIONS = new Category("subscriptions", "Subscriptions", false);
    public static final Category SHOPPING = new Category("shopping", "Shopping", false);
    public static final Category ENTERTAINMENT = new Category("entertainment", "Entertainment", false);
    public static final Category HEALTH = new Category("health", "Health", true);
    public static final Category INCOME = new Category("income", "Income", false);

    private static final List<Category> DEFAULTS = List.of(GROCERIES, DINING, TRANSPORT, RENT,
            UTILITIES, SUBSCRIPTIONS, SHOPPING, ENTERTAINMENT, HEALTH, INCOME);

    private Categories() {
    }

    public static List<Category> defaults() {
        return DEFAULTS;
    }

    /** Finds a default category by its id, ignoring case. */
    public static Optional<Category> find(String id) {
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        return DEFAULTS.stream().filter(c -> c.id().equals(wanted)).findFirst();
    }
}
