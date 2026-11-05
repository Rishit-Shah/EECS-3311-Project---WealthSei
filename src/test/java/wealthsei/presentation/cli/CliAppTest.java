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

    @Test
    void safeWithoutAPlanAsksForOne() {
        assertTrue(run("safe").startsWith("Error: No plan yet"));
        assertTrue(run("forecast").startsWith("Error: No plan yet"));
    }

    @Test
    void safeShowsTheDailyAllowanceAndTheExplanation() {
        run("plan balance 1000");
        run("plan buffer 200");
        run("plan income Job 2000 monthly 2026-11-15");
        run("plan bill Rent 900 monthly 2026-11-20");
        String out = run("safe");
        assertTrue(out.contains("Safe to spend on 2026-11-02:"), out);
        assertTrue(out.contains("per day"), out);
        assertTrue(out.contains("30 days") || out.contains("29 days"), out);
        assertTrue(out.contains("balance 1000.00 + income 2000.00 - bills 900.00 - goals 0.00 - buffer 200.00"), out);
        assertTrue(out.contains("You can spend about"), out);
    }

    @Test
    void safeAcceptsADateAndRejectsABadOne() {
        run("plan balance 1000");
        assertTrue(run("safe 2026-11-30").contains("Safe to spend on 2026-11-30:"));
        assertTrue(run("safe 2026-11-30").contains("over 1 days"));
        assertTrue(run("safe tomorrow").startsWith("Error:"));
        assertTrue(run("safe a b").startsWith("Usage:"));
    }

    @Test
    void safeSaysOverCommittedAndAlertsWhenTheBalanceWillDropBelowTheBuffer() {
        run("plan balance 300");
        run("plan buffer 200");
        run("plan bill Rent 900 monthly 2026-11-20");
        String out = run("safe");
        assertTrue(out.contains("0.00 per day (over-committed by 800.00)"), out);
        assertTrue(out.contains("ALERT [OVER]"), out);
    }

    @Test
    void forecastListsDaysWithActivityAndTheLowestBalance() {
        run("plan balance 1000");
        run("plan income Job 500 monthly 2026-11-10");
        run("plan bill Rent 700 monthly 2026-11-20");
        String out = run("forecast 30");
        assertTrue(out.contains("2026-11-02"), out);
        assertTrue(out.contains("2026-11-10") && out.contains("500.00"), out);
        assertTrue(out.contains("2026-11-20") && out.contains("700.00"), out);
        assertTrue(out.contains("Lowest balance 800.00 on 2026-11-20."), out);
    }

    @Test
    void forecastRejectsBadDays() {
        run("plan balance 1000");
        assertTrue(run("forecast 0").startsWith("Usage:"));
        assertTrue(run("forecast 400").startsWith("Usage:"));
        assertTrue(run("forecast abc").startsWith("Error:"));
    }

    @Test
    void habitsNeedMoreDataWhenHistoryIsShort() throws IOException {
        run("import " + csv("2026-10-01,Starbucks,-5\n2026-10-10,Starbucks,-5\n"));
        assertTrue(run("habits 2026-10").contains("Need more data"));
    }

    @Test
    void habitsReportsAWeekendSpike() throws IOException {
        StringBuilder rows = new StringBuilder();
        for (int day = 1; day <= 31; day++) {
            LocalDate d = LocalDate.of(2026, 10, day);
            boolean weekend = d.getDayOfWeek().getValue() >= 6;
            rows.append(d).append(",Pizza Place,-").append(weekend ? "70" : "20").append("\n");
        }
        run("import " + csv(rows.toString()));
        String out = run("habits 2026-10");
        assertTrue(out.contains("Weekend spending spike"), out);
    }

    @Test
    void habitsWithSteadySpendingSaysNoPatterns() throws IOException {
        StringBuilder rows = new StringBuilder();
        for (int day = 1; day <= 31; day++) {
            rows.append(LocalDate.of(2026, 10, day)).append(",Pizza Place,-20\n");
        }
        run("import " + csv(rows.toString()));
        assertTrue(run("habits 2026-10").contains("No notable patterns in 2026-10"));
    }

    @Test
    void scoreIsNotAvailableWithoutData() {
        String out = run("score");
        assertTrue(out.contains("Health score: N/A"), out);
    }

    @Test
    void scoreUsesTheBudgetAndThePlan() {
        run("plan balance 3000");
        run("plan income Salary 3000 monthly 2026-11-15");
        run("budget set 2026-11 dining=300");
        String out = run("score 2026-11");
        assertTrue(out.contains("Health score for 2026-11"), out);
        assertTrue(out.contains("Budget adherence"), out);
        assertTrue(out.contains("(left out)"), out);
    }

    @Test
    void goalAddListAndReplan() {
        run("plan income Job 1000 monthly 2026-11-15");
        String added = run("goal add \"Laptop\" 1200 2027-11-02");
        assertTrue(added.contains("Goal added"), added);
        assertTrue(added.contains("ALERT [WARNING]"), added);
        String list = run("goal list");
        assertTrue(list.contains("Laptop"), list);
        assertTrue(list.contains("AT_RISK"), list);
        String id = list.lines().filter(l -> l.contains("Laptop")).findFirst().orElseThrow().split("\\s+")[0];
        String replan = run("goal replan " + id);
        assertTrue(replan.contains("Needs 100.00 per month"), replan);
        assertTrue(replan.contains("can be reached on time"), replan);
    }

    @Test
    void goalReplanSuggestsChangesWhenItCannotBeReached() {
        run("plan income Job 1000 monthly 2026-11-15");
        run("plan bill Rent 940 monthly 2026-11-01");
        run("goal add \"Laptop\" 1200 2027-11-02");
        String id = run("goal list").lines().filter(l -> l.contains("Laptop")).findFirst().orElseThrow().split("\\s+")[0];
        String replan = run("goal replan " + id);
        assertTrue(replan.contains("will not be reached on time"), replan);
        assertTrue(replan.contains("Move the deadline"), replan);
    }

    @Test
    void goalMonthlyAndSaveChangeTheStatus() {
        run("plan income Job 1000 monthly 2026-11-15");
        run("goal add \"Laptop\" 1200 2027-11-02");
        String id = run("goal list").lines().filter(l -> l.contains("Laptop")).findFirst().orElseThrow().split("\\s+")[0];
        assertTrue(run("goal monthly " + id + " 100").contains("ON_TRACK"));
        assertTrue(run("goal save " + id + " 1200").contains("ACHIEVED"));
    }

    @Test
    void goalCommandsReportMistakes() {
        assertTrue(run("goal").startsWith("Usage:"));
        assertTrue(run("goal add Laptop").startsWith("Usage:"));
        assertTrue(run("goal add \"X\" 100 2020-01-01").startsWith("Error:"));
        assertTrue(run("goal replan nope").startsWith("Error:"));
        assertTrue(run("goal list").contains("No goals yet"));
    }

    @Test
    void affordWithoutAPlanExplainsWhatToDoFirst() {
        String out = run("afford \"laptop\" 600");
        assertTrue(out.contains("plan balance"), out);
        assertTrue(out.contains("TOOL_FAILURE"), out);
    }

    @Test
    void affordWithoutTheModelAnswersFromTheRulesAndShowsTheTrace() {
        run("plan balance 3000");
        run("plan income Job 3000 monthly 2026-11-15");
        String out = run("afford \"laptop\" 600");
        assertTrue(out.contains("Verdict: Yes"), out);
        assertTrue(out.contains("Buying laptop for $600.00"), out);
        assertTrue(out.contains("MODEL_UNAVAILABLE"), out);
        String trace = run("trace");
        assertTrue(trace.contains("TOOL_CALL: forecast"), trace);
        assertTrue(trace.contains("purchase_impact"), trace);
        assertTrue(trace.contains("ERROR"), trace);
    }

    @Test
    void affordShowsNoWhenItWouldOverdraw() {
        run("plan balance 300");
        String out = run("afford \"laptop\" 600");
        assertTrue(out.contains("Verdict: No"), out);
    }

    @Test
    void affordChecksItsInput() {
        assertTrue(run("afford \"laptop\"").startsWith("Usage:"));
        assertTrue(run("afford \"laptop\" 0").startsWith("Error:"));
        assertTrue(run("afford \"laptop\" abc").startsWith("Error:"));
        assertTrue(run("afford \"laptop\" 600 not-a-date").startsWith("Error:"));
    }

    @Test
    void traceBeforeAnyQuestionAndWithAWrongId() {
        assertTrue(run("trace").contains("Nothing to show yet"));
        assertTrue(run("trace nope").contains("No trace with id"));
    }

    @Test
    void whatifWithTypedChangesShowsABaselineAgainstScenarioTable() throws IOException {
        run("import " + csv("2026-10-05,Netflix,-20\n2026-09-05,Netflix,-20\n2026-08-05,Netflix,-20\n2026-08-12,Pizza Place,-300\n"
                + "2026-09-12,Pizza Place,-300\n2026-10-12,Pizza Place,-300\n"));
        run("plan balance 3000");
        run("plan income Job 3000 monthly 2026-11-15");
        String out = run("whatif cancel=Netflix caps=dining=150");
        assertTrue(out.contains("What-if over the next 6 months"), out);
        assertTrue(out.contains("Cancel Netflix"), out);
        assertTrue(out.contains("End balance"), out);
        assertTrue(out.contains("Baseline"), out);
    }

    @Test
    void whatifReportsMistakesInTypedChanges() {
        run("plan balance 3000");
        assertTrue(run("whatif cancel=Hulu").startsWith("Error:"));
        assertTrue(run("whatif caps=pets=10").startsWith("Error:"));
        assertTrue(run("whatif").startsWith("Usage:"));
    }

    @Test
    void whatifInPlainWordsNeedsTheModel() {
        String out = run("whatif cancel my gym");
        assertTrue(out.contains("whatif cancel=Spotify"), out);
        assertTrue(out.contains("MODEL_UNAVAILABLE"), out);
    }

    @Test
    void transactionsCanBeFilteredToTheUncategorizedOnes() throws IOException {
        Path file = csv("Date,Description,Amount\n2026-10-01,Zzqx Unknown Place,-5.00\n2026-10-02,Loblaws,-20.00\n");
        run("import " + file);
        String out = run("transactions category=uncategorized");
        assertTrue(out.contains("Zzqx Unknown Place"));
        assertFalse(out.contains("Loblaws"));
    }
}
