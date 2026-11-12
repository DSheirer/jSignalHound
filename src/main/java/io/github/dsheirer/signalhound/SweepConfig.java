package io.github.dsheirer.signalhound;

/**
 * Settings for a swept spectrum measurement, the same for every device family.
 * The sweep uses a min/max detector on a logarithmic (dBm) scale.
 *
 * @param centerFrequency center of the sweep in Hz
 * @param span width of the sweep in Hz
 * @param rbw resolution bandwidth in Hz
 * @param vbw video bandwidth in Hz, at most {@code rbw}
 * @param sweepTime suggested time of one sweep in seconds, which the API may
 *        lengthen to satisfy the bandwidths
 * @param referenceLevel the expected maximum input level in dBm; at most 20
 */
public record SweepConfig(double centerFrequency, double span, double rbw, double vbw, double sweepTime,
    double referenceLevel) {

    public SweepConfig {
        if (!(centerFrequency > 0) || !(span > 0) || !(rbw > 0) || !(vbw > 0) || !(sweepTime >= 0)) {
            throw new IllegalArgumentException("Frequencies and bandwidths must be positive and sweepTime not negative");
        }
        if (vbw > rbw) {
            throw new IllegalArgumentException("vbw must not exceed rbw: " + vbw + " > " + rbw);
        }
        if (!(referenceLevel <= 20.0)) {
            throw new IllegalArgumentException("referenceLevel must be at most 20 dBm: " + referenceLevel);
        }
    }

    /** Settings with {@code vbw} equal to {@code rbw}, a 1 ms sweep time and a -20 dBm reference level. */
    public static SweepConfig of(double centerFrequency, double span, double rbw) {
        return new SweepConfig(centerFrequency, span, rbw, rbw, 0.001, IqConfig.DEFAULT_REFERENCE_LEVEL);
    }
}
