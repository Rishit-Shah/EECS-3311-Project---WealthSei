package wealthsei.agent;

import java.util.List;
import wealthsei.llm.LLMRequest;
import wealthsei.llm.Message;

/** Builds the instructions and conversation sent to the model. */
public class PromptBuilder {

    private final String model;

    public PromptBuilder(String model) {
        this.model = model;
    }

    public LLMRequest build(AgentContext ctx) {
        return new LLMRequest(systemPrompt(ctx), ctx.history(), ctx.toolSpecs(), model);
    }

    /** Asks the model to redo an unusable reply. */
    public LLMRequest buildRepairPrompt(AgentContext ctx, String rawText) {
        List<Message> messages = new java.util.ArrayList<>(ctx.history());
        messages.add(Message.assistant(rawText.isBlank() ? "(empty reply)" : rawText, List.of()));
        messages.add(Message.user("That reply was empty or unusable. Either call one of the tools or give your final answer in plain text."));
        return new LLMRequest(systemPrompt(ctx), messages, ctx.toolSpecs(), model);
    }

    String systemPrompt(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are WealthSei's finance assistant. You help one person understand their own money. Today is ")
                .append(ctx.today()).append(".\n\nRules:\n")
                .append("1. Every number you state must come from a tool result. Never calculate, estimate or invent figures. ")
                .append("If you need a number you do not have, call a tool. Do not add up or subtract tool figures yourself.\n")
                .append("2. Call one tool at a time. When you have enough information, answer.\n")
                .append("3. If a tool reports an error, say which information was unavailable and qualify your answer.\n")
                .append("4. Be brief: at most six sentences, plain language, no headings or bullet lists.\n")
                .append("5. You are not a financial adviser. Describe the situation and the options; do not give orders.\n");
        if (ctx.task().type() == TaskType.AFFORDABILITY) {
            sb.append("\nThe user is asking whether they can afford an item. The first tool result (purchase_impact) already ")
                    .append("holds the key figures, including a suggestedVerdict from fixed rules. Use the budget_status, goals and ")
                    .append("transactions tools when they help (for example the budget category for this kind of item, or goals the ")
                    .append("purchase could delay).\n")
                    .append("Start your answer with exactly one line: VERDICT: Yes, VERDICT: Yes-but, VERDICT: Wait or VERDICT: No. ")
                    .append("Never give a more optimistic verdict than suggestedVerdict. Then explain with figures from the tools.\n");
        }
        if (ctx.task().type() == TaskType.WHAT_IF) {
            sb.append("\nThe user describes a what-if. Turn it into ONE call of the scenario tool with structured changes ")
                    .append("(cancel, caps, income_increase, one_time_expenses). Use budget_status, goals or transactions first only if you ")
                    .append("need to check that a name exists. If the request is too vague to turn into changes (for example 'cut my ")
                    .append("expenses'), do not call the tool: ask ONE short clarifying question. If it has more than 5 changes, ask the ")
                    .append("user to split it. If the tool says something was not found or a value is invalid, tell the user what to fix. ")
                    .append("Then explain the comparison: the change in end balance, lowest balance, monthly savings and goal dates, ")
                    .append("using only the tool's figures.\n");
        }
        return sb.toString();
    }
}
