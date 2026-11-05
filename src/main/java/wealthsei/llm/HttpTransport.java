package wealthsei.llm;

/** Sends one JSON body to the model service and returns the JSON reply. */
public interface HttpTransport {

    /** @throws LLMException on a network problem or an error status */
    String post(String jsonBody);
}
