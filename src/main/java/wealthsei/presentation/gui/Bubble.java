package wealthsei.presentation.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/** One chat message in a rounded bubble: orange on the right for you, dark on the left for the assistant. */
@SuppressWarnings({"serial", "this-escape"})
public class Bubble extends JComponent {

    private static final java.awt.font.FontRenderContext CONTEXT = new java.awt.font.FontRenderContext(null, true, true);
    private static final int PAD_X = 16;
    private static final int PAD_Y = 12;

    private final boolean fromUser;
    private final String text;
    private final String footnote;
    private final int maxTextWidth;
    private final List<TextLayout> lines = new ArrayList<>();
    private final Font font = Theme.regular(13.5f);
    private int textWidth;
    private int textHeight;

    /** @param maxWidth the widest the bubble may be, including its padding; @param footnote small muted line below the text, or null */
    public Bubble(boolean fromUser, String text, String footnote, int maxWidth) {
        this.fromUser = fromUser;
        this.text = text;
        this.footnote = footnote;
        this.maxTextWidth = maxWidth - 2 * PAD_X;
        setOpaque(false);
        measure();
    }

    private void measure() {
        float y = 0;
        float widest = 0;
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                y += font.getSize2D() * 0.7f;
                continue;
            }
            AttributedString as = new AttributedString(para);
            as.addAttribute(TextAttribute.FONT, font);
            LineBreakMeasurer m = new LineBreakMeasurer(as.getIterator(), CONTEXT);
            while (m.getPosition() < para.length()) {
                TextLayout layout = m.nextLayout(maxTextWidth);
                lines.add(layout);
                widest = Math.max(widest, layout.getAdvance());
                y += layout.getAscent() + layout.getDescent() + layout.getLeading() + 4;
            }
        }
        textWidth = Math.round(widest);
        textHeight = Math.round(y);
    }

    @Override
    public Dimension getPreferredSize() {
        int foot = footnote == null ? 0 : 20;
        int fw = footnote == null ? 0 : (int) Math.ceil(Theme.regular(11f).getStringBounds(footnote, CONTEXT).getWidth());
        return new Dimension(Math.max(textWidth, Math.min(fw, maxTextWidth)) + 2 * PAD_X + 1, textHeight + 2 * PAD_Y + foot);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(fromUser ? Theme.p().accent() : Theme.p().surfaceRaised());
        g.fillRoundRect(0, 0, w, h, 20, 20);
        float y = PAD_Y;
        g.setColor(fromUser ? Theme.ON_ORANGE : Theme.p().text());
        for (TextLayout layout : lines) {
            y += layout.getAscent();
            layout.draw(g, PAD_X, y);
            y += layout.getDescent() + layout.getLeading() + 4;
        }
        if (footnote != null) {
            g.setFont(Theme.regular(11f));
            g.setColor(fromUser ? Theme.ON_ORANGE : Theme.p().muted());
            g.drawString(footnote, PAD_X, h - PAD_Y + 2);
        }
        g.dispose();
    }
}
