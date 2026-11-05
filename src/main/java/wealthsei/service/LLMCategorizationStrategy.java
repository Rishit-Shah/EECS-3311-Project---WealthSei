package wealthsei.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMProvider;
import wealthsei.llm.LLMRequest;

/**
 * Last resort in the categorization chain: asks the model to place a merchant the keyword rules
 * and learned rules do not know (Strategy pattern, same interface as the other strategies).
 *
 * <p>Privacy and cost: only the merchant name is sent (never amounts, dates or account text).
 * Each distinct merchant is asked about once per run. The model may only answer with one of the
 * listed category ids or "unknown"; any other reply is ignored, so a confused model can never
 * invent a category. After two failures in a row it stops asking for the rest of the run, and
 * the transaction simply stays Uncategorized for manual review.
 */
public class LLMCategorizationStrategy implements CategorizationStrategy {

    static final int MAX_CONSECUTIVE_FAILURES = 2;

    private final LLMProvider provider;
    private final String model;
    private final List<Category> choices;
    private final Map<String, Optional<Category>> answered = new HashMap<>();
    private int consecutiveFailures;

    public LLMCategorizationStrategy(LLMProvider provider, String model) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.model = Objects.requireNonNull(model, "model");
        this.choices = Categories.defaults().stream().filter(c -> !c.equals(Categories.INCOME)).toList();
    }

    @Override
    public Optional<Category> categorize(Transaction tx) {
        if (tx.income() || consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            return Optional.empty();
        }
        String merchant = tx.merchant().trim();
        if (merchant.isEmpty()) {
            return Optional.empty();
        }
        String key = merchant.toLowerCase(Locale.ROOT);
        if (answered.containsKey(key)) {
            return answered.get(key);
        }
        try {
            String reply = provider.complete(LLMRequest.simple(systemPrompt(), "Merchant: " + merchant, model)).text();
            consecutiveFailures = 0;
            Optional<Category> category = parse(reply);
            answered.put(key, category);
            return category;
        } catch (LLMException e) {
            consecutiveFailures++;
            return Optional.empty();
        }
    }

    /** Accepts only an exact category id (ignoring case, quotes and a trailing full stop). */
    Optional<Category> parse(String reply) {
        String id = reply.trim().toLowerCase(Locale.ROOT).replaceAll("^[\"'`]+|[\"'`.]+$", "");
        return choices.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    private String systemPrompt() {
        StringBuilder sb = new StringBuilder("You sort a purchase into a spending category from the merchant name alone. ")
                .append("Reply with exactly one category id from this list, or the word unknown if you are not fairly sure. ")
                .append("Reply with nothing else.\n");
        for (Category c : choices) {
            sb.append("- ").append(c.id()).append(": ").append(c.name()).append('\n');
        }
        return sb.toString();
    }
}
