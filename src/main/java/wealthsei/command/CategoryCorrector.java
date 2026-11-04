package wealthsei.command;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.service.LearnedRuleStrategy;
import wealthsei.service.TransactionService;

/** Entry point for category corrections with undo and redo. */
public class CategoryCorrector {

    private final TransactionService transactions;
    private final LearnedRuleStrategy learned;
    private final CommandHistory<RecategorizeCommand> history = new CommandHistory<>();

    public CategoryCorrector(TransactionService transactions, LearnedRuleStrategy learned) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.learned = Objects.requireNonNull(learned, "learned");
    }

    /**
     * Changes a transaction's category and learns the merchant.
     *
     * @throws wealthsei.service.ValidationException if the id is unknown
     */
    public Transaction correct(String txId, Category category) {
        RecategorizeCommand command = new RecategorizeCommand(transactions, learned, txId, category);
        history.execute(command);
        return command.transaction();
    }

    /** Reverses the latest correction; returns the transaction as restored, or empty. */
    public Optional<Transaction> undo() {
        return history.undo().map(RecategorizeCommand::transaction);
    }

    public Optional<Transaction> redo() {
        return history.redo().map(RecategorizeCommand::transaction);
    }

    public boolean canUndo() {
        return history.canUndo();
    }

    public boolean canRedo() {
        return history.canRedo();
    }
}
