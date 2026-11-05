package wealthsei.analytics;

import wealthsei.domain.Money;
import wealthsei.domain.Verdict;

/**
 * The rules behind the affordability verdict, applied to figures the forecast produced.
 * First rule that fits wins:
 * <ol>
 *   <li>NO: the balance would go below zero.</li>
 *   <li>WAIT: the balance would fall below the safety buffer, or the daily allowance would be used up.</li>
 *   <li>YES_BUT: affordable, but a goal is at risk or the daily allowance would drop by half or more.</li>
 *   <li>YES: none of the above.</li>
 * </ol>
 */
public final class PurchaseAssessor {

    /** A verdict and the sentence that explains it. */
    public record Assessment(Verdict verdict, String reason) { }

    private PurchaseAssessor() {
    }

    public static Assessment assess(Money lowestAfter, Money buffer, Money allowanceBefore, Money allowanceAfter,
                                    boolean overCommittedAfter, int goalsNeedingHelp) {
        if (lowestAfter.isNegative()) {
            return new Assessment(Verdict.NO, "the balance would drop below zero (lowest " + lowestAfter + ")");
        }
        if (lowestAfter.isLessThan(buffer)) {
            return new Assessment(Verdict.WAIT, "the balance would fall below your " + buffer
                    + " safety buffer (lowest " + lowestAfter + ")");
        }
        if (overCommittedAfter) {
            return new Assessment(Verdict.WAIT, "it would use up the rest of this month's spending allowance");
        }
        if (goalsNeedingHelp > 0) {
            return new Assessment(Verdict.YES_BUT, "you can afford it, but " + goalsNeedingHelp
                    + (goalsNeedingHelp == 1 ? " savings goal is" : " savings goals are") + " already at risk or behind");
        }
        if (allowanceBefore.isPositive() && allowanceAfter.multiply(2).compareTo(allowanceBefore) <= 0) {
            return new Assessment(Verdict.YES_BUT, "it would cut your daily spending allowance from "
                    + allowanceBefore + " to " + allowanceAfter);
        }
        return new Assessment(Verdict.YES, "your balance stays above your safety buffer and your allowance holds up");
    }
}
