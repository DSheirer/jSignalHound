package io.github.dsheirer.signalhound;

import io.github.dsheirer.signalhound.internal.NativeLibrary;
import java.util.Optional;

/**
 * The Signal Hound device APIs that jSignalHound binds. Each is a separate
 * native library from the Signal Hound SDK, found and loaded on first use; see
 * {@link io.github.dsheirer.signalhound.internal.NativeLibraryLoader} for where
 * it is looked for. Any of them may be missing without affecting the others.
 */
public enum SignalHoundApi {
    /** {@code bb_api}, for the BB60A, BB60C and BB60D. */
    BB("bb_api", "bb", "BB60A/C/D"),
    /** {@code sm_api}, for the SM200A/B/C and SM435B/C. */
    SM("sm_api", "sm", "SM200 and SM435"),
    /** {@code sp_api}, for the SP145. */
    SP("sp_api", "sp", "SP145");

    private final String libraryName;
    private final String prefix;
    private final String description;

    SignalHoundApi(String libraryName, String prefix, String description) {
        this.libraryName = libraryName;
        this.prefix = prefix;
        this.description = description;
    }

    /** Base name of the native library, such as {@code bb_api}. */
    public String libraryName() {
        return libraryName;
    }

    /** Prefix of the API's C functions, such as {@code bb}. */
    public String prefix() {
        return prefix;
    }

    /** The devices the API supports, such as {@code BB60A/C/D}. */
    public String description() {
        return description;
    }

    /** Returns whether the native library was found and loads, loading it if not done yet. */
    public boolean isAvailable() {
        return NativeLibrary.of(this).isLoaded();
    }

    /**
     * Returns the version that the native library reports, such as {@code 5.0.12}.
     *
     * @throws UnsatisfiedLinkError when the library cannot be loaded
     */
    public String version() {
        return NativeLibrary.of(this).version();
    }

    /** Where the native library was loaded from, if it was. */
    public Optional<String> location() {
        return NativeLibrary.of(this).location();
    }

    /** Why the native library could not be loaded, listing the places tried, if it could not. */
    public Optional<String> loadFailure() {
        return NativeLibrary.of(this).failure();
    }
}
