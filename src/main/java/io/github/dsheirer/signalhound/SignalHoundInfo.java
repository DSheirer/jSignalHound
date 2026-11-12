package io.github.dsheirer.signalhound;

import io.github.dsheirer.signalhound.internal.Platform;
import java.util.List;
import java.util.Locale;

/**
 * Command line tool that reports which Signal Hound libraries are installed,
 * where they were found, and the devices connected. Run with
 * {@code java -jar jsignalhound-<version>.jar}.
 */
public final class SignalHoundInfo {
    private SignalHoundInfo() {
    }

    /** Runs the tool. */
    public static void main(String[] args) {
        System.out.println("jSignalHound " + version() + " on " + Platform.current()
            + ", Java " + Runtime.version());
        for (SignalHoundApi api : SignalHoundApi.values()) {
            System.out.println();
            if (!api.isAvailable()) {
                System.out.println(api.libraryName() + " (" + api.description() + "): not installed");
                if (args.length > 0 && args[0].equals("-v")) {
                    System.out.println(api.loadFailure().orElse(""));
                }
                continue;
            }
            System.out.println(api.libraryName() + " " + api.version() + " (" + api.description() + ")");
            System.out.println("  Location: " + api.location().orElse("?"));
            List<DeviceDescriptor> devices;
            try {
                devices = SignalHound.devices(api);
            } catch (SignalHoundException e) {
                // Machines without USB access, such as containers, end up here.
                System.out.println("  Unable to list devices: " + e.getMessage());
                continue;
            }
            System.out.println("  Devices found: " + devices.size());
            for (DeviceDescriptor descriptor : devices) {
                System.out.println("  " + descriptor);
                try (SignalHoundDevice device = SignalHound.open(descriptor)) {
                    Diagnostics diagnostics = device.diagnostics();
                    System.out.println("    Model:       " + device.model());
                    System.out.println("    Firmware:    " + device.firmwareVersion());
                    System.out.println(String.format(Locale.ROOT, "    Diagnostics: %.1f C, %.2f V, %.3f A",
                        diagnostics.temperature(), diagnostics.voltage(), diagnostics.current()));
                } catch (SignalHoundException | RuntimeException e) {
                    System.out.println("    Unable to open: " + e.getMessage());
                }
            }
        }
        if (args.length == 0 && SignalHound.availableApis().size() < SignalHoundApi.values().length) {
            System.out.println();
            System.out.println("Run with -v to list the places searched for missing libraries.");
        }
    }

    private static String version() {
        String version = SignalHoundInfo.class.getPackage().getImplementationVersion();
        return version == null ? "(development)" : version;
    }
}
