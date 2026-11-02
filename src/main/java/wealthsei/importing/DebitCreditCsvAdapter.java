package wealthsei.importing;

import java.util.List;
import java.util.function.Supplier;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

/**
 * Layout with separate columns: Date, Description, Debit, Credit. A debit is money out and a
 * credit is money in. A row must have exactly one of them filled in (zero counts as empty).
 */
public class DebitCreditCsvAdapter extends AbstractCsvAdapter {

    private final int dateCol;
    private final int descriptionCol;
    private final int debitCol;
    private final int creditCol;

    public DebitCreditCsvAdapter(CsvParser parser, Supplier<String> idSupplier) {
        super(parser, idSupplier);
        List<String> header = parser.header();
        this.dateCol = columnIndex(header, DATE_NAMES);
        this.descriptionCol = columnIndex(header, DESCRIPTION_NAMES);
        this.debitCol = columnIndex(header, DEBIT_NAMES);
        this.creditCol = columnIndex(header, CREDIT_NAMES);
    }

    @Override
    public boolean canHandle(List<String> header) {
        return columnIndex(header, DATE_NAMES) >= 0
                && columnIndex(header, DESCRIPTION_NAMES) >= 0
                && columnIndex(header, DEBIT_NAMES) >= 0
                && columnIndex(header, CREDIT_NAMES) >= 0;
    }

    @Override
    public String layoutDescription() {
        return "Date, Description, Debit, Credit";
    }

    @Override
    protected Transaction toTransaction(CsvRecord row) {
        if (dateCol < 0 || descriptionCol < 0 || debitCol < 0 || creditCol < 0) {
            throw new IllegalStateException("Header does not match this layout; call canHandle first");
        }
        var date = RowParsing.parseDate(row.get(dateCol));
        String description = row.get(descriptionCol);
        if (description.isEmpty()) {
            throw new InvalidRowException("missing description");
        }
        Money debit = amountOrZero(row.get(debitCol));
        Money credit = amountOrZero(row.get(creditCol));
        if (debit.isZero() && credit.isZero()) {
            throw new InvalidRowException("no debit or credit amount");
        }
        if (!debit.isZero() && !credit.isZero()) {
            throw new InvalidRowException("both debit and credit are filled in");
        }
        boolean income = debit.isZero();
        Money amount = income ? credit : debit;
        return new Transaction(idSupplier.get(), date, amount,
                RowParsing.normaliseMerchant(description), description, income, Category.UNCATEGORIZED);
    }

    private static Money amountOrZero(String text) {
        return text.isEmpty() ? Money.ZERO : RowParsing.parseSignedAmount(text).abs();
    }
}
