package io.github.dsheirer.signalhound;

/**
 * An open Signal Hound device. The BB60, SM200/SM435 and SP145 families all
 * implement this interface, so that I/Q streaming and sweeps work the same way
 * on each; their classes add the features particular to the family.
 *
 * <p>Calls to one device are serialized: a method waits while another thread
 * is inside a call to the same device. {@link #close()} therefore waits for a
 * {@link #readIq} in progress, which returns once the samples arrive.
 */
public interface SignalHoundDevice extends AutoCloseable {
    /** The API that drives the device. */
    SignalHoundApi api();

    /** The serial number of the device. */
    int serialNumber();

    /** The model, such as {@code BB60D}, {@code SM200B} or {@code SP145}. */
    String model();

    /** The firmware version, such as {@code 8} or {@code 1.4.2}, as the API reports it. */
    String firmwareVersion() throws SignalHoundException;

    /** Reads the device's temperature, voltage and current sensors. */
    Diagnostics diagnostics() throws SignalHoundException;

    /**
     * Configures and starts I/Q streaming, stopping any measurement in progress.
     *
     * @return the sample rate and bandwidth obtained
     */
    IqStreamInfo startIq(IqConfig config) throws SignalHoundException;

    /**
     * Reads the next {@code buffer.capacity()} samples of the I/Q stream,
     * blocking until they are available.
     *
     * @param buffer receives the samples; its data type must be that of the stream
     * @param purge whether to discard samples buffered in the API first, so
     *        that the block starts with the newest samples
     * @throws IllegalStateException when the device is not streaming I/Q
     */
    IqBlock readIq(IqBuffer buffer, boolean purge) throws SignalHoundException;

    /**
     * The amplitude correction that the API applies to obtain
     * {@link IqDataType#FLOAT32} samples, for converting full scale
     * {@link IqDataType#INT16} samples of the current stream; see "I/Q data
     * types" in the API documentation for the formula.
     */
    float iqCorrection() throws SignalHoundException;

    /**
     * Configures and starts swept spectrum measurements, stopping any
     * measurement in progress.
     *
     * @return the frequencies and size of the sweeps
     */
    SweepInfo startSweep(SweepConfig config) throws SignalHoundException;

    /**
     * Performs one sweep and returns it, blocking until it completes.
     *
     * @throws IllegalStateException when the device is not configured for sweeps
     */
    Sweep readSweep() throws SignalHoundException;

    /** Stops the measurement in progress, if any, and leaves the device idle. */
    void stop() throws SignalHoundException;

    /** Returns whether {@link #close()} has been called. */
    boolean isClosed();

    /** Stops any measurement and closes the device. Closing a closed device does nothing. */
    @Override
    void close();
}
