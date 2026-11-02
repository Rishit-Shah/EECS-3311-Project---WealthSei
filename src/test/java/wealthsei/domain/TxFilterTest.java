package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TxFilterTest {

    private static Transaction tx(String date, String merchant, Category c) {
        return new Transaction("id-" + date + merchant, LocalDate.parse(date), Money.of("10"),
                merchant, merchant, false, c);
    }

    @Test
    void dateRangeIsInclusive() {
        TxFilter f = TxFilter.between(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
        assertTrue(f.matches(tx("2026-10-01", "A", Categories.DINING)));
        assertTrue(f.matches(tx("2026-10-31", "A", Categories.DINING)));
        assertFalse(f.matches(tx("2026-09-30", "A", Categories.DINING)));
        assertFalse(f.matches(tx("2026-11-01", "A", Categories.DINING)));
    }

    @Test
    void categoryAndMerchantNarrowTheMatch() {
        TxFilter f = TxFilter.all().withCategory(Categories.DINING).withMerchant("tim");
        assertTrue(f.matches(tx("2026-10-05", "Tim Hortons", Categories.DINING)));
        assertFalse(f.matches(tx("2026-10-05", "Tim Hortons", Categories.GROCERIES)));
        assertFalse(f.matches(tx("2026-10-05", "Starbucks", Categories.DINING)));
    }

    @Test
    void allMatchesEverything() {
        assertTrue(TxFilter.all().matches(tx("1999-01-01", "X", Category.UNCATEGORIZED)));
        assertTrue(TxFilter.all().matches(tx("2999-01-01", "X", Category.UNCATEGORIZED)));
    }

    @Test
    void rejectsReversedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> TxFilter.between(LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01")));
    }

    @Test
    void importResultAndParseResultValidate() {
        assertThrows(IllegalArgumentException.class, () -> new ImportResult(-1, 0, List.of()));
        assertEquals(List.of("x"), new ImportResult(0, 0, List.of("x")).rejected());
        assertTrue(new ParseResult(List.of(), List.of()).transactions().isEmpty());
    }
}
