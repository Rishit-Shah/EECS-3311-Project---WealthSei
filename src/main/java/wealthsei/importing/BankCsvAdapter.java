package wealthsei.importing;

import java.util.List;
import wealthsei.domain.ParseResult;

/**
 * Reads one particular bank CSV layout and turns its rows into transactions (Adapter pattern:
 * the adapter wraps a {@link CsvParser} and presents it as a source of {@code Transaction}s).
 */
public interface BankCsvAdapter {

    /** True if the header has the columns this layout needs. */
    boolean canHandle(List<String> header);

    /** Converts every record. Bad rows are collected in the result; they never abort the file. */
    ParseResult readTransactions();

    /** Plain-language description of the columns expected, for error messages. */
    String layoutDescription();
}
