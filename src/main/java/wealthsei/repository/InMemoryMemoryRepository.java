package wealthsei.repository;

import wealthsei.agent.UserProfileMemory;

/** Keeps the profile for the length of the session. */
public class InMemoryMemoryRepository implements MemoryRepository {

    private UserProfileMemory stored = new UserProfileMemory();

    @Override
    public void savePreferences(UserProfileMemory profile) {
        UserProfileMemory copy = new UserProfileMemory();
        copy.replaceProtected(profile.getProtectedCategories());
        copy.replaceReviewSummaries(profile.getPastReviewSummaries());
        stored = copy;
    }

    @Override
    public UserProfileMemory loadPreferences() {
        UserProfileMemory copy = new UserProfileMemory();
        copy.replaceProtected(stored.getProtectedCategories());
        copy.replaceReviewSummaries(stored.getPastReviewSummaries());
        return copy;
    }
}
