package wealthsei.agent;

import java.util.regex.Pattern;

/**
 * Removes the markdown a model sometimes adds (bold asterisks, heading marks, back-ticks), because
 * the screens and the command line show answers as plain text.
 */
final class PlainText {

    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.+?)\\1");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s+");
    private static final Pattern STAR_BULLET = Pattern.compile("(?m)^(\\s*)\\*\\s+");

    private PlainText() {
    }

    static String of(String text) {
        String s = BOLD.matcher(text).replaceAll("$2");
        s = STAR_BULLET.matcher(s).replaceAll("$1- ");
        s = HEADING.matcher(s).replaceAll("");
        s = s.replace("**", "").replace("`", "");
        return s;
    }
}
