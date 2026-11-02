package wealthsei.service;

import java.time.YearMonth;
import java.util.List;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

/**
 * Works out spent against limit for each budgeted category. Pure arithmetic, no AI: this is
 * the number the agent and the screens both rely on.
 *
 * <p>Only expenses dated in the budget's month count. Income (including refunds marked as
 * income) is ignored, and spending in categories without a limit does not appear.
 */
public final class BudgetCalculator {

    private BudgetCalculator() {
    }

    public static BudgetStatus compute(Budget budget, List<Transaction> transactions) {
        YearMonth month = budget.month();
        List<BudgetLineStatus> lines = budget.lines().stream()
                .map(line -> statusFor(line, month, transactions))
                .toList();
        return new BudgetStatus(month, lines);
    }

    private static BudgetLineStatus statusFor(BudgetLine line, YearMonth month, List<Transaction> txs) {
        Money spent = txs.stream()
                .filter(Transaction::isExpense)
                .filter(tx -> YearMonth.from(tx.date()).equals(month))
                .filter(tx -> tx.category().equals(line.category()))
                .map(Transaction::amount)
                .reduce(Money.ZERO, Money::add);
        return new BudgetLineStatus(line.category(), spent, line.limit());
    }
}
