package wealthsei.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;

/** Keeps transactions in memory. Used by tests now; a SQLite version replaces it for the app. */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> byId = new LinkedHashMap<>();

    @Override
    public void saveAll(List<Transaction> txs) {
        Objects.requireNonNull(txs, "txs");
        // Check first so a failure saves nothing.
        List<String> seen = new ArrayList<>();
        for (Transaction tx : txs) {
            if (byId.containsKey(tx.id()) || seen.contains(tx.id())) {
                throw new IllegalArgumentException("Transaction id already exists: " + tx.id());
            }
            seen.add(tx.id());
        }
        for (Transaction tx : txs) {
            byId.put(tx.id(), tx);
        }
    }

    @Override
    public List<Transaction> query(TxFilter filter) {
        Objects.requireNonNull(filter, "filter");
        return byId.values().stream()
                .filter(filter::matches)
                .sorted(Comparator.comparing(Transaction::date)) // stable: keeps save order within a day
                .toList();
    }

    @Override
    public void updateCategory(String txId, Category category) {
        Transaction tx = byId.get(txId);
        if (tx == null) {
            throw new IllegalArgumentException("Unknown transaction id: " + txId);
        }
        byId.put(txId, tx.withCategory(Objects.requireNonNull(category, "category")));
    }

    @Override
    public Optional<Transaction> findById(String txId) {
        return Optional.ofNullable(byId.get(txId));
    }
}
