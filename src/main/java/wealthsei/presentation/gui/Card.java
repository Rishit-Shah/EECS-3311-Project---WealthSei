package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** A dark rounded card with an optional small label, a title and a faint number, like the design. */
@SuppressWarnings({"serial", "this-escape"})
public class Card extends JPanel {

    static final int RADIUS = 28;

    private final JPanel body = new JPanel(new BorderLayout());
    private int minHeight = 40;

    public Card() {
        this(null, null, null);
    }

    /** A card with a title and a small label above it on the left. */
    public Card(String title, String eyebrow) {
        this(eyebrow, title, null);
    }

    /** @param eyebrow small label (may be null); @param title heading (may be null); @param number faint number such as "01" (may be null) */
    public Card(String eyebrow, String title, String number) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        body.setOpaque(false);
        if (title != null || eyebrow != null) {
            super.add(new Header(eyebrow, title, number), BorderLayout.NORTH);
        }
        super.add(body, BorderLayout.CENTER);
    }

    /** The area under the heading where the card's content goes. */
    public JPanel body() {
        return body;
    }

    /** The card never gets squeezed below this height, even in a small window (the page scrolls instead). */
    public Card minHeight(int height) {
        this.minHeight = height;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, RADIUS, RADIUS);
        g.setColor(Theme.p().surface());
        g.fill(shape);
        g.setColor(Theme.p().border());
        g.draw(shape);
        g.dispose();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(60, minHeight);
    }

    /** Small label with a line over it, the title, and the faint number. */
    private static final class Header extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String eyebrow;
        private final String title;
        private final String number;

        Header(String eyebrow, String title, String number) {
            this.eyebrow = eyebrow;
            this.title = title;
            this.number = number;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, (eyebrow != null ? 22 : 0) + (title != null ? 34 : 0) + 14);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            int y = 0;
            if (eyebrow != null) {
                g.setFont(Theme.tracked(Theme.bold(10.5f), 0.14f));
                java.awt.FontMetrics fm = g.getFontMetrics();
                int w = (int) Math.ceil(g.getFont().getStringBounds(eyebrow.toUpperCase(java.util.Locale.ENGLISH),
                        g.getFontRenderContext()).getWidth());
                g.setColor(Theme.alpha(Theme.p().muted(), 120));
                g.fillRect(0, 0, w, 1);
                g.setColor(Theme.p().muted());
                g.drawString(eyebrow.toUpperCase(java.util.Locale.ENGLISH), 0, 5 + fm.getAscent());
                y = 22;
            }
            if (title != null) {
                g.setFont(Theme.tracked(Theme.font(java.awt.Font.PLAIN, 21f), -0.02f));
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.setColor(Theme.p().text());
                g.drawString(title, 0, y + fm.getAscent() + 2);
            }
            if (number != null) {
                g.setFont(Theme.bold(30f));
                g.setColor(Theme.alpha(Theme.p().muted(), 55));
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.drawString(number, getWidth() - fm.stringWidth(number), fm.getAscent() - 4);
            }
            g.dispose();
        }
    }
}
