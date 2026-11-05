package wealthsei.llm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Talks to Claude through the Messages API (Adapter pattern): our {@link LLMRequest} and
 * {@link LLMResponse} are translated to and from the service's JSON, and the actual sending is
 * done by an {@link HttpTransport}, so tests can supply a fake one.
 */
public class ClaudeProvider implements LLMProvider {

    /** Longest reply asked for. */
    static final int MAX_TOKENS = 1024;

    private final HttpTransport transport;

    public ClaudeProvider(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        Objects.requireNonNull(request, "request");
        String reply = transport.post(Json.write(toBody(request)));
        return parse(reply);
    }

    // ---- our request to the service's JSON ----

    static Map<String, Object> toBody(LLMRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.model());
        body.put("max_tokens", MAX_TOKENS);
        if (!request.systemPrompt().isBlank()) {
            body.put("system", request.systemPrompt());
        }
        List<Object> messages = new ArrayList<>();
        List<Object> pendingResults = new ArrayList<>();
        for (Message m : request.messages()) {
            if (m.role() == Message.Role.TOOL_RESULT) {
                Map<String, Object> block = new LinkedHashMap<>();
                block.put("type", "tool_result");
                block.put("tool_use_id", m.toolCallId());
                block.put("content", m.text());
                pendingResults.add(block);
                continue;
            }
            flush(messages, pendingResults);
            messages.add(m.role() == Message.Role.USER ? userMessage(m) : assistantMessage(m));
        }
        flush(messages, pendingResults);
        body.put("messages", messages);
        if (!request.toolSpecs().isEmpty()) {
            List<Object> tools = new ArrayList<>();
            for (ToolSpec spec : request.toolSpecs()) {
                tools.add(toolJson(spec));
            }
            body.put("tools", tools);
            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("type", "auto");
            choice.put("disable_parallel_tool_use", Boolean.TRUE);
            body.put("tool_choice", choice);
        }
        return body;
    }

    private static void flush(List<Object> messages, List<Object> pendingResults) {
        if (!pendingResults.isEmpty()) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", "user");
            msg.put("content", new ArrayList<>(pendingResults));
            messages.add(msg);
            pendingResults.clear();
        }
    }

    private static Map<String, Object> userMessage(Message m) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "user");
        msg.put("content", m.text());
        return msg;
    }

    private static Map<String, Object> assistantMessage(Message m) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        List<Object> blocks = new ArrayList<>();
        if (!m.text().isBlank()) {
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("type", "text");
            text.put("text", m.text());
            blocks.add(text);
        }
        for (ToolCall call : m.toolCalls()) {
            Map<String, Object> use = new LinkedHashMap<>();
            use.put("type", "tool_use");
            use.put("id", call.id());
            use.put("name", call.toolName());
            Map<String, Object> input = new LinkedHashMap<>(call.args().asMap());
            use.put("input", input);
            blocks.add(use);
        }
        msg.put("content", blocks);
        return msg;
    }

    private static Map<String, Object> toolJson(ToolSpec spec) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<Object> required = new ArrayList<>();
        for (ParamSpec p : spec.params()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("type", p.type().jsonName());
            prop.put("description", p.description());
            properties.put(p.name(), prop);
            if (p.required()) {
                required.add(p.name());
            }
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", spec.name());
        tool.put("description", spec.description());
        tool.put("input_schema", schema);
        return tool;
    }

    // ---- the service's JSON to our response ----

    @SuppressWarnings("unchecked")
    static LLMResponse parse(String reply) {
        Map<String, Object> json;
        try {
            json = Json.parseObject(reply);
        } catch (IllegalArgumentException e) {
            throw new LLMException("The model service sent a reply that could not be read: " + e.getMessage(), false, e);
        }
        if ("error".equals(json.get("type"))) {
            Object err = json.get("error");
            String detail = err instanceof Map<?, ?> m && m.get("message") != null ? m.get("message").toString() : "unknown error";
            throw new LLMException("The model service reported an error: " + detail, false);
        }
        Object content = json.get("content");
        if (!(content instanceof List<?> blocks)) {
            throw new LLMException("The model service reply had no content", false);
        }
        StringBuilder text = new StringBuilder();
        List<ToolCall> calls = new ArrayList<>();
        for (Object block : blocks) {
            if (!(block instanceof Map<?, ?> b)) {
                continue;
            }
            Object type = b.get("type");
            if ("text".equals(type) && b.get("text") != null) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(b.get("text"));
            } else if ("tool_use".equals(type)) {
                Object input = b.get("input");
                Map<String, Object> inputMap = input instanceof Map<?, ?> in ? (Map<String, Object>) in : Map.of();
                calls.add(new ToolCall(String.valueOf(b.get("id")), String.valueOf(b.get("name")),
                        ToolArgs.fromJson(inputMap)));
            }
        }
        int tokens = 0;
        if (json.get("usage") instanceof Map<?, ?> usage) {
            tokens = number(usage.get("input_tokens")) + number(usage.get("output_tokens"));
        }
        return new LLMResponse(text.toString(), calls, tokens);
    }

    private static int number(Object value) {
        return value instanceof BigDecimal d ? d.intValue() : 0;
    }
}
