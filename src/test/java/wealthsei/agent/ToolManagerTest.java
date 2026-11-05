package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolCall;
import wealthsei.llm.ToolSpec;

class ToolManagerTest {

    private static Tool tool(String name, boolean explode) {
        return new Tool() {
            @Override
            public ToolSpec spec() {
                return new ToolSpec(name, "desc", List.of(new ParamSpec("n", ParamType.INTEGER, "number", true)));
            }

            @Override
            public ToolResult run(ToolArgs args) {
                if (explode) {
                    throw new IllegalStateException("kaboom");
                }
                return ToolResult.ok(Map.of("n", args.integer("n").orElseThrow()));
            }
        };
    }

    @Test
    void registersListsAndRuns() {
        ToolManager manager = new ToolManager();
        manager.register(tool("a", false));
        manager.register(tool("b", false));
        assertEquals(List.of("a", "b"), manager.listSpecs().stream().map(ToolSpec::name).toList());
        assertTrue(manager.has("a"));
        ToolResult r = manager.execute(new ToolCall("1", "a", ToolArgs.of("n", "4")));
        assertTrue(r.success());
        assertEquals("{\"n\":4}", r.dataJson());
    }

    @Test
    void duplicateNamesAreRejected() {
        ToolManager manager = new ToolManager();
        manager.register(tool("a", false));
        assertThrows(IllegalArgumentException.class, () -> manager.register(tool("a", false)));
    }

    @Test
    void problemsComeBackAsFailedResultsNotExceptions() {
        ToolManager manager = new ToolManager();
        manager.register(tool("a", false));
        manager.register(tool("boom", true));
        ToolResult unknown = manager.execute(new ToolCall("1", "nope", ToolArgs.of()));
        assertFalse(unknown.success());
        assertTrue(unknown.error().contains("nope"));
        ToolResult missing = manager.execute(new ToolCall("2", "a", ToolArgs.of()));
        assertFalse(missing.success());
        assertTrue(missing.error().contains("'n'"));
        ToolResult bad = manager.execute(new ToolCall("3", "a", ToolArgs.of("n", "x")));
        assertFalse(bad.success());
        ToolResult exploded = manager.execute(new ToolCall("4", "boom", ToolArgs.of("n", "1")));
        assertFalse(exploded.success());
        assertTrue(exploded.error().contains("kaboom"));
    }

    @Test
    void resultsDescribeThemselvesForTheModelAndTheTrace() {
        assertEquals("{\"error\":\"No budget\"}", ToolResult.failure("No budget").forModel());
        assertTrue(ToolResult.failure("No budget").summary().startsWith("ERROR"));
        String longText = "x".repeat(500);
        assertTrue(new ToolResult(true, longText, "").summary().length() <= 300);
    }
}
