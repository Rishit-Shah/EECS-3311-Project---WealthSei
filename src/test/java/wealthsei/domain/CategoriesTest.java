package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CategoriesTest {

    @Test
    void findIgnoresCaseAndSpaces() {
        assertEquals(Categories.GROCERIES, Categories.find(" Groceries ").orElseThrow());
    }

    @Test
    void unknownIdIsEmpty() {
        assertTrue(Categories.find("nope").isEmpty());
    }

    @Test
    void everyDefaultCanBeFoundById() {
        for (Category c : Categories.defaults()) {
            assertEquals(c, Categories.find(c.id()).orElseThrow());
        }
    }
}
