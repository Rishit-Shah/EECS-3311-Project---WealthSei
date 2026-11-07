package wealthsei.presentation.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Money;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.llm.LlmConfig;
import wealthsei.llm.UnavailableLLMProvider;

/** The parts of the GUI that are logic rather than painting. */
class GuiLogicTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    private WealthSeiFacade facade() {
        return WealthSeiFactory.inMemory(new UnavailableLLMProvider(), LlmConfig.none(), () -> TODAY);
    }

    @Test
    void amountsAcceptDollarSignsAndCommas() {
        assertEquals(Money.of("1400.50"), Dialogs.money("$1,400.50"));
        assertEquals(Money.of("250"), Dialogs.money(" 250 "));
    }

    @Test
    void badAmountsAndDatesGiveReadableMessages() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dialogs.money("abc")).getMessage().contains("abc"));
        assertThrows(IllegalArgumentException.class, () -> Dialogs.money(""));
        assertEquals(LocalDate.of(2026, 12, 15), Dialogs.date("2026-12-15"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dialogs.date("tomorrow")).getMessage().contains("2026-12-15"));
    }

    @Test
    void wrappedTextBreaksLongLinesAndKeepsParagraphs() {
        Text t = Text.wrapped("one two three four five six seven eight nine ten\nsecond", java.awt.Color.WHITE, 14f, false, 120);
        var lines = t.lines(120);
        assertTrue(lines.size() > 3, "long text should wrap: " + lines);
        assertEquals("second", lines.get(lines.size() - 1));
    }

    @Test
    void dashboardWithNoDataSaysSoAndUsesTheCurrentMonth() {
        DashboardData d = DashboardData.load(facade(), TODAY);
        assertEquals(YearMonth.of(2026, 11), d.month());
        assertTrue(!d.hasData());
    }

    @Test
    void dashboardUsesTheChosenMonth() {
        DashboardData d = DashboardData.load(facade(), TODAY, YearMonth.of(2026, 9));
        assertEquals(YearMonth.of(2026, 9), d.month());
    }

    @Test
    void planMenuListsFourActions() {
        assertEquals(4, PlanDialogs.planMenu().size());
    }
}
