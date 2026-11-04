package wealthsei.presentation.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArgTokenizerTest {

    @Test
    void splitsOnWhitespace() {
        assertEquals(List.of("budget", "set", "2026-10"), ArgTokenizer.tokenize("  budget   set\t2026-10 "));
    }

    @Test
    void quotesKeepSpacesTogether() {
        assertEquals(List.of("plan", "bill", "Phone bill", "50"), ArgTokenizer.tokenize("plan bill \"Phone bill\" 50"));
    }

    @Test
    void emptyQuotesGiveAnEmptyWord() {
        assertEquals(List.of("a", "", "b"), ArgTokenizer.tokenize("a \"\" b"));
    }

    @Test
    void emptyLineGivesNoWords() {
        assertTrue(ArgTokenizer.tokenize("   ").isEmpty());
    }

    @Test
    void unclosedQuoteIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> ArgTokenizer.tokenize("say \"hello"));
    }
}
