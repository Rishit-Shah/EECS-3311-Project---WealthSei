package wealthsei.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Sends the request over HTTPS with the JDK's built-in client. The key goes only in the
 * request header; it is never printed, logged or included in an exception message.
 */
public class JdkHttpTransport implements HttpTransport {

    public static final String DEFAULT_URL = "https://api.anthropic.com/v1/messages";
    static final String API_VERSION = "2023-06-01";

    private final LlmConfig config;
    private final URI url;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public JdkHttpTransport(LlmConfig config) {
        this(config, DEFAULT_URL);
    }

    public JdkHttpTransport(LlmConfig config, String url) {
        this.config = Objects.requireNonNull(config, "config");
        this.url = URI.create(url);
    }

    @Override
    public String post(String jsonBody) {
        String key = config.apiKey().orElseThrow(() -> new LLMException("No API key is set", false));
        HttpRequest request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json")
                .header("x-api-key", key)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("The request was interrupted", false, e);
        } catch (IOException e) {
            throw new LLMException("Could not reach the model service: " + e.getClass().getSimpleName(), true, e);
        }
        int status = response.statusCode();
        if (status == 200) {
            return response.body();
        }
        boolean retryable = status == 429 || status >= 500;
        String hint = switch (status) {
            case 401 -> " (the API key was rejected: check ANTHROPIC_API_KEY in .env)";
            case 402, 403 -> " (check the account has credits)";
            case 404 -> " (check the model name in .env)";
            case 429 -> " (rate limited or out of credits)";
            default -> "";
        };
        throw new LLMException("The model service answered with status " + status + hint + detail(response.body()), retryable);
    }

    private static String detail(String body) {
        try {
            Object err = Json.parseObject(body).get("error");
            if (err instanceof java.util.Map<?, ?> m && m.get("message") != null) {
                return ": " + m.get("message");
            }
        } catch (IllegalArgumentException e) {
            // not JSON; say nothing more
        }
        return "";
    }
}
