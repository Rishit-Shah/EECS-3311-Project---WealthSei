package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void holdsFields() {
        Category c = new Category("groceries", "Groceries", true);
        assertEquals("groceries", c.id());
        assertEquals("Groceries", c.name());
        assertTrue(c.essential());
    }

    @Test
    void rejectsBlankOrNull() {
        assertThrows(IllegalArgumentException.class, () -> new Category(" ", "X", false));
        assertThrows(IllegalArgumentException.class, () -> new Category("x", "", false));
        assertThrows(NullPointerException.class, () -> new Category(null, "X", false));
    }

    @Test
    void uncategorizedIsRecognised() {
        assertTrue(Category.UNCATEGORIZED.isUncategorized());
        assertFalse(new Category("rent", "Rent", true).isUncategorized());
    }
}
