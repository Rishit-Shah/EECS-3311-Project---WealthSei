package wealthsei.command;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.repository.LearnedRuleRepository;
import wealthsei.service.TransactionService;

/** Entry point for category corrections with undo and redo. */
public class CategoryCorrector {

    private final TransactionService transactions;
    private final LearnedRuleRepository rules;
    private final CommandHistory<CorrectCategoryCommand> history = new CommandHistory<>();

    public CategoryCorrector(TransactionService transactions, LearnedRuleRepository rules) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * Changes a transaction's category and learns the merchant.
     *
     * @throws wealthsei.service.ValidationException if the id is unknown
     */
    public Transaction correct(String txId, Category category) {
        CorrectCategoryCommand command = new CorrectCategoryCommand(transactions, rules, txId, category);
        history.run(command);
        return command.transaction();
    }

    /** Reverses the latest correction; returns the transaction as restored, or empty. */
    public Optional<Transaction> undo() {
        return history.undo().map(CorrectCategoryCommand::transaction);
    }

    public Optional<Transaction> redo() {
        return history.redo().map(CorrectCategoryCommand::transaction);
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }
}
