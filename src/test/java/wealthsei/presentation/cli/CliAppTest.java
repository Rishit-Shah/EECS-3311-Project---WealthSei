package wealthsei.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.facade.WealthSeiFactory;

class CliAppTest {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final CliApp cli = new CliApp(WealthSeiFactory.inMemory(),
            new PrintStream(buffer, true, StandardCharsets.UTF_8), () -> LocalDate.of(2026, 11, 2));

    /** Runs a command and returns only what it printed. */
    private String run(String line) {
        buffer.reset();
        cli.execute(line);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static Path csv(String body) throws IOException {
        Path p = Files.createTempFile("wealthsei-cli", ".csv");
        Files.writeString(p, "Date,Description,Amount\n" + body);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test
    void helpListsTheCommands() {
        String out = run("help");
        assertTrue(out.contains("import <file.csv>"));
        assertTrue(out.contains("budget status"));
    }

    @Test
    void unknownCommandIsReportedNotFatal() {
        assertTrue(run("dance").contains("Unknown command 'dance'"));
    }

    @Test
    void blankLinesAndCommentsDoNothing() {
        assertEquals("", run("   "));
        assertEquals("", run("# a note"));
    }

    @Test
    void exitStopsTheSession() {
        assertFalse(cli.execute("exit"));
        assertFalse(cli.execute("QUIT"));
        assertTrue(cli.execute("help"));
    }

    @Test
    void importThenListTransactions() throws IOException {
        String out = run("import " + csv("2026-10-02,LOBLAWS,-45.10\n2026-10-03,TIM HORTONS,-4.50\n2026-10-04,BAD ROW,abc\n"));
        assertTrue(out.contains("Imported 2 transactions, skipped 0 duplicates, rejected 1 rows."), out);
        String list = run("transactions");
        assertTrue(list.contains("Loblaws"));
        assertTrue(list.contains("Groceries"));
        assertTrue(list.contains("-45.10"));
        assertTrue(list.contains("2 transactions."));
    }

    @Test
    void reimportingReportsDuplicates() throws IOException {
        Path file = csv("2026-10-02,LOBLAWS,-45.10\n");
        run("import " + file);
        assertTrue(run("import " + file).contains("Imported 0 transactions, skipped 1 duplicates"));
    }

    @Test
    void importOfAMissingFileGivesAnErrorLine() {
        assertTrue(run("import /no/such/file.csv").startsWith("Error:"));
    }

    @Test
    void transactionsCanBeFiltered() throws IOException {
        run("import " + csv("2026-10-02,LOBLAWS,-45.10\n2026-11-03,TIM HORTONS,-4.50\n"));
        String oct = run("transactions month=2026-10");
        assertTrue(oct.contains("Loblaws") && !oct.contains("Tim Hortons"));
        String dining = run("transactions category=dining");
        assertTrue(dining.contains("Tim Hortons") && !dining.contains("Loblaws"));
        assertTrue(run("transactions merchant=lob").contains("Loblaws"));
        assertTrue(run("transactions merchant=zzz").contains("No transactions."));
    }

    @Test
    void badFilterArgumentsGiveUsageOrError() {
        assertTrue(run("transactions month").startsWith("Usage:"));
        assertTrue(run("transactions month=oct").startsWith("Error:"));
        assertTrue(run("transactions category=nope").contains("Unknown category 'nope'"));
    }

    @Test
    void categoriesListsTheIds() {
        String out = run("categories");
        assertTrue(out.contains("groceries") && out.contains("income"));
    }

    @Test
    void correctUndoRedoRoundTrip() throws IOException {
        run("import " + csv("2026-10-02,ZXQV WIDGETS,-5\n"));
        String id = run("transactions").lines().skip(1).findFirst().orElseThrow().split("\\s+")[0];
        assertTrue(run("correct " + id + " shopping").contains("is now Shopping"));
        assertTrue(run("transactions").contains("Shopping"));
        assertTrue(run("undo").contains("back to Uncategorized"));
        assertTrue(run("undo").contains("Nothing to undo."));
        assertTrue(run("redo").contains("Redone"));
        assertTrue(run("redo").contains("Nothing to redo."));
    }

    @Test
    void correctAcceptsAShortIdPrefixAndRejectsUnknownOnes() throws IOException {
        run("import " + csv("2026-10-02,ZXQV WIDGETS,-5\n2026-10-03,QQQ SHOP,-6\n"));
        String shortId = run("transactions").lines().skip(1).findFirst().orElseThrow().split("\\s+")[0];
        assertEquals(8, shortId.length());
        assertTrue(run("correct " + shortId + " shopping").contains("is now Shopping"));
        assertTrue(run("correct zzzzzzzz shopping").contains("No transaction with id"));
        assertTrue(run("correct \"\" shopping").contains("matches 2 transactions"));
    }

    @Test
    void correctWithBadArgumentsIsHandled() {
        assertTrue(run("correct onlyone").startsWith("Usage:"));
        assertTrue(run("correct nope shopping").startsWith("Error:"));
    }

    @Test
    void budgetStatusShowsSpendingAndAlertsAppearOnImport() throws IOException {
        String set = run("budget set 2026-10 groceries=100 dining=50");
        assertTrue(set.contains("Budget for 2026-10") && set.contains("Groceries"));
        String out = run("import " + csv("2026-10-02,LOBLAWS,-90\n"));
        assertTrue(out.contains("ALERT [WARNING]"), out);
        String status = run("budget status 2026-10");
        assertTrue(status.contains("90.00") || status.contains("90"), status);
        assertTrue(status.contains("WARNING"));
        assertTrue(status.contains("Total"));
    }

    @Test
    void budgetBadInputIsHandled() {
        assertTrue(run("budget").startsWith("Usage:"));
        assertTrue(run("budget set 2026-10 groceries").startsWith("Usage:"));
        assertTrue(run("budget set 2026-10 groceries=abc").startsWith("Error:"));
        assertTrue(run("budget status 2026-13").startsWith("Error:"));
        assertTrue(run("budget status 2026-10").contains("No budget has been set"));
    }

    @Test
    void planCanBeBuiltAndShown() {
        assertTrue(run("plan show").contains("No plan yet"));
        run("plan balance 1500");
        run("plan buffer 200");
        assertTrue(run("plan income Job 2000 monthly 2026-11-15").contains("Income added."));
        assertTrue(run("plan bill \"Phone bill\" 50 monthly 2026-11-20 utilities").contains("Bill added."));
        assertTrue(run("plan expense Laptop 1200 2026-12-01").contains("Planned expense added."));
        String show = run("plan show");
        assertTrue(show.contains("Balance 1500.00 as of 2026-11-02"), show);
        assertTrue(show.contains("safety buffer 200.00"));
        assertTrue(show.contains("Job 2000.00 monthly"));
        assertTrue(show.contains("Phone bill 50.00 monthly"));
        assertTrue(show.contains("Laptop 1200.00 on 2026-12-01"));
    }

    @Test
    void planBadInputIsHandled() {
        assertTrue(run("plan balance 100 2027-01-01").startsWith("Error:"));
        assertTrue(run("plan income Job 2000 sometimes 2026-11-15").contains("Unknown frequency"));
        assertTrue(run("plan income Job").startsWith("Usage:"));
        assertTrue(run("plan buffer -5").startsWith("Error:"));
        assertTrue(run("plan").startsWith("Usage:"));
    }

    @Test
    void runReadsLinesUntilExit() throws IOException {
        cli.run(new BufferedReader(new StringReader("categories\nexit\ncategories\n")));
        String out = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("WealthSei - type 'help'"));
        assertTrue(out.contains("Goodbye."));
        assertEquals(1, out.split("Groceries", -1).length - 1);
    }

    @Test
    void unclosedQuoteIsAnError() {
        assertTrue(run("plan bill \"Phone").startsWith("Error:"));
    }

    private static final String SUBSCRIPTIONS = "2026-07-05,NETFLIX.COM,-16.49\n2026-08-05,NETFLIX.COM,-16.49\n"
            + "2026-09-05,NETFLIX.COM,-16.49\n2026-10-05,NETFLIX.COM,-18.99\n"
            + "2026-08-12,SPOTIFY,-10.99\n2026-09-12,SPOTIFY,-10.99\n2026-10-12,SPOTIFY,-10.99\n";

    @Test
    void recurringListsSubscriptionsWithNotesAndASummary() throws IOException {
        run("import " + csv(SUBSCRIPTIONS));
        String out = run("recurring");
        assertTrue(out.contains("Netflix.com") && out.contains("18.99") && out.contains("monthly"), out);
        assertTrue(out.contains("2026-11-05"), out);
        assertTrue(out.contains("price up 2.50"), out);
        assertTrue(out.contains("Spotify"), out);
        assertTrue(out.contains("About 29.98 per month."), out);
        assertTrue(out.contains("You have 2 recurring payments"), out);
    }

    @Test
    void recurringWithNoDataAsksForAnImport() {
        assertTrue(run("recurring").contains("Import a bank CSV"));
    }

    @Test
    void recurringFlagAndUnflag() throws IOException {
        run("import " + csv(SUBSCRIPTIONS));
        assertTrue(run("recurring flag netflix.com").contains("flagged for review"));
        assertTrue(run("recurring").contains("FLAGGED"));
        assertTrue(run("recurring unflag netflix.com").contains("no longer flagged"));
        assertFalse(run("recurring").contains("FLAGGED"));
    }

    @Test
    void recurringBadArgumentsAreHandled() throws IOException {
        run("import " + csv(SUBSCRIPTIONS));
        assertTrue(run("recurring flag").startsWith("Usage:"));
        assertTrue(run("recurring dance x").startsWith("Usage:"));
        assertTrue(run("recurring flag Costco").startsWith("Error: No recurring payment found"));
    }
}
