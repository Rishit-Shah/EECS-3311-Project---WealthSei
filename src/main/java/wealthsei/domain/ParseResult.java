package wealthsei.domain;

import java.util.List;
import java.util.Objects;

/**
 * What a CSV adapter produced: the transactions it could read, and a reason for each row it
 * could not.
 *
 * @param transactions successfully parsed transactions, in file order
 * @param rejectedRows one reason per rejected row, e.g. "line 7: invalid date 'abc'"
 */
public record ParseResult(List<Transaction> transactions, List<String> rejectedRows) {

    public ParseResult {
        transactions = List.copyOf(Objects.requireNonNull(transactions, "transactions"));
        rejectedRows = List.copyOf(Objects.requireNonNull(rejectedRows, "rejectedRows"));
    }
}
