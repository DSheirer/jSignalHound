package io.github.dsheirer.signalhound.sp;

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
import io.github.dsheirer.signalhound.internal.SpNative;
import java.lang.foreign.MemorySegment;

/**
 * An open SP145. Obtain one from {@link Sp145}.
 *
 * <p>Attenuation is automatic, set from the reference level, unless
 * {@link #setAttenuation(int)} chooses it; the choice applies to the
 * measurements started afterwards.
 */
public final class SpDevice extends AbstractDevice {
    private final SpModel spModel;
    private int attenuation = SpNative.SP_AUTO_ATTEN;

    SpDevice(int handle, int serialNumber, SpModel model) {
        super(SignalHoundApi.SP, handle, serialNumber, model == SpModel.UNKNOWN ? "SP145" : model.name());
        this.spModel = model;
    }

    /** The model of the device, {@link SpModel#UNKNOWN} when the library cannot tell. */
    public SpModel spModel() {
        return spModel;
    }

    @Override
    public synchronized String firmwareVersion() throws SignalHoundException {
        ensureOpen();
        SpNative.spGetFirmwareVersion.check(handle, slot(0), slot(1), slot(2));
        return intSlot(0) + "." + intSlot(1) + "." + intSlot(2);
    }

    @Override
    public synchronized Diagnostics diagnostics() throws SignalHoundException {
        ensureOpen();
        SpNative.spGetDeviceDiagnostics.check(handle, slot(0), slot(1), slot(2));
        return new Diagnostics(floatSlot(2), floatSlot(0), floatSlot(1));
    }

    /**
     * Chooses the attenuation of the measurements started afterwards.
     *
     * @param attenuation 0 to 6, in steps of 5 dB, or -1 for automatic
     */
    public synchronized void setAttenuation(int attenuation) {
        ensureOpen();
        if (attenuation < -1 || attenuation > 6) {
            throw new IllegalArgumentException("attenuation must be -1 to 6: " + attenuation);
        }
        this.attenuation = attenuation;
    }

    /**
     * Enables or disables the RF preselector of an SP145B in all measurement
     * modes.
     *
     * @throws UnsupportedOperationException when the library lacks
     *         {@code spSetPreselector}, as the Linux x86-64 and macOS builds do
     */
    public synchronized void setPreselector(boolean enabled) throws SignalHoundException {
        ensureOpen();
        SpNative.spSetPreselector.check(handle, enabled ? SpNative.SP_TRUE : SpNative.SP_FALSE);
    }

    /** Uses an external 10 MHz reference, or the internal one. */
    public synchronized void setExternalReference(boolean external) throws SignalHoundException {
        ensureOpen();
        SpNative.spSetReference.check(handle, external ? 1 : 0);
    }

    @Override
    protected IqStreamInfo configureIq(IqConfig config) throws SignalHoundException {
        SpNative.spSetRefLevel.check(handle, config.referenceLevel());
        SpNative.spSetAttenuator.check(handle, attenuation);
        SpNative.spSetIQDataType.check(handle, config.dataType().code());
        SpNative.spSetIQCenterFreq.check(handle, config.centerFrequency());
        SpNative.spSetIQSampleRate.check(handle, config.decimation());
        SpNative.spSetIQSoftwareFilter.check(handle, SpNative.SP_TRUE);
        SpNative.spSetIQBandwidth.check(handle, config.bandwidth());
        SpNative.spConfigure.check(handle, SpNative.SP_MODE_IQ_STREAMING);
        SpNative.spGetIQParameters.check(handle, slot(0), slot(1));
        return new IqStreamInfo(doubleSlot(0), doubleSlot(1), config.dataType());
    }

    @Override
    protected IqBlock readIqBlock(IqBuffer buffer, boolean purge) throws SignalHoundException {
        int status = SpNative.spGetIQ.check(handle, buffer.segment(), buffer.capacity(), MemorySegment.NULL, 0,
            slot(0), purge ? SpNative.SP_TRUE : SpNative.SP_FALSE, slot(1), slot(2));
        return new IqBlock(buffer.capacity(), longSlot(0), intSlot(1) != 0, intSlot(2), warning(status));
    }

    @Override
    protected float readIqCorrection() throws SignalHoundException {
        SpNative.spGetIQCorrection.check(handle, slot(0));
        return floatSlot(0);
    }

    @Override
    protected SweepInfo configureSweep(SweepConfig config) throws SignalHoundException {
        SpNative.spSetRefLevel.check(handle, config.referenceLevel());
        SpNative.spSetAttenuator.check(handle, attenuation);
        SpNative.spSetSweepCenterSpan.check(handle, config.centerFrequency(), config.span());
        SpNative.spSetSweepCoupling.check(handle, config.rbw(), config.vbw(), config.sweepTime());
        SpNative.spSetSweepDetector.check(handle, SpNative.SP_DETECTOR_MIN_MAX, SpNative.SP_VIDEO_LOG);
        SpNative.spSetSweepScale.check(handle, SpNative.SP_SCALE_LOG);
        SpNative.spSetSweepWindow.check(handle, SpNative.SP_WINDOW_FLAT_TOP);
        SpNative.spConfigure.check(handle, SpNative.SP_MODE_SWEEPING);
        SpNative.spGetSweepParameters.check(handle, slot(0), slot(1), slot(2), slot(3), slot(4));
        return new SweepInfo(doubleSlot(2), doubleSlot(3), intSlot(4), doubleSlot(0), doubleSlot(1));
    }

    @Override
    protected int fetchSweep(int bins, MemorySegment min, MemorySegment max, long[] timestamp)
        throws SignalHoundException {
        int status = SpNative.spGetSweep.check(handle, min, max, slot(0));
        timestamp[0] = longSlot(0);
        return status;
    }

    @Override
    protected void abort() throws SignalHoundException {
        SpNative.spAbort.check(handle);
    }

    @Override
    protected void closeDevice() {
        SpNative.spCloseDevice.call(handle);
    }
}
