package wealthsei.repository;

import java.util.List;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;

/** Storage for transactions. Services depend on this interface, never on a database. */
public interface TransactionRepository {

    /** Stores all transactions. Throws {@link IllegalArgumentException} if an id already exists. */
    void saveAll(List<Transaction> txs);

    /** Transactions matching the filter, oldest first (ties keep the order they were saved). */
    List<Transaction> query(TxFilter filter);

    /** Throws {@link IllegalArgumentException} if the id is unknown. */
    void updateCategory(String txId, Category category);

    Optional<Transaction> findById(String txId);
}
