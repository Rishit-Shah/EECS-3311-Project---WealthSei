package wealthsei.presentation.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** A thin rounded scroll bar that matches the theme. */
public final class ModernScrollBar extends BasicScrollBarUI {

    private ModernScrollBar() {
    }

    /** Gives the scroll pane thin, themed scroll bars. */
    public static void apply(JScrollPane pane) {
        for (JScrollBar bar : new JScrollBar[] {pane.getVerticalScrollBar(), pane.getHorizontalScrollBar()}) {
            bar.setUI(new ModernScrollBar());
            bar.setOpaque(false);
            bar.setPreferredSize(bar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 0) : new Dimension(0, 12));
        }
        pane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new javax.swing.JPanel() {
            private static final long serialVersionUID = 1L;

            {
                setOpaque(false);
            }
        });
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return empty();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return empty();
    }

    private static JButton empty() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // transparent track
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setColor(isThumbRollover() || isDragging ? Theme.alpha(Theme.p().muted(), 170) : Theme.alpha(Theme.p().muted(), 90));
        boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
        int pad = 3;
        if (vertical) {
            g.fillRoundRect(r.x + pad, r.y + 2, r.width - 2 * pad, r.height - 4, r.width, r.width);
        } else {
            g.fillRoundRect(r.x + 2, r.y + pad, r.width - 4, r.height - 2 * pad, r.height, r.height);
        }
        g.dispose();
    }
}
