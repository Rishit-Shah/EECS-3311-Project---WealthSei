package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalisesToTwoDecimalsSoEqualsWorks() {
        assertEquals(Money.of("5"), Money.of("5.00"));
        assertEquals(Money.of("5").hashCode(), Money.of("5.0").hashCode());
        assertEquals("5.00", Money.of("5").toString());
    }

    @Test
    void usesBankersRounding() {
        assertEquals(Money.of("0.12"), Money.of("0.125"));  // tie -> even
        assertEquals(Money.of("0.14"), Money.of("0.135"));  // tie -> even
        assertEquals(Money.of("0.13"), Money.of("0.1251"));
    }

    @Test
    void avoidsFloatingPointError() {
        assertEquals(Money.of("0.30"), Money.of("0.10").add(Money.of("0.20")));
    }

    @Test
    void addSubtractNegateAbs() {
        Money a = Money.of("10.50");
        Money b = Money.of("3.25");
        assertEquals(Money.of("13.75"), a.add(b));
        assertEquals(Money.of("7.25"), a.subtract(b));
        assertEquals(Money.of("-3.25"), b.negate());
        assertEquals(b, b.negate().abs());
    }

    @Test
    void multiplyAndDivide() {
        assertEquals(Money.of("30.00"), Money.of("10.00").multiply(3));
        assertEquals(Money.of("2.50"), Money.of("10.00").multiply(new BigDecimal("0.25")));
        assertEquals(Money.of("333.33"), Money.of("1000").divide(3));
        assertThrows(ArithmeticException.class, () -> Money.of("1").divide(0));
    }

    @Test
    void signTests() {
        assertTrue(Money.ZERO.isZero());
        assertTrue(Money.of("0.01").isPositive());
        assertTrue(Money.of("-0.01").isNegative());
        assertFalse(Money.ZERO.isPositive());
        assertFalse(Money.ZERO.isNegative());
    }

    @Test
    void comparison() {
        Money small = Money.of("1");
        Money big = Money.of("2");
        assertTrue(big.isGreaterThan(small));
        assertTrue(small.isLessThan(big));
        assertEquals(big, Money.max(small, big));
        assertEquals(small, Money.min(small, big));
        assertEquals(0, Money.of("2").compareTo(Money.of("2.00")));
    }

    @Test
    void ofCents() {
        assertEquals(Money.of("12.34"), Money.ofCents(1234));
        assertEquals(Money.of("-0.05"), Money.ofCents(-5));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(""));
        assertThrows(NullPointerException.class, () -> Money.of((String) null));
    }

    @Test
    void isImmutable() {
        Money a = Money.of("5");
        a.add(Money.of("1"));
        assertEquals(Money.of("5"), a);
    }
}
