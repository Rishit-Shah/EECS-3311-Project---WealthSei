package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

/** The big orange card: balance today in huge numbers, with a few figures along the bottom. */
@SuppressWarnings({"serial", "this-escape"})
public class HeroBalanceCard extends JComponent {

    /** A small figure with its label. */
    public record Stat(String label, String value) {
    }

    private final String label;
    private final String dollars;
    private final String cents;
    private final String chip;
    private final List<Stat> stats;
    private final String pill;
    private final String note;

    /** @param pill short text in the black pill at the bottom right, or null; @param note small line under the number, or null */
    public HeroBalanceCard(String label, String dollars, String cents, String chip, List<Stat> stats, String pill, String note) {
        this.label = label;
        this.dollars = dollars;
        this.cents = cents;
        this.chip = chip;
        this.stats = stats;
        this.pill = pill;
        this.note = note;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 270);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, 250);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        Color orange = Theme.p().accent();
        RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, Card.RADIUS, Card.RADIUS);
        g.setPaint(new GradientPaint(0, 0, Theme.mix(orange, Color.WHITE, 0.06f), w, h, Theme.mix(orange, new Color(0xFF7A1F), 0.4f)));
        g.fill(shape);
        // two big circles at the bottom right, drawn as thin rings
        java.awt.Shape oldClip = g.getClip();
        g.setClip(shape);
        g.setStroke(new java.awt.BasicStroke(1.2f));
        g.setColor(new Color(0, 0, 0, 38));
        g.draw(new Ellipse2D.Double(w * 0.72, h * 0.22, h * 1.15, h * 1.15));
        g.draw(new Ellipse2D.Double(w * 0.80, h * 0.50, h * 0.78, h * 0.78));
        g.setClip(oldClip);

        // chips
        Font chipFont = Theme.tracked(Theme.bold(10.5f), 0.1f);
        g.setFont(chipFont);
        String up = label.toUpperCase(java.util.Locale.ENGLISH);
        int cw = (int) Math.ceil(chipFont.getStringBounds(up, g.getFontRenderContext()).getWidth()) + 40;
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRoundRect(26, 24, cw, 30, 30, 30);
        g.setColor(Theme.ON_ORANGE);
        g.drawString(up, 26 + 30, 43);
        g.setStroke(new java.awt.BasicStroke(1.4f));
        g.drawRoundRect(26 + 10, 33, 12, 9, 3, 3);
        if (chip != null) {
            g.setFont(Theme.bold(11f));
            int lw = g.getFontMetrics().stringWidth(chip) + 34;
            g.setColor(new Color(255, 255, 255, 70));
            g.fillRoundRect(w - 26 - lw, 24, lw, 30, 30, 30);
            g.setColor(Theme.ON_ORANGE);
            g.drawString(chip, w - 26 - lw + 26, 43);
            g.setColor(new Color(0x1FAA6B));
            g.fillOval(w - 26 - lw + 11, 35, 8, 8);
        }

        // the number
        float size = Math.min(132f, Math.max(70f, h * 0.43f));
        Font big = Theme.tracked(Theme.bold(size), -0.06f);
        g.setFont(big);
        g.setColor(Theme.ON_ORANGE);
        FontMetrics fm = g.getFontMetrics();
        int baseline = 66 + fm.getAscent();
        int x = 26;
        g.setFont(Theme.bold(size * 0.3f));
        g.drawString("$", x, baseline - Math.round(size * 0.5f));
        int dw = g.getFontMetrics().stringWidth("$") + 6;
        g.setFont(big);
        g.drawString(dollars, x + dw, baseline);
        int numW = g.getFontMetrics().stringWidth(dollars);
        g.setFont(Theme.bold(size * 0.3f));
        g.drawString(cents, x + dw + numW + 4, baseline - Math.round(size * 0.5f));
        if (note != null) {
            g.setFont(Theme.regular(13f));
            g.setColor(new Color(26, 18, 8, 190));
            g.drawString(note, 28, h - 30);
        }

        // bottom row: stats on the right, as in the design
        int rx = w - 26;
        if (pill != null) {
            g.setFont(Theme.bold(11.5f));
            int pw = g.getFontMetrics().stringWidth(pill) + 28;
            g.setColor(new Color(0x15120D));
            g.fillRoundRect(rx - pw, h - 62, pw, 34, 34, 34);
            g.setColor(Color.WHITE);
            g.drawString(pill, rx - pw + 14, h - 40);
            rx -= pw + 22;
        }
        for (int i = stats.size() - 1; i >= 0; i--) {
            Stat s = stats.get(i);
            g.setFont(Theme.bold(19f));
            int vw = g.getFontMetrics().stringWidth(s.value());
            g.setFont(Theme.tracked(Theme.bold(9.5f), 0.08f));
            String lab = s.label().toUpperCase(java.util.Locale.ENGLISH);
            int lw = (int) Math.ceil(g.getFont().getStringBounds(lab, g.getFontRenderContext()).getWidth());
            int bw = Math.max(vw, lw);
            g.setColor(new Color(26, 18, 8, 170));
            g.drawString(lab, rx - bw, h - 58);
            g.setFont(Theme.bold(19f));
            g.setColor(Theme.ON_ORANGE);
            g.drawString(s.value(), rx - bw, h - 36);
            rx -= bw + 26;
        }
        g.dispose();
    }
}
