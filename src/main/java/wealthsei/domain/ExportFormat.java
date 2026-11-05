package wealthsei.domain;

import java.util.Locale;

/** The file formats a report can be saved in. */
public enum ExportFormat {
    MARKDOWN("md"),
    CSV("csv"),
    TEXT("txt");

    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    /** Accepts md, markdown, csv, txt or text (any case). */
    public static ExportFormat parse(String text) {
        String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "md", "markdown" -> MARKDOWN;
            case "csv" -> CSV;
            case "txt", "text" -> TEXT;
            default -> throw new IllegalArgumentException("Unknown format '" + text + "'. Use md, csv or txt.");
        };
    }
}
