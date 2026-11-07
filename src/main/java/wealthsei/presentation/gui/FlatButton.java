package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** A rounded button: filled in the accent colour when primary, outlined otherwise. */
@SuppressWarnings({"serial", "this-escape"})
public class FlatButton extends JComponent {

    private final String text;
    private final Icons.Kind icon;
    private final boolean primary;
    private boolean over;
    private boolean onPage;
    private final Runnable action;

    public FlatButton(String text, Icons.Kind icon, boolean primary, Runnable action) {
        this.text = text;
        this.icon = icon;
        this.primary = primary;
        this.action = action;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.bold(13f));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                over = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                over = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
    }

    /** Draws the button for the page background (outside any card) instead of for a card. */
    public FlatButton onPage() {
        this.onPage = true;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = (text.isEmpty() ? 0 : fm.stringWidth(text)) + (icon != null ? 20 : 0) + 30;
        return new Dimension(Math.max(w, text.isEmpty() ? 40 : 60), 40);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        Color fg;
        if (primary) {
            g.setColor(over ? Theme.mix(Theme.p().accent(), Color.WHITE, 0.15) : Theme.p().accent());
            g.fillRoundRect(0, 0, w, h, h, h);
            fg = Theme.ON_ORANGE;
        } else {
            Color base = onPage ? Theme.pageButton() : Theme.p().surfaceRaised();
            g.setColor(over ? Theme.mix(base, onPage ? Theme.pageMuted() : Color.WHITE, 0.12f) : base);
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(onPage ? Theme.pageButtonBorder() : Theme.p().border());
            g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            fg = onPage ? Theme.pageText() : Theme.p().text();
        }
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int tw = text.isEmpty() ? 0 : fm.stringWidth(text);
        int iw = icon != null ? 18 : 0;
        int gap = icon != null && !text.isEmpty() ? 8 : 0;
        int x = (w - tw - iw - gap) / 2;
        if (icon != null) {
            Icons.draw(g, icon, x, (h - 18) / 2.0, 18, fg);
            x += iw + gap;
        }
        g.setColor(fg);
        g.drawString(text, x, (h - fm.getHeight()) / 2f + fm.getAscent());
        g.dispose();
    }
}
