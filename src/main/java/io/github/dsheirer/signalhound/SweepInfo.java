package io.github.dsheirer.signalhound;

/**
 * The sweep that a device started, as the API reports it.
 *
 * @param startFrequency frequency of the first bin in Hz
 * @param binSize width of a bin in Hz
 * @param bins number of bins in a sweep
 * @param rbw resolution bandwidth in Hz; the requested value for the BB60,
 *        whose API does not report the one used
 * @param vbw video bandwidth in Hz; the requested value for the BB60
 */
public record SweepInfo(double startFrequency, double binSize, int bins, double rbw, double vbw) {
    /** Frequency of a bin in Hz. */
    public double frequency(int bin) {
        return startFrequency + bin * binSize;
    }
}
