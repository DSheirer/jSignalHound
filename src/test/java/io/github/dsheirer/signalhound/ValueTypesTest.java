package io.github.dsheirer.signalhound;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.Test;

/** Tests of the configuration and data classes, which need no Signal Hound library. */
class ValueTypesTest {
    @Test
    void validatesIqConfig() {
        IqConfig config = IqConfig.of(100e6, 4, 5e6);
        assertEquals(IqDataType.FLOAT32, config.dataType());
        assertEquals(IqConfig.DEFAULT_REFERENCE_LEVEL, config.referenceLevel());
        assertEquals(8, config.withDecimation(8).decimation());
        assertEquals(IqDataType.INT16, config.withDataType(IqDataType.INT16).dataType());
        assertThrows(IllegalArgumentException.class, () -> config.withDecimation(3));
        assertThrows(IllegalArgumentException.class, () -> config.withDecimation(0));
        assertThrows(IllegalArgumentException.class, () -> config.withBandwidth(0));
        assertThrows(IllegalArgumentException.class, () -> config.withCenterFrequency(-1));
        assertThrows(IllegalArgumentException.class, () -> config.withReferenceLevel(21));
        assertThrows(IllegalArgumentException.class, () -> config.withCenterFrequency(Double.NaN));
    }

    @Test
    void validatesSweepConfig() {
        SweepConfig config = SweepConfig.of(1e9, 20e6, 10e3);
        assertEquals(config.rbw(), config.vbw());
        assertThrows(IllegalArgumentException.class, () -> new SweepConfig(1e9, 20e6, 10e3, 20e3, 0.001, 0));
        assertThrows(IllegalArgumentException.class, () -> SweepConfig.of(1e9, 0, 10e3));
    }

    @Test
    void sweepInfoGivesBinFrequencies() {
        SweepInfo info = new SweepInfo(100e6, 1e3, 1000, 1e3, 1e3);
        assertEquals(100e6, info.frequency(0));
        assertEquals(100.5e6, info.frequency(500));
    }

    @Test
    void sizesSamples() {
        assertEquals(8, IqDataType.FLOAT32.sampleBytes());
        assertEquals(4, IqDataType.INT16.sampleBytes());
    }

    @Test
    void buffersCopyTheirSamples() {
        try (IqBuffer buffer = IqBuffer.allocate(IqDataType.FLOAT32, 2)) {
            assertEquals(16, buffer.segment().byteSize());
            for (int i = 0; i < 4; i++) {
                buffer.segment().setAtIndex(ValueLayout.JAVA_FLOAT, i, i + 0.5f);
            }
            assertArrayEquals(new float[] {0.5f, 1.5f, 2.5f, 3.5f}, buffer.copyFloats());
            float[] copy = new float[5];
            buffer.copyFloats(copy);
            assertEquals(3.5f, copy[3]);
            assertThrows(IllegalStateException.class, buffer::copyShorts);
        }
        try (IqBuffer buffer = IqBuffer.allocate(new IqStreamInfo(1e6, 1e6, IqDataType.INT16), 3)) {
            buffer.segment().setAtIndex(ValueLayout.JAVA_SHORT, 5, (short) -7);
            assertEquals(-7, buffer.copyShorts()[5]);
            assertThrows(IllegalStateException.class, buffer::copyFloats);
        }
    }

    @Test
    void closingABufferTwiceIsHarmless() {
        IqBuffer buffer = IqBuffer.allocate(IqDataType.INT16, 1);
        buffer.close();
        buffer.close();
        assertThrows(IllegalStateException.class, () -> buffer.segment().get(ValueLayout.JAVA_SHORT, 0));
    }

    @Test
    void rejectsEmptyBuffers() {
        assertThrows(IllegalArgumentException.class, () -> IqBuffer.allocate(IqDataType.FLOAT32, 0));
    }
}
