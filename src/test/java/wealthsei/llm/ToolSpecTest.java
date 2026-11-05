package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolSpecTest {

    private final ToolSpec spec = new ToolSpec("query", "Looks things up", List.of(
            new ParamSpec("month", ParamType.STRING, "YYYY-MM", true),
            new ParamSpec("limit", ParamType.INTEGER, "how many", false),
            new ParamSpec("min", ParamType.NUMBER, "smallest amount", false)));

    @Test
    void acceptsGoodArguments() {
        spec.validate(ToolArgs.of("month", "2026-10", "limit", "5", "min", "12.5"));
        spec.validate(ToolArgs.of("month", "2026-10"));
    }

    @Test
    void rejectsMissingBadAndUnknownArguments() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> spec.validate(ToolArgs.of())).getMessage().contains("month"));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(ToolArgs.of("month", "x", "limit", "2.5")));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(ToolArgs.of("month", "x", "min", "abc")));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(ToolArgs.of("month", "x", "color", "red")));
    }

    @Test
    void argsConvertValues() {
        ToolArgs args = ToolArgs.of("a", " text ", "n", "7", "d", "1.25");
        assertEquals("text", args.requireText("a"));
        assertEquals(7, args.integer("n").orElseThrow().intValue());
        assertEquals(new BigDecimal("1.25"), args.decimal("d").orElseThrow());
        assertTrue(args.text("missing").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> args.requireText("missing"));
        assertFalse(ToolArgs.of("a", "  ").has("a"));
    }

    @Test
    void argsFromJsonKeepNumbersPlain() {
        ToolArgs args = ToolArgs.fromJson(Json.parseObject("{\"price\": 600.50, \"n\": 3, \"s\": \"x\", \"skip\": null}"));
        assertEquals("600.50", args.requireText("price"));
        assertEquals("3", args.requireText("n"));
        assertFalse(args.has("skip"));
        assertEquals(Map.of("n", "3", "price", "600.50", "s", "x"), args.asMap());
    }

    @Test
    void ofNeedsPairs() {
        assertThrows(IllegalArgumentException.class, () -> ToolArgs.of("a"));
    }
}
