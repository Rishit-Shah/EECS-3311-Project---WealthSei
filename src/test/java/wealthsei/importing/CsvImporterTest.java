package wealthsei.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.ParseResult;

class CsvImporterTest {

    private final AtomicInteger ids = new AtomicInteger();
    private final CsvImporter importer = new CsvImporter(() -> "t" + ids.incrementAndGet());

    private static Path csv(String content) throws IOException {
        Path p = Files.createTempFile("wealthsei-test", ".csv");
        Files.writeString(p, content);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test
    void detectsSignedAmountLayout() throws IOException {
        ParseResult r = importer.parse(csv("Date,Description,Amount\n2026-10-01,Coffee,-3.50\n"));
        assertEquals(1, r.transactions().size());
        assertFalse(r.transactions().get(0).income());
    }

    @Test
    void detectsDebitCreditLayout() throws IOException {
        ParseResult r = importer.parse(csv("Date,Description,Debit,Credit\n2026-10-01,Pay,,100\n"));
        assertEquals(1, r.transactions().size());
        assertTrue(r.transactions().get(0).income());
    }

    @Test
    void unknownHeaderListsSupportedLayouts() throws IOException {
        Path p = csv("Foo,Bar\n1,2\n");
        UnsupportedFormatException e = assertThrows(UnsupportedFormatException.class, () -> importer.parse(p));
        assertTrue(e.getMessage().contains("Supported layouts"));
        assertTrue(e.getMessage().contains("Date, Description, Amount"));
        assertTrue(e.getMessage().contains("Date, Description, Debit, Credit"));
    }

    @Test
    void emptyFileIsUnsupported() throws IOException {
        Path p = csv("\n\n");
        assertThrows(UnsupportedFormatException.class, () -> importer.parse(p));
    }

    @Test
    void missingFileIsUnsupported() {
        Path p = Path.of("this-file-does-not-exist.csv");
        assertThrows(UnsupportedFormatException.class, () -> importer.parse(p));
    }

    @Test
    void brokenQuotingIsUnsupported() throws IOException {
        Path p = csv("Date,Description,Amount\n2026-10-01,\"Coffee,-3\n");
        UnsupportedFormatException e = assertThrows(UnsupportedFormatException.class, () -> importer.parse(p));
        assertTrue(e.getMessage().startsWith("Not a valid CSV file"));
    }

    @Test
    void nonTextFileIsUnsupported() throws IOException {
        Path p = Files.createTempFile("wealthsei-test", ".csv");
        p.toFile().deleteOnExit();
        Files.write(p, new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0xFF, (byte) 0xFE, (byte) 0x80});
        assertThrows(UnsupportedFormatException.class, () -> importer.parse(p));
    }

    @Test
    void utf8TextWithAccentsIsReadFine() throws IOException {
        Path p = Files.createTempFile("wealthsei-test", ".csv");
        p.toFile().deleteOnExit();
        Files.writeString(p, "Date,Description,Amount\n2026-10-01,Café Crème,-4.00\n", StandardCharsets.UTF_8);
        assertEquals(1, importer.parse(p).transactions().size());
    }
}
