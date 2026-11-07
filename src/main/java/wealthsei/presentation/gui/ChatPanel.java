package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import wealthsei.agent.AgentResult;
import wealthsei.agent.AgentStatus;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.service.ValidationException;

/** The conversation: messages, suggested questions and the box you type in. Used in the corner popup and in the Assistant screen. */
@SuppressWarnings({"serial", "this-escape"})
public class ChatPanel extends JPanel {

    private static final List<String> SUGGESTIONS = List.of(
            "How much did I spend on dining?", "Am I on track this month?", "What are my spending habits?");

    private final WealthSeiFacade facade;
    private final boolean aiOn;
    private final int bubbleWidth;
    private final JPanel messages = new JPanel();
    private final JScrollPane scroll;
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JTextField input = new JTextField() {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (getText().isEmpty()) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.quality(g);
                g.setFont(getFont());
                g.setColor(Theme.alpha(Theme.p().muted(), 200));
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.drawString(isEnabled() ? "Ask about your money…" : "AI is off", getInsets().left,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g.dispose();
            }
        }
    };
    private boolean waiting;

    public ChatPanel(WealthSeiFacade facade, boolean aiOn, int bubbleWidth) {
        super(new BorderLayout());
        this.facade = facade;
        this.aiOn = aiOn;
        this.bubbleWidth = bubbleWidth;
        setOpaque(false);

        messages.setOpaque(false);
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(messages, BorderLayout.NORTH);
        scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        ModernScrollBar.apply(scroll);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        chips.setOpaque(false);
        bottom.add(chips, BorderLayout.NORTH);
        bottom.add(inputBar(), BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        addMessage(false, aiOn
                ? "Hi! I can answer questions about your money, like what you spent on dining or whether you are on track. What would you like to know?"
                : "The AI model is off because no API key was found, so I cannot answer free-form questions. "
                        + "The dashboard and the other screens still work.", null);
        showChips(true);
    }

    private JComponent inputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.quality(g);
                g.setColor(Theme.p().surfaceRaised());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.setColor(Theme.p().border());
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(5, 18, 5, 5));
        input.setOpaque(false);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(Theme.regular(13.5f));
        input.setForeground(Theme.p().text());
        input.setCaretColor(Theme.p().accent());
        input.setEnabled(aiOn);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send(input.getText());
                }
            }
        });
        bar.add(input, BorderLayout.CENTER);
        JComponent send = new JComponent() {
            private static final long serialVersionUID = 1L;

            {
                setPreferredSize(new Dimension(38, 38));
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        send(input.getText());
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.quality(g);
                g.setColor(aiOn ? Theme.p().accent() : Theme.p().border());
                g.fillOval(0, 0, getWidth(), getHeight());
                Icons.draw(g, Icons.Kind.SEND, 7, 7, 24, Theme.ON_ORANGE);
                g.dispose();
            }
        };
        bar.add(send, BorderLayout.EAST);
        bar.setPreferredSize(new Dimension(100, 48));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        wrap.add(bar, BorderLayout.CENTER);
        return wrap;
    }

    private void showChips(boolean show) {
        chips.removeAll();
        if (show && aiOn) {
            for (String s : SUGGESTIONS) {
                chips.add(new FlatButton(s, null, false, () -> send(s)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Dimension getPreferredSize() {
                        Dimension d = super.getPreferredSize();
                        return new Dimension(d.width, 32);
                    }
                });
            }
        }
        chips.revalidate();
        chips.repaint();
    }

    private void addMessage(boolean fromUser, String text, String footnote) {
        Bubble b = new Bubble(fromUser, text, footnote, bubbleWidth);
        JPanel row = new JPanel(new FlowLayout(fromUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(b);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        messages.add(row);
        messages.add(Box.createVerticalStrut(2));
        messages.revalidate();
        messages.repaint();
        SwingUtilities.invokeLater(() -> {
            javax.swing.JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    /** Shows a question as if it was typed and answers it (used by suggestion chips and the Assistant screen). */
    public void send(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty() || waiting || !aiOn) {
            return;
        }
        input.setText("");
        showChips(false);
        addMessage(true, q, null);
        waiting = true;
        addMessage(false, "Thinking…", null);
        int thinkingIndex = messages.getComponentCount() - 2;
        new SwingWorker<AgentResult, Void>() {
            @Override
            protected AgentResult doInBackground() {
                return facade.ask(q);
            }

            @Override
            protected void done() {
                messages.remove(thinkingIndex + 1);
                messages.remove(thinkingIndex);
                waiting = false;
                try {
                    AgentResult r = get();
                    String note = r.status() == AgentStatus.COMPLETED ? "Checked against your data · trace " + r.traceId()
                            : r.status().name() + " · trace " + r.traceId();
                    addMessage(false, r.answer(), note);
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    addMessage(false, cause instanceof ValidationException ? cause.getMessage()
                            : "Something went wrong: " + cause.getMessage(), null);
                }
            }
        }.execute();
    }

    /** Puts a finished exchange on screen without calling the assistant (for previews). */
    public void showExample(String question, String answer) {
        showChips(false);
        addMessage(true, question, null);
        addMessage(false, answer, "Checked against your data");
    }
}
