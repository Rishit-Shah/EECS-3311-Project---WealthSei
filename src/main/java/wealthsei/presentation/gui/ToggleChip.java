package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JComponent;

/** A rounded on/off chip with a coloured dot, such as a protected category. */
@SuppressWarnings({"serial", "this-escape"})
public class ToggleChip extends JComponent {

    private final String text;
    private final Color colour;
    private boolean on;

    public ToggleChip(String text, Color colour, boolean on, Consumer<Boolean> onChange) {
        this.text = text;
        this.colour = colour;
        this.on = on;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ToggleChip.this.on = !ToggleChip.this.on;
                repaint();
                onChange.accept(ToggleChip.this.on);
            }
        });
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.bold(12.5f));
        return new Dimension(fm.stringWidth(text) + 56, 34);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(on ? Theme.alpha(Theme.p().accent(), 46) : Theme.p().surfaceRaised());
        g.fillRoundRect(0, 0, w, h, h, h);
        g.setColor(on ? Theme.p().accent() : Theme.p().border());
        g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
        g.setColor(colour);
        g.fillOval(14, h / 2 - 4, 8, 8);
        g.setFont(Theme.bold(12.5f));
        g.setColor(on ? Theme.p().text() : Theme.p().muted());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, 30, (h - fm.getHeight()) / 2f + fm.getAscent());
        g.dispose();
    }
}
