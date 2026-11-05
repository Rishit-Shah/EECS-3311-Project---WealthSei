package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DecoratorsTest {

    private static final LLMRequest REQUEST = LLMRequest.simple("", "q", "m");

    @Test
    void mockReturnsRepliesInOrderAndRecordsRequests() {
        MockLLMProvider mock = new MockLLMProvider(LLMResponse.text("one"), LLMResponse.text("two"));
        assertEquals("one", mock.complete(REQUEST).text());
        assertEquals("two", mock.complete(REQUEST).text());
        assertEquals(2, mock.requests().size());
        assertThrows(IllegalStateException.class, () -> mock.complete(REQUEST));
    }

    @Test
    void mockCanFailOnCue() {
        MockLLMProvider mock = new MockLLMProvider().thenFail(new LLMException("down", true)).thenReply(LLMResponse.text("ok"));
        assertThrows(LLMException.class, () -> mock.complete(REQUEST));
        assertEquals("ok", mock.complete(REQUEST).text());
    }

    @Test
    void retryingTriesAgainOnRetryableFailuresWithGrowingPauses() {
        List<Long> pauses = new ArrayList<>();
        MockLLMProvider mock = new MockLLMProvider().thenFail(new LLMException("busy", true))
                .thenFail(new LLMException("busy", true)).thenReply(LLMResponse.text("ok"));
        RetryingLLMProvider retrying = new RetryingLLMProvider(mock, 3, 100, pauses::add);
        assertEquals("ok", retrying.complete(REQUEST).text());
        assertEquals(List.of(100L, 200L), pauses);
        assertEquals(3, mock.requests().size());
    }

    @Test
    void retryingGivesUpAfterTheLimit() {
        MockLLMProvider mock = new MockLLMProvider().thenFail(new LLMException("a", true))
                .thenFail(new LLMException("b", true)).thenFail(new LLMException("c", true));
        RetryingLLMProvider retrying = new RetryingLLMProvider(mock, 2, 0, ms -> { });
        LLMException e = assertThrows(LLMException.class, () -> retrying.complete(REQUEST));
        assertEquals("c", e.getMessage());
        assertEquals(3, mock.requests().size());
    }

    @Test
    void retryingDoesNotRepeatFailuresThatCannotPass() {
        MockLLMProvider mock = new MockLLMProvider().thenFail(new LLMException("bad key", false)).thenReply(LLMResponse.text("never"));
        assertThrows(LLMException.class, () -> new RetryingLLMProvider(mock, 3, 0, ms -> { }).complete(REQUEST));
        assertEquals(1, mock.requests().size());
    }

    @Test
    void retryingRejectsNegativeSettings() {
        assertThrows(IllegalArgumentException.class, () -> new RetryingLLMProvider(new MockLLMProvider(), -1, 0));
    }

    @Test
    void loggingCountsCallsFailuresAndTokensWithoutLoggingContent() {
        List<String> lines = new ArrayList<>();
        MockLLMProvider mock = new MockLLMProvider(new LLMResponse("secret answer", List.of(), 42))
                .thenFail(new LLMException("boom", false));
        LoggingLLMProvider logging = new LoggingLLMProvider(mock, lines::add);
        logging.complete(LLMRequest.simple("system text", "private question", "claude-x"));
        assertThrows(LLMException.class, () -> logging.complete(REQUEST));
        assertEquals(2, logging.calls());
        assertEquals(1, logging.failures());
        assertEquals(42, logging.totalTokens());
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("tokens=42"));
        assertTrue(lines.get(1).contains("FAILED"));
        assertFalse(String.join("\n", lines).contains("secret answer"));
        assertFalse(String.join("\n", lines).contains("private question"));
    }

    @Test
    void decoratorsNest() {
        List<String> lines = new ArrayList<>();
        MockLLMProvider mock = new MockLLMProvider().thenFail(new LLMException("busy", true)).thenReply(new LLMResponse("ok", List.of(), 5));
        LoggingLLMProvider logging = new LoggingLLMProvider(new RetryingLLMProvider(mock, 1, 0, ms -> { }), lines::add);
        assertEquals("ok", logging.complete(REQUEST).text());
        assertEquals(1, logging.calls());
        assertEquals(0, logging.failures());
    }

    @Test
    void unavailableProviderAlwaysFails() {
        assertThrows(LLMException.class, () -> new UnavailableLLMProvider("off").complete(REQUEST));
    }
}
