package wealthsei.command;

/** A proposal was asked to do something its current state does not allow, e.g. approving one already applied. */
public class IllegalStateTransitionException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
