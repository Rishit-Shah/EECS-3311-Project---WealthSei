package wealthsei.report;

import wealthsei.domain.SavingsGoal;

/**
 * One CSV table that a spreadsheet can open: {@code section,name,amount,limit,remaining,status}.
 * Text with commas, quotes or line breaks is quoted.
 */
public class CsvExporter extends ReportExporter {

    @Override
    protected String writeHeader(ReportData data) {
        return row("section", "name", "amount", "limit", "remaining", "status");
    }

    @Override
    protected String writeSummary(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(row("summary", "month", data.month().toString(), "", "", ""));
        sb.append(row("summary", "money_in", data.income().toString(), "", "", ""));
        sb.append(row("summary", "money_out", data.spending().toString(), "", "", ""));
        sb.append(row("summary", "net", data.net().toString(), "", "", ""));
        sb.append(row("summary", "transactions", Integer.toString(data.transactionCount()), "", "", ""));
        for (SavingsGoal g : data.goals()) {
            sb.append(row("goal", g.name(), g.saved().toString(), g.target().toString(),
                    g.remaining().toString(), g.status().name()));
        }
        return sb.toString();
    }

    @Override
    protected String writeCategoryTable(ReportData data) {
        StringBuilder sb = new StringBuilder();
        for (ReportData.CategoryRow r : data.categories()) {
            sb.append(row("category", r.category().name(), r.spent().toString(),
                    r.limit().map(Object::toString).orElse(""), r.remaining().map(Object::toString).orElse(""),
                    r.level().map(Enum::name).orElse("NO_BUDGET")));
        }
        return sb.toString();
    }

    @Override
    protected String writeReviewSection(ReportData data) {
        if (data.review().isEmpty()) {
            return row("review", "note", "", "", "", noReviewNote(data));
        }
        var review = data.review().get();
        return row("review", "health_score", Integer.toString(review.healthScore().total()), "", "", "")
                + row("review", "narrative", "", "", "", review.narrative());
    }

    @Override
    protected String writeFooter(ReportData data) {
        return row("footer", "generated_on", data.generatedOn().toString(), "", "", "");
    }

    private static String row(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        return sb.append('\n').toString();
    }

    static String escape(String cell) {
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
