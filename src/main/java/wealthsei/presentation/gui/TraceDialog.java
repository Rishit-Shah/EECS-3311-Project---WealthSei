package wealthsei.presentation.gui;

import java.awt.Component;
import java.util.Optional;
import wealthsei.agent.AgentTrace;
import wealthsei.agent.TraceStep;
import wealthsei.facade.WealthSeiFacade;

/** Shows "how did you get this?": every tool the assistant called and what came back. */
public final class TraceDialog {

    private TraceDialog() {
    }

    /** Opens the trace with this id, or the latest one when the id is blank. */
    public static void show(Component parent, WealthSeiFacade facade, String traceId) {
        Optional<AgentTrace> found = traceId == null || traceId.isBlank() ? facade.latestTrace() : facade.getTrace(traceId);
        if (found.isEmpty()) {
            Dialogs.message(parent, "How was this worked out?", "Nothing to show yet. Ask a question first, and the steps taken will appear here.");
            return;
        }
        AgentTrace t = found.get();
        StringBuilder text = new StringBuilder();
        text.append("Question: ").append(t.question()).append("\n\n");
        for (TraceStep s : t.steps()) {
            text.append(s.index()).append(". ");
            if (s.kind().equals(TraceStep.TOOL_CALL)) {
                text.append("Looked up ").append(s.toolName());
                if (!s.argsJson().isBlank()) {
                    text.append(" ").append(s.argsJson());
                }
                text.append("\n   Got back: ").append(s.resultSummary());
            } else {
                text.append(label(s.kind())).append(": ").append(s.resultSummary());
            }
            text.append("\n\n");
        }
        Dialogs.message(parent, "How was this worked out?", text.toString().trim());
    }

    private static String label(String kind) {
        return switch (kind) {
            case TraceStep.ANSWER -> "Answer";
            case TraceStep.GROUNDING -> "Checked against the data";
            case TraceStep.GUARDRAIL -> "Guardrail";
            case TraceStep.ERROR -> "Problem";
            default -> kind;
        };
    }
}
