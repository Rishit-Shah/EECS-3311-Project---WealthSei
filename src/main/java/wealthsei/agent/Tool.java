package wealthsei.agent;

import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;

/** Something the agent can ask for: it looks at real data and returns figures. */
public interface Tool {

    ToolSpec spec();

    /** Runs with arguments that have already been validated against {@link #spec()}. */
    ToolResult run(ToolArgs args);
}
