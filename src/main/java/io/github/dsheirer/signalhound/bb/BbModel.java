package io.github.dsheirer.signalhound.bb;

/** BB60 models, as {@code bbGetDeviceType} reports them. */
public enum BbModel {
    BB60A(1),
    BB60C(2),
    BB60D(3),
    /** A model that this version of jSignalHound does not know. */
    UNKNOWN(-1);

    private final int code;

    BbModel(int code) {
        this.code = code;
    }

    /** The {@code BB_DEVICE_*} value. */
    public int code() {
        return code;
    }

    /** Returns the model of a {@code BB_DEVICE_*} value. */
    public static BbModel fromCode(int code) {
        for (BbModel model : values()) {
            if (model.code == code) {
                return model;
            }
        }
        return UNKNOWN;
    }
}
