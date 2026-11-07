package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import javax.swing.JComponent;

/** A single line of text that is measured and drawn the same way, and ends in "…" when it does not fit. */
@SuppressWarnings({"serial", "this-escape"})
public class Label extends JComponent {

    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private String text;

    public Label(String text, Font font, Color colour) {
        this.text = text;
        setFont(font);
        setForeground(colour);
        setOpaque(false);
    }

    public void setText(String newText) {
        this.text = newText;
        revalidate();
        repaint();
    }

    public String getText() {
        return text;
    }

    private static double width(String s, Font f) {
        return f.getStringBounds(s, CONTEXT).getWidth();
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = getFont();
        return new Dimension((int) Math.ceil(width(text, f)) + 2, (int) Math.ceil(f.getSize2D() * 1.35));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(12, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setFont(getFont());
        g.setColor(getForeground());
        String shown = text;
        double avail = getWidth();
        if (width(shown, getFont()) > avail) {
            while (shown.length() > 1 && width(shown + "…", getFont()) > avail) {
                shown = shown.substring(0, shown.length() - 1);
            }
            shown = shown.trim() + "…";
        }
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(shown, 0, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
        g.dispose();
    }
}
