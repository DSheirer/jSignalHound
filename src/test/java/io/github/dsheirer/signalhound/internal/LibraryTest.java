package io.github.dsheirer.signalhound.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.SignalHound;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests that need a Signal Hound library but no hardware. An API whose library
 * is not installed is skipped, unless the {@code jsignalhound.test.require}
 * system property, set from {@code -PrequireApis=bb,sm,sp}, names it.
 */
class LibraryTest {
    private static final Set<String> REQUIRED_APIS = Arrays.stream(
            System.getProperty("jsignalhound.test.require", "").split(","))
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .map(name -> name.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());

    /**
     * Functions that the device classes call unconditionally, which every
     * supported build of a library must export. The others are optional: the
     * device classes check for them, or only users of the bindings call them.
     */
    private static final Map<SignalHoundApi, List<String>> CORE = Map.of(
        SignalHoundApi.BB, List.of("bbGetSerialNumberList", "bbOpenDevice", "bbOpenDeviceBySerialNumber",
            "bbCloseDevice", "bbGetSerialNumber", "bbGetDeviceType", "bbGetFirmwareVersion",
            "bbGetDeviceDiagnostics", "bbConfigureRefLevel", "bbConfigureGainAtten", "bbConfigureIQCenter",
            "bbConfigureIQ", "bbConfigureIQDataType", "bbInitiate", "bbAbort", "bbQueryIQParameters",
            "bbGetIQUnpacked", "bbGetIQCorrection", "bbConfigureCenterSpan", "bbConfigureSweepCoupling",
            "bbConfigureAcquisition", "bbConfigureProcUnits", "bbQueryTraceInfo", "bbFetchTrace_32f",
            "bbGetAPIVersion", "bbGetErrorString"),
        SignalHoundApi.SM, List.of("smGetDeviceList", "smOpenDevice", "smOpenDeviceBySerial",
            "smOpenNetworkedDevice", "smCloseDevice", "smGetDeviceInfo", "smGetFirmwareVersion",
            "smGetDeviceDiagnostics", "smSetRefLevel", "smSetAttenuator", "smSetIQDataType", "smSetIQCenterFreq",
            "smSetIQSampleRate", "smSetIQBandwidth", "smConfigure", "smGetIQParameters", "smGetIQ",
            "smGetIQCorrection", "smSetSweepSpeed", "smSetSweepCenterSpan", "smSetSweepCoupling",
            "smSetSweepDetector", "smSetSweepScale", "smSetSweepWindow", "smGetSweepParameters", "smGetSweep",
            "smAbort", "smGetAPIVersion", "smGetErrorString"),
        SignalHoundApi.SP, List.of("spGetDeviceList", "spOpenDevice", "spOpenDeviceBySerial", "spCloseDevice",
            "spGetSerialNumber", "spGetFirmwareVersion", "spGetDeviceDiagnostics", "spSetRefLevel",
            "spSetAttenuator", "spSetIQDataType", "spSetIQCenterFreq", "spSetIQSampleRate",
            "spSetIQSoftwareFilter", "spSetIQBandwidth", "spConfigure", "spGetIQParameters", "spGetIQ",
            "spGetIQCorrection", "spSetSweepCenterSpan", "spSetSweepCoupling", "spSetSweepDetector",
            "spSetSweepScale", "spSetSweepWindow", "spGetSweepParameters", "spGetSweep", "spAbort",
            "spGetAPIVersion", "spGetErrorString"));

    /** Skips the test when the library is missing and not required; fails it when required. */
    private static void requireLibrary(SignalHoundApi api) {
        if (api.isAvailable()) {
            System.out.println(api.libraryName() + " " + api.version() + " from " + api.location().orElse("?"));
            return;
        }
        if (REQUIRED_APIS.contains(api.prefix())) {
            fail(api.loadFailure().orElse("not loaded"));
        }
        assumeTrue(false, api.libraryName() + " is not installed");
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void reportsItsVersion(SignalHoundApi api) {
        requireLibrary(api);
        assertTrue(api.version().matches("\\d+\\.\\d+\\.\\d+.*"), api.version());
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void exportsTheCoreFunctions(SignalHoundApi api) throws IllegalAccessException {
        requireLibrary(api);
        List<NativeFunction> functions = bindings(api);
        List<String> missing = new ArrayList<>();
        for (NativeFunction function : functions) {
            if (!function.isAvailable()) {
                missing.add(function.name());
            }
        }
        System.out.println(api.libraryName() + " provides " + (functions.size() - missing.size()) + " of "
            + functions.size() + " bound functions" + (missing.isEmpty() ? "" : "; missing " + missing));
        List<String> missingCore = CORE.get(api).stream().filter(missing::contains).toList();
        assertTrue(missingCore.isEmpty(), "Missing core functions: " + missingCore);
        // Each core name must be a binding, so that a typo above cannot pass.
        Set<String> bound = functions.stream().map(NativeFunction::name).collect(Collectors.toSet());
        assertTrue(bound.containsAll(CORE.get(api)));
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void describesErrors(SignalHoundApi api) {
        requireLibrary(api);
        String description = NativeLibrary.of(api).errorString(-1);
        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void reportsAMissingFunctionWhenCalled(SignalHoundApi api) {
        requireLibrary(api);
        NativeFunction absent = new NativeFunction(api, api.prefix() + "NoSuchFunction",
            FunctionDescriptor.of(ValueLayout.JAVA_INT));
        assertFalse(absent.isAvailable());
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, absent::call);
        assertTrue(e.getMessage().contains(absent.name()), e.getMessage());
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void listsDevicesWithoutFailing(SignalHoundApi api) {
        requireLibrary(api);
        // CI machines have no devices, and may have no USB access, which the
        // library reports as an error.
        try {
            List<DeviceDescriptor> devices = SignalHound.devices(api);
            assertNotNull(devices);
            devices.forEach(device -> assertEquals(api, device.api()));
        } catch (SignalHoundException e) {
            assertTrue(e.code() < 0, e.getMessage());
        }
    }

    @ParameterizedTest
    @EnumSource(SignalHoundApi.class)
    void failsToOpenAnAbsentDevice(SignalHoundApi api) {
        requireLibrary(api);
        assumeTrue(!Boolean.getBoolean("jsignalhound.hardware.test"), "a device with serial 1 might be connected");
        SignalHoundException e = assertThrows(SignalHoundException.class,
            () -> SignalHound.open(new DeviceDescriptor(api, 1, "absent")).close());
        assertEquals(api, e.api());
        assertTrue(e.code() < 0);
    }

    /** The bindings of an API, read from its {@code *Native} class. */
    private static List<NativeFunction> bindings(SignalHoundApi api) throws IllegalAccessException {
        Class<?> type = switch (api) {
            case BB -> BbNative.class;
            case SM -> SmNative.class;
            case SP -> SpNative.class;
        };
        List<NativeFunction> functions = new ArrayList<>();
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == NativeFunction.class) {
                NativeFunction function = (NativeFunction) field.get(null);
                assertEquals(field.getName(), function.name(), "Field and C function names differ");
                functions.add(function);
            }
        }
        return functions;
    }
}
