package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrequencyTest {

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void monthlyEquivalentConvertsEachFrequency() {
        assertEquals(Money.of("433.33"), Frequency.WEEKLY.monthlyEquivalent(Money.of("100")));
        assertEquals(Money.of("2166.67"), Frequency.BIWEEKLY.monthlyEquivalent(Money.of("1000")));
        assertEquals(Money.of("1500.00"), Frequency.MONTHLY.monthlyEquivalent(Money.of("1500")));
        assertEquals(Money.of("100.00"), Frequency.YEARLY.monthlyEquivalent(Money.of("1200")));
    }

    @Test
    void weeklyOccurrences() {
        assertEquals(
                List.of(d(2026, 10, 1), d(2026, 10, 8), d(2026, 10, 15), d(2026, 10, 22), d(2026, 10, 29)),
                Frequency.WEEKLY.occurrences(d(2026, 10, 1), d(2026, 10, 1), d(2026, 10, 31)));
    }

    @Test
    void biweeklyOccurrences() {
        assertEquals(List.of(d(2026, 10, 2), d(2026, 10, 16), d(2026, 10, 30)),
                Frequency.BIWEEKLY.occurrences(d(2026, 10, 2), d(2026, 10, 1), d(2026, 10, 31)));
    }

    @Test
    void monthlyDoesNotDriftAfterShortMonth() {
        assertEquals(List.of(d(2026, 1, 31), d(2026, 2, 28), d(2026, 3, 31), d(2026, 4, 30)),
                Frequency.MONTHLY.occurrences(d(2026, 1, 31), d(2026, 1, 1), d(2026, 4, 30)));
    }

    @Test
    void yearlyOnLeapDay() {
        assertEquals(List.of(d(2025, 2, 28)),
                Frequency.YEARLY.occurrences(d(2024, 2, 29), d(2025, 1, 1), d(2025, 12, 31)));
    }

    @Test
    void rangeEndsAreInclusive() {
        assertEquals(List.of(d(2026, 10, 15)),
                Frequency.MONTHLY.occurrences(d(2026, 9, 15), d(2026, 10, 15), d(2026, 10, 15)));
    }

    @Test
    void neverReturnsDatesBeforeTheAnchor() {
        assertEquals(List.of(d(2026, 10, 20)),
                Frequency.MONTHLY.occurrences(d(2026, 10, 20), d(2026, 9, 1), d(2026, 10, 31)));
    }

    @Test
    void emptyWhenAnchorIsAfterTheRangeOrRangeIsReversed() {
        assertTrue(Frequency.WEEKLY.occurrences(d(2026, 12, 1), d(2026, 10, 1), d(2026, 10, 31)).isEmpty());
        assertTrue(Frequency.WEEKLY.occurrences(d(2026, 10, 1), d(2026, 10, 31), d(2026, 10, 1)).isEmpty());
    }
}
