package wealthsei.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Money;

class RowParsingTest {

    @Test
    void parsesSupportedDateLayouts() {
        assertEquals(LocalDate.of(2026, 10, 5), RowParsing.parseDate("2026-10-05"));
        assertEquals(LocalDate.of(2026, 10, 5), RowParsing.parseDate("2026/10/05"));
        assertEquals(LocalDate.of(2026, 10, 5), RowParsing.parseDate("10/05/2026"));
        assertEquals(LocalDate.of(2026, 1, 5), RowParsing.parseDate(" 1/5/2026 "));
    }

    @Test
    void rejectsBadDates() {
        assertThrows(InvalidRowException.class, () -> RowParsing.parseDate(""));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseDate("abc"));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseDate("2026-02-30"));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseDate("13/25/2026"));
    }

    @Test
    void parsesAmountsWithSigns() {
        assertEquals(Money.of("12.50"), RowParsing.parseSignedAmount("12.50"));
        assertEquals(Money.of("-12.50"), RowParsing.parseSignedAmount("-12.50"));
        assertEquals(Money.of("-12.50"), RowParsing.parseSignedAmount("(12.50)"));
        assertEquals(Money.of("12.50"), RowParsing.parseSignedAmount("+12.50"));
        assertEquals(Money.of("0.50"), RowParsing.parseSignedAmount(".5"));
    }

    @Test
    void parsesCurrencySymbolsAndThousandsSeparators() {
        assertEquals(Money.of("1234.50"), RowParsing.parseSignedAmount("$1,234.50"));
        assertEquals(Money.of("-1234567.00"), RowParsing.parseSignedAmount("-$1,234,567"));
    }

    @Test
    void refusesToGuessAtAmbiguousOrBrokenAmounts() {
        assertThrows(InvalidRowException.class, () -> RowParsing.parseSignedAmount("12,50"));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseSignedAmount("1,23,456"));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseSignedAmount("abc"));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseSignedAmount(""));
        assertThrows(InvalidRowException.class, () -> RowParsing.parseSignedAmount("1.2.3"));
    }

    @Test
    void normalisesMerchantNames() {
        assertEquals("Loblaws Toronto", RowParsing.normaliseMerchant("LOBLAWS #1234 TORONTO"));
        assertEquals("Tim Hortons", RowParsing.normaliseMerchant("  tim   hortons   #55 "));
        assertEquals("Netflix", RowParsing.normaliseMerchant("NETFLIX"));
    }

    @Test
    void merchantFallsBackToRawTextWhenNothingLeft() {
        assertEquals("12345", RowParsing.normaliseMerchant("12345"));
    }
}
