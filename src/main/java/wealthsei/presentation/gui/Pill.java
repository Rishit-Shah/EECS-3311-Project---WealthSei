package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A small rounded label such as "OVER" or "ON TRACK". */
@SuppressWarnings({"serial", "this-escape"})
public class Pill extends JComponent {

    private String text;
    private Color colour;

    public Pill(String text, Color colour) {
        this.text = text;
        this.colour = colour;
        setOpaque(false);
        setFont(Theme.bold(10.5f));
    }

    public void set(String newText, Color newColour) {
        this.text = newText;
        this.colour = newColour;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text) + 18, 22);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setColor(Theme.alpha(colour, 38));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.setColor(colour);
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2f,
                (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g.dispose();
    }
}
