package io.github.dsheirer.signalhound.sm;

/** SM models, as the {@code SmDeviceType} enum lists them. */
public enum SmModel {
    SM200A(0),
    SM200B(1),
    /** SM200C, connected over 10 Gigabit Ethernet. */
    SM200C(2),
    SM435B(3),
    /** SM435C, connected over 10 Gigabit Ethernet. */
    SM435C(4),
    /** A model that this version of jSignalHound does not know. */
    UNKNOWN(-1);

    private final int code;

    SmModel(int code) {
        this.code = code;
    }

    /** The {@code SmDeviceType} value. */
    public int code() {
        return code;
    }

    /** Returns the model of an {@code SmDeviceType} value. */
    public static SmModel fromCode(int code) {
        for (SmModel model : values()) {
            if (model.code == code) {
                return model;
            }
        }
        return UNKNOWN;
    }
}
