package wealthsei.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.ParseResult;
import wealthsei.domain.Transaction;
import wealthsei.service.KeywordRuleStrategy;

/** Uses made-up rows in the layout of a card statement; no real account data. */
class CardStatementCsvAdapterTest {

    private static final String HEADER = "Transaction Date,Posted Date,Status,Description,Amount\n";

    private final AtomicInteger ids = new AtomicInteger();
    private final CsvImporter importer = new CsvImporter(() -> "t" + ids.incrementAndGet());

    private ParseResult parse(String rows) throws IOException {
        Path p = Files.createTempFile("wealthsei-card", ".csv");
        Files.writeString(p, HEADER + rows);
        p.toFile().deleteOnExit();
        return importer.parse(p);
    }

    private static Transaction categorized(Transaction tx) {
        return tx.withCategory(KeywordRuleStrategy.withDefaults().categorize(tx).orElse(tx.category()));
    }

    @Test
    void readsPurchaseAsExpenseUsingTransactionDate() throws IOException {
        ParseResult r = parse("2026-10-30,2026-10-31,Posted,EXAMPLE CAFE          TORONTO       CAN,-12.34\n");
        assertEquals(1, r.transactions().size());
        Transaction tx = r.transactions().get(0);
        assertEquals(java.time.LocalDate.of(2026, 10, 30), tx.date());
        assertFalse(tx.income());
        assertEquals("12.34", tx.amount().toString());
    }

    @Test
    void skipsPaymentsToTheCardAndSaysWhy() throws IOException {
        ParseResult r = parse("2026-10-28,2026-10-28,Posted,Payment Received - Thank you,50.00\n");
        assertTrue(r.transactions().isEmpty());
        assertEquals(1, r.rejectedRows().size());
        assertTrue(r.rejectedRows().get(0).contains("payment to the card"));
    }

    @Test
    void skipsPendingRows() throws IOException {
        ParseResult r = parse("2026-10-28,2026-10-28,Pending,EXAMPLE CAFE   TORONTO   CAN,-5.00\n");
        assertTrue(r.transactions().isEmpty());
        assertTrue(r.rejectedRows().get(0).contains("not posted"));
    }

    @Test
    void keepsCashBackAndAdjustmentsAsCredits() throws IOException {
        ParseResult r = parse("2026-10-09,2026-10-09,Posted,Reward Cashed Out,5.22\n"
                + "2026-10-25,2026-10-25,Posted,Admin Account Adjustment,9.99\n");
        assertEquals(2, r.transactions().size());
        assertTrue(r.transactions().get(0).income());
    }

    @Test
    void aMembershipFeeWithPaymentInTheNameIsStillAPurchase() throws IOException {
        ParseResult r = parse("2026-10-01,2026-10-01,Posted,Example Membership Payment,-9.99\n");
        assertEquals(1, r.transactions().size());
        assertFalse(r.transactions().get(0).income());
    }

    @Test
    void identicalPurchasesOnOneDayAreBothKept() throws IOException {
        ParseResult r = parse("2026-10-01,2026-10-02,Posted,PRESTO GOOG/AAAAAA1111 TORONTO       CAN,-3.00\n"
                + "2026-10-01,2026-10-02,Posted,PRESTO GOOG/BBBBBB2222 TORONTO       CAN,-3.00\n");
        assertEquals(2, r.transactions().size());
    }

    @Test
    void cleansMerchantNames() {
        assertEquals("Fresh Choice Indian Su", CardMerchantCleaner.clean("FRESH CHOICE INDIAN SU TORONTO       CAN"));
        assertEquals("Lyft", CardMerchantCleaner.clean("LYFT   *TEMP AUTH HOLD VANCOUVER     CAN"));
        assertEquals("Hale Coffee Co.", CardMerchantCleaner.clean("SQ *HALE COFFEE CO.    Toronto       CAN"));
        assertEquals("Qishr Coffee House", CardMerchantCleaner.clean("TST-Qishr Coffee House Toronto       CAN"));
        assertEquals("Rabba Fine Foods", CardMerchantCleaner.clean("RABBA FINE FOODS       ETOBICOKE     CAN"));
        assertEquals("Presto", CardMerchantCleaner.clean("PRESTO GOOG/SW6LPBCLHX TORONTO       CAN"));
        assertEquals("Rogers", CardMerchantCleaner.clean("ROGERS ******0831      TORONTO       CAN"));
        assertEquals("Shoppers Drug Mart", CardMerchantCleaner.clean("SHOPPERS DRUG MART #14 TORONTO       CAN"));
        assertEquals("DoorDash", CardMerchantCleaner.clean("DOORDASHEXAMPLESHOP    DOWNTOWN TORO CAN"));
        assertEquals("Uber Eats", CardMerchantCleaner.clean("UBER CANADA/UBEREATS   TORONTO       CAN"));
        assertEquals("Anthropic Claude", CardMerchantCleaner.clean("ANTHROPIC* CLAUDE SUB  SAN FRANCISCO USA"));
    }

    @Test
    void cleanerFallsBackToTheRawTextWhenNothingIsLeft() {
        assertFalse(CardMerchantCleaner.clean("WF/CA2805841390        VANCOUVER     CAN").isBlank());
    }

    @Test
    void commonCardMerchantsGetSensibleCategories() throws IOException {
        ParseResult r = parse("2026-10-01,2026-10-02,Posted,UBER CANADA/UBEREATS   TORONTO       CAN,-20.00\n"
                + "2026-10-01,2026-10-02,Posted,Dineen Coffee Co       Toronto       CAN,-4.00\n"
                + "2026-10-01,2026-10-02,Posted,LOBLAW MLG #1007       TORONTO       CAN,-40.00\n"
                + "2026-10-01,2026-10-02,Posted,LYFT   *TEMP AUTH HOLD VANCOUVER     CAN,-10.00\n"
                + "2026-10-01,2026-10-02,Posted,REXALL PHARMACY #8161  TORONTO       CAN,-6.00\n"
                + "2026-10-01,2026-10-02,Posted,PRESTO GOOG/AAAAAA1111 TORONTO       CAN,-3.00\n");
        var cats = r.transactions().stream().map(t -> categorized(t).category()).toList();
        assertEquals(Categories.DINING, cats.get(0));
        assertEquals(Categories.DINING, cats.get(1));
        assertEquals(Categories.GROCERIES, cats.get(2));
        assertEquals(Categories.TRANSPORT, cats.get(3));
        assertEquals(Categories.HEALTH, cats.get(4));
        assertEquals(Categories.TRANSPORT, cats.get(5));
    }
}
