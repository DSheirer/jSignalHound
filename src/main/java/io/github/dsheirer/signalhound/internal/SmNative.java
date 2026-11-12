package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

/**
 * Bindings for every function of {@code sm_api.h}, the SM200 and SM435 API,
 * except the deprecated {@code smSetIQUSBQueueSize}. Fields are named after the
 * C functions. C enums and {@code uint32_t} map to {@code int},
 * {@code uint16_t} to {@code short}, {@code uint8_t} to {@code byte} and C++
 * {@code bool} to {@code boolean}. {@code native/check-bindings.sh} compares
 * this list against the header.
 */
public final class SmNative {
    private static final SignalHoundApi API = SignalHoundApi.SM;

    public static final int SM_MAX_DEVICES = 16;
    public static final int SM_AUTO_ATTEN = -1;
    public static final int SM_FALSE = 0;
    public static final int SM_TRUE = 1;
    public static final int SM_MODE_IDLE = 0;
    public static final int SM_MODE_SWEEPING = 1;
    public static final int SM_MODE_IQ_STREAMING = 3;
    public static final int SM_DATA_TYPE_32FC = 0;
    public static final int SM_DATA_TYPE_16SC = 1;
    public static final int SM_IQ_STREAM_SAMPLE_RATE_NATIVE = 0;
    public static final int SM_SWEEP_SPEED_AUTO = 0;
    public static final int SM_DETECTOR_MIN_MAX = 1;
    public static final int SM_VIDEO_LOG = 0;
    public static final int SM_SCALE_LOG = 0;
    public static final int SM_WINDOW_FLAT_TOP = 0;

    /** {@code SmGPIOStep}, the element of the {@link #smSetGPIOSweep} array. */
    public static final StructLayout SM_GPIO_STEP = MemoryLayout.structLayout(
        JAVA_DOUBLE.withName("freq"),
        JAVA_BYTE.withName("mask"),
        MemoryLayout.paddingLayout(7));

    /** {@code SmDeviceDiagnostics}, the argument of {@link #smGetFullDeviceDiagnostics}. */
    public static final StructLayout SM_DEVICE_DIAGNOSTICS = MemoryLayout.structLayout(
        JAVA_FLOAT.withName("voltage"),
        JAVA_FLOAT.withName("currentInput"),
        JAVA_FLOAT.withName("currentOCXO"),
        JAVA_FLOAT.withName("current58"),
        JAVA_FLOAT.withName("tempFPGAInternal"),
        JAVA_FLOAT.withName("tempFPGANear"),
        JAVA_FLOAT.withName("tempOCXO"),
        JAVA_FLOAT.withName("tempVCO"),
        JAVA_FLOAT.withName("tempRFBoardLO"),
        JAVA_FLOAT.withName("tempPowerSupply"));

    // Devices
    public static final NativeFunction smGetDeviceList = status("smGetDeviceList", ADDRESS, ADDRESS);
    public static final NativeFunction smGetDeviceList2 = status("smGetDeviceList2", ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smOpenDevice = status("smOpenDevice", ADDRESS);
    public static final NativeFunction smOpenDeviceBySerial = status("smOpenDeviceBySerial", ADDRESS, JAVA_INT);
    public static final NativeFunction smOpenNetworkedDevice =
        status("smOpenNetworkedDevice", ADDRESS, ADDRESS, ADDRESS, JAVA_SHORT);
    public static final NativeFunction smCloseDevice = status("smCloseDevice", JAVA_INT);
    public static final NativeFunction smPreset = status("smPreset", JAVA_INT);
    public static final NativeFunction smPresetSerial = status("smPresetSerial", JAVA_INT);
    public static final NativeFunction smNetworkedSpeedTest =
        status("smNetworkedSpeedTest", JAVA_INT, JAVA_DOUBLE, ADDRESS);
    public static final NativeFunction smGetDeviceInfo = status("smGetDeviceInfo", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smGetFirmwareVersion =
        status("smGetFirmwareVersion", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smHasIFOutput = status("smHasIFOutput", JAVA_INT, ADDRESS);
    public static final NativeFunction smGetDeviceDiagnostics =
        status("smGetDeviceDiagnostics", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smGetFullDeviceDiagnostics =
        status("smGetFullDeviceDiagnostics", JAVA_INT, ADDRESS);
    public static final NativeFunction smGetSFPDiagnostics =
        status("smGetSFPDiagnostics", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smSetPowerState = status("smSetPowerState", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetPowerState = status("smGetPowerState", JAVA_INT, ADDRESS);

    // Front end and reference
    public static final NativeFunction smSetAttenuator = status("smSetAttenuator", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetAttenuator = status("smGetAttenuator", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetRefLevel = status("smSetRefLevel", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smGetRefLevel = status("smGetRefLevel", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetPreselector = status("smSetPreselector", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetPreselector = status("smGetPreselector", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetExternalReference = status("smSetExternalReference", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetExternalReference = status("smGetExternalReference", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetReference = status("smSetReference", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetReference = status("smGetReference", JAVA_INT, ADDRESS);

    // GPIO and SPI
    public static final NativeFunction smSetGPIOState = status("smSetGPIOState", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetGPIOState = status("smGetGPIOState", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smWriteGPIOImm = status("smWriteGPIOImm", JAVA_INT, JAVA_BYTE);
    public static final NativeFunction smReadGPIOImm = status("smReadGPIOImm", JAVA_INT, ADDRESS);
    public static final NativeFunction smWriteSPI = status("smWriteSPI", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetGPIOSweepDisabled = status("smSetGPIOSweepDisabled", JAVA_INT);
    public static final NativeFunction smSetGPIOSweep = status("smSetGPIOSweep", JAVA_INT, ADDRESS, JAVA_INT);
    public static final NativeFunction smSetGPIOSwitchingDisabled = status("smSetGPIOSwitchingDisabled", JAVA_INT);
    public static final NativeFunction smSetGPIOSwitching =
        status("smSetGPIOSwitching", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);

    // GPS
    public static final NativeFunction smSetGPSTimebaseUpdate = status("smSetGPSTimebaseUpdate", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetGPSTimebaseUpdate = status("smGetGPSTimebaseUpdate", JAVA_INT, ADDRESS);
    public static final NativeFunction smGetGPSHoldoverInfo =
        status("smGetGPSHoldoverInfo", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smGetGPSState = status("smGetGPSState", JAVA_INT, ADDRESS);
    public static final NativeFunction smGetGPSInfo = status("smGetGPSInfo", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS,
        ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smWriteToGPS = status("smWriteToGPS", JAVA_INT, ADDRESS, JAVA_INT);
    public static final NativeFunction smSetGPSPlatformModel = status("smSetGPSPlatformModel", JAVA_INT, JAVA_INT);

    // Sweeps
    public static final NativeFunction smSetSweepSpeed = status("smSetSweepSpeed", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSweepCenterSpan =
        status("smSetSweepCenterSpan", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetSweepStartStop =
        status("smSetSweepStartStop", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetSweepCoupling =
        status("smSetSweepCoupling", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetSweepDetector = status("smSetSweepDetector", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSweepScale = status("smSetSweepScale", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSweepWindow = status("smSetSweepWindow", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSweepSpurReject = status("smSetSweepSpurReject", JAVA_INT, JAVA_INT);

    // Real-time spectrum analysis
    public static final NativeFunction smSetRealTimeCenterSpan =
        status("smSetRealTimeCenterSpan", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetRealTimeRBW = status("smSetRealTimeRBW", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetRealTimeDetector = status("smSetRealTimeDetector", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetRealTimeScale =
        status("smSetRealTimeScale", JAVA_INT, JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetRealTimeWindow = status("smSetRealTimeWindow", JAVA_INT, JAVA_INT);

    // I/Q streaming
    public static final NativeFunction smSetIQBaseSampleRate = status("smSetIQBaseSampleRate", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQDataType = status("smSetIQDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQCenterFreq = status("smSetIQCenterFreq", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smGetIQCenterFreq = status("smGetIQCenterFreq", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetIQSampleRate = status("smSetIQSampleRate", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQBandwidth =
        status("smSetIQBandwidth", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetIQExtTriggerEdge = status("smSetIQExtTriggerEdge", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQTriggerSentinel = status("smSetIQTriggerSentinel", JAVA_DOUBLE);
    public static final NativeFunction smSetIQQueueSize = status("smSetIQQueueSize", JAVA_INT, JAVA_FLOAT);

    // I/Q sweep list
    public static final NativeFunction smSetIQSweepListDataType =
        status("smSetIQSweepListDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQSweepListCorrected =
        status("smSetIQSweepListCorrected", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQSweepListSteps = status("smSetIQSweepListSteps", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetIQSweepListSteps = status("smGetIQSweepListSteps", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetIQSweepListFreq =
        status("smSetIQSweepListFreq", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetIQSweepListRef =
        status("smSetIQSweepListRef", JAVA_INT, JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetIQSweepListAtten =
        status("smSetIQSweepListAtten", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQSweepListSampleCount =
        status("smSetIQSweepListSampleCount", JAVA_INT, JAVA_INT, JAVA_INT);

    // Segmented I/Q capture
    public static final NativeFunction smSetSegIQDataType = status("smSetSegIQDataType", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSegIQCenterFreq = status("smSetSegIQCenterFreq", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetSegIQVideoTrigger =
        status("smSetSegIQVideoTrigger", JAVA_INT, JAVA_DOUBLE, JAVA_INT);
    public static final NativeFunction smSetSegIQExtTrigger = status("smSetSegIQExtTrigger", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSegIQFMTParams =
        status("smSetSegIQFMTParams", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    public static final NativeFunction smSetSegIQSegmentCount = status("smSetSegIQSegmentCount", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetSegIQSegment =
        status("smSetSegIQSegment", JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_DOUBLE);

    // Audio
    public static final NativeFunction smSetAudioCenterFreq = status("smSetAudioCenterFreq", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetAudioType = status("smSetAudioType", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetAudioFilters =
        status("smSetAudioFilters", JAVA_INT, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE);
    public static final NativeFunction smSetAudioFMDeemphasis =
        status("smSetAudioFMDeemphasis", JAVA_INT, JAVA_DOUBLE);

    // Measurements
    public static final NativeFunction smConfigure = status("smConfigure", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetCurrentMode = status("smGetCurrentMode", JAVA_INT, ADDRESS);
    public static final NativeFunction smAbort = status("smAbort", JAVA_INT);
    public static final NativeFunction smGetSweepParameters =
        status("smGetSweepParameters", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smGetRealTimeParameters = status("smGetRealTimeParameters",
        JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smGetIQParameters = status("smGetIQParameters", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smGetIQCorrection = status("smGetIQCorrection", JAVA_INT, ADDRESS);
    public static final NativeFunction smIQSweepListGetCorrections =
        status("smIQSweepListGetCorrections", JAVA_INT, ADDRESS);
    public static final NativeFunction smSegIQGetMaxCaptures = status("smSegIQGetMaxCaptures", JAVA_INT, ADDRESS);
    public static final NativeFunction smGetSweep = status("smGetSweep", JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smSetSweepGPIO = status("smSetSweepGPIO", JAVA_INT, JAVA_INT, JAVA_BYTE);
    public static final NativeFunction smStartSweep = status("smStartSweep", JAVA_INT, JAVA_INT);
    public static final NativeFunction smFinishSweep =
        status("smFinishSweep", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smGetRealTimeFrame = status("smGetRealTimeFrame",
        JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final NativeFunction smGetIQ = status("smGetIQ", JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
        ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smIQSweepListGetSweep =
        status("smIQSweepListGetSweep", JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smIQSweepListStartSweep =
        status("smIQSweepListStartSweep", JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smIQSweepListFinishSweep =
        status("smIQSweepListFinishSweep", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSegIQCaptureStart = status("smSegIQCaptureStart", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSegIQCaptureWait = status("smSegIQCaptureWait", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSegIQCaptureWaitAsync =
        status("smSegIQCaptureWaitAsync", JAVA_INT, JAVA_INT, ADDRESS);
    public static final NativeFunction smSegIQCaptureTimeout =
        status("smSegIQCaptureTimeout", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    public static final NativeFunction smSegIQCaptureTime =
        status("smSegIQCaptureTime", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    public static final NativeFunction smSegIQCaptureRead =
        status("smSegIQCaptureRead", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
    public static final NativeFunction smSegIQCaptureFinish = status("smSegIQCaptureFinish", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSegIQCaptureFull = status("smSegIQCaptureFull",
        JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);
    public static final NativeFunction smSegIQLTEResample =
        status("smSegIQLTEResample", ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_BOOLEAN);

    // Full band I/Q
    public static final NativeFunction smSetIQFullBandAtten = status("smSetIQFullBandAtten", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQFullBandCorrected =
        status("smSetIQFullBandCorrected", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQFullBandSamples = status("smSetIQFullBandSamples", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQFullBandTriggerType =
        status("smSetIQFullBandTriggerType", JAVA_INT, JAVA_INT);
    public static final NativeFunction smSetIQFullBandVideoTrigger =
        status("smSetIQFullBandVideoTrigger", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smSetIQFullBandTriggerTimeout =
        status("smSetIQFullBandTriggerTimeout", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smGetIQFullBand = status("smGetIQFullBand", JAVA_INT, ADDRESS, JAVA_INT);
    public static final NativeFunction smGetIQFullBandSweep =
        status("smGetIQFullBandSweep", JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetAudio = status("smGetAudio", JAVA_INT, ADDRESS);

    // Fan, IF output and calibration
    public static final NativeFunction smSetFanThreshold = status("smSetFanThreshold", JAVA_INT, JAVA_INT);
    public static final NativeFunction smGetFanThreshold = status("smGetFanThreshold", JAVA_INT, ADDRESS);
    public static final NativeFunction smSetIFOutput = status("smSetIFOutput", JAVA_INT, JAVA_DOUBLE);
    public static final NativeFunction smGetCalDate = status("smGetCalDate", JAVA_INT, ADDRESS);

    // Network configuration of the 10GbE models
    public static final NativeFunction smBroadcastNetworkConfig =
        status("smBroadcastNetworkConfig", ADDRESS, ADDRESS, JAVA_SHORT, JAVA_INT);
    public static final NativeFunction smNetworkConfigGetDeviceList =
        status("smNetworkConfigGetDeviceList", ADDRESS, ADDRESS);
    public static final NativeFunction smNetworkConfigOpenDevice =
        status("smNetworkConfigOpenDevice", ADDRESS, JAVA_INT);
    public static final NativeFunction smNetworkConfigCloseDevice = status("smNetworkConfigCloseDevice", JAVA_INT);
    public static final NativeFunction smNetworkConfigGetMAC = status("smNetworkConfigGetMAC", JAVA_INT, ADDRESS);
    public static final NativeFunction smNetworkConfigSetIP =
        status("smNetworkConfigSetIP", JAVA_INT, ADDRESS, JAVA_INT);
    public static final NativeFunction smNetworkConfigGetIP = status("smNetworkConfigGetIP", JAVA_INT, ADDRESS);
    public static final NativeFunction smNetworkConfigSetPort =
        status("smNetworkConfigSetPort", JAVA_INT, JAVA_INT, JAVA_INT);
    public static final NativeFunction smNetworkConfigGetPort = status("smNetworkConfigGetPort", JAVA_INT, ADDRESS);

    // Library
    public static final NativeFunction smGetAPIVersion = NativeFunction.returning(API, "smGetAPIVersion", ADDRESS);
    public static final NativeFunction smGetErrorString =
        NativeFunction.returning(API, "smGetErrorString", ADDRESS, JAVA_INT);

    private SmNative() {
    }

    private static NativeFunction status(String name, MemoryLayout... arguments) {
        return NativeFunction.status(API, name, arguments);
    }
}
