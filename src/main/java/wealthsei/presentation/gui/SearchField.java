package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** A rounded text box with a hint shown while it is empty. */
@SuppressWarnings({"serial", "this-escape"})
public class SearchField extends JPanel {

    private final JTextField field;

    public SearchField(String hint, int width, Consumer<String> onChange) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 14));
        field = new JTextField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (getText().isEmpty()) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.quality(g);
                    g.setFont(getFont());
                    g.setColor(Theme.alpha(Theme.p().muted(), 200));
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    g.drawString(hint, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setFont(Theme.regular(13.5f));
        field.setForeground(Theme.p().text());
        field.setCaretColor(Theme.p().accent());
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.accept(field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.accept(field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.accept(field.getText());
            }
        });
        add(field, BorderLayout.CENTER);
        setPreferredSize(new Dimension(width, 40));
    }

    public String text() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setColor(Theme.p().surfaceRaised());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.setColor(field.hasFocus() ? Theme.p().accent() : Theme.p().border());
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g.dispose();
    }
}
