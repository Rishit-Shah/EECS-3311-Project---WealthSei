package wealthsei.command;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.service.LearnedRuleStrategy;
import wealthsei.service.TransactionService;
import wealthsei.service.ValidationException;

/**
 * The user changes one transaction's category (Command pattern; the receivers are
 * {@link TransactionService} and {@link LearnedRuleStrategy}). Running it also teaches the
 * app: future transactions from the same merchant get the same category. Undoing it puts back
 * the old category and the old learned rule (or makes the app forget the new one).
 */
public class RecategorizeCommand implements Command {

    private final TransactionService transactions;
    private final LearnedRuleStrategy learned;
    private final String txId;
    private final Category newCategory;

    private Category oldCategory;
    private Transaction original;
    private Optional<Category> previousRule = Optional.empty();
    private Transaction current;

    public RecategorizeCommand(TransactionService transactions, LearnedRuleStrategy learned,
                               String txId, Category newCategory) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.learned = Objects.requireNonNull(learned, "learned");
        this.txId = Objects.requireNonNull(txId, "txId");
        this.newCategory = Objects.requireNonNull(newCategory, "newCategory");
    }

    @Override
    public void execute() {
        original = transactions.findById(txId)
                .orElseThrow(() -> new ValidationException("Unknown transaction: " + txId));
        oldCategory = original.category();
        previousRule = learned.ruleFor(original.merchant());
        current = transactions.updateCategory(txId, newCategory);
        if (learnable()) {
            learned.learn(original.merchant(), newCategory);
        }
    }

    @Override
    public void undo() {
        if (original == null) {
            throw new IllegalStateException("Command has not been executed");
        }
        current = transactions.updateCategory(txId, oldCategory);
        if (learnable()) {
            previousRule.ifPresentOrElse(
                    category -> learned.learn(original.merchant(), category),
                    () -> learned.forget(original.merchant()));
        }
    }

    @Override
    public String describe() {
        return "Set " + (original == null ? txId : original.merchant()) + " to " + newCategory.name();
    }

    /** The transaction as it stands after the last execute or undo; null before the first run. */
    public Transaction transaction() {
        return current;
    }

    /** Income and "Uncategorized" are not worth remembering as merchant rules. */
    private boolean learnable() {
        return !original.income() && !newCategory.isUncategorized() && !original.merchant().isBlank();
    }
}
