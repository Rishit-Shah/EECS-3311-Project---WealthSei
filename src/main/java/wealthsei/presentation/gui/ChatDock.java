package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import wealthsei.facade.WealthSeiFacade;

/**
 * The assistant in the bottom-left corner: an orange "Ask WealthSei" button that opens a chat popup
 * above it. Only the button and the popup react to the mouse; everything else passes through.
 */
@SuppressWarnings({"serial", "this-escape"})
public class ChatDock extends JComponent {

    static final int POPUP_WIDTH = 396;
    private static final int MARGIN = 24;

    private final Launcher launcher = new Launcher();
    private final Popup popup;
    private final ChatPanel chat;
    private boolean open;

    public ChatDock(WealthSeiFacade facade, boolean aiOn) {
        setOpaque(false);
        setLayout(null);
        this.chat = new ChatPanel(facade, aiOn, 300);
        this.popup = new Popup(chat, aiOn);
        popup.setVisible(false);
        add(launcher);
        add(popup);
    }

    public ChatPanel chat() {
        return chat;
    }

    public void setOpen(boolean value) {
        open = value;
        popup.setVisible(open);
        launcher.repaint();
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        launcher.setBounds(MARGIN, h - MARGIN - 54, 188, 54);
        int ph = Math.min(580, h - 54 - MARGIN - 12 - 90);
        popup.setBounds(MARGIN, h - MARGIN - 54 - 12 - ph, Math.min(POPUP_WIDTH, w - 2 * MARGIN), ph);
    }

    @Override
    public boolean contains(int x, int y) {
        return launcher.getBounds().contains(x, y) || (popup.isVisible() && popup.getBounds().contains(x, y));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (!popup.isVisible()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        Rectangle r = popup.getBounds();
        for (int i = 14; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 7));
            g.fillRoundRect(r.x - i, r.y - i + 8, r.width + 2 * i, r.height + 2 * i, 28 + i, 28 + i);
        }
        g.dispose();
    }

    /** The orange pill in the corner. */
    private final class Launcher extends JComponent {
        private static final long serialVersionUID = 1L;
        private boolean over;

        Launcher() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setOpen(!open);
                }

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
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            int w = getWidth();
            int h = getHeight();
            for (int i = 8; i >= 1; i--) {
                g.setColor(new Color(0xFF, 0x8F, 0x3F, 6));
                g.fillRoundRect(-i + 8, -i + 12, w + 2 * i - 16, h + 2 * i - 14, h, h);
            }
            Color top = over ? Theme.mix(Theme.p().accent(), Color.WHITE, 0.18f) : Theme.p().accent();
            g.setPaint(new GradientPaint(0, 0, top, w, h, Theme.mix(Theme.p().accent(), new Color(0xFF7A1F), 0.5f)));
            g.fillRoundRect(8, 4, w - 16, h - 12, h, h);
            Icons.draw(g, open ? Icons.Kind.CLOSE : Icons.Kind.SPARKLE, 26, 14, 24, Theme.ON_ORANGE);
            g.setColor(Theme.ON_ORANGE);
            g.setFont(Theme.bold(14f));
            g.drawString(open ? "Close chat" : "Ask WealthSei", 58, 31);
            g.dispose();
        }
    }

    /** The chat window. */
    private static final class Popup extends JPanel {
        private static final long serialVersionUID = 1L;

        Popup(ChatPanel chat, boolean aiOn) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
            add(new Header(aiOn), BorderLayout.NORTH);
            add(chat, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setColor(Theme.p().surface());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.setColor(Theme.p().border());
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.dispose();
        }
    }

    private static final class Header extends JComponent {
        private static final long serialVersionUID = 1L;
        private final boolean aiOn;

        Header(boolean aiOn) {
            this.aiOn = aiOn;
            setOpaque(false);
            setPreferredSize(new Dimension(100, 74));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setPaint(new GradientPaint(4, 14, Theme.p().accent(), 40, 52, Theme.mix(Theme.p().accent(), new Color(0xFF7A1F), 0.6f)));
            g.fillOval(2, 16, 40, 40);
            Icons.draw(g, Icons.Kind.SPARKLE, 9, 23, 26, Theme.ON_ORANGE);
            g.setColor(Theme.p().text());
            g.setFont(Theme.tracked(Theme.bold(16f), -0.01f));
            g.drawString("WealthSei AI", 54, 34);
            Color dot = aiOn ? Theme.p().good() : Theme.p().warn();
            g.setColor(dot);
            g.fillOval(54, 43, 8, 8);
            g.setColor(Theme.p().muted());
            g.setFont(Theme.regular(12f));
            g.drawString(aiOn ? "Answers come from your own data" : "Offline: no API key", 68, 51);
            g.setColor(Theme.p().border());
            g.fillRect(0, getHeight() - 4, getWidth(), 1);
            g.dispose();
        }
    }
}
