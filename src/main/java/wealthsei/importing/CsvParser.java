package wealthsei.importing;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A small CSV reader (RFC 4180 style): commas, quoted fields, doubled quotes ("") inside
 * quotes, line breaks inside quotes, CRLF or LF line ends, an optional byte-order mark and
 * blank lines. This is the "adaptee" that the bank adapters wrap.
 *
 * <p>The first non-blank line is the header; every later non-blank line is a {@link CsvRecord}.
 */
public final class CsvParser {

    private final List<String> header;
    private final List<CsvRecord> records;

    private CsvParser(List<String> header, List<CsvRecord> records) {
        this.header = List.copyOf(header);
        this.records = List.copyOf(records);
    }

    public static CsvParser parse(Reader reader) throws IOException {
        StringBuilder all = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) {
            all.append(buf, 0, n);
        }
        return parse(all.toString());
    }

    public static CsvParser parse(String text) {
        String s = text.startsWith("﻿") ? text.substring(1) : text;
        s = s.replace("\r\n", "\n").replace('\r', '\n');

        List<List<String>> rows = new ArrayList<>();
        List<Integer> startLines = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldHadQuotes = false;
        int line = 1;
        int recordStart = 1;
        int quoteOpenedOnLine = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == '"' && field.length() == 0 && !fieldHadQuotes) {
                inQuotes = true;
                fieldHadQuotes = true;
                quoteOpenedOnLine = line;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                fieldHadQuotes = false;
            } else if (c == '\n') {
                fields.add(field.toString());
                addRow(rows, startLines, fields, recordStart);
                fields = new ArrayList<>();
                field.setLength(0);
                fieldHadQuotes = false;
                line++;
                recordStart = line;
            } else {
                field.append(c);
            }
        }
        if (inQuotes) {
            throw new CsvFormatException("A quote opened on line " + quoteOpenedOnLine + " is never closed");
        }
        if (field.length() > 0 || !fields.isEmpty() || fieldHadQuotes) {
            fields.add(field.toString());
            addRow(rows, startLines, fields, recordStart);
        }

        if (rows.isEmpty()) {
            return new CsvParser(List.of(), List.of());
        }
        List<String> header = new ArrayList<>();
        for (String h : rows.get(0)) {
            header.add(h.trim());
        }
        List<CsvRecord> records = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            records.add(new CsvRecord(startLines.get(r), rows.get(r)));
        }
        return new CsvParser(header, records);
    }

    private static void addRow(List<List<String>> rows, List<Integer> startLines,
                               List<String> fields, int startLine) {
        boolean blank = fields.stream().allMatch(f -> f.trim().isEmpty());
        if (!blank) {
            rows.add(fields);
            startLines.add(startLine);
        }
    }

    /** The header names, trimmed; empty if the text had no non-blank line. */
    public List<String> header() {
        return header;
    }

    public List<CsvRecord> records() {
        return records;
    }
}
