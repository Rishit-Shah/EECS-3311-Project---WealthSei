package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.llm.ToolArgs;
import wealthsei.service.ValidationException;

class ScenarioParserTest {

    private static List<String> describe(String... pairs) {
        return ScenarioParser.parse(ToolArgs.of(pairs)).stream().map(ScenarioChange::describe).toList();
    }

    @Test
    void parsesEveryKindOfChange() {
        assertEquals(List.of("Cancel Spotify", "Cap Dining out at 150.00 a month",
                        "Cap Shopping at 100.00 a month", "Earn 200.00 more each month", "Spend 600.00 once on 2026-12-01"),
                describe("cancel", "Spotify", "caps", "dining=150, Shopping=$100", "income_increase", "200",
                        "one_time_expenses", "600@2026-12-01"));
        assertEquals(List.of("Cancel Spotify", "Cancel Netflix"), describe("cancel", " Spotify , Netflix "));
    }

    @Test
    void categoriesMayBeGivenByNameOrId() {
        assertEquals(List.of("Cap Dining out at 50.00 a month"), describe("caps", "Dining out=50"));
    }

    @Test
    void badInputIsRejectedWithAnExplanation() {
        assertTrue(msg("caps", "dining").contains("dining=150"));
        assertTrue(msg("caps", "pets=10").contains("Unknown category 'pets'"));
        assertTrue(msg("caps", "dining=abc").contains("not a number"));
        assertTrue(msg("one_time_expenses", "600").contains("600@2026-12-01"));
        assertTrue(msg("one_time_expenses", "600@tomorrow").contains("must look like"));
        assertTrue(msg("income_increase", "-5").contains("more than zero"));
        assertTrue(msg("income_increase", "0").contains("more than zero"));
    }

    @Test
    void noChangesAndTooManyChangesAreRejected() {
        assertTrue(msg().contains("No changes"));
        assertTrue(msg("cancel", "a,b,c,d,e,f").contains("at most 5"));
    }

    private static String msg(String... pairs) {
        return assertThrows(ValidationException.class, () -> ScenarioParser.parse(ToolArgs.of(pairs))).getMessage();
    }
}
