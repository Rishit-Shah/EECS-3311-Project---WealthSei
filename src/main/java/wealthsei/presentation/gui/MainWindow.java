package wealthsei.presentation.gui;

import java.awt.Dimension;
import java.time.LocalDate;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import wealthsei.facade.WealthSeiFacade;

/** The application window. All the content lives in a {@link ShellPanel}. */
@SuppressWarnings({"serial", "this-escape"})
public class MainWindow extends JFrame {

    private final ShellPanel shell;

    public MainWindow(WealthSeiFacade facade, Supplier<LocalDate> today, boolean aiOn, String dataFolder) {
        super("WealthSei");
        this.shell = new ShellPanel(facade, today, aiOn, dataFolder);
        setContentPane(shell);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1380, 900);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                shell.refreshIfDateChanged();
            }
        });
    }

    public ShellPanel shell() {
        return shell;
    }

    /** Opens the window on the Swing thread. */
    public static void launch(WealthSeiFacade facade, Supplier<LocalDate> today, boolean aiOn, String dataFolder) {
        System.setProperty("apple.awt.application.name", "WealthSei");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                // the default look is fine; every screen paints itself
            }
            new MainWindow(facade, today, aiOn, dataFolder).setVisible(true);
        });
    }
}
