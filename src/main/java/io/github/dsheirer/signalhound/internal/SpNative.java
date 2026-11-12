package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.lang.foreign.MemoryLayout;

/**
 * Bindings for every function of {@code sp_api.h}, the SP145 API. Fields are
 * named after the C functions. C enums and {@code uint32_t} map to {@code int}
 * and {@code uint8_t} to {@code byte}.
 *
 * <p>The SDK ships two versions of the header: the Windows and Linux aarch64
 * libraries add {@code spGetDeviceType}, {@code spSetPreselector} and
 * {@code spGetPreselector}, which the Linux x86-64 and macOS ones lack. These
 * bindings follow the newer header. {@code native/check-bindings.sh} compares
 * this list against it.
 */
public final class SpNative {
    private static final SignalHoundApi API = SignalHoundApi.SP;

    public static final int SP_MAX_DEVICES = 9;
    public static final int SP_AUTO_ATTEN = -1;
    public static final int SP_FALSE = 0;
    public static final int SP_TRUE = 1;
    public static final int SP_MODE_IDLE = 0;
    public static final int SP_MODE_SWEEPING = 1;
    public static final int SP_MODE_IQ_STREAMING = 3;
    public static final int SP_DATA_TYPE_32FC = 0;
    public static final int SP_DATA_TYPE_16SC = 1;
    public static final int SP_DETECTOR_MIN_MAX = 1;
    public static final int SP_VIDEO_LOG = 0;
    public static final int SP_SCALE_LOG = 0;
    public static final int SP_WINDOW_FLAT_TOP = 0;

    // Devices
    public static final NativeFunction spGetDeviceList = status("spGetDeviceList", ADDRESS, ADDRESS);
    public static final NativeFunction spOpenDevice = status("spOpenDevice", ADDRESS);
    public static final NativeFunction spOpenDeviceBySerial = status("spOpenDeviceBySerial", ADDRESS, JAVA_INT);
    public static final NativeFunction spCloseDevice = status("spCloseDevice", JAVA_INT);
    public static final NativeFunction spPresetDevice = status("spPresetDevice", JAVA_INT);
    public static final NativeFunction spSetPowerState = status("spSetPowerState", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetPowerState = status("spGetPowerState", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetDeviceType = status("spGetDeviceType", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetSerialNumber = status("spGetSerialNumber", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetFirmwareVersion =
        status("spGetFirmwareVersion", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetDeviceDiagnostics =
        status("spGetDeviceDiagnostics", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetCalDate = status("spGetCalDate", JAVA_INT, ADDRESS);

    // Reference, GPIO and UART
    public static final NativeFunction spSetReference = status("spSetReference", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetReference = status("spGetReference", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetGPIOPort = status("spSetGPIOPort", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetGPIOPort = status("spGetGPIOPort", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetUARTBaudRate = status("spSetUARTBaudRate", JAVA_INT, JAVA_FLOAT);
    public static final NativeFunction spGetUARTBaudRate = status("spGetUARTBaudRate", JAVA_INT, ADDRESS);
    public static final NativeFunction spWriteUARTDirect = status("spWriteUARTDirect", JAVA_INT, JAVA_BYTE);

    // GPS
    public static final NativeFunction spSetGPSTimebaseUpdate = status("spSetGPSTimebaseUpdate", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetGPSTimebaseUpdate = status("spGetGPSTimebaseUpdate", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetGPSHoldoverInfo =
        status("spGetGPSHoldoverInfo", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spGetGPSState = status("spGetGPSState", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetGPSInfo = status("spGetGPSInfo", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS,
        ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spWriteToGPS = status("spWriteToGPS", JAVA_INT, ADDRESS, JAVA_INT);
    public static final NativeFunction spSetGPSPlatformModel = status("spSetGPSPlatformModel", JAVA_INT, JAVA_INT);

    // Front end
    public static final NativeFunction spSetRefLevel = status("spSetRefLevel", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spGetRefLevel = status("spGetRefLevel", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetAttenuator = status("spSetAttenuator", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetAttenuator = status("spGetAttenuator", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetPreselector = status("spSetPreselector", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetPreselector = status("spGetPreselector", JAVA_INT, ADDRESS);

    // Sweeps
    public static final NativeFunction spSetSweepCenterSpan =
        status("spSetSweepCenterSpan", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetSweepStartStop =
        status("spSetSweepStartStop", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetSweepCoupling =
        status("spSetSweepCoupling", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetSweepDetector = status("spSetSweepDetector", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetSweepScale = status("spSetSweepScale", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetSweepWindow = status("spSetSweepWindow", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetSweepGPIOSwitching =
        status("spSetSweepGPIOSwitching", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    public static final NativeFunction spSetSweepGPIOSwitchingDisabled =
        status("spSetSweepGPIOSwitchingDisabled", JAVA_INT);

    // Real-time spectrum analysis
    public static final NativeFunction spSetRealTimeCenterSpan =
        status("spSetRealTimeCenterSpan", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetRealTimeRBW = status("spSetRealTimeRBW", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spSetRealTimeDetector = status("spSetRealTimeDetector", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetRealTimeScale =
        status("spSetRealTimeScale", JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetRealTimeWindow = status("spSetRealTimeWindow", JAVA_INT, JAVA_INT);

    // I/Q streaming
    public static final NativeFunction spSetIQDataType = status("spSetIQDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQCenterFreq = status("spSetIQCenterFreq", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spGetIQCenterFreq = status("spGetIQCenterFreq", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetIQSampleRate = status("spSetIQSampleRate", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQSoftwareFilter = status("spSetIQSoftwareFilter", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQBandwidth = status("spSetIQBandwidth", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spSetIQExtTriggerEdge = status("spSetIQExtTriggerEdge", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQTriggerSentinel = status("spSetIQTriggerSentinel", JAVA_DOUBLE);
    public static final NativeFunction spSetIQQueueSize = status("spSetIQQueueSize", JAVA_INT, JAVA_INT);

    // I/Q sweep list
    public static final NativeFunction spSetIQSweepListDataType =
        status("spSetIQSweepListDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQSweepListCorrected =
        status("spSetIQSweepListCorrected", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQSweepListSteps = status("spSetIQSweepListSteps", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetIQSweepListSteps = status("spGetIQSweepListSteps", JAVA_INT, ADDRESS);
    public static final NativeFunction spSetIQSweepListFreq =
        status("spSetIQSweepListFreq", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spSetIQSweepListRef =
        status("spSetIQSweepListRef", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spSetIQSweepListAtten =
        status("spSetIQSweepListAtten", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetIQSweepListSampleCount =
        status("spSetIQSweepListSampleCount", JAVA_INT, JAVA_INT, JAVA_INT);

    // Audio
    public static final NativeFunction spSetAudioCenterFreq = status("spSetAudioCenterFreq", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction spSetAudioType = status("spSetAudioType", JAVA_INT, JAVA_INT);
    public static final NativeFunction spSetAudioFilters =
        status("spSetAudioFilters", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction spSetAudioFMDeemphasis =
        status("spSetAudioFMDeemphasis", JAVA_INT, JAVA_DOUBLE);

    // Measurements
    public static final NativeFunction spConfigure = status("spConfigure", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetCurrentMode = status("spGetCurrentMode", JAVA_INT, ADDRESS);
    public static final NativeFunction spAbort = status("spAbort", JAVA_INT);
    public static final NativeFunction spGetSweepParameters =
        status("spGetSweepParameters", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetRealTimeParameters = status("spGetRealTimeParameters",
        JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetIQParameters = status("spGetIQParameters", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spGetIQCorrection = status("spGetIQCorrection", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetIQThreads = status("spGetIQThreads", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spIQSweepListGetCorrections =
        status("spIQSweepListGetCorrections", JAVA_INT, ADDRESS);
    public static final NativeFunction spGetSweep = status("spGetSweep", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetRealTimeFrame = status("spGetRealTimeFrame",
        JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction spGetIQ = status("spGetIQ", JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
        ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spIQSweepListGetSweep =
        status("spIQSweepListGetSweep", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spIQSweepListStartSweep =
        status("spIQSweepListStartSweep", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spIQSweepListFinishSweep =
        status("spIQSweepListFinishSweep", JAVA_INT, JAVA_INT);
    public static final NativeFunction spGetAudio = status("spGetAudio", JAVA_INT, ADDRESS);

    // Fan and reset
    public static final NativeFunction spSetFanSetPoint = status("spSetFanSetPoint", JAVA_INT, JAVA_FLOAT);
    public static final NativeFunction spGetFanSettings = status("spGetFanSettings", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction spSetDeviceResetTimeout = status("spSetDeviceResetTimeout", JAVA_INT);

    // Library
    public static final NativeFunction spGetErrorString =
        NativeFunction.returning(API, "spGetErrorString", ADDRESS, JAVA_INT);
    public static final NativeFunction spGetAPIVersion = NativeFunction.returning(API, "spGetAPIVersion", ADDRESS);

    private SpNative() {
    }

    private static NativeFunction status(String name, MemoryLayout... arguments) {
        return NativeFunction.status(API, name, arguments);
    }
}
