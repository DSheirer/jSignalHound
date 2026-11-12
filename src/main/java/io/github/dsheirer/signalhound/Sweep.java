package io.github.dsheirer.signalhound;

/**
 * One sweep, in dBm per bin.
 *
 * @param min the minimum detector values
 * @param max the maximum detector values
 * @param timestampNanos time of the sweep in nanoseconds since the epoch, or 0
 *        when the API does not report it, as the BB60 API does not
 * @param warning the API's description of a warning status, or {@code null}
 *        when there was none
 */
public record Sweep(float[] min, float[] max, long timestampNanos, String warning) {
}
