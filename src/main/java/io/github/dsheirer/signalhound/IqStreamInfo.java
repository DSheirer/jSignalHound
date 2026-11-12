package io.github.dsheirer.signalhound;

/**
 * The I/Q stream that a device started, as the API reports it.
 *
 * @param sampleRate samples per second
 * @param bandwidth bandwidth in Hz
 * @param dataType the sample format, which {@link IqBuffer}s used to read the stream must have
 */
public record IqStreamInfo(double sampleRate, double bandwidth, IqDataType dataType) {
}
