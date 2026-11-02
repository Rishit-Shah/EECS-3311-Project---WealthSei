package wealthsei.domain;

import java.util.List;
import java.util.Objects;

/**
 * Summary of one CSV import.
 *
 * @param imported   how many new transactions were saved
 * @param duplicates how many rows were skipped because they were already stored
 * @param rejected   one human-readable reason per row that could not be read
 */
public record ImportResult(int imported, int duplicates, List<String> rejected) {

    public ImportResult {
        if (imported < 0 || duplicates < 0) {
            throw new IllegalArgumentException("Counts must not be negative");
        }
        rejected = List.copyOf(Objects.requireNonNull(rejected, "rejected"));
    }
}
