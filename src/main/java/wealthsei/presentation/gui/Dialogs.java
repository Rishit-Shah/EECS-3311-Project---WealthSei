package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Small dark dialogs: a form with text fields, and a read-only message. */
public final class Dialogs {

    private Dialogs() {
    }

    /** Reads an amount such as "1,400" or "$1400.50". */
    public static wealthsei.domain.Money money(String text) {
        String clean = text.replace("$", "").replace(",", "").trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount");
        }
        try {
            return wealthsei.domain.Money.of(clean);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + text + "' is not an amount like 250 or 1400.50");
        }
    }

    /** Reads a date written as 2026-12-15. */
    public static java.time.LocalDate date(String text) {
        try {
            return java.time.LocalDate.parse(text.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + text + "' is not a date. Write it like 2026-12-15");
        }
    }

    /**
     * Shows a form and returns the typed values in order, or null when cancelled.
     *
     * @param defaults starting text for each field (same length as labels)
     */
    public static String[] form(Component parent, String title, String intro, String[] labels, String[] defaults, String okText) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, JDialog.DEFAULT_MODALITY_TYPE);
        JPanel root = new Surface();
        root.setLayout(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(26, 28, 22, 28));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        Label heading = new Label(title, Theme.bold(20f), Theme.p().text());
        heading.setAlignmentX(0f);
        top.add(heading);
        if (intro != null && !intro.isBlank()) {
            top.add(Box.createVerticalStrut(6));
            Label sub = new Label(intro, Theme.regular(13f), Theme.p().muted());
            sub.setAlignmentX(0f);
            top.add(sub);
        }
        root.add(top, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        JTextField[] inputs = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            Label l = new Label(labels[i], Theme.bold(11.5f), Theme.p().muted());
            l.setAlignmentX(0f);
            fields.add(l);
            fields.add(Box.createVerticalStrut(5));
            inputs[i] = new JTextField(defaults[i]);
            inputs[i].setFont(Theme.regular(14f));
            inputs[i].setForeground(Theme.p().text());
            inputs[i].setCaretColor(Theme.p().accent());
            inputs[i].setOpaque(false);
            inputs[i].setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            Field box = new Field(inputs[i]);
            box.setAlignmentX(0f);
            fields.add(box);
            fields.add(Box.createVerticalStrut(12));
        }
        root.add(fields, BorderLayout.CENTER);

        String[][] result = new String[1][];
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(new FlatButton("Cancel", null, false, dialog::dispose));
        buttons.add(new FlatButton(okText, null, true, () -> {
            String[] values = new String[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                values[i] = inputs[i].getText().trim();
            }
            result[0] = values;
            dialog.dispose();
        }));
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(null);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(Math.max(440, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        if (inputs.length > 0) {
            inputs[0].requestFocusInWindow();
        }
        dialog.setVisible(true);
        return result[0];
    }

    /** Shows a read-only message with an OK button. */
    public static void message(Component parent, String title, String text) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, JDialog.DEFAULT_MODALITY_TYPE);
        JPanel root = new Surface();
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(26, 28, 22, 28));
        root.add(new Label(title, Theme.bold(20f), Theme.p().text()), BorderLayout.NORTH);
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(Theme.regular(13.5f));
        area.setForeground(Theme.p().text());
        area.setBorder(BorderFactory.createEmptyBorder());
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(520, 260));
        ModernScrollBar.apply(scroll);
        root.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(new FlatButton("Close", null, true, dialog::dispose));
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    @SuppressWarnings({"serial", "this-escape"})
    private static final class Surface extends JPanel {
        Surface() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(Theme.p().surface());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    @SuppressWarnings({"serial", "this-escape"})
    private static final class Field extends JComponent {
        private final JTextField input;

        Field(JTextField input) {
            this.input = input;
            setLayout(new BorderLayout());
            add(input, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(360, 44);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 44);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setColor(Theme.p().surfaceRaised());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setColor(input.hasFocus() ? Theme.p().accent() : Theme.p().border());
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.dispose();
        }
    }
}
