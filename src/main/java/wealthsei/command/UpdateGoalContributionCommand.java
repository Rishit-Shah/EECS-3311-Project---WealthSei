package wealthsei.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.domain.Money;
import wealthsei.service.GoalService;

/** Changes how much is put aside each month for a goal; undoing it restores the old amount. */
public class UpdateGoalContributionCommand implements Command {

    private final GoalService goals;
    private final Supplier<LocalDate> today;
    private final String goalId;
    private final Money newAmount;
    private Money oldAmount;
    private String goalName = "";

    public UpdateGoalContributionCommand(GoalService goals, Supplier<LocalDate> today, String goalId, Money newAmount) {
        this.goals = Objects.requireNonNull(goals, "goals");
        this.today = Objects.requireNonNull(today, "today");
        this.goalId = Objects.requireNonNull(goalId, "goalId");
        this.newAmount = Objects.requireNonNull(newAmount, "newAmount");
    }

    @Override
    public void execute() {
        var goal = goals.find(goalId);
        oldAmount = goal.monthlyContribution();
        goalName = goal.name();
        goals.updateContribution(goalId, newAmount, today.get());
    }

    @Override
    public void undo() {
        if (oldAmount != null) {
            goals.updateContribution(goalId, oldAmount, today.get());
        }
    }

    @Override
    public String describe() {
        return "Put " + newAmount + " a month toward " + (goalName.isEmpty() ? "the goal" : "'" + goalName + "'")
                + (oldAmount != null ? " (was " + oldAmount + ")" : "");
    }
}
