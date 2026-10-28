package wealthsei.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A notification for the user (budget threshold, low balance, goal at risk).
 *
 * @param type      what the alert is about
 * @param severity  how serious it is
 * @param message   plain-language text shown to the user
 * @param createdAt when it was raised
 */
public record Alert(AlertType type, AlertSeverity severity, String message, Instant createdAt) {

    public Alert {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(createdAt, "createdAt");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Alert message must not be blank");
        }
    }
}
