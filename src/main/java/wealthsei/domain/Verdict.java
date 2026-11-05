package wealthsei.domain;

/** The answer to "can I afford it?". */
public enum Verdict {
    YES("Yes"),
    YES_BUT("Yes-but"),
    WAIT("Wait"),
    NO("No");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Reads "Yes", "yes-but", "YES_BUT", "wait" and so on; returns null if it is none of them. */
    public static Verdict parse(String text) {
        if (text == null) {
            return null;
        }
        String normal = text.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (Verdict v : values()) {
            if (v.name().equals(normal)) {
                return v;
            }
        }
        return null;
    }
}
