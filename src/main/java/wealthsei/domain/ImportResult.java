package wealthsei.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Summary of one CSV import.
 *
 * @param imported       how many new transactions were saved
 * @param duplicates     how many rows were skipped because they were already stored
 * @param rejected       one human-readable reason per row that could not be read
 * @param affectedMonths the months that received at least one new transaction (used to
 *                       refresh budget alerts)
 */
public record ImportResult(int imported, int duplicates, List<String> rejected,
                           Set<YearMonth> affectedMonths) {

    public ImportResult {
        if (imported < 0 || duplicates < 0) {
            throw new IllegalArgumentException("Counts must not be negative");
        }
        rejected = List.copyOf(Objects.requireNonNull(rejected, "rejected"));
        affectedMonths = Set.copyOf(Objects.requireNonNull(affectedMonths, "affectedMonths"));
    }

    public ImportResult(int imported, int duplicates, List<String> rejected) {
        this(imported, duplicates, rejected, Set.of());
    }
}
