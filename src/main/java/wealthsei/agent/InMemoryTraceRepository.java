package wealthsei.agent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Keeps traces for the length of the session. */
public class InMemoryTraceRepository implements TraceRepository {

    private final Map<String, AgentTrace> byId = new LinkedHashMap<>();
    private String lastId;

    @Override
    public void save(AgentTrace trace) {
        Objects.requireNonNull(trace, "trace");
        byId.put(trace.id(), trace);
        lastId = trace.id();
    }

    @Override
    public Optional<AgentTrace> load(String traceId) {
        return Optional.ofNullable(byId.get(traceId));
    }

    @Override
    public Optional<AgentTrace> latest() {
        return Optional.ofNullable(lastId).map(byId::get);
    }
}
