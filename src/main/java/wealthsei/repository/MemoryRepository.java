package wealthsei.repository;

import wealthsei.agent.UserProfileMemory;

/** Storage for what the app remembers about the person. */
public interface MemoryRepository {

    void savePreferences(UserProfileMemory profile);

    UserProfileMemory loadPreferences();
}
