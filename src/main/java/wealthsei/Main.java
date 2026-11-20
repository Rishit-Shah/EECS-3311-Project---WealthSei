package wealthsei;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.llm.LLMProvider;
import wealthsei.llm.LLMProviders;
import wealthsei.llm.LlmConfig;
import wealthsei.presentation.cli.CliApp;
import wealthsei.repository.file.StorageException;

/**
 * Starts the command-line version, or the window with the argument {@code gui}. With a command, runs that one command and exits
 * (for example {@code import bank.csv}); with {@code -input task.txt}, runs every command in the file; with nothing, starts an
 * interactive session. See {@link CliOptions} for the options.
 *
 * <p>Data is saved in the folder named by the {@code WEALTHSEI_HOME} environment variable, or
 * in {@code .wealthsei} inside the user's home folder.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
            return;
        }
        if (options.help()) {
            System.out.println(CliOptions.USAGE);
            return;
        }
        Path dataDir = options.dataDir() != null ? options.dataDir() : dataDirectory();
        LlmConfig config = options.noAi() ? LlmConfig.none() : LlmConfig.load(Path.of(".env"), System.getenv());
        if (options.requireAi() && !config.hasKey()) {
            System.err.println("Error: -LLMAPI needs an API key. Put ANTHROPIC_API_KEY in the .env file (see .env.example).");
            System.exit(2);
            return;
        }
        LLMProvider provider = LLMProviders.create(config, line -> appendLog(dataDir, line));
        if (options.command().size() == 1 && options.command().get(0).equalsIgnoreCase("gui")) {
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
        if (options.input() != null) {
            try (BufferedReader in = Files.newBufferedReader(options.input(), StandardCharsets.UTF_8)) {
                cli.runScript(in);
            } catch (IOException e) {
                System.err.println("Error: could not read the task file " + options.input() + ": " + e.getMessage());
                System.exit(2);
            }
            return;
        }
        if (!options.command().isEmpty()) {
            cli.execute(String.join(" ", options.command().stream().map(Main::quoteIfNeeded).toList()));
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
