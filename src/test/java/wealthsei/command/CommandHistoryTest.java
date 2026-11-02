package wealthsei.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandHistoryTest {

    private final List<String> log = new ArrayList<>();

    private Command cmd(String name) {
        return new Command() {
            @Override public void execute() { log.add("do " + name); }
            @Override public void undo() { log.add("undo " + name); }
            @Override public String description() { return name; }
        };
    }

    @Test
    void undoReversesInLastInFirstOutOrder() {
        CommandHistory<Command> h = new CommandHistory<>();
        h.run(cmd("a"));
        h.run(cmd("b"));
        h.undo();
        h.undo();
        assertEquals(List.of("do a", "do b", "undo b", "undo a"), log);
        assertFalse(h.canUndo());
    }

    @Test
    void redoRunsTheUndoneCommandAgain() {
        CommandHistory<Command> h = new CommandHistory<>();
        h.run(cmd("a"));
        h.undo();
        assertTrue(h.canRedo());
        assertEquals("a", h.redo().orElseThrow().description());
        assertEquals(List.of("do a", "undo a", "do a"), log);
    }

    @Test
    void aNewCommandClearsRedo() {
        CommandHistory<Command> h = new CommandHistory<>();
        h.run(cmd("a"));
        h.undo();
        h.run(cmd("b"));
        assertFalse(h.canRedo());
        assertTrue(h.redo().isEmpty());
    }

    @Test
    void undoOnEmptyHistoryDoesNothing() {
        assertTrue(new CommandHistory<Command>().undo().isEmpty());
    }

    @Test
    void aCommandThatFailsIsNotRecorded() {
        CommandHistory<Command> h = new CommandHistory<>();
        Command bad = new Command() {
            @Override public void execute() { throw new IllegalStateException("boom"); }
            @Override public void undo() { }
            @Override public String description() { return "bad"; }
        };
        assertThrows(IllegalStateException.class, () -> h.run(bad));
        assertFalse(h.canUndo());
    }
}
