package wealthsei.agent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import wealthsei.domain.Verdict;
import wealthsei.llm.LLMException;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolCall;

/**
 * Runs the agent loop: ask the planner for a step, run the tool it picks, repeat until it answers;
 * then check the answer's figures against the tool results, allow one correction, and save the trace.
 * The model never touches data directly; it can only ask for tools.
 *
 * <p>For an affordability question the controller first runs the purchase-impact tool itself, so the
 * rule-based verdict is always available as a floor: the model may be more cautious but never
 * more optimistic. If the model cannot be reached, the answer is written from those figures.
 */
public class AgentController {

    private static final String VERDICT_PREFIX = "VERDICT:";

    private final Planner planner;
    private final ToolManager tools;
    private final GroundingChecker grounding;
    private final TraceRepository traces;
    private final int maxSteps;
    private final Supplier<LocalDate> today;
    private final Supplier<String> ids;

    public AgentController(Planner planner, ToolManager tools, GroundingChecker grounding, TraceRepository traces,
                           int maxSteps, Supplier<LocalDate> today) {
        this(planner, tools, grounding, traces, maxSteps, today, () -> UUID.randomUUID().toString().substring(0, 8));
    }

    public AgentController(Planner planner, ToolManager tools, GroundingChecker grounding, TraceRepository traces,
                           int maxSteps, Supplier<LocalDate> today, Supplier<String> ids) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.grounding = Objects.requireNonNull(grounding, "grounding");
        this.traces = Objects.requireNonNull(traces, "traces");
        if (maxSteps < 1) {
            throw new IllegalArgumentException("maxSteps must be at least 1");
        }
        this.maxSteps = maxSteps;
        this.today = Objects.requireNonNull(today, "today");
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    public Optional<AgentTrace> getTrace(String traceId) {
        return traces.load(traceId);
    }

    public Optional<AgentTrace> latestTrace() {
        return traces.latest();
    }

    public AgentResult handle(AgentTask task) {
        Objects.requireNonNull(task, "task");
        AgentTrace trace = new AgentTrace(ids.get(), task.type(), task.userInput());
        AgentContext ctx = new AgentContext(task, today.get());
        ctx.setToolSpecs(tools.listSpecs());
        Map<String, Object> impact = null;
        if (task.type() == TaskType.AFFORDABILITY) {
            ToolResult seeded = seedAffordability(ctx, trace);
            if (!seeded.success()) {
                return finish(ctx, trace, "I cannot check that yet: " + seeded.error(), AgentStatus.TOOL_FAILURE, null, null);
            }
            impact = AffordabilityFallback.parse(seeded.dataJson());
        }
        try {
            return loop(ctx, trace, impact);
        } catch (LLMException e) {
            trace.addStep(TraceStep.ERROR, "", "", e.getMessage());
            String answer = impact != null ? AffordabilityFallback.write(impact)
                    : task.type() == TaskType.WHAT_IF
                    ? "I need the AI model to read a free-text what-if. You can give the changes directly instead, for example: "
                    + "whatif cancel=Spotify caps=dining=150 income=200 buy=600@2026-12-01"
                    : "I cannot answer that right now.";
            Verdict verdict = impact != null ? AffordabilityFallback.suggested(impact) : null;
            return finish(ctx, trace, answer + "\n(The AI explanation is unavailable: " + e.getMessage() + ")",
                    AgentStatus.MODEL_UNAVAILABLE, verdict, null);
        }
    }

    private AgentResult loop(AgentContext ctx, AgentTrace trace, Map<String, Object> impact) {
        AgentStep pending = null;
        int revisions = 0;
        for (int i = 0; i < maxSteps; i++) {
            AgentStep step = pending != null ? pending : planner.nextStep(ctx);
            pending = null;
            if (step instanceof ToolCallStep call) {
                ToolResult result = tools.execute(call.call());
                ctx.addObservation(call.call(), result);
                trace.addStep(TraceStep.TOOL_CALL, call.call().toolName(), call.call().args().toString(), result.summary());
                continue;
            }
            FinalAnswerStep done = (FinalAnswerStep) step;
            GroundingReport report = grounding.verify(done.answer(), ctx.results(), allowedFigures(ctx.task()));
            trace.addStep(TraceStep.GROUNDING, "", "", report.grounded() ? "All figures match tool results"
                    : "Not found in tool results: " + String.join(", ", report.ungroundedFigures()));
            if (report.grounded()) {
                return finish(ctx, trace, done.answer(), AgentStatus.COMPLETED, null, impact, report, done.drafts());
            }
            if (revisions < 1) {
                revisions++;
                ctx.addAssistantText(done.answer());
                pending = planner.revise(ctx, report);
                continue;
            }
            return finish(ctx, trace, done.answer() + "\n(Warning: these figures could not be verified: "
                    + String.join(", ", report.ungroundedFigures()) + ")", AgentStatus.UNGROUNDED, null, impact, report,
                    done.drafts());
        }
        String partial = impact != null ? AffordabilityFallback.write(impact)
                : "I ran out of steps before I could finish.";
        return finish(ctx, trace, "I ran out of steps before finishing, so this is only partial. " + partial,
                AgentStatus.STEP_LIMIT, AffordabilityFallback.suggestedOrNull(impact), null);
    }

    /** Runs the purchase-impact tool before the model is asked anything. */
    private ToolResult seedAffordability(AgentContext ctx, AgentTrace trace) {
        TaskParams p = ctx.task().params();
        List<String> pairs = new ArrayList<>();
        p.get("item").ifPresent(v -> {
            pairs.add("item");
            pairs.add(v);
        });
        p.get("price").ifPresent(v -> {
            pairs.add("price");
            pairs.add(v);
        });
        p.get("date").ifPresent(v -> {
            pairs.add("date");
            pairs.add(v);
        });
        ToolCall call = new ToolCall("seed_1", ForecastTool.NAME, ToolArgs.of(pairs.toArray(new String[0])));
        ToolResult result = tools.execute(call);
        ctx.addObservation(call, result);
        trace.addStep(TraceStep.TOOL_CALL, call.toolName(), call.args().toString(), result.summary());
        return result;
    }

    private static List<BigDecimal> allowedFigures(AgentTask task) {
        List<BigDecimal> allowed = new ArrayList<>(GroundingChecker.numbersIn(task.userInput()));
        task.params().values().values().forEach(v -> allowed.addAll(GroundingChecker.numbersIn(v)));
        return allowed;
    }

    private AgentResult finish(AgentContext ctx, AgentTrace trace, String answer, AgentStatus status, Verdict fallbackVerdict,
                               Map<String, Object> impact) {
        return finish(ctx, trace, answer, status, fallbackVerdict, impact, GroundingReport.ok(), List.of());
    }

    private AgentResult finish(AgentContext ctx, AgentTrace trace, String answer, AgentStatus status, Verdict fallbackVerdict,
                               Map<String, Object> impact, GroundingReport report, List<ProposalDraft> drafts) {
        String text = answer;
        Verdict verdict = fallbackVerdict;
        if (ctx.task().type() == TaskType.AFFORDABILITY && impact != null && status != AgentStatus.MODEL_UNAVAILABLE
                && status != AgentStatus.STEP_LIMIT) {
            Verdict suggested = AffordabilityFallback.suggested(impact);
            Verdict asked = readVerdict(text);
            text = stripVerdictLine(text);
            verdict = asked;
            if (suggested != null && (asked == null || asked.compareTo(suggested) < 0)) {
                verdict = suggested;
                String why = asked == null ? "The answer gave no verdict, so the budget rules' verdict is used: "
                        : "The answer was more optimistic than the budget rules, so their verdict is used: ";
                trace.addStep(TraceStep.GUARDRAIL, "", "", why + suggested.label());
                text = text + "\n(" + why.trim() + " " + suggested.label() + " - " + impact.get("reason") + ".)";
            }
        }
        if (status == AgentStatus.COMPLETED && ctx.results().stream().anyMatch(r -> !r.success())) {
            status = AgentStatus.TOOL_FAILURE;
        }
        trace.addStep(TraceStep.ANSWER, "", "", (verdict == null ? "" : verdict.label() + ": ") + text);
        traces.save(trace);
        return new AgentResult(text, drafts, status, report, trace.id(), verdict, ctx.tokensUsed());
    }

    private static Verdict readVerdict(String answer) {
        for (String line : answer.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith(VERDICT_PREFIX)) {
                return Verdict.parse(trimmed.substring(VERDICT_PREFIX.length()).replaceAll("[.*]", ""));
            }
        }
        return null;
    }

    private static String stripVerdictLine(String answer) {
        StringBuilder sb = new StringBuilder();
        for (String line : answer.split("\n")) {
            if (!line.trim().toUpperCase().startsWith(VERDICT_PREFIX)) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }
}
