package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** One line in a list: a coloured dot, a main text with a muted second line, and a value on the right. */
@SuppressWarnings({"serial", "this-escape"})
public class ListRow extends JComponent {

    private final Color dot;
    private final String main;
    private final String sub;
    private final String right;
    private final Color rightColour;

    public ListRow(Color dot, String main, String sub, String right, Color rightColour) {
        this.dot = dot;
        this.main = main;
        this.sub = sub;
        this.right = right;
        this.rightColour = rightColour;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, sub == null || sub.isEmpty() ? 38 : 50);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        boolean two = sub != null && !sub.isEmpty();
        int rightW = 0;
        if (right != null) {
            g.setFont(Theme.bold(13f));
            rightW = g.getFontMetrics().stringWidth(right);
            g.setColor(rightColour == null ? Theme.p().text() : rightColour);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(right, w - rightW, (h - fm.getHeight()) / 2f + fm.getAscent());
        }
        int textX = 0;
        if (dot != null) {
            g.setColor(Theme.alpha(dot, 45));
            g.fillRoundRect(0, h / 2 - 15, 30, 30, 10, 10);
            g.setColor(dot);
            g.fillOval(11, h / 2 - 4, 8, 8);
            textX = 42;
        }
        int avail = w - textX - rightW - 12;
        g.setFont(Theme.font(java.awt.Font.PLAIN, 13.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.p().text());
        int mainY = two ? h / 2 - 4 : (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(clip(main, fm, avail), textX, mainY);
        if (two) {
            g.setFont(Theme.regular(12f));
            fm = g.getFontMetrics();
            g.setColor(Theme.p().muted());
            g.drawString(clip(sub, fm, avail), textX, h / 2 + 13);
        }
        g.setColor(Theme.p().gridLine());
        g.drawLine(textX, h - 1, w, h - 1);
        g.dispose();
    }

    static String clip(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > width) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim() + "…";
    }
}
