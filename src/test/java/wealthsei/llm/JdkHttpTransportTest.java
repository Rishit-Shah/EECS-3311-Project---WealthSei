package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises the real HTTP code against a small local server (no real key or network needed). */
class JdkHttpTransportTest {

    private record Seen(String key, String version, String type, String body) { }

    private static HttpServer server(int status, String reply, List<Seen> seen) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/v1/messages", exchange -> {
            seen.add(new Seen(exchange.getRequestHeaders().getFirst("x-api-key"),
                    exchange.getRequestHeaders().getFirst("anthropic-version"),
                    exchange.getRequestHeaders().getFirst("content-type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        s.start();
        return s;
    }

    private static String url(HttpServer s) {
        return "http://127.0.0.1:" + s.getAddress().getPort() + "/v1/messages";
    }

    @Test
    void sendsTheKeyAndVersionHeadersAndReturnsTheBody() throws IOException {
        List<Seen> seen = new ArrayList<>();
        HttpServer s = server(200, "{\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}", seen);
        try {
            LLMProvider provider = new ClaudeProvider(new JdkHttpTransport(new LlmConfig("sk-ant-test-key", null, null), url(s)));
            LLMResponse r = provider.complete(LLMRequest.simple("sys", "hi", "claude-sonnet-5-5"));
            assertEquals("hello", r.text());
            assertEquals(1, seen.size());
            assertEquals("sk-ant-test-key", seen.get(0).key());
            assertEquals("2023-06-01", seen.get(0).version());
            assertEquals("application/json", seen.get(0).type());
            assertTrue(seen.get(0).body().contains("claude-sonnet-5-5"));
        } finally {
            s.stop(0);
        }
    }

    @Test
    void aRejectedKeyGivesAHintAndIsNotRetried() throws IOException {
        List<Seen> seen = new ArrayList<>();
        HttpServer s = server(401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}", seen);
        try {
            JdkHttpTransport transport = new JdkHttpTransport(new LlmConfig("sk-ant-wrong-key", null, null), url(s));
            LLMException e = assertThrows(LLMException.class, () -> transport.post("{}"));
            assertTrue(e.getMessage().contains("401"));
            assertTrue(e.getMessage().contains(".env"));
            assertTrue(e.getMessage().contains("invalid x-api-key"));
            assertFalse(e.getMessage().contains("sk-ant-wrong-key"));
            assertFalse(e.isRetryable());
        } finally {
            s.stop(0);
        }
    }

    @Test
    void rateLimitsAndServerErrorsAreRetryable() throws IOException {
        List<Seen> seen = new ArrayList<>();
        HttpServer s = server(429, "not json", seen);
        HttpServer s2 = server(503, "{}", seen);
        try {
            LlmConfig config = new LlmConfig("sk-ant-x", null, null);
            assertTrue(assertThrows(LLMException.class, () -> new JdkHttpTransport(config, url(s)).post("{}")).isRetryable());
            assertTrue(assertThrows(LLMException.class, () -> new JdkHttpTransport(config, url(s2)).post("{}")).isRetryable());
        } finally {
            s.stop(0);
            s2.stop(0);
        }
    }

    @Test
    void aServerThatIsNotThereIsRetryable() {
        JdkHttpTransport transport = new JdkHttpTransport(new LlmConfig("sk-ant-x", null, null), "http://127.0.0.1:1/v1/messages");
        LLMException e = assertThrows(LLMException.class, () -> transport.post("{}"));
        assertTrue(e.isRetryable());
    }

    @Test
    void noKeyMeansNoRequest() {
        JdkHttpTransport transport = new JdkHttpTransport(LlmConfig.none(), "http://127.0.0.1:1/v1/messages");
        assertFalse(assertThrows(LLMException.class, () -> transport.post("{}")).isRetryable());
    }
}
