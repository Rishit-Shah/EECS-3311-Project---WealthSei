package wealthsei.repository.file;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.TransactionRepository;

/**
 * Adds saving to a file on top of any {@link TransactionRepository} (Decorator pattern). The
 * file is read once when this object is created and rewritten after every change.
 */
public class PersistentTransactionRepository implements TransactionRepository {

    private final TransactionRepository inner;
    private final Path file;

    public PersistentTransactionRepository(TransactionRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        List<Transaction> loaded = new ArrayList<>();
        for (TsvFile.Row row : TsvFile.read(file)) {
            loaded.add(FileCodec.parse(file, row, PersistentTransactionRepository::toTransaction));
        }
        try {
            inner.saveAll(loaded);
        } catch (IllegalArgumentException e) {
            throw new StorageException(file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<Transaction> txs) {
        inner.saveAll(txs);
        persist();
    }

    @Override
    public List<Transaction> query(TxFilter filter) {
        return inner.query(filter);
    }

    @Override
    public void updateCategory(String txId, Category category) {
        inner.updateCategory(txId, category);
        persist();
    }

    @Override
    public Optional<Transaction> findById(String txId) {
        return inner.findById(txId);
    }

    private void persist() {
        List<List<String>> rows = new ArrayList<>();
        for (Transaction tx : inner.query(TxFilter.all())) {
            List<String> row = new ArrayList<>();
            row.add(tx.id());
            row.add(tx.date().toString());
            row.add(tx.amount().toString());
            row.add(tx.merchant());
            row.add(tx.description());
            row.add(Boolean.toString(tx.income()));
            FileCodec.addCategory(row, tx.category());
            rows.add(row);
        }
        TsvFile.write(file, rows);
    }

    private static Transaction toTransaction(TsvFile.Row row) {
        Category category = FileCodec.readCategory(row, 6);
        if (category == null) {
            throw new IllegalArgumentException("missing category");
        }
        return new Transaction(FileCodec.field(row, 0), LocalDate.parse(FileCodec.field(row, 1)),
                Money.of(FileCodec.field(row, 2)), FileCodec.field(row, 3), FileCodec.field(row, 4),
                FileCodec.parseBoolean(FileCodec.field(row, 5)), category);
    }
}
