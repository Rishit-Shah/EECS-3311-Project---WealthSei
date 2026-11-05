package wealthsei.repository.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import wealthsei.agent.UserProfileMemory;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.repository.MemoryRepository;

/** Adds saving to a file on top of any {@link MemoryRepository} (Decorator pattern). */
public class PersistentMemoryRepository implements MemoryRepository {

    private final MemoryRepository inner;
    private final Path file;

    public PersistentMemoryRepository(MemoryRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        List<TsvFile.Row> rows = TsvFile.read(file);
        if (!rows.isEmpty()) {
            Set<Category> protectedCategories = new LinkedHashSet<>();
            List<String> reviews = new ArrayList<>();
            for (TsvFile.Row row : rows) {
                FileCodec.parse(file, row, r -> {
                    String kind = FileCodec.field(r, 0);
                    if (kind.equals("protected")) {
                        Categories.find(FileCodec.field(r, 1)).ifPresent(protectedCategories::add);
                    } else if (kind.equals("review")) {
                        reviews.add(FileCodec.field(r, 1));
                    }
                    return kind;
                });
            }
            UserProfileMemory loaded = new UserProfileMemory();
            loaded.replaceProtected(protectedCategories);
            loaded.replaceReviewSummaries(reviews);
            inner.savePreferences(loaded);
        }
    }

    @Override
    public void savePreferences(UserProfileMemory profile) {
        inner.savePreferences(profile);
        List<List<String>> rows = new ArrayList<>();
        profile.getProtectedCategories().stream().map(Category::id).sorted()
                .forEach(id -> rows.add(List.of("protected", id)));
        profile.getPastReviewSummaries().forEach(text -> rows.add(List.of("review", text)));
        TsvFile.write(file, rows);
    }

    @Override
    public UserProfileMemory loadPreferences() {
        return inner.loadPreferences();
    }
}
