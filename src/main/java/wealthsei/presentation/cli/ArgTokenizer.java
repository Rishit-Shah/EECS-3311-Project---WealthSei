package wealthsei.presentation.cli;

import java.util.ArrayList;
import java.util.List;

/** Splits a command line into words. Double quotes keep spaces inside one word. */
final class ArgTokenizer {

    private ArgTokenizer() {
    }

    /** @throws IllegalArgumentException if a quote is never closed */
    static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean hasToken = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                hasToken = true;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (hasToken) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
            } else {
                current.append(c);
                hasToken = true;
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Missing closing quote");
        }
        if (hasToken) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
