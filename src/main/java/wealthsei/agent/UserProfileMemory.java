package wealthsei.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;

/** What the app remembers about the person between sessions: protected categories and past review summaries. */
public class UserProfileMemory {

    static final int MAX_REVIEW_SUMMARIES = 3;

    private final Set<Category> protectedCategories = new LinkedHashSet<>(Set.of(Categories.GROCERIES));
    private final List<String> pastReviewSummaries = new ArrayList<>();

    public Set<Category> getProtectedCategories() {
        return Set.copyOf(protectedCategories);
    }

    public void setProtected(Category category, boolean isProtected) {
        if (isProtected) {
            protectedCategories.add(category);
        } else {
            protectedCategories.remove(category);
        }
    }

    /** Replaces the protected categories (used when loading). */
    public void replaceProtected(Set<Category> categories) {
        protectedCategories.clear();
        protectedCategories.addAll(categories);
    }

    public List<String> getPastReviewSummaries() {
        return List.copyOf(pastReviewSummaries);
    }

    /** Remembers a review summary; only the newest few are kept. */
    public void addReviewSummary(String summary) {
        pastReviewSummaries.add(summary);
        while (pastReviewSummaries.size() > MAX_REVIEW_SUMMARIES) {
            pastReviewSummaries.remove(0);
        }
    }

    public void replaceReviewSummaries(List<String> summaries) {
        pastReviewSummaries.clear();
        summaries.forEach(this::addReviewSummary);
    }
}
