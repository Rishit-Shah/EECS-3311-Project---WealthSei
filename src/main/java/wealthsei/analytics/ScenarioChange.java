package wealthsei.analytics;

/** One thing changed in a what-if scenario. Applying it never alters the inputs it is given; it returns a changed copy. */
public interface ScenarioChange {

    /**
     * @return a modified copy of the inputs
     * @throws wealthsei.service.ValidationException if the change refers to something that does not exist
     */
    ForecastInputs applyTo(ForecastInputs inputs);

    /** A short sentence for the user, such as "Cancel Spotify". */
    String describe();
}
