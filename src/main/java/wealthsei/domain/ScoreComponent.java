package wealthsei.domain;

import java.util.Objects;

/**
 * One part of the health score.
 *
 * @param name      e.g. "Budget adherence"
 * @param points    points earned
 * @param maxPoints the most this part can give
 * @param detail    the figure behind the points, e.g. "2 of 3 budget lines within limit"
 */
public record ScoreComponent(String name, int points, int maxPoints, String detail) {

    public ScoreComponent {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(detail, "detail");
        if (maxPoints < 1 || points < 0 || points > maxPoints) {
            throw new IllegalArgumentException("Points must be between 0 and " + maxPoints + ", was " + points);
        }
    }

    public int missing() {
        return maxPoints - points;
    }
}
