package io.github.dsheirer.signalhound.bb;

import static io.github.dsheirer.signalhound.internal.BbNative.BB_AUTO_ATTEN;
import static io.github.dsheirer.signalhound.internal.BbNative.BB_AUTO_GAIN;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.Diagnostics;
import io.github.dsheirer.signalhound.IqBlock;
import io.github.dsheirer.signalhound.IqBuffer;
import io.github.dsheirer.signalhound.IqConfig;
import io.github.dsheirer.signalhound.IqStreamInfo;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.SweepConfig;
import io.github.dsheirer.signalhound.SweepInfo;
import io.github.dsheirer.signalhound.internal.AbstractDevice;
import io.github.dsheirer.signalhound.internal.BbNative;
import java.lang.foreign.MemorySegment;

/**
 * An open BB60A, BB60C or BB60D. Obtain one from {@link Bb60}.
 *
 * <p>Gain and attenuation are automatic, set from the reference level, unless
 * {@link #setGainAttenuation(int, int)} chooses them; the choice applies to the
 * measurements started afterwards.
 */
public final class Bb60Device extends AbstractDevice {
    private final BbModel bbModel;
    private int gain = BB_AUTO_GAIN;
    private int attenuation = BB_AUTO_ATTEN;

    Bb60Device(int handle, int serialNumber, BbModel model) {
        super(SignalHoundApi.BB, handle, serialNumber, model.name());
        this.bbModel = model;
    }

    /** The model of the device. */
    public BbModel bbModel() {
        return bbModel;
    }

    @Override
    public synchronized String firmwareVersion() throws SignalHoundException {
        ensureOpen();
        BbNative.bbGetFirmwareVersion.check(handle, slot(0));
        return Integer.toString(intSlot(0));
    }

    @Override
    public synchronized Diagnostics diagnostics() throws SignalHoundException {
        ensureOpen();
        BbNative.bbGetDeviceDiagnostics.check(handle, slot(0), slot(1), slot(2));
        return new Diagnostics(floatSlot(0), floatSlot(1), floatSlot(2));
    }

    /**
     * Chooses the gain and attenuation of the measurements started afterwards.
     * Both must be manual to override the reference level; Signal Hound
     * recommends leaving them automatic, which optimizes them for each band.
     *
     * @param gain 0 to 3, or -1 for automatic
     * @param attenuation 0 to 3, in steps of 10 dB, or -1 for automatic
     */
    public synchronized void setGainAttenuation(int gain, int attenuation) {
        ensureOpen();
        if (gain < -1 || gain > 3 || attenuation < -1 || attenuation > 3) {
            throw new IllegalArgumentException("gain and attenuation must be -1 to 3: " + gain + ", " + attenuation);
        }
        this.gain = gain;
        this.attenuation = attenuation;
    }

    /**
     * Configures the BNC ports; see {@code bbConfigureIO} for the values, the
     * {@code BB60C_PORT*} or {@code BB60D_PORT*} constants of the header.
     * Call while no measurement is active.
     */
    public synchronized void configureIo(int port1, int port2) throws SignalHoundException {
        ensureOpen();
        BbNative.bbConfigureIO.check(handle, port1, port2);
    }

    /**
     * Power cycles the device with {@code bbPreset}, which can revive an
     * unresponsive device that still accepts USB commands, and closes this
     * handle. The device reappears after about 3 seconds, to be opened again.
     */
    public synchronized void powerCycleAndClose() throws SignalHoundException {
        ensureOpen();
        if (mode() != Mode.IDLE) {
            stop();
        }
        BbNative.bbPreset.check(handle);
        close();
    }

    @Override
    protected IqStreamInfo configureIq(IqConfig config) throws SignalHoundException {
        BbNative.bbConfigureRefLevel.check(handle, config.referenceLevel());
        BbNative.bbConfigureGainAtten.check(handle, gain, attenuation);
        BbNative.bbConfigureIQCenter.check(handle, config.centerFrequency());
        BbNative.bbConfigureIQ.check(handle, config.decimation(), config.bandwidth());
        BbNative.bbConfigureIQDataType.check(handle, config.dataType().code());
        BbNative.bbInitiate.check(handle, BbNative.BB_STREAMING, BbNative.BB_STREAM_IQ);
        BbNative.bbQueryIQParameters.check(handle, slot(0), slot(1));
        return new IqStreamInfo(doubleSlot(0), doubleSlot(1), config.dataType());
    }

    @Override
    protected IqBlock readIqBlock(IqBuffer buffer, boolean purge) throws SignalHoundException {
        int status = BbNative.bbGetIQUnpacked.check(handle, buffer.segment(), buffer.capacity(),
            MemorySegment.NULL, 0, purge ? 1 : 0, slot(0), slot(1), slot(2), slot(3));
        // The seconds are a C int; reading them unsigned keeps them right past 2038.
        long nanos = Integer.toUnsignedLong(intSlot(2)) * 1_000_000_000L + intSlot(3);
        return new IqBlock(buffer.capacity(), nanos, intSlot(1) != 0, intSlot(0), warning(status));
    }

    @Override
    protected float readIqCorrection() throws SignalHoundException {
        BbNative.bbGetIQCorrection.check(handle, slot(0));
        return floatSlot(0);
    }

    @Override
    protected SweepInfo configureSweep(SweepConfig config) throws SignalHoundException {
        BbNative.bbConfigureRefLevel.check(handle, config.referenceLevel());
        BbNative.bbConfigureGainAtten.check(handle, gain, attenuation);
        BbNative.bbConfigureCenterSpan.check(handle, config.centerFrequency(), config.span());
        BbNative.bbConfigureSweepCoupling.check(handle, config.rbw(), config.vbw(), config.sweepTime(),
            BbNative.BB_RBW_SHAPE_NUTTALL, BbNative.BB_NO_SPUR_REJECT);
        BbNative.bbConfigureAcquisition.check(handle, BbNative.BB_MIN_AND_MAX, BbNative.BB_LOG_SCALE);
        BbNative.bbConfigureProcUnits.check(handle, BbNative.BB_LOG);
        BbNative.bbInitiate.check(handle, BbNative.BB_SWEEPING, 0);
        BbNative.bbQueryTraceInfo.check(handle, slot(0), slot(1), slot(2));
        return new SweepInfo(doubleSlot(2), doubleSlot(1), slot(0).get(JAVA_INT, 0), config.rbw(), config.vbw());
    }

    @Override
    protected int fetchSweep(int bins, MemorySegment min, MemorySegment max, long[] timestamp)
        throws SignalHoundException {
        timestamp[0] = 0;
        return BbNative.bbFetchTrace_32f.check(handle, bins, min, max);
    }

    @Override
    protected void abort() throws SignalHoundException {
        BbNative.bbAbort.check(handle);
    }

    @Override
    protected void closeDevice() {
        BbNative.bbCloseDevice.call(handle);
    }
}
