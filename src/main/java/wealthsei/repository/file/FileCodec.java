package wealthsei.repository.file;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import wealthsei.domain.Category;

/** Small helpers shared by the file-backed repositories. */
final class FileCodec {

    private FileCodec() {
    }

    /** Appends a category as three columns: id, name, essential. A null category becomes three empty columns. */
    static void addCategory(List<String> row, Category c) {
        if (c == null) {
            row.add("");
            row.add("");
            row.add("");
        } else {
            row.add(c.id());
            row.add(c.name());
            row.add(Boolean.toString(c.essential()));
        }
    }

    /** Reads three category columns starting at {@code index}; returns null when they are empty. */
    static Category readCategory(TsvFile.Row row, int index) {
        String id = field(row, index);
        if (id.isEmpty()) {
            return null;
        }
        return new Category(id, field(row, index + 1), parseBoolean(field(row, index + 2)));
    }

    static String field(TsvFile.Row row, int index) {
        if (index >= row.fields().size()) {
            throw new IllegalArgumentException("missing column " + (index + 1));
        }
        return row.fields().get(index);
    }

    static boolean parseBoolean(String text) {
        if (text.equals("true")) {
            return true;
        }
        if (text.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("expected true or false but found '" + text + "'");
    }

    /** Parses a row, turning any parsing problem into a {@link StorageException} naming file and line. */
    static <T> T parse(Path file, TsvFile.Row row, Function<TsvFile.Row, T> parser) {
        try {
            return parser.apply(row);
        } catch (RuntimeException e) {
            throw new StorageException(file + " line " + row.line() + ": " + e.getMessage(), e);
        }
    }
}
