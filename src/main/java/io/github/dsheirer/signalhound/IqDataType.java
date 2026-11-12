package io.github.dsheirer.signalhound;

/** Format of I/Q samples, which every Signal Hound API offers. */
public enum IqDataType {
    /** Interleaved 32-bit float I and Q, amplitude corrected by the API. */
    FLOAT32(0, Float.BYTES),
    /**
     * Interleaved 16-bit integer I and Q, full scale and not amplitude
     * corrected. {@link SignalHoundDevice#iqCorrection()} gives the correction;
     * see "I/Q data types" in the API documentation for the conversion.
     */
    INT16(1, Short.BYTES);

    private final int code;
    private final int componentBytes;

    IqDataType(int code, int componentBytes) {
        this.code = code;
        this.componentBytes = componentBytes;
    }

    /**
     * The value of the C enum ({@code bbDataType}, {@code SmDataType},
     * {@code SpDataType}), which is the same in each API.
     */
    public int code() {
        return code;
    }

    /** Bytes taken by one complex sample. */
    public int sampleBytes() {
        return 2 * componentBytes;
    }
}
