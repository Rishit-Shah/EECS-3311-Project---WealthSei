package wealthsei;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The options of the standalone program, read from the command line.
 *
 * <p>Options may be written with one or two dashes ({@code -input} or {@code --input}). Anything that is not an option is the
 * command to run (for example {@code afford "laptop" 600}).
 */
final class CliOptions {

    static final String USAGE = """
            WealthSei - personal-finance agent

            Usage:
              java -jar wealthsei.jar                          interactive session (type 'help')
              java -jar wealthsei.jar <command ...>            run one command and exit, e.g. safe
              java -jar wealthsei.jar -input <file>            run every command in a task file, then exit
              java -jar wealthsei.jar gui                      open the window

            Options (one or two dashes):
              -input <file>      task file: one command per line; blank lines and lines starting with # are skipped
              -LLMAPI            use the Claude API; stop with an error if no API key is configured
              -no-ai             do not call the API even if a key is configured (rules and templates only)
              -data-dir <dir>    folder for saved data (default: WEALTHSEI_HOME, else ~/.wealthsei)
              -help              show this text

            The API key is read from the .env file in the current folder or the ANTHROPIC_API_KEY variable.""";

    private final Path input;
    private final Path dataDir;
    private final boolean requireAi;
    private final boolean noAi;
    private final boolean help;
    private final List<String> command;

    private CliOptions(Path input, Path dataDir, boolean requireAi, boolean noAi, boolean help, List<String> command) {
        this.input = input;
        this.dataDir = dataDir;
        this.requireAi = requireAi;
        this.noAi = noAi;
        this.help = help;
        this.command = List.copyOf(command);
    }

    /** @throws IllegalArgumentException if an option is unknown, lacks its value, or options conflict */
    static CliOptions parse(String[] args) {
        Path input = null;
        Path dataDir = null;
        boolean requireAi = false;
        boolean noAi = false;
        boolean help = false;
        List<String> command = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (command.isEmpty() && arg.startsWith("-") && arg.length() > 1) {
                String name = arg.replaceFirst("^--?", "").toLowerCase();
                switch (name) {
                    case "input" -> {
                        input = Path.of(value(args, ++i, arg));
                    }
                    case "data-dir" -> {
                        dataDir = Path.of(value(args, ++i, arg));
                    }
                    case "llmapi" -> requireAi = true;
                    case "no-ai" -> noAi = true;
                    case "help", "h" -> help = true;
                    default -> throw new IllegalArgumentException("Unknown option '" + arg + "'. Use -help to see the options.");
                }
            } else {
                command.add(arg);
            }
            i++;
        }
        if (requireAi && noAi) {
            throw new IllegalArgumentException("-LLMAPI and -no-ai cannot be used together.");
        }
        if (input != null && !command.isEmpty()) {
            throw new IllegalArgumentException("Use either -input <file> or a command, not both.");
        }
        return new CliOptions(input, dataDir, requireAi, noAi, help, command);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].isBlank()) {
            throw new IllegalArgumentException("Option " + option + " needs a value.");
        }
        return args[index];
    }

    /** The task file given with {@code -input}, or {@code null}. */
    Path input() {
        return input;
    }

    /** The data folder given with {@code -data-dir}, or {@code null} to use the default. */
    Path dataDir() {
        return dataDir;
    }

    boolean requireAi() {
        return requireAi;
    }

    boolean noAi() {
        return noAi;
    }

    boolean help() {
        return help;
    }

    /** The words of the command to run once, or an empty list. */
    List<String> command() {
        return command;
    }
}
