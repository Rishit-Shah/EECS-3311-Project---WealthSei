package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/** Text that wraps to the width it is given (never wider than {@code maxWidth}), drawn in the theme's fonts. */
@SuppressWarnings({"serial", "this-escape"})
public final class Text extends JComponent {

    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private final String text;
    private final Font font;
    private final Color colour;
    private final int maxWidth;
    private int knownWidth;

    private Text(String text, Color colour, float size, boolean bold, int maxWidth) {
        this.text = text;
        this.font = bold ? Theme.bold(size) : Theme.regular(size);
        this.colour = colour;
        this.maxWidth = maxWidth;
        setOpaque(false);
        setAlignmentX(0f);
    }

    /** Wrapped text; the width is the most it will take, it narrows to fit the space it is given. */
    public static Text wrapped(String text, Color colour, float size, boolean bold, int width) {
        return new Text(text, colour, size, bold, width);
    }

    private double measure(String s) {
        return font.getStringBounds(s, CONTEXT).getWidth();
    }

    private int lineHeight() {
        return Math.round(font.getSize2D() * 1.5f);
    }

    List<String> lines(int width) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String tryLine = line.isEmpty() ? word : line + " " + word;
                if (line.isEmpty() || measure(tryLine) <= width) {
                    line = new StringBuilder(tryLine);
                } else {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private int availableWidth() {
        int w = getWidth() > 0 ? getWidth() : knownWidth > 0 ? knownWidth : maxWidth;
        return Math.max(60, Math.min(maxWidth, w));
    }

    @Override
    public Dimension getPreferredSize() {
        int w = availableWidth();
        return new Dimension(w, lines(w).size() * lineHeight() + 2);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean changed = width != knownWidth;
        knownWidth = width;
        super.setBounds(x, y, width, height);
        if (changed) {
            revalidate();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.setFont(font);
        g.setColor(colour);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int lh = lineHeight();
        int y = (lh - fm.getHeight()) / 2 + fm.getAscent();
        for (String line : lines(availableWidth())) {
            g.drawString(line, 0, y);
            y += lh;
        }
        g.dispose();
    }
}
