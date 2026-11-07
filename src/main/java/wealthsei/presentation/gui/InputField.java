package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** A small label above a rounded text box. */
@SuppressWarnings({"serial", "this-escape"})
public class InputField extends JPanel {

    private final JTextField input = new JTextField();
    private final String hint;

    public InputField(String label, String hint) {
        super(new BorderLayout(0, 6));
        this.hint = hint;
        setOpaque(false);
        setAlignmentX(0f);
        add(new Label(label, Theme.bold(11.5f), Theme.p().muted()), BorderLayout.NORTH);
        input.setOpaque(false);
        input.setFont(Theme.regular(14f));
        input.setForeground(Theme.p().text());
        input.setCaretColor(Theme.p().accent());
        input.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.quality(g);
                g.setColor(Theme.p().surfaceRaised());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.setColor(input.hasFocus() ? Theme.p().accent() : Theme.p().border());
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                if (input.getText().isEmpty() && !InputField.this.hint.isEmpty()) {
                    g.setFont(input.getFont());
                    g.setColor(Theme.alpha(Theme.p().muted(), 170));
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    g.drawString(InputField.this.hint, 16, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                }
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(200, 44));
        box.add(input, BorderLayout.CENTER);
        input.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                box.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                box.repaint();
            }
        });
        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                box.repaint();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                box.repaint();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                box.repaint();
            }
        });
        add(box, BorderLayout.CENTER);
    }

    public String text() {
        return input.getText().trim();
    }

    public void setText(String text) {
        input.setText(text);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
