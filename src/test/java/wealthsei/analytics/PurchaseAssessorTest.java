package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wealthsei.domain.Money;
import wealthsei.domain.Verdict;

class PurchaseAssessorTest {

    private static Money m(String s) {
        return Money.of(s);
    }

    private static PurchaseAssessor.Assessment assess(String lowest, String allowBefore, String allowAfter, boolean over, int goals) {
        return PurchaseAssessor.assess(m(lowest), m("200"), m(allowBefore), m(allowAfter), over, goals);
    }

    @Test
    void negativeBalanceIsNo() {
        assertEquals(Verdict.NO, assess("-0.01", "100", "0", true, 0).verdict());
    }

    @Test
    void belowTheBufferIsWait() {
        PurchaseAssessor.Assessment a = assess("199.99", "100", "50", false, 0);
        assertEquals(Verdict.WAIT, a.verdict());
        assertTrue(a.reason().contains("199.99"));
    }

    @Test
    void exactlyAtTheBufferIsNotWait() {
        assertEquals(Verdict.YES, assess("200", "100", "90", false, 0).verdict());
    }

    @Test
    void usingUpTheAllowanceIsWait() {
        assertEquals(Verdict.WAIT, assess("500", "100", "0", true, 0).verdict());
    }

    @Test
    void goalsInTroubleMakeItYesBut() {
        PurchaseAssessor.Assessment a = assess("500", "100", "90", false, 2);
        assertEquals(Verdict.YES_BUT, a.verdict());
        assertTrue(a.reason().contains("2 savings goals are"));
        assertTrue(assess("500", "100", "90", false, 1).reason().contains("1 savings goal is"));
    }

    @Test
    void halvingTheAllowanceIsYesBut() {
        assertEquals(Verdict.YES_BUT, assess("500", "100", "50", false, 0).verdict());
        assertEquals(Verdict.YES, assess("500", "100", "50.01", false, 0).verdict());
    }

    @Test
    void noAllowanceBeforeCannotBeHalved() {
        assertEquals(Verdict.YES, assess("500", "0", "0", false, 0).verdict());
    }

    @Test
    void verdictsParseFromText() {
        assertEquals(Verdict.YES_BUT, Verdict.parse("Yes-but"));
        assertEquals(Verdict.YES_BUT, Verdict.parse(" yes but "));
        assertEquals(Verdict.WAIT, Verdict.parse("WAIT"));
        assertNull(Verdict.parse("maybe"));
        assertNull(Verdict.parse(null));
        assertEquals("Yes-but", Verdict.YES_BUT.label());
        assertTrue(Verdict.NO.compareTo(Verdict.YES) > 0);
    }
}
