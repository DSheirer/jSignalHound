package io.github.dsheirer.signalhound.sm;

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
import io.github.dsheirer.signalhound.internal.SmNative;
import java.lang.foreign.MemorySegment;

/**
 * An open SM200 or SM435. Obtain one from {@link Sm}.
 *
 * <p>Attenuation is automatic, set from the reference level, unless
 * {@link #setAttenuation(int)} chooses it. Attenuation and the preselector
 * apply to the measurements started afterwards.
 */
public final class SmDevice extends AbstractDevice {
    private final SmModel smModel;
    private int attenuation = SmNative.SM_AUTO_ATTEN;

    SmDevice(int handle, int serialNumber, SmModel model) {
        super(SignalHoundApi.SM, handle, serialNumber, model.name());
        this.smModel = model;
    }

    /** The model of the device. */
    public SmModel smModel() {
        return smModel;
    }

    @Override
    public synchronized String firmwareVersion() throws SignalHoundException {
        ensureOpen();
        SmNative.smGetFirmwareVersion.check(handle, slot(0), slot(1), slot(2));
        return intSlot(0) + "." + intSlot(1) + "." + intSlot(2);
    }

    @Override
    public synchronized Diagnostics diagnostics() throws SignalHoundException {
        ensureOpen();
        SmNative.smGetDeviceDiagnostics.check(handle, slot(0), slot(1), slot(2));
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

    /** Enables or disables the RF preselector, which works up to 645 MHz, in all measurement modes. */
    public synchronized void setPreselector(boolean enabled) throws SignalHoundException {
        ensureOpen();
        SmNative.smSetPreselector.check(handle, enabled ? SmNative.SM_TRUE : SmNative.SM_FALSE);
    }

    /** Uses an external 10 MHz reference, or the internal one. */
    public synchronized void setExternalReference(boolean external) throws SignalHoundException {
        ensureOpen();
        SmNative.smSetReference.check(handle, external ? 1 : 0);
    }

    @Override
    protected IqStreamInfo configureIq(IqConfig config) throws SignalHoundException {
        SmNative.smSetRefLevel.check(handle, config.referenceLevel());
        SmNative.smSetAttenuator.check(handle, attenuation);
        // Older builds of the library lack the LTE base rate and this function.
        if (SmNative.smSetIQBaseSampleRate.isAvailable()) {
            SmNative.smSetIQBaseSampleRate.check(handle, SmNative.SM_IQ_STREAM_SAMPLE_RATE_NATIVE);
        }
        SmNative.smSetIQDataType.check(handle, config.dataType().code());
        SmNative.smSetIQCenterFreq.check(handle, config.centerFrequency());
        SmNative.smSetIQSampleRate.check(handle, config.decimation());
        SmNative.smSetIQBandwidth.check(handle, SmNative.SM_TRUE, config.bandwidth());
        SmNative.smConfigure.check(handle, SmNative.SM_MODE_IQ_STREAMING);
        SmNative.smGetIQParameters.check(handle, slot(0), slot(1));
        return new IqStreamInfo(doubleSlot(0), doubleSlot(1), config.dataType());
    }

    @Override
    protected IqBlock readIqBlock(IqBuffer buffer, boolean purge) throws SignalHoundException {
        int status = SmNative.smGetIQ.check(handle, buffer.segment(), buffer.capacity(), MemorySegment.NULL, 0,
            slot(0), purge ? SmNative.SM_TRUE : SmNative.SM_FALSE, slot(1), slot(2));
        return new IqBlock(buffer.capacity(), longSlot(0), intSlot(1) != 0, intSlot(2), warning(status));
    }

    @Override
    protected float readIqCorrection() throws SignalHoundException {
        SmNative.smGetIQCorrection.check(handle, slot(0));
        return floatSlot(0);
    }

    @Override
    protected SweepInfo configureSweep(SweepConfig config) throws SignalHoundException {
        SmNative.smSetRefLevel.check(handle, config.referenceLevel());
        SmNative.smSetAttenuator.check(handle, attenuation);
        SmNative.smSetSweepSpeed.check(handle, SmNative.SM_SWEEP_SPEED_AUTO);
        SmNative.smSetSweepCenterSpan.check(handle, config.centerFrequency(), config.span());
        SmNative.smSetSweepCoupling.check(handle, config.rbw(), config.vbw(), config.sweepTime());
        SmNative.smSetSweepDetector.check(handle, SmNative.SM_DETECTOR_MIN_MAX, SmNative.SM_VIDEO_LOG);
        SmNative.smSetSweepScale.check(handle, SmNative.SM_SCALE_LOG);
        SmNative.smSetSweepWindow.check(handle, SmNative.SM_WINDOW_FLAT_TOP);
        SmNative.smConfigure.check(handle, SmNative.SM_MODE_SWEEPING);
        SmNative.smGetSweepParameters.check(handle, slot(0), slot(1), slot(2), slot(3), slot(4));
        return new SweepInfo(doubleSlot(2), doubleSlot(3), intSlot(4), doubleSlot(0), doubleSlot(1));
    }

    @Override
    protected int fetchSweep(int bins, MemorySegment min, MemorySegment max, long[] timestamp)
        throws SignalHoundException {
        int status = SmNative.smGetSweep.check(handle, min, max, slot(0));
        timestamp[0] = longSlot(0);
        return status;
    }

    @Override
    protected void abort() throws SignalHoundException {
        SmNative.smAbort.check(handle);
    }

    @Override
    protected void closeDevice() {
        SmNative.smCloseDevice.call(handle);
    }
}
