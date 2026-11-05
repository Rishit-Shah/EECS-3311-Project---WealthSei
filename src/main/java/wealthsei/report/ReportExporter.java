package wealthsei.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Writes a report file (Template Method pattern). {@link #export} fixes the order of the steps:
 * header, summary, category table, review, footer. Each format only says how to write each step.
 */
public abstract class ReportExporter {

    /**
     * @return the file that was written
     * @throws ExportException if the file cannot be written
     */
    public final Path export(ReportData data, Path out) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(out, "out");
        String content = writeHeader(data) + writeSummary(data) + writeCategoryTable(data)
                + writeReviewSection(data) + writeFooter(data);
        try {
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null && !Files.isDirectory(parent)) {
                throw new ExportException("The folder " + parent + " does not exist");
            }
            Files.writeString(out, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExportException("Could not write " + out + ": " + e.getMessage(), e);
        }
        return out;
    }

    protected abstract String writeHeader(ReportData data);

    protected abstract String writeSummary(ReportData data);

    protected abstract String writeCategoryTable(ReportData data);

    protected abstract String writeReviewSection(ReportData data);

    protected abstract String writeFooter(ReportData data);

    /** The note used by every format when no review has been generated. */
    protected static String noReviewNote(ReportData data) {
        return "No monthly review has been generated for " + data.month() + ", so this report has figures only.";
    }
}
