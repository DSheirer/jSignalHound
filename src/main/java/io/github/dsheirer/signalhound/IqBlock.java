package io.github.dsheirer.signalhound;

/**
 * What {@link SignalHoundDevice#readIq(IqBuffer, boolean)} read into a buffer.
 *
 * @param samples number of complex samples read, which is the capacity of the buffer
 * @param timestampNanos time of the first sample in nanoseconds since the epoch,
 *        from the system clock or from GPS when the device is disciplined to it
 * @param sampleLoss whether samples were dropped before this block because they
 *        were not read fast enough
 * @param samplesRemaining samples buffered in the API after this block
 * @param warning the API's description of a warning status, such as an ADC
 *        overflow, or {@code null} when there was none
 */
public record IqBlock(int samples, long timestampNanos, boolean sampleLoss, int samplesRemaining, String warning) {
}
