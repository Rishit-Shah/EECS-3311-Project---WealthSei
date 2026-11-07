package wealthsei.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wealthsei.llm.Json;

/**
 * Checks that the figures in an answer came from tool results, so the model cannot invent or
 * miscalculate numbers. A figure is checked when it has a dollar sign, a percent sign, a
 * decimal point, follows the word "score", or is 100 or more (so "3 goals" or "30 days" are not checked, and neither are
 * dates, standalone years or the scale of a score such as "out of 100"). A whole-number figure may be a rounding of a tool figure
 * (within 0.5); a figure with decimals must match to the cent.
 */
public class GroundingChecker {

    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}(-\\d{2})?");
    /** The scale of a score ("out of 100", "/100") is not a figure the tools have to produce. */
    /** A whole number right after the word "score" is a figure too (a score of 55 must really be 55). */
    private static final Pattern SCORE_BEFORE = Pattern.compile("(?i)\\bscore(?:d)?\\D{0,25}$");
    private static final Pattern SCALE = Pattern.compile("(?i)\\bout of 100\\b|/\\s?100\\b");
    private static final Pattern NUMBER = Pattern.compile("(\\$)?(\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d+)?(%)?");
    private static final BigDecimal CENT = new BigDecimal("0.005");
    private static final BigDecimal HALF = new BigDecimal("0.5");

    /**
     * @param answer       what the model wrote
     * @param observations the results of the tools it called
     * @param allowed      other figures that are fine to repeat, such as the price the user typed
     */
    public GroundingReport verify(String answer, List<ToolResult> observations, List<BigDecimal> allowed) {
        Objects.requireNonNull(answer, "answer");
        List<BigDecimal> known = new ArrayList<>(allowed);
        for (ToolResult r : observations) {
            if (r.success()) {
                collect(parseQuietly(r.dataJson()), known);
            }
        }
        List<String> bad = new ArrayList<>();
        for (Figure f : figures(answer)) {
            if (!f.checked()) {
                continue;
            }
            BigDecimal tolerance = f.wholeNumber() ? HALF : CENT;
            boolean found = known.stream().anyMatch(k -> k.abs().subtract(f.value().abs()).abs().compareTo(tolerance) <= 0);
            if (!found) {
                bad.add(f.text());
            }
        }
        return bad.isEmpty() ? GroundingReport.ok() : new GroundingReport(false, bad);
    }

    /** The numbers written in a piece of text (used for the user's own words as well). */
    public static List<BigDecimal> numbersIn(String text) {
        List<BigDecimal> out = new ArrayList<>();
        for (Figure f : figures(text)) {
            out.add(f.value());
        }
        return out;
    }

    private record Figure(String text, BigDecimal value, boolean checked, boolean wholeNumber) { }

    private static List<Figure> figures(String text) {
        String cleaned = SCALE.matcher(DATE.matcher(text).replaceAll(" ")).replaceAll(" ");
        List<Figure> out = new ArrayList<>();
        Matcher m = NUMBER.matcher(cleaned);
        while (m.find()) {
            boolean dollar = m.group(1) != null;
            boolean decimal = m.group(3) != null;
            boolean percent = m.group(4) != null;
            String digits = m.group(2).replace(",", "") + (decimal ? m.group(3) : "");
            BigDecimal value = new BigDecimal(digits);
            boolean year = !dollar && !decimal && !percent && value.compareTo(new BigDecimal(1900)) >= 0
                    && value.compareTo(new BigDecimal(2100)) <= 0;
            boolean afterScore = SCORE_BEFORE.matcher(cleaned.substring(Math.max(0, m.start() - 30), m.start())).find();
            boolean checked = !year && (dollar || percent || decimal || afterScore || value.compareTo(new BigDecimal(100)) >= 0);
            out.add(new Figure(m.group().trim(), value, checked, !decimal));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void collect(Object json, List<BigDecimal> known) {
        if (json instanceof BigDecimal d) {
            known.add(d);
        } else if (json instanceof String s) {
            known.addAll(numbersIn(s));
        } else if (json instanceof Map<?, ?> map) {
            map.values().forEach(v -> collect(v, known));
        } else if (json instanceof List<?> list) {
            list.forEach(v -> collect(v, known));
        }
    }

    private static Object parseQuietly(String json) {
        try {
            return Json.parse(json);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Rounds to two decimals; exposed so tests and tools agree on how cents are shown. */
    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
