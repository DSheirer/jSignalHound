package io.github.dsheirer.signalhound.sp;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.internal.NativeFunction;
import io.github.dsheirer.signalhound.internal.SpNative;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/** Entry point for SP145 devices, driven by the Signal Hound {@code sp_api} library. */
public final class Sp145 {
    private Sp145() {
    }

    /** Returns the version of the {@code sp_api} library, such as {@code 1.0.10}. */
    public static String libraryVersion() {
        return SignalHoundApi.SP.version();
    }

    /**
     * Lists the SP145 devices connected and not open. The library does not tell
     * the model before a device is opened, so each is listed as {@code SP145}.
     */
    public static List<DeviceDescriptor> devices() throws SignalHoundException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment serials = arena.allocate(JAVA_INT, SpNative.SP_MAX_DEVICES);
            MemorySegment count = arena.allocate(JAVA_INT);
            SpNative.spGetDeviceList.check(serials, count);
            int found = Math.min(count.get(JAVA_INT, 0), SpNative.SP_MAX_DEVICES);
            List<DeviceDescriptor> devices = new ArrayList<>(found);
            for (int i = 0; i < found; i++) {
                devices.add(new DeviceDescriptor(SignalHoundApi.SP, serials.getAtIndex(JAVA_INT, i), "SP145"));
            }
            return List.copyOf(devices);
        }
    }

    /** Opens the first SP145 found. */
    public static SpDevice open() throws SignalHoundException {
        return open(SpNative.spOpenDevice);
    }

    /** Opens the SP145 with a serial number. */
    public static SpDevice open(int serialNumber) throws SignalHoundException {
        return open(SpNative.spOpenDeviceBySerial, serialNumber);
    }

    private static SpDevice open(NativeFunction function, Object... arguments) throws SignalHoundException {
        int handle = function.openHandle(arguments);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(JAVA_INT);
            SpNative.spGetSerialNumber.check(handle, value);
            int serial = value.get(JAVA_INT, 0);
            SpModel model = SpModel.UNKNOWN;
            if (SpNative.spGetDeviceType.isAvailable()) {
                SpNative.spGetDeviceType.check(handle, value);
                model = SpModel.fromCode(value.get(JAVA_INT, 0));
            }
            return new SpDevice(handle, serial, model);
        } catch (SignalHoundException | RuntimeException e) {
            SpNative.spCloseDevice.call(handle);
            throw e;
        }
    }
}
