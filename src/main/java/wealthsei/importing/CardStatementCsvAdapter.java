package wealthsei.importing;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

/**
 * Credit-card statement layout (for example Neo Financial): Transaction Date, Posted Date,
 * Status, Description, Amount. Negative amounts are purchases; positive amounts are credits.
 *
 * <p>Card-aware rules: rows that are not yet posted are skipped (they may change), and
 * payments made to the card ("Payment Received") are skipped because they move money you
 * already have, they are not income. Other credits (cash-back, refunds, adjustments) are kept.
 * Skipped rows are listed in the import report, never dropped silently.
 */
public class CardStatementCsvAdapter extends AbstractCsvAdapter {

    private static final String POSTED = "posted";

    private final int dateCol;
    private final int descriptionCol;
    private final int amountCol;
    private final int statusCol;

    public CardStatementCsvAdapter(CsvParser parser, Supplier<String> idSupplier) {
        super(parser, idSupplier);
        List<String> header = parser.header();
        this.dateCol = columnIndex(header, List.of("transaction date"));
        this.descriptionCol = columnIndex(header, DESCRIPTION_NAMES);
        this.amountCol = columnIndex(header, AMOUNT_NAMES);
        this.statusCol = columnIndex(header, List.of("status"));
    }

    @Override
    public boolean canHandle(List<String> header) {
        return columnIndex(header, List.of("transaction date")) >= 0
                && columnIndex(header, List.of("posted date")) >= 0
                && columnIndex(header, List.of("status")) >= 0
                && columnIndex(header, DESCRIPTION_NAMES) >= 0
                && columnIndex(header, AMOUNT_NAMES) >= 0;
    }

    @Override
    public String layoutDescription() {
        return "Card statement: Transaction Date, Posted Date, Status, Description, Amount";
    }

    @Override
    protected Transaction toTransaction(CsvRecord row) {
        if (dateCol < 0 || descriptionCol < 0 || amountCol < 0 || statusCol < 0) {
            throw new IllegalStateException("Header does not match this layout; call canHandle first");
        }
        var date = RowParsing.parseDate(row.get(dateCol));
        String description = row.get(descriptionCol);
        if (description.isEmpty()) {
            throw new InvalidRowException("missing description");
        }
        Money signed = RowParsing.parseSignedAmount(row.get(amountCol));
        if (signed.isZero()) {
            throw new InvalidRowException("amount is zero");
        }
        String status = row.get(statusCol).trim().toLowerCase(Locale.ROOT);
        if (!status.equals(POSTED)) {
            throw new InvalidRowException("skipped, not posted yet (status '" + row.get(statusCol).trim() + "')");
        }
        if (signed.isPositive() && isCardPayment(description)) {
            throw new InvalidRowException("skipped, payment to the card (a transfer, not income)");
        }
        return new Transaction(idSupplier.get(), date, signed.abs(),
                CardMerchantCleaner.clean(description), description.trim().replaceAll("\\s+", " "),
                signed.isPositive(), Category.UNCATEGORIZED);
    }

    private static boolean isCardPayment(String description) {
        String d = description.toLowerCase(Locale.ROOT);
        return d.startsWith("payment") || d.contains("autopay") || d.contains("payment received");
    }
}
