package wealthsei.domain;

import java.time.LocalDate;
import java.util.Optional;

/**
 * One transaction typed in by hand instead of imported from a CSV file (Stage 1 F01 input).
 *
 * @param category optional; when empty the app categorizes by merchant like it does for imports
 * @param income   true for money received, false for money spent
 */
public record NewTransaction(LocalDate date, Money amount, String merchant, String description,
                             Optional<Category> category, boolean income) {

    public NewTransaction {
        description = description == null ? "" : description;
        category = category == null ? Optional.empty() : category;
    }

    /** An expense with no description and no chosen category. */
    public static NewTransaction expense(LocalDate date, Money amount, String merchant) {
        return new NewTransaction(date, amount, merchant, "", Optional.empty(), false);
    }
}
