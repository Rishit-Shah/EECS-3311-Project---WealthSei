package wealthsei;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliOptionsTest {

    @Test
    void noArgumentsMeansInteractive() {
        CliOptions o = CliOptions.parse(new String[0]);
        assertNull(o.input());
        assertTrue(o.command().isEmpty());
        assertFalse(o.noAi() || o.requireAi() || o.help());
    }

    @Test
    void inputAcceptsOneOrTwoDashesAndAnyCase() {
        assertEquals(Path.of("task.txt"), CliOptions.parse(new String[] {"-input", "task.txt"}).input());
        assertEquals(Path.of("task.txt"), CliOptions.parse(new String[] {"--input", "task.txt"}).input());
        assertEquals(Path.of("task.txt"), CliOptions.parse(new String[] {"-INPUT", "task.txt"}).input());
    }

    @Test
    void llmApiAndNoAiAndDataDir() {
        CliOptions o = CliOptions.parse(new String[] {"-LLMAPI", "-data-dir", "/tmp/x", "-input", "t.txt"});
        assertTrue(o.requireAi());
        assertEquals(Path.of("/tmp/x"), o.dataDir());
        assertTrue(CliOptions.parse(new String[] {"--no-ai", "safe"}).noAi());
    }

    @Test
    void wordsThatAreNotOptionsAreTheCommand() {
        CliOptions o = CliOptions.parse(new String[] {"afford", "laptop", "600"});
        assertEquals(List.of("afford", "laptop", "600"), o.command());
    }

    @Test
    void aDashAfterTheCommandStartsIsPartOfTheCommand() {
        CliOptions o = CliOptions.parse(new String[] {"add", "2026-11-07", "-12.50", "Shop"});
        assertEquals(List.of("add", "2026-11-07", "-12.50", "Shop"), o.command());
    }

    @Test
    void badUsageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[] {"-bogus"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[] {"-input"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[] {"-LLMAPI", "-no-ai"}));
        assertThrows(IllegalArgumentException.class, () -> CliOptions.parse(new String[] {"-input", "t.txt", "safe"}));
    }

    @Test
    void helpIsRecognised() {
        assertTrue(CliOptions.parse(new String[] {"-help"}).help());
        assertTrue(CliOptions.USAGE.contains("-input <file>"));
    }
}
