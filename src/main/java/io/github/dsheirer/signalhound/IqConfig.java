package io.github.dsheirer.signalhound;

import java.util.Objects;

/**
 * Settings for I/Q streaming, the same for every device family.
 *
 * <p>The sample rate is the device's base rate, 40 MS/s for the BB60, divided
 * by {@code decimation}. The rate obtained, and the bandwidth after clamping,
 * are reported by
 * {@link SignalHoundDevice#startIq(IqConfig)}. The ARM builds of the APIs
 * support fewer decimations than the x86-64 builds: see the README of the
 * SDK's library folder.
 *
 * @param centerFrequency center frequency in Hz
 * @param decimation a power of two from 1 up to the device's maximum (8192 for
 *        the BB60 and SP145, 4096 for the SM200 and SM435)
 * @param bandwidth bandwidth of the software filter in Hz
 * @param dataType the sample format
 * @param referenceLevel the expected maximum input level in dBm, which sets
 *        the attenuation and gain automatically; at most 20
 */
public record IqConfig(double centerFrequency, int decimation, double bandwidth, IqDataType dataType,
    double referenceLevel) {

    /** Reference level used by {@link #of}. */
    public static final double DEFAULT_REFERENCE_LEVEL = -20.0;

    public IqConfig {
        Objects.requireNonNull(dataType, "dataType");
        if (!(centerFrequency > 0)) {
            throw new IllegalArgumentException("centerFrequency must be positive: " + centerFrequency);
        }
        if (decimation < 1 || Integer.bitCount(decimation) != 1) {
            throw new IllegalArgumentException("decimation must be a power of two: " + decimation);
        }
        if (!(bandwidth > 0)) {
            throw new IllegalArgumentException("bandwidth must be positive: " + bandwidth);
        }
        if (!(referenceLevel <= 20.0)) {
            throw new IllegalArgumentException("referenceLevel must be at most 20 dBm: " + referenceLevel);
        }
    }

    /** Settings for float samples at the default reference level. */
    public static IqConfig of(double centerFrequency, int decimation, double bandwidth) {
        return new IqConfig(centerFrequency, decimation, bandwidth, IqDataType.FLOAT32, DEFAULT_REFERENCE_LEVEL);
    }

    public IqConfig withCenterFrequency(double value) {
        return new IqConfig(value, decimation, bandwidth, dataType, referenceLevel);
    }

    public IqConfig withDecimation(int value) {
        return new IqConfig(centerFrequency, value, bandwidth, dataType, referenceLevel);
    }

    public IqConfig withBandwidth(double value) {
        return new IqConfig(centerFrequency, decimation, value, dataType, referenceLevel);
    }

    public IqConfig withDataType(IqDataType value) {
        return new IqConfig(centerFrequency, decimation, bandwidth, value, referenceLevel);
    }

    public IqConfig withReferenceLevel(double value) {
        return new IqConfig(centerFrequency, decimation, bandwidth, dataType, value);
    }
}
