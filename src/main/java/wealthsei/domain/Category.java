package wealthsei.domain;

import java.util.Objects;

/**
 * A spending category such as Groceries or Rent.
 *
 * @param id        stable lower-case identifier, e.g. {@code "groceries"}
 * @param name      display name, e.g. {@code "Groceries"}
 * @param essential true for needs (rent, groceries); false for wants. Used by the
 *                  health score and by protected-category rules.
 */
public record Category(String id, String name, boolean essential) {

    /** Fallback used when no strategy could pick a category (flagged for manual review). */
    public static final Category UNCATEGORIZED = new Category("uncategorized", "Uncategorized", false);

    public Category {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Category id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
    }

    public boolean isUncategorized() {
        return id.equals(UNCATEGORIZED.id);
    }
}
