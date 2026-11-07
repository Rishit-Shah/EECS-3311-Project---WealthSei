package wealthsei;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.llm.LLMProvider;
import wealthsei.llm.LLMProviders;
import wealthsei.llm.LlmConfig;
import wealthsei.presentation.cli.CliApp;
import wealthsei.repository.file.StorageException;

/**
 * Starts the command-line version, or the window with the argument {@code gui}. With arguments, runs that one command and exits
 * (for example {@code import bank.csv}); with none, starts an interactive session.
 *
 * <p>Data is saved in the folder named by the {@code WEALTHSEI_HOME} environment variable, or
 * in {@code .wealthsei} inside the user's home folder.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = dataDirectory();
        LlmConfig config = LlmConfig.load(Path.of(".env"), System.getenv());
        LLMProvider provider = LLMProviders.create(config, line -> appendLog(dataDir, line));
        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            try {
                wealthsei.presentation.gui.MainWindow.launch(WealthSeiFactory.persistent(dataDir, provider, config),
                        java.time.LocalDate::now, config.hasKey(), dataDir.toString());
            } catch (StorageException e) {
                System.err.println("Cannot open the data folder: " + e.getMessage());
                System.exit(1);
            }
            return;
        }
        CliApp cli;
        try {
            cli = new CliApp(WealthSeiFactory.persistent(dataDir, provider, config), System.out);
            cli.setCallLog(dataDir.resolve("llm.log"));
        } catch (StorageException e) {
            System.err.println("Cannot open the data folder: " + e.getMessage());
            System.exit(1);
            return;
        }
        if (args.length > 0) {
            cli.execute(String.join(" ", Arrays.stream(args).map(Main::quoteIfNeeded).toList()));
            return;
        }
        System.out.println("Data is saved in " + dataDir);
        System.out.println(config.hasKey() ? "AI features: on (" + config.sonnetModel() + ")"
                : "AI features: off (no API key found; answers use fixed rules and templates)");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            cli.run(in);
        }
    }

    /** Appends a line (call details only, never prompts or keys) to llm.log in the data folder. */
    private static void appendLog(Path dataDir, String line) {
        try {
            Files.createDirectories(dataDir);
            Files.writeString(dataDir.resolve("llm.log"), java.time.Instant.now() + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // logging must never stop the program
        }
    }

    static Path dataDirectory() {
        String custom = System.getenv("WEALTHSEI_HOME");
        if (custom != null && !custom.isBlank()) {
            return Path.of(custom);
        }
        return Path.of(System.getProperty("user.home"), ".wealthsei");
    }

    private static String quoteIfNeeded(String arg) {
        return arg.contains(" ") ? "\"" + arg + "\"" : arg;
    }
}
