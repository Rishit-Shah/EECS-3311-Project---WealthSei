package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmConfigTest {

    private static Path envFile(String body) throws IOException {
        Path p = Files.createTempFile("wealthsei-env", ".env");
        Files.writeString(p, body);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test
    void readsTheEnvFile() throws IOException {
        LlmConfig c = LlmConfig.load(envFile("# comment\nANTHROPIC_API_KEY=sk-ant-abc123\nWEALTHSEI_SONNET_MODEL=\"model-s\"\n"
                + "export WEALTHSEI_HAIKU_MODEL='model-h'\n\nnot a pair\n"), Map.of());
        assertTrue(c.hasKey());
        assertEquals("model-s", c.sonnetModel());
        assertEquals("model-h", c.haikuModel());
    }

    @Test
    void environmentVariablesWinOverTheFile() throws IOException {
        LlmConfig c = LlmConfig.load(envFile("ANTHROPIC_API_KEY=sk-ant-file\nWEALTHSEI_SONNET_MODEL=file-model\n"),
                Map.of("WEALTHSEI_SONNET_MODEL", "env-model"));
        assertEquals("env-model", c.sonnetModel());
        assertEquals(LlmConfig.DEFAULT_HAIKU, c.haikuModel());
    }

    @Test
    void placeholderBlankAndMissingKeysMeanNoKey() throws IOException {
        assertFalse(LlmConfig.load(envFile("ANTHROPIC_API_KEY=sk-ant-REPLACE_ME\n"), Map.of()).hasKey());
        assertFalse(LlmConfig.load(envFile("ANTHROPIC_API_KEY=\n"), Map.of()).hasKey());
        assertFalse(LlmConfig.load(Path.of("/no/such/file/.env"), Map.of()).hasKey());
        assertFalse(LlmConfig.none().hasKey());
        assertFalse(LlmConfig.load(null, Map.of()).hasKey());
    }

    @Test
    void keyFromTheEnvironmentAloneWorks() {
        assertTrue(LlmConfig.load(null, Map.of("ANTHROPIC_API_KEY", "sk-ant-xyz")).hasKey());
    }

    @Test
    void defaultsApplyWhenModelsAreNotSet() {
        LlmConfig c = LlmConfig.none();
        assertEquals("claude-sonnet-5-5", c.sonnetModel());
        assertEquals("claude-haiku-5-5", c.haikuModel());
    }

    @Test
    void theKeyNeverAppearsInText() {
        LlmConfig c = new LlmConfig("sk-ant-SECRET-VALUE", null, null);
        assertFalse(c.toString().contains("SECRET"));
        assertTrue(c.toString().contains("set"));
    }

    @Test
    void providersFactoryUsesTheStandInWithoutAKey() {
        LLMException e = assertThrows(LLMException.class,
                () -> LLMProviders.create(LlmConfig.none(), s -> { }).complete(LLMRequest.simple("", "q", "m")));
        assertTrue(e.getMessage().contains("ANTHROPIC_API_KEY"));
        assertNotNull(LLMProviders.create(new LlmConfig("sk-ant-x", null, null), s -> { }));
    }
}
