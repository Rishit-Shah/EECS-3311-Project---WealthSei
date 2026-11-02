package wealthsei.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Money;
import wealthsei.domain.ParseResult;
import wealthsei.domain.Transaction;

class CsvAdaptersTest {

    private static Supplier<String> counter() {
        AtomicInteger n = new AtomicInteger();
        return () -> "t" + n.incrementAndGet();
    }

    @Test
    void signedAmountReadsExpensesAndIncome() {
        CsvParser p = CsvParser.parse("Date,Description,Amount\n"
                + "2026-10-01,LOBLAWS #12 TORONTO,-45.10\n"
                + "2026-10-02,PAYROLL ACME,2000.00\n");
        ParseResult r = new SignedAmountCsvAdapter(p, counter()).readTransactions();
        assertEquals(2, r.transactions().size());
        assertTrue(r.rejectedRows().isEmpty());
        Transaction spend = r.transactions().get(0);
        assertEquals("t1", spend.id());
        assertEquals(LocalDate.of(2026, 10, 1), spend.date());
        assertEquals(Money.of("45.10"), spend.amount());
        assertFalse(spend.income());
        assertEquals("Loblaws Toronto", spend.merchant());
        assertEquals("LOBLAWS #12 TORONTO", spend.description());
        assertTrue(r.transactions().get(1).income());
        assertEquals(Money.of("2000"), r.transactions().get(1).amount());
    }

    @Test
    void signedAmountRejectsBadRowsButKeepsTheRest() {
        CsvParser p = CsvParser.parse("Date,Description,Amount\n"
                + "not-a-date,Coffee,-3\n"
                + "2026-10-02,Coffee,-3\n"
                + "2026-10-03,,-3\n"
                + "2026-10-04,Coffee,abc\n"
                + "2026-10-05,Coffee,0\n");
        ParseResult r = new SignedAmountCsvAdapter(p, counter()).readTransactions();
        assertEquals(1, r.transactions().size());
        assertEquals(4, r.rejectedRows().size());
        assertEquals("line 2: invalid date 'not-a-date'", r.rejectedRows().get(0));
        assertEquals("line 4: missing description", r.rejectedRows().get(1));
        assertTrue(r.rejectedRows().get(2).startsWith("line 5: invalid amount"));
        assertEquals("line 6: amount is zero", r.rejectedRows().get(3));
    }

    @Test
    void signedAmountCanHandleChecksHeaderCaseInsensitively() {
        SignedAmountCsvAdapter a = new SignedAmountCsvAdapter(CsvParser.parse("x\n"), counter());
        assertTrue(a.canHandle(List.of("DATE", "details", "Amount")));
        assertTrue(a.canHandle(List.of("Posted Date", "Description", "Transaction Amount", "Balance")));
        assertFalse(a.canHandle(List.of("Date", "Description", "Debit", "Credit")));
        assertFalse(a.canHandle(List.of("Date", "Amount")));
    }

    @Test
    void debitCreditReadsBothDirections() {
        CsvParser p = CsvParser.parse("Date,Description,Debit,Credit\n"
                + "2026-10-01,Rent,1500.00,\n"
                + "2026-10-02,Salary,,2000.00\n"
                + "2026-10-03,Refund,0.00,12.00\n");
        ParseResult r = new DebitCreditCsvAdapter(p, counter()).readTransactions();
        assertEquals(3, r.transactions().size());
        assertFalse(r.transactions().get(0).income());
        assertEquals(Money.of("1500"), r.transactions().get(0).amount());
        assertTrue(r.transactions().get(1).income());
        assertTrue(r.transactions().get(2).income());
        assertEquals(Money.of("12"), r.transactions().get(2).amount());
    }

    @Test
    void debitCreditRejectsAmbiguousRows() {
        CsvParser p = CsvParser.parse("Date,Description,Debit,Credit\n"
                + "2026-10-01,X,,\n"
                + "2026-10-02,Y,5,5\n");
        ParseResult r = new DebitCreditCsvAdapter(p, counter()).readTransactions();
        assertTrue(r.transactions().isEmpty());
        assertEquals(List.of("line 2: no debit or credit amount", "line 3: both debit and credit are filled in"),
                r.rejectedRows());
    }

    @Test
    void debitCreditCanHandleNeedsBothColumns() {
        DebitCreditCsvAdapter a = new DebitCreditCsvAdapter(CsvParser.parse("x\n"), counter());
        assertTrue(a.canHandle(List.of("Date", "Description", "Withdrawals", "Deposits")));
        assertFalse(a.canHandle(List.of("Date", "Description", "Debit")));
        assertFalse(a.canHandle(List.of("Date", "Description", "Amount")));
    }
}
