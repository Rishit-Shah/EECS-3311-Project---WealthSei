package wealthsei.command;

/** An action that can be undone (Command pattern). */
public interface Command {

    void execute();

    void undo();

    /** Short text for the user, e.g. "Set Netflix to Subscriptions". */
    String describe();
}
