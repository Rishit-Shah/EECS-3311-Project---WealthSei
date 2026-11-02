package wealthsei.importing;

import java.util.List;
import java.util.function.Supplier;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

/**
 * Layout with one signed amount column: Date, Description, Amount. A negative amount is money
 * going out; a positive amount is money coming in.
 *
 * <p>Limitation: some credit-card exports show purchases as positive numbers. Those would be
 * read as income here.
 */
public class SignedAmountCsvAdapter extends AbstractCsvAdapter {

    private final int dateCol;
    private final int descriptionCol;
    private final int amountCol;

    public SignedAmountCsvAdapter(CsvParser parser, Supplier<String> idSupplier) {
        super(parser, idSupplier);
        List<String> header = parser.header();
        this.dateCol = columnIndex(header, DATE_NAMES);
        this.descriptionCol = columnIndex(header, DESCRIPTION_NAMES);
        this.amountCol = columnIndex(header, AMOUNT_NAMES);
    }

    @Override
    public boolean canHandle(List<String> header) {
        return columnIndex(header, DATE_NAMES) >= 0
                && columnIndex(header, DESCRIPTION_NAMES) >= 0
                && columnIndex(header, AMOUNT_NAMES) >= 0;
    }

    @Override
    public String layoutDescription() {
        return "Date, Description, Amount (negative = money out)";
    }

    @Override
    protected Transaction toTransaction(CsvRecord row) {
        if (dateCol < 0 || descriptionCol < 0 || amountCol < 0) {
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
        return new Transaction(idSupplier.get(), date, signed.abs(),
                RowParsing.normaliseMerchant(description), description,
                signed.isPositive(), Category.UNCATEGORIZED);
    }
}
