package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;

/** The health score as a ring that fills clockwise, coloured by how healthy it is. */
@SuppressWarnings({"serial", "this-escape"})
public class ScoreRing extends JComponent {

    private final Integer score;

    /** @param score 0 to 100, or null when no score could be worked out */
    public ScoreRing(Integer score) {
        this.score = score;
        setOpaque(false);
    }

    public static Color colourFor(int score) {
        return score >= 70 ? Theme.p().good() : score >= 40 ? Theme.p().warn() : Theme.p().danger();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(84, 84);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int size = Math.min(getWidth(), getHeight()) - 6;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        float t = Math.max(8f, size * 0.12f);
        Arc2D.Double track = new Arc2D.Double(x + t / 2, y + t / 2, size - t, size - t, 0, 360, Arc2D.OPEN);
        g.setStroke(new BasicStroke(t));
        g.setColor(Theme.p().surfaceRaised());
        g.draw(track);
        String text = score == null ? "N/A" : String.valueOf(score);
        if (score != null) {
            g.setColor(colourFor(score));
            g.setStroke(new BasicStroke(t, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(x + t / 2, y + t / 2, size - t, size - t, 90, -3.6 * score, Arc2D.OPEN));
        }
        g.setColor(Theme.p().text());
        g.setFont(Theme.bold(size * 0.3f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, getWidth() / 2f - fm.stringWidth(text) / 2f, getHeight() / 2f + fm.getAscent() / 2f - 2);
        g.dispose();
    }
}
