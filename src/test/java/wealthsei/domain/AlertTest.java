package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertTest {

    @Test
    void holdsFields() {
        Instant now = Instant.parse("2026-10-27T12:00:00Z");
        Alert a = new Alert(AlertType.BUDGET, AlertSeverity.WARNING, "Groceries is at 85%", now);
        assertEquals(AlertType.BUDGET, a.type());
        assertEquals(AlertSeverity.WARNING, a.severity());
        assertEquals("Groceries is at 85%", a.message());
        assertEquals(now, a.createdAt());
    }

    @Test
    void rejectsBlankMessageAndNulls() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class, () -> new Alert(AlertType.BUDGET, AlertSeverity.INFO, " ", now));
        assertThrows(NullPointerException.class, () -> new Alert(null, AlertSeverity.INFO, "x", now));
    }

    @Test
    void severityIsOrderedFromMildToSevere() {
        assertTrue(AlertSeverity.INFO.compareTo(AlertSeverity.WARNING) < 0);
        assertTrue(AlertSeverity.WARNING.compareTo(AlertSeverity.OVER) < 0);
    }
}
