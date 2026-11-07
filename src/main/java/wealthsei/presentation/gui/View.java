package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;

/** A screen in the main window. {@link #refresh()} rebuilds it from the latest data. */
@SuppressWarnings({"serial", "this-escape"})
public abstract class View extends JPanel {

    protected View() {
        super(new BorderLayout());
        setOpaque(false);
    }

    /** The name shown in the top bar. */
    public abstract String title();

    /** The short paragraph on the right of the big headline. */
    public abstract String subtitle();

    /** An optional small label above the headline; null (the default) shows none. */
    public String eyebrow() {
        return null;
    }

    /** The big headline, in two lines. */
    public String[] headline() {
        return new String[] {title(), ""};
    }

    /** Rebuilds the screen from the latest data. */
    public abstract void refresh();
}
