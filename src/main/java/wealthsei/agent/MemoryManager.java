package wealthsei.agent;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import wealthsei.domain.Category;
import wealthsei.llm.Message;
import wealthsei.repository.MemoryRepository;

/**
 * Gives the agent what it should know before a chat answer (recent turns and the person's
 * preferences) and stores each finished turn afterwards. Short-term memory lasts for the session;
 * preferences are saved.
 */
public class MemoryManager {

    private final ConversationMemory shortTerm;
    private final UserProfileMemory longTerm;
    private final MemoryRepository repo;

    public MemoryManager(MemoryRepository repo) {
        this(new ConversationMemory(), repo);
    }

    public MemoryManager(ConversationMemory shortTerm, MemoryRepository repo) {
        this.shortTerm = Objects.requireNonNull(shortTerm, "shortTerm");
        this.repo = Objects.requireNonNull(repo, "repo");
        this.longTerm = repo.loadPreferences();
    }

    /** A fresh context for the task with the recent conversation and preferences added. */
    public AgentContext buildContext(AgentTask task, LocalDate today) {
        AgentContext ctx = new AgentContext(task, today);
        ctx.useMemory(shortTerm.recent(), profileNote());
        return ctx;
    }

    /** Stores the question and the answer so a follow-up can refer to them. */
    public void remember(AgentTask task, AgentResult result) {
        shortTerm.append(Message.user(task.userInput()));
        shortTerm.append(Message.assistant(result.answer(), java.util.List.of()));
    }

    public void forgetConversation() {
        shortTerm.clear();
    }

    public Set<Category> protectedCategories() {
        return longTerm.getProtectedCategories();
    }

    public void setProtectedCategory(Category category, boolean isProtected) {
        longTerm.setProtected(category, isProtected);
        repo.savePreferences(longTerm);
    }

    /** Keeps a review summary for the next review to compare against. */
    public void rememberReview(String summary) {
        longTerm.addReviewSummary(summary);
        repo.savePreferences(longTerm);
    }

    public java.util.List<String> pastReviewSummaries() {
        return longTerm.getPastReviewSummaries();
    }

    String profileNote() {
        StringBuilder sb = new StringBuilder();
        Set<Category> protectedCategories = longTerm.getProtectedCategories();
        if (!protectedCategories.isEmpty()) {
            sb.append("Protected categories the user never wants cut: ")
                    .append(protectedCategories.stream().map(Category::name).sorted().collect(Collectors.joining(", ")))
                    .append(". ");
        }
        if (!longTerm.getPastReviewSummaries().isEmpty()) {
            sb.append("Earlier monthly reviews: ").append(String.join(" | ", longTerm.getPastReviewSummaries()));
        }
        return sb.toString().trim();
    }
}
