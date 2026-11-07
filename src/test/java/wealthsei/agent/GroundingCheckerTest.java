package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroundingCheckerTest {

    private final GroundingChecker checker = new GroundingChecker();
    private final List<ToolResult> observations = List.of(
            new ToolResult(true, "{\"lowestBalanceWith\":2400.00,\"safeToSpendPerDay\":192.59,\"goals\":[{\"name\":\"Trip\",\"target\":1500.00}],"
                    + "\"percentUsed\":80,\"note\":\"Balance 2,400.00 on 2026-11-12\"}", ""));

    private GroundingReport check(String answer) {
        return checker.verify(answer, observations, List.of(new BigDecimal("600")));
    }

    @Test
    void figuresFromToolsPass() {
        assertTrue(check("You would have $2,400.00 left and can spend $192.59 a day.").grounded());
        assertTrue(check("The Trip goal needs $1500.00 and you have used 80% of dining.").grounded());
    }

    @Test
    void aScoreMustMatchTheTools() {
        List<ToolResult> scored = List.of(new ToolResult(true, "{\"healthScore\":58}", ""));
        assertTrue(checker.verify("Your score was 58 out of 100.", scored, List.of()).grounded());
        GroundingReport wrong = checker.verify("Your health score was 55 out of 100.", scored, List.of());
        assertFalse(wrong.grounded());
        assertEquals(List.of("55"), wrong.ungroundedFigures());
    }

    @Test
    void theScaleOfAScoreIsNotAFigure() {
        List<ToolResult> scored = List.of(new ToolResult(true, "{\"healthScore\":58,\"percentUsed\":80}", ""));
        assertTrue(checker.verify("Your health score was 58 out of 100, and you used 80% of dining.", scored, List.of()).grounded());
        assertTrue(checker.verify("Score 58/100.", scored, List.of()).grounded());
        assertFalse(check("You have 250 left.").grounded());
    }

    @Test
    void inventedOrCalculatedFiguresAreFlagged() {
        GroundingReport r = check("You would have $2,350.00 left and spend $193.10 a day.");
        assertFalse(r.grounded());
        assertEquals(List.of("$2,350.00", "$193.10"), r.ungroundedFigures());
    }

    @Test
    void wholeNumbersMayBeRoundedButDecimalsMustBeExact() {
        assertTrue(check("About $2,400 would remain.").grounded());
        assertTrue(check("About $193 a day.").grounded());
        assertFalse(check("Exactly $192.60 a day.").grounded());
        assertFalse(check("About $195 a day.").grounded());
    }

    @Test
    void theUsersOwnFigureIsAllowed() {
        assertTrue(check("The $600 laptop fits.").grounded());
        assertTrue(check("The $600.00 laptop fits.").grounded());
    }

    @Test
    void smallPlainNumbersDatesAndYearsAreNotChecked() {
        assertTrue(check("In 3 of your 2 goals, within 30 days, by 2026-11-12 or November 2027.").grounded());
    }

    @Test
    void percentsAndBigPlainNumbersAreChecked() {
        assertFalse(check("You used 95% of it.").grounded());
        assertFalse(check("That is 5000 in total.").grounded());
    }

    @Test
    void numbersInsideToolTextCount() {
        assertTrue(check("The note says 2,400.00.").grounded());
    }

    @Test
    void signsAreIgnored() {
        assertTrue(check("You are short by -$2,400.00.").grounded());
    }

    @Test
    void failedToolsContributeNothing() {
        List<ToolResult> failed = List.of(ToolResult.failure("No budget"));
        assertFalse(checker.verify("You spent $50.00.", failed, List.of()).grounded());
    }

    @Test
    void answerWithoutFiguresIsGrounded() {
        assertTrue(check("Yes, it looks fine.").grounded());
    }

    @Test
    void numbersInExtractsWhatTheUserTyped() {
        assertEquals(List.of(new BigDecimal("600"), new BigDecimal("1,200".replace(",", ""))),
                GroundingChecker.numbersIn("Can I afford $600 and 1,200?"));
    }
}
