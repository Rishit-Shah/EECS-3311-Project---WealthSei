package wealthsei.importing;

import java.util.List;

/** One data row of a CSV file. */
public final class CsvRecord {

    private final int lineNumber;
    private final List<String> values;

    public CsvRecord(int lineNumber, List<String> values) {
        this.lineNumber = lineNumber;
        this.values = List.copyOf(values);
    }

    /** The line of the file this record starts on (the first line is 1). */
    public int lineNumber() {
        return lineNumber;
    }

    public int size() {
        return values.size();
    }

    /** The value at the column index, trimmed; an empty string if the row is shorter. */
    public String get(int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }
}
