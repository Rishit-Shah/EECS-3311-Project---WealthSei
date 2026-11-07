package wealthsei.presentation.gui;

import java.awt.Component;
import java.time.LocalDate;
import wealthsei.facade.WealthSeiFacade;

/** What every screen can ask of the window around it: the data, today's date, moving to another screen, and showing a message. */
public interface AppContext {

    int DASHBOARD = 0;
    int TRANSACTIONS = 1;
    int GOALS = 2;
    int PROPOSALS = 3;
    int ASSISTANT = 4;
    int REPORTS = 5;

    WealthSeiFacade facade();

    /** Today's date. Everything that depends on the date (days left, forecast window, current month) starts from here. */
    LocalDate today();

    /** True when an AI model is available (an API key was found). */
    boolean aiOn();

    void navigate(int screen);

    void toast(String message, Toast.Kind kind);

    /** Tells the window the data changed somewhere, so counters such as the proposals badge are updated. */
    void changed();

    /** A component to anchor dialogs on. */
    Component window();

    /** Asks for a bank CSV, imports it and refreshes every screen. */
    void importCsv();

    /** Opens the assistant popup in the corner and sends it a question. */
    void askInPopup(String question);
}
