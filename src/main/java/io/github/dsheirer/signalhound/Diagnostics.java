package io.github.dsheirer.signalhound;

/**
 * Readings of a device's sensors.
 *
 * @param temperature device temperature in degrees Celsius
 * @param voltage supply voltage in volts: USB bus voltage for the BB60
 * @param current supply current in amperes: USB current for the BB60
 */
public record Diagnostics(float temperature, float voltage, float current) {
}
