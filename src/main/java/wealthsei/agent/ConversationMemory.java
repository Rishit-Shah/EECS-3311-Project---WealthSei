package wealthsei.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import wealthsei.llm.Message;

/**
 * The last few messages of the conversation, so a follow-up such as "and last month?" can be
 * understood. It keeps only the newest {@code windowSize} messages; older ones are dropped.
 */
public class ConversationMemory {

    public static final int DEFAULT_WINDOW = 10;

    private final int windowSize;
    private final Deque<Message> messages = new ArrayDeque<>();

    public ConversationMemory() {
        this(DEFAULT_WINDOW);
    }

    public ConversationMemory(int windowSize) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("The window must hold at least one question and one answer");
        }
        this.windowSize = windowSize;
    }

    public void append(Message message) {
        Objects.requireNonNull(message, "message");
        messages.addLast(message);
        while (messages.size() > windowSize) {
            messages.removeFirst();
        }
    }

    /** The remembered messages, oldest first. Always starts with a question, as the model requires. */
    public List<Message> recent() {
        List<Message> list = new ArrayList<>(messages);
        while (!list.isEmpty() && list.get(0).role() != Message.Role.USER) {
            list.remove(0);
        }
        return List.copyOf(list);
    }

    public void clear() {
        messages.clear();
    }
}
