package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import javax.swing.JComponent;
import javax.swing.Timer;

/** A short message that slides in at the top right and goes away by itself. */
@SuppressWarnings({"serial", "this-escape"})
public class Toast extends JComponent {

    /** How serious the message is; decides the colour of the dot. */
    public enum Kind { INFO, SUCCESS, WARNING, ERROR }

    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);

    private final String message;
    private final Kind kind;

    public Toast(String message, Kind kind) {
        this.message = message;
        this.kind = kind;
        setOpaque(false);
        Timer timer = new Timer(4200, e -> {
            java.awt.Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
                parent.revalidate();
                parent.repaint();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        double w = Theme.regular(13.5f).getStringBounds(message, CONTEXT).getWidth();
        return new Dimension((int) Math.min(520, Math.ceil(w) + 62), 48);
    }

    Color colour() {
        return switch (kind) {
            case SUCCESS -> Theme.p().good();
            case WARNING -> Theme.p().warn();
            case ERROR -> Theme.p().danger();
            default -> Theme.p().accent();
        };
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        for (int i = 8; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 9));
            g.fillRoundRect(-i + 8, -i + 12, w + 2 * i - 16, h + 2 * i - 14, h, h);
        }
        g.setColor(Theme.p().surfaceRaised());
        g.fillRoundRect(8, 4, w - 16, h - 12, h, h);
        g.setColor(Theme.p().border());
        g.drawRoundRect(8, 4, w - 17, h - 13, h, h);
        g.setColor(colour());
        g.fillOval(24, h / 2 - 5, 9, 9);
        g.setFont(Theme.regular(13.5f));
        g.setColor(Theme.p().text());
        g.drawString(ListRow.clip(message, g.getFontMetrics(), w - 66), 42, h / 2 + 4);
        g.dispose();
    }
}
