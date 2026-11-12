package io.github.dsheirer.signalhound;

import io.github.dsheirer.signalhound.bb.Bb60;
import io.github.dsheirer.signalhound.sm.Sm;
import io.github.dsheirer.signalhound.sp.Sp145;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finds and opens Signal Hound devices of every family whose API library is
 * installed. Use the family classes, {@link Bb60}, {@link Sm} and
 * {@link Sp145}, for the features particular to a family.
 */
public final class SignalHound {
    private SignalHound() {
    }

    /** The APIs whose native library is installed and loads. */
    public static List<SignalHoundApi> availableApis() {
        List<SignalHoundApi> apis = new ArrayList<>();
        for (SignalHoundApi api : SignalHoundApi.values()) {
            if (api.isAvailable()) {
                apis.add(api);
            }
        }
        return List.copyOf(apis);
    }

    /** Lists the connected USB devices of an API that are not open. */
    public static List<DeviceDescriptor> devices(SignalHoundApi api) throws SignalHoundException {
        return switch (api) {
            case BB -> Bb60.devices();
            case SM -> Sm.devices();
            case SP -> Sp145.devices();
        };
    }

    /**
     * Lists the connected USB devices, which are not open, of every API that
     * is installed. APIs that are not installed are skipped.
     */
    public static List<DeviceDescriptor> devices() throws SignalHoundException {
        List<DeviceDescriptor> devices = new ArrayList<>();
        for (SignalHoundApi api : availableApis()) {
            devices.addAll(devices(api));
        }
        return List.copyOf(devices);
    }

    /** Opens a device that {@link #devices()} listed. */
    public static SignalHoundDevice open(DeviceDescriptor device) throws SignalHoundException {
        Objects.requireNonNull(device, "device");
        return switch (device.api()) {
            case BB -> Bb60.open(device.serialNumber());
            case SM -> Sm.open(device.serialNumber());
            case SP -> Sp145.open(device.serialNumber());
        };
    }
}
