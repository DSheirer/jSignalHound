package io.github.dsheirer.signalhound;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Native memory that {@link SignalHoundDevice#readIq(IqBuffer, boolean)} fills
 * with interleaved I/Q samples. Allocate one and reuse it for every read, then
 * close it. A buffer may be read from any thread, but must not be closed while
 * a read into it is in progress.
 */
public final class IqBuffer implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final IqDataType dataType;
    private final int capacity;

    private IqBuffer(IqDataType dataType, int capacity) {
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.arena = Arena.ofShared();
        this.segment = arena.allocate((long) capacity * dataType.sampleBytes(), 16);
    }

    /** Allocates a buffer for {@code capacity} complex samples of a type. */
    public static IqBuffer allocate(IqDataType dataType, int capacity) {
        return new IqBuffer(dataType, capacity);
    }

    /** Allocates a buffer for the samples of a stream. */
    public static IqBuffer allocate(IqStreamInfo stream, int capacity) {
        return new IqBuffer(stream.dataType(), capacity);
    }

    public IqDataType dataType() {
        return dataType;
    }

    /** Number of complex samples the buffer holds; each read fills all of them. */
    public int capacity() {
        return capacity;
    }

    /**
     * The native memory, for zero-copy access. Its contents change with each
     * read, and it cannot be used after the buffer is closed.
     */
    public MemorySegment segment() {
        return segment;
    }

    /** Copies the samples of a {@link IqDataType#FLOAT32} buffer: I, Q, I, Q, ... */
    public float[] copyFloats() {
        require(IqDataType.FLOAT32);
        return segment.toArray(ValueLayout.JAVA_FLOAT);
    }

    /** Copies the samples of a {@link IqDataType#FLOAT32} buffer into an array of at least {@code 2 * capacity()}. */
    public void copyFloats(float[] destination) {
        require(IqDataType.FLOAT32);
        MemorySegment.copy(segment, ValueLayout.JAVA_FLOAT, 0, destination, 0, 2 * capacity);
    }

    /** Copies the samples of a {@link IqDataType#INT16} buffer: I, Q, I, Q, ... */
    public short[] copyShorts() {
        require(IqDataType.INT16);
        return segment.toArray(ValueLayout.JAVA_SHORT);
    }

    /** Copies the samples of a {@link IqDataType#INT16} buffer into an array of at least {@code 2 * capacity()}. */
    public void copyShorts(short[] destination) {
        require(IqDataType.INT16);
        MemorySegment.copy(segment, ValueLayout.JAVA_SHORT, 0, destination, 0, 2 * capacity);
    }

    private void require(IqDataType type) {
        if (dataType != type) {
            throw new IllegalStateException("Buffer holds " + dataType + " samples, not " + type);
        }
    }

    /** Frees the native memory. Closing a closed buffer does nothing. */
    @Override
    public void close() {
        if (arena.scope().isAlive()) {
            arena.close();
        }
    }
}
