package wealthsei.agent;

import java.math.BigDecimal;
import java.util.Map;
import wealthsei.domain.Verdict;
import wealthsei.llm.Json;

/** Writes the affordability answer straight from the purchase figures; used when the model cannot be reached. */
final class AffordabilityFallback {

    private AffordabilityFallback() {
    }

    static Verdict suggested(Map<String, Object> impact) {
        return Verdict.parse(String.valueOf(impact.get("suggestedVerdict")));
    }

    static Verdict suggestedOrNull(Map<String, Object> impact) {
        return impact == null ? null : suggested(impact);
    }

    static Map<String, Object> parse(String dataJson) {
        return Json.parseObject(dataJson);
    }

    static String write(Map<String, Object> d) {
        String reason = String.valueOf(d.get("reason"));
        String sentence = Character.toUpperCase(reason.charAt(0)) + reason.substring(1);
        return sentence + ". Buying " + d.get("item") + " for " + money(d.get("price")) + " on " + d.get("date")
                + " would change your lowest balance in the next month from " + money(d.get("lowestBalanceWithout"))
                + " to " + money(d.get("lowestBalanceWith")) + " (your safety buffer is " + money(d.get("safetyBuffer"))
                + ") and your daily spending allowance from " + money(d.get("safeToSpendPerDayWithout")) + " to "
                + money(d.get("safeToSpendPerDayWith")) + ".";
    }

    /** Shows a figure as dollars, with the minus sign in front: -$2000.00. */
    private static String money(Object value) {
        if (value instanceof BigDecimal b) {
            return (b.signum() < 0 ? "-$" : "$") + b.abs().toPlainString();
        }
        return "$" + value;
    }
}
