package wealthsei.agent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import wealthsei.command.ProposalType;
import wealthsei.llm.ToolArgs;

/**
 * Turns the changes of a what-if into suggestions the user can approve: a cap becomes a budget
 * limit for this month, and a payment to cancel becomes a flag for review (the app cannot cancel
 * anything for the user). Changes with no matching action, such as extra income, are left out.
 */
public final class ScenarioDrafts {

    private ScenarioDrafts() {
    }

    public static List<ProposalDraft> from(ToolArgs args, LocalDate today) {
        List<ProposalDraft> drafts = new ArrayList<>();
        YearMonth month = YearMonth.from(today);
        for (String merchant : split(args, "cancel")) {
            drafts.add(new ProposalDraft(ProposalType.FLAG_SUBSCRIPTION, "Flag " + merchant + " for review",
                    "You tried cancelling " + merchant + " in a what-if. Flagging keeps it on your review list.",
                    ParamSet.of("merchant", merchant)));
        }
        for (String pair : split(args, "caps")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String category = pair.substring(0, eq).trim().toLowerCase();
                String limit = pair.substring(eq + 1).trim();
                String name = wealthsei.domain.Categories.find(category).map(wealthsei.domain.Category::name).orElse(category);
                drafts.add(new ProposalDraft(ProposalType.BUDGET_ADJUSTMENT,
                        "Set the " + name + " limit for " + month + " to " + limit,
                        "You tried capping " + name + " at " + limit + " a month in a what-if.",
                        ParamSet.of("month", month.toString(), "category", category, "limit", limit)));
            }
        }
        return drafts;
    }

    private static List<String> split(ToolArgs args, String name) {
        List<String> parts = new ArrayList<>();
        args.text(name).ifPresent(v -> {
            for (String part : v.split(",")) {
                if (!part.isBlank()) {
                    parts.add(part.trim());
                }
            }
        });
        return parts;
    }
}
