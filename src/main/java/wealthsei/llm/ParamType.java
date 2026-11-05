package wealthsei.llm;

/** The kinds of value a tool parameter can take. */
public enum ParamType {
    STRING("string"),
    NUMBER("number"),
    INTEGER("integer");

    private final String jsonName;

    ParamType(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }
}
