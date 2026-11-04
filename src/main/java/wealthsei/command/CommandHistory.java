package wealthsei.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs commands and remembers them so they can be undone and redone. Running a new command
 * clears the redo list. If a command throws while running, it is not recorded.
 */
public class CommandHistory<C extends Command> {

    private final Deque<C> done = new ArrayDeque<>();
    private final Deque<C> undone = new ArrayDeque<>();

    public void execute(C command) {
        Objects.requireNonNull(command, "command");
        command.execute();
        done.push(command);
        undone.clear();
    }

    /** Undoes the most recent command, or returns empty when there is nothing to undo. */
    public Optional<C> undo() {
        if (done.isEmpty()) {
            return Optional.empty();
        }
        C command = done.pop();
        command.undo();
        undone.push(command);
        return Optional.of(command);
    }

    /** Re-runs the most recently undone command, or returns empty. */
    public Optional<C> redo() {
        if (undone.isEmpty()) {
            return Optional.empty();
        }
        C command = undone.pop();
        command.execute();
        done.push(command);
        return Optional.of(command);
    }

    public boolean canUndo() {
        return !done.isEmpty();
    }

    public boolean canRedo() {
        return !undone.isEmpty();
    }
}
