package wealthsei.presentation.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import javax.swing.JComponent;

/** The big headline at the top of each screen: a small label, the headline in two lines, and a short paragraph on the right. */
@SuppressWarnings({"serial", "this-escape"})
public class HeroHeader extends JComponent {

    private final String eyebrow;
    private final String line1;
    private final String line2;
    private final String paragraph;

    public HeroHeader(String eyebrow, String line1, String line2, String paragraph) {
        this.eyebrow = eyebrow;
        this.line1 = line1;
        this.line2 = line2;
        this.paragraph = paragraph;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(900, eyebrow == null ? 196 : 218);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int top = 0;
        if (eyebrow != null) {
            g.setFont(Theme.tracked(Theme.bold(10.5f), 0.16f));
            String up = eyebrow.toUpperCase(java.util.Locale.ENGLISH);
            int ew = (int) Math.ceil(g.getFont().getStringBounds(up, g.getFontRenderContext()).getWidth());
            g.setColor(Theme.alpha(Theme.pageMuted(), 150));
            g.fillRect(0, 22, ew, 1);
            g.setColor(Theme.pageMuted());
            g.drawString(up, 0, 38);
        } else {
            top = -22;
        }

        float size = Math.min(78f, Math.max(48f, w * 0.068f));
        Font big = Theme.tracked(Theme.bold(size), -0.055f);
        g.setFont(big);
        g.setColor(Theme.pageText());
        FontMetrics fm = g.getFontMetrics();
        int y = 60 + top + fm.getAscent() - 8;
        g.drawString(line1, 0, y);
        g.drawString(line2, 0, y + Math.round(size * 0.9f));

        if (paragraph != null && !paragraph.isEmpty()) {
            int pw = Math.min(380, w / 3);
            int px = w - pw;
            Font f = Theme.regular(14f);
            AttributedString as = new AttributedString(paragraph);
            as.addAttribute(TextAttribute.FONT, f);
            LineBreakMeasurer m = new LineBreakMeasurer(as.getIterator(), g.getFontRenderContext());
            g.setColor(Theme.pageMuted());
            float py = getHeight() - 70;
            while (m.getPosition() < paragraph.length()) {
                TextLayout layout = m.nextLayout(pw);
                py += layout.getAscent();
                layout.draw(g, px, py);
                py += layout.getDescent() + layout.getLeading() + 5;
            }
        }
        g.dispose();
    }
}
