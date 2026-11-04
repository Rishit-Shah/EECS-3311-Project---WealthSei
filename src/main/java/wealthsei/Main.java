package wealthsei;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.presentation.cli.CliApp;
import wealthsei.repository.file.StorageException;

/**
 * Starts the command-line version. With arguments, runs that one command and exits
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
        CliApp cli;
        try {
            cli = new CliApp(WealthSeiFactory.persistent(dataDir), System.out);
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            cli.run(in);
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
