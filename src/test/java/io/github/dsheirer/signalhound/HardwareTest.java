package io.github.dsheirer.signalhound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests with connected devices, run with {@code ./gradlew test -PhardwareTest=true}.
 * Every device of every installed API is exercised. The tests only receive.
 */
class HardwareTest {
    private static final double FREQUENCY = 100e6;
    private static final int BLOCK = 16_384;

    @BeforeAll
    static void requireHardware() {
        assumeTrue(Boolean.getBoolean("jsignalhound.hardware.test"), "hardware tests not enabled");
    }

    private static List<DeviceDescriptor> devices() throws SignalHoundException {
        List<DeviceDescriptor> devices = SignalHound.devices();
        assumeTrue(!devices.isEmpty(), "no Signal Hound devices connected");
        return devices;
    }

    @Test
    void describesDevices() throws SignalHoundException {
        for (DeviceDescriptor descriptor : devices()) {
            try (SignalHoundDevice device = SignalHound.open(descriptor)) {
                assertEquals(descriptor.serialNumber(), device.serialNumber());
                Diagnostics diagnostics = device.diagnostics();
                System.out.println(device.model() + " " + device.serialNumber() + " firmware "
                    + device.firmwareVersion() + ", " + diagnostics);
                assertTrue(diagnostics.temperature() > -40 && diagnostics.temperature() < 120);
            }
        }
    }

    @Test
    void streamsFloatSamples() throws SignalHoundException {
        for (DeviceDescriptor descriptor : devices()) {
            try (SignalHoundDevice device = SignalHound.open(descriptor);
                 IqBuffer buffer = IqBuffer.allocate(IqDataType.FLOAT32, BLOCK)) {
                IqStreamInfo stream = device.startIq(IqConfig.of(FREQUENCY, 4, 5e6));
                System.out.println(device.model() + " streaming at " + stream.sampleRate() + " S/s, "
                    + stream.bandwidth() + " Hz");
                assertTrue(stream.sampleRate() > 1e6, stream.toString());
                long previous = 0;
                for (int i = 0; i < 50; i++) {
                    IqBlock block = device.readIq(buffer, i == 0);
                    assertEquals(BLOCK, block.samples());
                    assertTrue(block.timestampNanos() > previous, "timestamps must increase");
                    previous = block.timestampNanos();
                }
                float[] samples = buffer.copyFloats();
                double power = 0;
                for (float sample : samples) {
                    power += sample * sample;
                }
                assertTrue(power > 0, "samples must not all be zero");
                device.stop();
                assertThrows(IllegalStateException.class, () -> device.readIq(buffer, false));
            }
        }
    }

    @Test
    void streamsShortSamples() throws SignalHoundException {
        for (DeviceDescriptor descriptor : devices()) {
            try (SignalHoundDevice device = SignalHound.open(descriptor)) {
                IqStreamInfo stream = device.startIq(IqConfig.of(FREQUENCY, 4, 5e6).withDataType(IqDataType.INT16));
                assertTrue(device.iqCorrection() > 0);
                try (IqBuffer floats = IqBuffer.allocate(IqDataType.FLOAT32, BLOCK)) {
                    assertThrows(IllegalArgumentException.class, () -> device.readIq(floats, false));
                }
                try (IqBuffer buffer = IqBuffer.allocate(stream, BLOCK)) {
                    IqBlock block = device.readIq(buffer, true);
                    assertEquals(BLOCK, block.samples());
                }
            }
        }
    }

    @Test
    void sweeps() throws SignalHoundException {
        for (DeviceDescriptor descriptor : devices()) {
            try (SignalHoundDevice device = SignalHound.open(descriptor)) {
                SweepInfo info = device.startSweep(SweepConfig.of(FREQUENCY, 20e6, 30e3));
                System.out.println(device.model() + " sweep " + info);
                assertTrue(info.bins() > 0);
                assertTrue(info.startFrequency() <= FREQUENCY - 10e6 + info.binSize());
                Sweep sweep = device.readSweep();
                assertEquals(info.bins(), sweep.max().length);
                assertEquals(info.bins(), sweep.min().length);
                for (float value : sweep.max()) {
                    assertFalse(Float.isNaN(value));
                    assertTrue(value < 30, "power above 30 dBm: " + value);
                }
            }
        }
    }

    @Test
    void closedDevicesRejectCalls() throws SignalHoundException {
        DeviceDescriptor descriptor = devices().getFirst();
        SignalHoundDevice device = SignalHound.open(descriptor);
        device.close();
        device.close();
        assertTrue(device.isClosed());
        assertThrows(IllegalStateException.class, device::diagnostics);
        // A closed device can be opened again.
        try (SignalHoundDevice again = SignalHound.open(descriptor)) {
            assertEquals(descriptor.serialNumber(), again.serialNumber());
        }
    }
}
