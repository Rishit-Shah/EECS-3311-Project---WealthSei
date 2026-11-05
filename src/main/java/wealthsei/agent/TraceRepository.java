package wealthsei.agent;

import java.util.Optional;

/** Where agent traces are kept. */
public interface TraceRepository {

    void save(AgentTrace trace);

    Optional<AgentTrace> load(String traceId);

    /** The trace saved most recently, if any. */
    Optional<AgentTrace> latest();
}
