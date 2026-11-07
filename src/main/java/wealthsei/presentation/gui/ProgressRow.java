package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A labelled progress bar: "Dining out   $563 of $400" with a bar that turns amber then red. */
@SuppressWarnings({"serial", "this-escape"})
public class ProgressRow extends JComponent {

    private final String label;
    private final String right;
    private final double fraction;
    private final Color colour;
    private final Color dot;

    /** @param fraction 0 to 1 or more (above 1 the bar is full and red); @param dot colour of the small marker, or null */
    public ProgressRow(String label, String right, double fraction, Color colour, Color dot) {
        this.label = label;
        this.right = right;
        this.fraction = fraction;
        this.colour = colour;
        this.dot = dot;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 46);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 46);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int x = 0;
        g.setFont(Theme.font(java.awt.Font.PLAIN, 13f));
        FontMetrics fm = g.getFontMetrics();
        int baseline = fm.getAscent() + 1;
        if (dot != null) {
            g.setColor(dot);
            g.fillOval(0, baseline - 9, 8, 8);
            x = 16;
        }
        g.setFont(Theme.regular(12.5f));
        int rw = g.getFontMetrics().stringWidth(right);
        g.setColor(Theme.p().muted());
        g.drawString(right, w - rw, baseline);
        g.setFont(Theme.font(java.awt.Font.PLAIN, 13f));
        g.setColor(Theme.p().text());
        g.drawString(ListRow.clip(label, g.getFontMetrics(), w - x - rw - 12), x, baseline);
        int barY = 27;
        int barH = 8;
        g.setColor(Theme.p().surfaceRaised());
        g.fillRoundRect(0, barY, w, barH, barH, barH);
        int filled = (int) Math.round(w * Math.min(1.0, Math.max(0.0, fraction)));
        if (filled > 0) {
            g.setPaint(new java.awt.GradientPaint(0, 0, Theme.mix(colour, Color.WHITE, 0.12), w, 0, colour));
            g.fillRoundRect(0, barY, Math.max(filled, barH), barH, barH, barH);
        }
        g.dispose();
    }
}
