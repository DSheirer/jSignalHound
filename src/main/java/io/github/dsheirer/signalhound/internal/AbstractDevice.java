package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import io.github.dsheirer.signalhound.IqBlock;
import io.github.dsheirer.signalhound.IqBuffer;
import io.github.dsheirer.signalhound.IqConfig;
import io.github.dsheirer.signalhound.IqStreamInfo;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundDevice;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.Sweep;
import io.github.dsheirer.signalhound.SweepConfig;
import io.github.dsheirer.signalhound.SweepInfo;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * State and locking shared by the device classes of every family. Subclasses
 * supply the native calls. All calls to a device hold its monitor, so that the
 * scratch memory for output parameters can be shared and so that
 * {@link #close()} cannot free the device during another call.
 */
public abstract class AbstractDevice implements SignalHoundDevice {
    /** Size of a scratch slot, which holds any scalar output parameter. */
    private static final long SLOT = 8;
    private static final int SLOTS = 8;

    /** What the device is configured to measure. */
    protected enum Mode {
        IDLE,
        IQ,
        SWEEP
    }

    /** The handle the API assigned to the device. */
    protected final int handle;
    private final SignalHoundApi api;
    private final int serialNumber;
    private final String model;
    private final Arena arena = Arena.ofShared();
    private final MemorySegment scratch = arena.allocate(SLOT * SLOTS, SLOT);
    private boolean closed;
    private Mode mode = Mode.IDLE;
    private IqStreamInfo iqStream;
    private SweepInfo sweepInfo;

    protected AbstractDevice(SignalHoundApi api, int handle, int serialNumber, String model) {
        this.api = api;
        this.handle = handle;
        this.serialNumber = serialNumber;
        this.model = model;
    }

    @Override
    public final SignalHoundApi api() {
        return api;
    }

    @Override
    public final int serialNumber() {
        return serialNumber;
    }

    @Override
    public final String model() {
        return model;
    }

    /** The API's handle of the device, for calling bindings that this class does not wrap. */
    public final int handle() {
        return handle;
    }

    @Override
    public final synchronized IqStreamInfo startIq(IqConfig config) throws SignalHoundException {
        Objects.requireNonNull(config, "config");
        ensureOpen();
        stopMeasurement();
        iqStream = configureIq(config);
        mode = Mode.IQ;
        return iqStream;
    }

    @Override
    public final synchronized IqBlock readIq(IqBuffer buffer, boolean purge) throws SignalHoundException {
        Objects.requireNonNull(buffer, "buffer");
        ensureOpen();
        if (mode != Mode.IQ) {
            throw new IllegalStateException("The device is not streaming I/Q; call startIq first");
        }
        if (buffer.dataType() != iqStream.dataType()) {
            throw new IllegalArgumentException("The stream has " + iqStream.dataType() + " samples but the buffer "
                + buffer.dataType());
        }
        return readIqBlock(buffer, purge);
    }

    @Override
    public final synchronized float iqCorrection() throws SignalHoundException {
        ensureOpen();
        return readIqCorrection();
    }

    @Override
    public final synchronized SweepInfo startSweep(SweepConfig config) throws SignalHoundException {
        Objects.requireNonNull(config, "config");
        ensureOpen();
        stopMeasurement();
        sweepInfo = configureSweep(config);
        mode = Mode.SWEEP;
        return sweepInfo;
    }

    @Override
    public final synchronized Sweep readSweep() throws SignalHoundException {
        ensureOpen();
        if (mode != Mode.SWEEP) {
            throw new IllegalStateException("The device is not configured for sweeps; call startSweep first");
        }
        int bins = sweepInfo.bins();
        try (Arena call = Arena.ofConfined()) {
            MemorySegment min = call.allocate(JAVA_FLOAT, bins);
            MemorySegment max = call.allocate(JAVA_FLOAT, bins);
            long[] timestamp = new long[1];
            int status = fetchSweep(bins, min, max, timestamp);
            return new Sweep(min.toArray(JAVA_FLOAT), max.toArray(JAVA_FLOAT), timestamp[0], warning(status));
        }
    }

    @Override
    public final synchronized void stop() throws SignalHoundException {
        ensureOpen();
        stopMeasurement();
    }

    private void stopMeasurement() throws SignalHoundException {
        if (mode != Mode.IDLE) {
            // Idle first: a failed abort leaves the device in an unknown state,
            // which a new configuration replaces anyway.
            mode = Mode.IDLE;
            abort();
        }
    }

    @Override
    public final synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public final synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (mode != Mode.IDLE) {
                abort();
            }
        } catch (SignalHoundException | RuntimeException e) {
            // Closing the device ends the measurement regardless.
        } finally {
            mode = Mode.IDLE;
            closeDevice();
            arena.close();
        }
    }

    /** Throws {@link IllegalStateException} when the device is closed. Call while holding the monitor. */
    protected final void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("The device is closed");
        }
    }

    /** The current measurement mode. Call while holding the monitor. */
    protected final Mode mode() {
        return mode;
    }

    /**
     * Returns scratch slot {@code index} (0 to 7), eight aligned bytes for an
     * output parameter. Only valid while holding the monitor.
     */
    protected final MemorySegment slot(int index) {
        return scratch.asSlice(index * SLOT, SLOT);
    }

    protected final int intSlot(int index) {
        return scratch.get(JAVA_INT, index * SLOT);
    }

    protected final long longSlot(int index) {
        return scratch.get(JAVA_LONG, index * SLOT);
    }

    protected final float floatSlot(int index) {
        return scratch.get(JAVA_FLOAT, index * SLOT);
    }

    protected final double doubleSlot(int index) {
        return scratch.get(JAVA_DOUBLE, index * SLOT);
    }

    /** The library's description of a warning status, or {@code null} for success. */
    protected final String warning(int status) {
        return status > 0 ? NativeLibrary.of(api).errorString(status) : null;
    }

    /** Configures and starts the I/Q stream. */
    protected abstract IqStreamInfo configureIq(IqConfig config) throws SignalHoundException;

    /** Reads {@code buffer.capacity()} samples. */
    protected abstract IqBlock readIqBlock(IqBuffer buffer, boolean purge) throws SignalHoundException;

    /** Returns the scale factor for 16-bit samples. */
    protected abstract float readIqCorrection() throws SignalHoundException;

    /** Configures and starts sweeps. */
    protected abstract SweepInfo configureSweep(SweepConfig config) throws SignalHoundException;

    /**
     * Fetches one sweep of {@code bins} floats into each array, and stores its
     * timestamp, or 0, in {@code timestamp[0]}.
     *
     * @return the status, zero or a warning
     */
    protected abstract int fetchSweep(int bins, MemorySegment min, MemorySegment max, long[] timestamp)
        throws SignalHoundException;

    /** Stops the measurement in progress. */
    protected abstract void abort() throws SignalHoundException;

    /** Closes the device handle; must not throw. */
    protected abstract void closeDevice();
}
