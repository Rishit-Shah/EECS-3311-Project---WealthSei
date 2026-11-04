package wealthsei.repository.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TsvFileTest {

    private static Path tempDir() throws IOException {
        Path dir = Files.createTempDirectory("wealthsei-tsv");
        dir.toFile().deleteOnExit();
        return dir;
    }

    @Test
    void missingFileReadsAsNoRows() throws IOException {
        assertTrue(TsvFile.read(tempDir().resolve("nope.tsv")).isEmpty());
    }

    @Test
    void awkwardTextSurvivesARoundTrip() throws IOException {
        Path file = tempDir().resolve("a.tsv");
        List<List<String>> rows = List.of(
                List.of("a\tb", "line1\nline2", "back\\slash", "", "café €"),
                List.of("x", "carriage\rreturn", "\\n literally"));
        TsvFile.write(file, rows);
        List<TsvFile.Row> back = TsvFile.read(file);
        assertEquals(2, back.size());
        assertEquals(rows.get(0), back.get(0).fields());
        assertEquals(rows.get(1), back.get(1).fields());
        assertEquals(1, back.get(0).line());
        assertEquals(2, back.get(1).line());
    }

    @Test
    void writeReplacesTheFileAndLeavesNoTemporaryFile() throws IOException {
        Path dir = tempDir();
        Path file = dir.resolve("a.tsv");
        TsvFile.write(file, List.of(List.of("old")));
        TsvFile.write(file, List.of(List.of("new")));
        assertEquals("new", TsvFile.read(file).get(0).fields().get(0));
        try (var names = Files.list(dir)) {
            assertEquals(1, names.count());
        }
    }

    @Test
    void writeCreatesMissingFolders() throws IOException {
        Path file = tempDir().resolve("deep/er/a.tsv");
        TsvFile.write(file, List.of(List.of("x")));
        assertTrue(Files.exists(file));
    }

    @Test
    void emptyLinesAreSkipped() throws IOException {
        Path file = tempDir().resolve("a.tsv");
        Files.writeString(file, "a\tb\n\nc\td\n");
        List<TsvFile.Row> rows = TsvFile.read(file);
        assertEquals(2, rows.size());
        assertEquals(3, rows.get(1).line());
    }

    @Test
    void badEscapeNamesTheFileAndLine() throws IOException {
        Path file = tempDir().resolve("a.tsv");
        Files.writeString(file, "ok\nbad\\q\n");
        StorageException e = assertThrows(StorageException.class, () -> TsvFile.read(file));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
        assertTrue(e.getMessage().contains("a.tsv"));
    }

    @Test
    void loneTrailingBackslashIsRejected() throws IOException {
        Path file = tempDir().resolve("a.tsv");
        Files.writeString(file, "oops\\\n");
        assertThrows(StorageException.class, () -> TsvFile.read(file));
    }
}
