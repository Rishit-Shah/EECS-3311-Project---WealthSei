package wealthsei.agent;

import java.util.Objects;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMProvider;
import wealthsei.llm.LLMRequest;
import wealthsei.llm.LLMResponse;

/** Asks the model what to do next, one step at a time. Gives an unusable reply one chance to be repaired. */
public class Planner {

    private final PromptBuilder promptBuilder;
    private final ResponseParser parser;
    private final LLMProvider llm;

    public Planner(PromptBuilder promptBuilder, ResponseParser parser, LLMProvider llm) {
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.llm = Objects.requireNonNull(llm, "llm");
    }

    /** @throws LLMException if the model cannot be used, or still gives nothing usable after a repair request */
    public AgentStep nextStep(AgentContext ctx) {
        LLMResponse response = ask(ctx, promptBuilder.build(ctx));
        AgentStep step = parser.parse(response);
        if (step != null) {
            return step;
        }
        response = ask(ctx, promptBuilder.buildRepairPrompt(ctx, response.text()));
        step = parser.parse(response);
        if (step == null) {
            throw new LLMException("The model gave a reply that could not be used", false);
        }
        return step;
    }

    /** Tells the model its answer had figures no tool produced, and asks again. */
    public AgentStep revise(AgentContext ctx, GroundingReport report) {
        ctx.addUserMessage("Your answer contained figures that no tool result contains: "
                + String.join(", ", report.ungroundedFigures())
                + ". Rewrite it using only figures from the tool results (call a tool if you need one). "
                + "Keep the VERDICT line if you had one. Reply with only the corrected answer itself: "
                + "do not explain what you changed and do not mention this message.");
        return nextStep(ctx);
    }

    private LLMResponse ask(AgentContext ctx, LLMRequest request) {
        LLMResponse response = llm.complete(request);
        ctx.addTokens(response.tokensUsed());
        return response;
    }
}
