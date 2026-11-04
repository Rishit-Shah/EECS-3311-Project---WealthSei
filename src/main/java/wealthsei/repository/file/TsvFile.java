package wealthsei.repository.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes small tab-separated text files. One record per line; tabs, new lines and
 * backslashes inside a value are escaped, so any text survives a round trip. Records must have
 * at least one non-empty field, because empty lines are skipped when reading.
 */
public final class TsvFile {

    /** One record and the line it came from (counting from 1), for error messages. */
    public record Row(int line, List<String> fields) {
    }

    private TsvFile() {
    }

    /** Returns no rows when the file does not exist yet. */
    public static List<Row> read(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Cannot read " + file + ": " + e.getMessage(), e);
        }
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty()) {
                continue;
            }
            List<String> fields = new ArrayList<>();
            for (String raw : line.split("\t", -1)) {
                try {
                    fields.add(unescape(raw));
                } catch (IllegalArgumentException e) {
                    throw new StorageException(file + " line " + (i + 1) + ": " + e.getMessage(), e);
                }
            }
            rows.add(new Row(i + 1, fields));
        }
        return rows;
    }

    /** Replaces the file. Writes a temporary file first so a crash cannot leave half a file. */
    public static void write(Path file, List<List<String>> rows) {
        StringBuilder text = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    text.append('\t');
                }
                text.append(escape(row.get(i)));
            }
            text.append('\n');
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temp = parent.resolve(file.getFileName() + ".tmp");
            Files.writeString(temp, text.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Cannot write " + file + ": " + e.getMessage(), e);
        }
    }

    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String unescape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= value.length()) {
                throw new IllegalArgumentException("text ends with a lone backslash");
            }
            char next = value.charAt(++i);
            switch (next) {
                case '\\' -> sb.append('\\');
                case 't' -> sb.append('\t');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                default -> throw new IllegalArgumentException("unknown escape \\" + next);
            }
        }
        return sb.toString();
    }
}
