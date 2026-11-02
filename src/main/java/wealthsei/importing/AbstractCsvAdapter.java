package wealthsei.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.domain.ParseResult;
import wealthsei.domain.Transaction;

/**
 * Shared reading loop for the bank adapters (Template Method): subclasses only say how one
 * record becomes one transaction.
 */
abstract class AbstractCsvAdapter implements BankCsvAdapter {

    static final List<String> DATE_NAMES = List.of("date", "transaction date", "posted date", "posting date");
    static final List<String> DESCRIPTION_NAMES = List.of("description", "details", "payee", "merchant", "memo", "name");
    static final List<String> AMOUNT_NAMES = List.of("amount", "transaction amount");
    static final List<String> DEBIT_NAMES = List.of("debit", "debits", "withdrawal", "withdrawals", "money out");
    static final List<String> CREDIT_NAMES = List.of("credit", "credits", "deposit", "deposits", "money in");

    protected final CsvParser parser;
    protected final Supplier<String> idSupplier;

    protected AbstractCsvAdapter(CsvParser parser, Supplier<String> idSupplier) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
    }

    @Override
    public final ParseResult readTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (CsvRecord record : parser.records()) {
            try {
                transactions.add(toTransaction(record));
            } catch (InvalidRowException e) {
                rejected.add("line " + record.lineNumber() + ": " + e.getMessage());
            }
        }
        return new ParseResult(transactions, rejected);
    }

    /** Builds one transaction or throws {@link InvalidRowException}. */
    protected abstract Transaction toTransaction(CsvRecord row);

    /** Index of the first of the names found in the header (case-insensitive), or -1. */
    static int columnIndex(List<String> header, List<String> names) {
        for (String name : names) {
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).trim().equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
