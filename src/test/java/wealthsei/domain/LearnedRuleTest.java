package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LearnedRuleTest {

    @Test
    void holdsMerchantAndCategory() {
        LearnedRule r = new LearnedRule("netflix", Categories.SUBSCRIPTIONS);
        assertEquals("netflix", r.merchant());
        assertEquals(Categories.SUBSCRIPTIONS, r.category());
    }

    @Test
    void rejectsBlankMerchantAndNulls() {
        assertThrows(IllegalArgumentException.class, () -> new LearnedRule(" ", Categories.DINING));
        assertThrows(NullPointerException.class, () -> new LearnedRule("x", null));
        assertThrows(NullPointerException.class, () -> new LearnedRule(null, Categories.DINING));
    }
}
