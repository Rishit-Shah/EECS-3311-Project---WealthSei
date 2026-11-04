package wealthsei.repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Keeps the flags in memory. */
public class InMemoryRecurringRepository implements RecurringRepository {

    private final Set<String> flagged = new LinkedHashSet<>();

    @Override
    public void saveFlag(String merchant, boolean flag) {
        String key = RecurringRepository.keyOf(merchant);
        if (flag) {
            flagged.add(key);
        } else {
            flagged.remove(key);
        }
    }

    @Override
    public boolean isFlagged(String merchant) {
        return flagged.contains(RecurringRepository.keyOf(merchant));
    }

    @Override
    public List<String> flaggedMerchants() {
        return List.copyOf(flagged);
    }
}
