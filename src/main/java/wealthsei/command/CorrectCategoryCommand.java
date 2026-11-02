package wealthsei.command;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.repository.LearnedRuleRepository;
import wealthsei.service.TransactionService;
import wealthsei.service.ValidationException;

/**
 * The user changes one transaction's category. Running it also teaches the app: future
 * transactions from the same merchant get the same category. Undoing it puts back the old
 * category and the old learned rule (or removes the new one).
 */
public class CorrectCategoryCommand implements Command {

    private final TransactionService transactions;
    private final LearnedRuleRepository rules;
    private final String txId;
    private final Category newCategory;

    private Transaction original;
    private Optional<Category> previousRule = Optional.empty();
    private Transaction current;

    public CorrectCategoryCommand(TransactionService transactions, LearnedRuleRepository rules,
                                  String txId, Category newCategory) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.txId = Objects.requireNonNull(txId, "txId");
        this.newCategory = Objects.requireNonNull(newCategory, "newCategory");
    }

    @Override
    public void execute() {
        original = transactions.findById(txId)
                .orElseThrow(() -> new ValidationException("Unknown transaction: " + txId));
        previousRule = rules.find(original.merchant());
        current = transactions.updateCategory(txId, newCategory);
        if (learnable()) {
            rules.save(original.merchant(), newCategory);
        }
    }

    @Override
    public void undo() {
        if (original == null) {
            throw new IllegalStateException("Command has not been executed");
        }
        current = transactions.updateCategory(txId, original.category());
        if (learnable()) {
            previousRule.ifPresentOrElse(
                    category -> rules.save(original.merchant(), category),
                    () -> rules.remove(original.merchant()));
        }
    }

    @Override
    public String description() {
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
