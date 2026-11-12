package io.github.dsheirer.signalhound.bb;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.internal.BbNative;
import io.github.dsheirer.signalhound.internal.NativeFunction;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for BB60A, BB60C and BB60D devices, driven by the Signal Hound
 * {@code bb_api} library.
 *
 * <p>On Linux the library needs libusb-1.0 and the FTDI D2XX library
 * ({@code libftd2xx.so}); on Windows {@code ftd2xx.dll}, which the SDK ships
 * next to {@code bb_api.dll}.
 */
public final class Bb60 {
    private Bb60() {
    }

    /** Returns the version of the {@code bb_api} library, such as {@code 5.0.12}. */
    public static String libraryVersion() {
        return SignalHoundApi.BB.version();
    }

    /** Returns the product ID of the {@code bb_api} library, of the form {@code ####-####}. */
    public static String productId() {
        return NativeFunction.cString(BbNative.bbGetProductID.callAddress());
    }

    /** Lists the BB60 devices connected and not open. */
    public static List<DeviceDescriptor> devices() throws SignalHoundException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment serials = arena.allocate(JAVA_INT, BbNative.BB_MAX_DEVICES);
            MemorySegment types = arena.allocate(JAVA_INT, BbNative.BB_MAX_DEVICES);
            MemorySegment count = arena.allocate(JAVA_INT);
            boolean typed = BbNative.bbGetSerialNumberList2.isAvailable();
            if (typed) {
                BbNative.bbGetSerialNumberList2.check(serials, types, count);
            } else {
                BbNative.bbGetSerialNumberList.check(serials, count);
            }
            int found = Math.min(count.get(JAVA_INT, 0), BbNative.BB_MAX_DEVICES);
            List<DeviceDescriptor> devices = new ArrayList<>(found);
            for (int i = 0; i < found; i++) {
                String model = typed ? BbModel.fromCode(types.getAtIndex(JAVA_INT, i)).name() : "BB60";
                devices.add(new DeviceDescriptor(SignalHoundApi.BB, serials.getAtIndex(JAVA_INT, i), model));
            }
            return List.copyOf(devices);
        }
    }

    /** Opens the first BB60 device found. */
    public static Bb60Device open() throws SignalHoundException {
        return open(BbNative.bbOpenDevice);
    }

    /** Opens the BB60 device with a serial number. */
    public static Bb60Device open(int serialNumber) throws SignalHoundException {
        return open(BbNative.bbOpenDeviceBySerialNumber, serialNumber);
    }

    private static Bb60Device open(NativeFunction function, Object... arguments) throws SignalHoundException {
        int handle = function.openHandle(arguments);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(JAVA_INT);
            BbNative.bbGetSerialNumber.check(handle, value);
            int serial = value.get(JAVA_INT, 0);
            BbNative.bbGetDeviceType.check(handle, value);
            return new Bb60Device(handle, serial, BbModel.fromCode(value.get(JAVA_INT, 0)));
        } catch (SignalHoundException | RuntimeException e) {
            BbNative.bbCloseDevice.call(handle);
            throw e;
        }
    }
}
