package io.github.dsheirer.signalhound;

/**
 * A device that can be opened, as found by {@link SignalHound#devices()} or the
 * family classes such as {@link io.github.dsheirer.signalhound.bb.Bb60#devices()}.
 *
 * @param api the API that drives the device
 * @param serialNumber the device's serial number
 * @param model the model, such as {@code BB60D}, or the family name, such as
 *        {@code SP145}, when the API does not tell the model before opening
 */
public record DeviceDescriptor(SignalHoundApi api, int serialNumber, String model) {
    @Override
    public String toString() {
        return model + " " + serialNumber;
    }
}
