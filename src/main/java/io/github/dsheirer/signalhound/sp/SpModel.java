package io.github.dsheirer.signalhound.sp;

/** SP145 models, as the {@code SpDeviceType} enum lists them. */
public enum SpModel {
    SP145A(0),
    /** SP145B, which adds a preselector. */
    SP145B(1),
    /**
     * A model that this version of jSignalHound does not know, or any SP145
     * when the library cannot tell the model, as the Linux x86-64 and macOS
     * builds of {@code sp_api} cannot.
     */
    UNKNOWN(-1);

    private final int code;

    SpModel(int code) {
        this.code = code;
    }

    /** The {@code SpDeviceType} value. */
    public int code() {
        return code;
    }

    /** Returns the model of an {@code SpDeviceType} value. */
    public static SpModel fromCode(int code) {
        for (SpModel model : values()) {
            if (model.code == code) {
                return model;
            }
        }
        return UNKNOWN;
    }
}
