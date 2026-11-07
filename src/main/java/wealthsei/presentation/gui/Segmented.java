package wealthsei.presentation.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/** A row of pill-shaped choices where exactly one is selected. */
@SuppressWarnings({"serial", "this-escape"})
public class Segmented extends JComponent {

    private final String[] labels;
    private final IntConsumer onChange;
    private int selected;

    public Segmented(String[] labels, int selected, IntConsumer onChange) {
        this.labels = labels.clone();
        this.selected = selected;
        this.onChange = onChange;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0 && i != Segmented.this.selected) {
                    Segmented.this.selected = i;
                    repaint();
                    Segmented.this.onChange.accept(i);
                }
            }
        });
    }

    private int width(int i) {
        FontMetrics fm = getFontMetrics(Theme.bold(13f));
        return fm.stringWidth(labels[i]) + 40;
    }

    private int indexAt(int x) {
        int cursor = 6;
        for (int i = 0; i < labels.length; i++) {
            int w = width(i);
            if (x >= cursor && x < cursor + w) {
                return i;
            }
            cursor += w;
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        int w = 12;
        for (int i = 0; i < labels.length; i++) {
            w += width(i);
        }
        return new Dimension(w, 46);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int h = getHeight();
        g.setColor(Theme.p().surface());
        g.fillRoundRect(0, 0, getWidth(), h, h, h);
        g.setColor(Theme.p().border());
        g.drawRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
        int x = 6;
        for (int i = 0; i < labels.length; i++) {
            int w = width(i);
            if (i == selected) {
                g.setColor(Theme.p().accent());
                g.fillRoundRect(x, 6, w, h - 12, h - 12, h - 12);
            }
            g.setFont(Theme.bold(13f));
            g.setColor(i == selected ? Theme.ON_ORANGE : Theme.p().muted());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(labels[i], x + (w - fm.stringWidth(labels[i])) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            x += w;
        }
        g.dispose();
    }
}
