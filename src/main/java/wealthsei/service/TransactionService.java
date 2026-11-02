package wealthsei.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import wealthsei.domain.Category;
import wealthsei.domain.ImportResult;
import wealthsei.domain.Money;
import wealthsei.domain.ParseResult;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.importing.CsvImporter;
import wealthsei.importing.UnsupportedFormatException;
import wealthsei.repository.TransactionRepository;

/**
 * Imports and looks up transactions. Dependencies come in through the constructor so tests
 * can supply an in-memory repository.
 */
public class TransactionService {

    private final TransactionRepository repo;
    private final CsvImporter importer;
    private final Categorizer categorizer;

    public TransactionService(TransactionRepository repo, CsvImporter importer, Categorizer categorizer) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.importer = Objects.requireNonNull(importer, "importer");
        this.categorizer = Objects.requireNonNull(categorizer, "categorizer");
    }

    /**
     * Reads a bank CSV, skips rows that are already stored, categorizes the rest and saves them.
     *
     * @throws TransactionImportException if the file cannot be read or has an unknown layout;
     *                                    nothing is saved in that case
     */
    public ImportResult importFile(Path path) {
        ParseResult parsed;
        try {
            parsed = importer.parse(path);
        } catch (UnsupportedFormatException e) {
            throw new TransactionImportException(e.getMessage(), e);
        }
        List<Transaction> fresh = removeDuplicates(parsed.transactions());
        List<Transaction> categorized = fresh.stream()
                .map(tx -> tx.withCategory(categorizer.categorize(tx)))
                .toList();
        repo.saveAll(categorized);
        return new ImportResult(categorized.size(), parsed.transactions().size() - fresh.size(),
                parsed.rejectedRows());
    }

    public List<Transaction> query(TxFilter filter) {
        return repo.query(filter);
    }

    /** Changes one transaction's category. Throws if the id is unknown. */
    public Transaction updateCategory(String txId, Category category) {
        repo.updateCategory(txId, category);
        return repo.findById(txId).orElseThrow();
    }

    /**
     * Drops rows that are already stored. Rows match on date, amount, merchant and direction.
     * Matching is by count: if the file has two identical coffees on one day and one is already
     * stored, only the second is new. That way re-importing a file adds nothing, but two real
     * identical purchases are never merged into one.
     */
    private List<Transaction> removeDuplicates(List<Transaction> txs) {
        if (txs.isEmpty()) {
            return txs;
        }
        LocalDate min = txs.stream().map(Transaction::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = txs.stream().map(Transaction::date).max(LocalDate::compareTo).orElseThrow();
        Map<DuplicateKey, Integer> stored = new HashMap<>();
        for (Transaction existing : repo.query(TxFilter.between(min, max))) {
            stored.merge(DuplicateKey.of(existing), 1, Integer::sum);
        }
        return txs.stream().filter(tx -> {
            DuplicateKey key = DuplicateKey.of(tx);
            int left = stored.getOrDefault(key, 0);
            if (left > 0) {
                stored.put(key, left - 1);
                return false;
            }
            return true;
        }).toList();
    }

    private record DuplicateKey(LocalDate date, Money amount, String merchant, boolean income) {
        static DuplicateKey of(Transaction tx) {
            return new DuplicateKey(tx.date(), tx.amount(), tx.merchant().toLowerCase(Locale.ROOT), tx.income());
        }
    }
}
