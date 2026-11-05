package wealthsei.repository;

import java.time.YearMonth;
import java.util.Optional;
import wealthsei.domain.MonthlyReview;

/** Storage for monthly reviews: one per month, saving again replaces it. */
public interface ReviewRepository {

    void save(MonthlyReview review);

    Optional<MonthlyReview> findByMonth(YearMonth month);
}
