package wealthsei.repository;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.MonthlyReview;

/** Keeps reviews for the length of the session. */
public class InMemoryReviewRepository implements ReviewRepository {

    private final Map<YearMonth, MonthlyReview> byMonth = new HashMap<>();

    @Override
    public void save(MonthlyReview review) {
        Objects.requireNonNull(review, "review");
        byMonth.put(review.month(), review);
    }

    @Override
    public Optional<MonthlyReview> findByMonth(YearMonth month) {
        return Optional.ofNullable(byMonth.get(month));
    }
}
