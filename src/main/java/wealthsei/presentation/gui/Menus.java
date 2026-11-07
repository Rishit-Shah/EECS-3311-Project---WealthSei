package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/** A dark pop-up list, used by drop-downs. */
public final class Menus {

    /** One choice: its text and an optional coloured dot. */
    public record Option(String label, Color dot) {
        public Option(String label) {
            this(label, null);
        }
    }

    private Menus() {
    }

    /** Shows the options under {@code anchor}; {@code onPick} gets the index chosen. */
    public static void popup(Component anchor, List<Option> options, int selected, IntConsumer onPick) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createEmptyBorder());
        menu.setBackground(Theme.p().surfaceRaised());
        JPanel list = new JPanel();
        list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
        list.setBackground(Theme.p().surfaceRaised());
        list.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.p().border()), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            list.add(new Item(options.get(i), i == selected, () -> {
                menu.setVisible(false);
                onPick.accept(index);
            }));
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(list, BorderLayout.CENTER);
        if (options.size() > 9) {
            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(list);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setPreferredSize(new Dimension(Math.max(anchor.getWidth(), 210), 9 * 36 + 14));
            ModernScrollBar.apply(sp);
            holder.removeAll();
            holder.add(sp, BorderLayout.CENTER);
        }
        menu.add(holder);
        menu.show(anchor, 0, anchor.getHeight() + 6);
    }

    @SuppressWarnings({"serial", "this-escape"})
    private static final class Item extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Option option;
        private final boolean selected;
        private boolean over;

        Item(Option option, boolean selected, Runnable action) {
            this.option = option;
            this.selected = selected;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                public void mouseReleased(MouseEvent e) {
                    action.run();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(210, 36);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 36);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            if (over || selected) {
                g.setColor(over ? Theme.alpha(Theme.p().accent(), 40) : Theme.alpha(Theme.p().muted(), 30));
                g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 10, 10);
            }
            int x = 12;
            if (option.dot() != null) {
                g.setColor(option.dot());
                g.fillOval(12, getHeight() / 2 - 4, 8, 8);
                x = 30;
            }
            g.setFont(selected ? Theme.bold(13.5f) : Theme.regular(13.5f));
            g.setColor(Theme.p().text());
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(option.label(), x, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }
}
