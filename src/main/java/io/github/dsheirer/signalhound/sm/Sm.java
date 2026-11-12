package io.github.dsheirer.signalhound.sm;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.internal.NativeFunction;
import io.github.dsheirer.signalhound.internal.SmNative;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entry point for SM200 and SM435 devices, driven by the Signal Hound
 * {@code sm_api} library. USB models (A and B) are found by
 * {@link #devices()}; 10GbE models (C) are opened by address with
 * {@link #openNetworked(String, String, int)}. The macOS build of the library
 * supports USB models only.
 */
public final class Sm {
    /** Host address that lets the operating system choose the network interface. */
    public static final String ADDRESS_ANY = "0.0.0.0";
    /** Factory address of a networked SM. */
    public static final String DEFAULT_DEVICE_ADDRESS = "192.168.2.10";
    /** Factory port of a networked SM. */
    public static final int DEFAULT_PORT = 51665;

    private Sm() {
    }

    /** Returns the version of the {@code sm_api} library, such as {@code 2.3.10}. */
    public static String libraryVersion() {
        return SignalHoundApi.SM.version();
    }

    /** Lists the USB SM devices connected and not open. Networked devices are not listed. */
    public static List<DeviceDescriptor> devices() throws SignalHoundException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment serials = arena.allocate(JAVA_INT, SmNative.SM_MAX_DEVICES);
            MemorySegment types = arena.allocate(JAVA_INT, SmNative.SM_MAX_DEVICES);
            MemorySegment count = arena.allocate(JAVA_INT);
            boolean typed = SmNative.smGetDeviceList2.isAvailable();
            if (typed) {
                SmNative.smGetDeviceList2.check(serials, types, count);
            } else {
                SmNative.smGetDeviceList.check(serials, count);
            }
            int found = Math.min(count.get(JAVA_INT, 0), SmNative.SM_MAX_DEVICES);
            List<DeviceDescriptor> devices = new ArrayList<>(found);
            for (int i = 0; i < found; i++) {
                String model = typed ? SmModel.fromCode(types.getAtIndex(JAVA_INT, i)).name() : "SM";
                devices.add(new DeviceDescriptor(SignalHoundApi.SM, serials.getAtIndex(JAVA_INT, i), model));
            }
            return List.copyOf(devices);
        }
    }

    /** Opens the first USB SM device found. */
    public static SmDevice open() throws SignalHoundException {
        return open(SmNative.smOpenDevice);
    }

    /** Opens the USB SM device with a serial number. */
    public static SmDevice open(int serialNumber) throws SignalHoundException {
        return open(SmNative.smOpenDeviceBySerial, serialNumber);
    }

    /** Opens a networked SM device at its factory address and port. */
    public static SmDevice openNetworked() throws SignalHoundException {
        return openNetworked(ADDRESS_ANY, DEFAULT_DEVICE_ADDRESS, DEFAULT_PORT);
    }

    /**
     * Opens a networked SM device.
     *
     * @param hostAddress IPv4 address of the network interface to use, or {@link #ADDRESS_ANY}
     * @param deviceAddress IPv4 address of the device
     * @param port port of the device
     */
    public static SmDevice openNetworked(String hostAddress, String deviceAddress, int port)
        throws SignalHoundException {
        Objects.requireNonNull(hostAddress, "hostAddress");
        Objects.requireNonNull(deviceAddress, "deviceAddress");
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port must be 0 to 65535: " + port);
        }
        try (Arena arena = Arena.ofConfined()) {
            return open(SmNative.smOpenNetworkedDevice, arena.allocateFrom(hostAddress),
                arena.allocateFrom(deviceAddress), (short) port);
        }
    }

    private static SmDevice open(NativeFunction function, Object... arguments) throws SignalHoundException {
        int handle = function.openHandle(arguments);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment type = arena.allocate(JAVA_INT);
            MemorySegment serial = arena.allocate(JAVA_INT);
            SmNative.smGetDeviceInfo.check(handle, type, serial);
            return new SmDevice(handle, serial.get(JAVA_INT, 0), SmModel.fromCode(type.get(JAVA_INT, 0)));
        } catch (SignalHoundException | RuntimeException e) {
            SmNative.smCloseDevice.call(handle);
            throw e;
        }
    }
}
