package io.github.dsheirer.signalhound;

/** Thrown when a Signal Hound API function returns an error status. */
public class SignalHoundException extends Exception {
    private static final long serialVersionUID = 1L;

    private final SignalHoundApi api;
    private final String function;
    private final int code;

    /**
     * Creates an exception for a failed call.
     *
     * @param api the API of the function
     * @param function the C name of the function, such as {@code bbOpenDevice}
     * @param code the negative status the function returned
     * @param description the library's description of the status
     */
    public SignalHoundException(SignalHoundApi api, String function, int code, String description) {
        super(function + " failed: " + description + " (" + code + ")");
        this.api = api;
        this.function = function;
        this.code = code;
    }

    public SignalHoundApi api() {
        return api;
    }

    /** The C name of the function that failed. */
    public String function() {
        return function;
    }

    /**
     * The status the function returned: a negative {@code bbStatus},
     * {@code SmStatus} or {@code SpStatus} value.
     */
    public int code() {
        return code;
    }
}
