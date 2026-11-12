package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

/**
 * Bindings for every function of {@code bb_api.h}, the BB60 API, except the
 * three that the header lists as deprecated ({@code bbConfigureLevel},
 * {@code bbConfigureGain} and {@code bbQueryStreamInfo}). Fields are named
 * after the C functions. C enums and {@code uint32_t} map to {@code int}, C++
 * {@code bool} to {@code boolean} and {@code uint8_t} to {@code byte}.
 * {@code native/check-bindings.sh} compares this list against the header.
 */
public final class BbNative {
    private static final SignalHoundApi API = SignalHoundApi.BB;

    public static final int BB_MAX_DEVICES = 8;
    public static final int BB_AUTO_ATTEN = -1;
    public static final int BB_AUTO_GAIN = -1;
    public static final int BB_SWEEPING = 0;
    public static final int BB_REAL_TIME = 1;
    public static final int BB_STREAMING = 4;
    public static final int BB_AUDIO_DEMOD = 7;
    public static final int BB_TG_SWEEPING = 8;
    public static final int BB_STREAM_IQ = 0x0;
    public static final int BB_NO_SPUR_REJECT = 0;
    public static final int BB_SPUR_REJECT = 1;
    public static final int BB_LOG_SCALE = 0;
    public static final int BB_RBW_SHAPE_NUTTALL = 0;
    public static final int BB_RBW_SHAPE_FLATTOP = 1;
    public static final int BB_MIN_AND_MAX = 0;
    public static final int BB_AVERAGE = 1;
    public static final int BB_LOG = 0;

    /** {@code bbIQPacket}, the argument of {@link #bbGetIQ}. */
    public static final StructLayout BB_IQ_PACKET = MemoryLayout.structLayout(
        ADDRESS.withName("iqData"),
        JAVA_INT.withName("iqCount"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("triggers"),
        JAVA_INT.withName("triggerCount"),
        JAVA_INT.withName("purge"),
        JAVA_INT.withName("dataRemaining"),
        JAVA_INT.withName("sampleLoss"),
        JAVA_INT.withName("sec"),
        JAVA_INT.withName("nano"));

    // Devices
    public static final NativeFunction bbGetSerialNumberList = status("bbGetSerialNumberList", ADDRESS, ADDRESS);
    public static final NativeFunction bbGetSerialNumberList2 =
        status("bbGetSerialNumberList2", ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction bbOpenDevice = status("bbOpenDevice", ADDRESS);
    public static final NativeFunction bbOpenDeviceBySerialNumber =
        status("bbOpenDeviceBySerialNumber", ADDRESS, JAVA_INT);
    public static final NativeFunction bbCloseDevice = status("bbCloseDevice", JAVA_INT);
    public static final NativeFunction bbSetPowerState = status("bbSetPowerState", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbGetPowerState = status("bbGetPowerState", JAVA_INT, ADDRESS);
    public static final NativeFunction bbPreset = status("bbPreset", JAVA_INT);
    public static final NativeFunction bbPresetFull = status("bbPresetFull", ADDRESS);
    public static final NativeFunction bbSelfCal = status("bbSelfCal", JAVA_INT);
    public static final NativeFunction bbGetSerialNumber = status("bbGetSerialNumber", JAVA_INT, ADDRESS);
    public static final NativeFunction bbGetDeviceType = status("bbGetDeviceType", JAVA_INT, ADDRESS);
    public static final NativeFunction bbGetFirmwareVersion = status("bbGetFirmwareVersion", JAVA_INT, ADDRESS);
    public static final NativeFunction bbGetDeviceDiagnostics =
        status("bbGetDeviceDiagnostics", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    // I/O ports, GPS and UART
    public static final NativeFunction bbConfigureIO = status("bbConfigureIO", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction bbSyncCPUtoGPS = status("bbSyncCPUtoGPS", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbSetUARTRate = status("bbSetUARTRate", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbEnableUARTSweeping =
        status("bbEnableUARTSweeping", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    public static final NativeFunction bbDisableUARTSweeping = status("bbDisableUARTSweeping", JAVA_INT);
    public static final NativeFunction bbEnableUARTStreaming =
        status("bbEnableUARTStreaming", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    public static final NativeFunction bbDisableUARTStreaming = status("bbDisableUARTStreaming", JAVA_INT);
    public static final NativeFunction bbWriteUARTImm = status("bbWriteUARTImm", JAVA_INT, JAVA_BYTE);

    // Configuration
    public static final NativeFunction bbConfigureRefLevel = status("bbConfigureRefLevel", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction bbConfigureGainAtten =
        status("bbConfigureGainAtten", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction bbConfigureCenterSpan =
        status("bbConfigureCenterSpan", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction bbConfigureSweepCoupling = status("bbConfigureSweepCoupling",
        JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_INT, JAVA_INT);
    public static final NativeFunction bbConfigureAcquisition =
        status("bbConfigureAcquisition", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction bbConfigureProcUnits = status("bbConfigureProcUnits", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbConfigureRealTime =
        status("bbConfigureRealTime", JAVA_INT, JAVA_DOUBLE, JAVA_INT);
    public static final NativeFunction bbConfigureRealTimeOverlap =
        status("bbConfigureRealTimeOverlap", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction bbConfigureIQCenter = status("bbConfigureIQCenter", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction bbConfigureIQ = status("bbConfigureIQ", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction bbConfigureIQDataType = status("bbConfigureIQDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbConfigureIQTriggerSentinel = status("bbConfigureIQTriggerSentinel", JAVA_INT);
    public static final NativeFunction bbConfigureDemod = status("bbConfigureDemod",
        JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT);

    // Measurements
    public static final NativeFunction bbInitiate = status("bbInitiate", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction bbAbort = status("bbAbort", JAVA_INT);
    public static final NativeFunction bbQueryTraceInfo =
        status("bbQueryTraceInfo", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction bbQueryRealTimeInfo = status("bbQueryRealTimeInfo", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction bbQueryRealTimePoi = status("bbQueryRealTimePoi", JAVA_INT, ADDRESS);
    public static final NativeFunction bbQueryIQParameters = status("bbQueryIQParameters", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction bbGetIQCorrection = status("bbGetIQCorrection", JAVA_INT, ADDRESS);
    public static final NativeFunction bbFetchTrace_32f =
        status("bbFetchTrace_32f", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction bbFetchTrace = status("bbFetchTrace", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction bbFetchRealTimeFrame =
        status("bbFetchRealTimeFrame", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction bbGetIQ = status("bbGetIQ", JAVA_INT, ADDRESS);
    public static final NativeFunction bbGetIQUnpacked = status("bbGetIQUnpacked", JAVA_INT, ADDRESS, JAVA_INT,
        ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction bbFetchAudio = status("bbFetchAudio", JAVA_INT, ADDRESS);

    // Tracking generator
    public static final NativeFunction bbAttachTg = status("bbAttachTg", JAVA_INT);
    public static final NativeFunction bbIsTgAttached = status("bbIsTgAttached", JAVA_INT, ADDRESS);
    public static final NativeFunction bbConfigTgSweep =
        status("bbConfigTgSweep", JAVA_INT, JAVA_INT, JAVA_BOOLEAN, JAVA_BOOLEAN);
    public static final NativeFunction bbStoreTgThru = status("bbStoreTgThru", JAVA_INT, JAVA_INT);
    public static final NativeFunction bbSetTg = status("bbSetTg", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction bbGetTgFreqAmpl = status("bbGetTgFreqAmpl", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction bbSetTgReference = status("bbSetTgReference", JAVA_INT, JAVA_INT);

    // Library
    public static final NativeFunction bbGetAPIVersion = NativeFunction.returning(API, "bbGetAPIVersion", ADDRESS);
    public static final NativeFunction bbGetProductID = NativeFunction.returning(API, "bbGetProductID", ADDRESS);
    public static final NativeFunction bbGetErrorString =
        NativeFunction.returning(API, "bbGetErrorString", ADDRESS, JAVA_INT);

    private BbNative() {
    }

    private static NativeFunction status(String name, MemoryLayout... arguments) {
        return NativeFunction.status(API, name, arguments);
    }
}
