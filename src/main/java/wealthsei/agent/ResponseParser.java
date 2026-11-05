package wealthsei.agent;

import wealthsei.llm.LLMResponse;

/** Turns the model's reply into the next step. */
public class ResponseParser {

    /**
     * @return a tool call if one was asked for, the final answer if there is text, or null if the reply is empty
     */
    public AgentStep parse(LLMResponse response) {
        if (!response.toolCalls().isEmpty()) {
            return new ToolCallStep(response.toolCalls().get(0));
        }
        String text = response.text().trim();
        return text.isEmpty() ? null : new FinalAnswerStep(text);
    }
}
