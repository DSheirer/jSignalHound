package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * One Signal Hound API library, loaded on first use. A library that cannot be
 * loaded is remembered as such, with the reason, and is not retried.
 */
public final class NativeLibrary {
    private static final Map<SignalHoundApi, NativeLibrary> LIBRARIES = new EnumMap<>(SignalHoundApi.class);

    private final SignalHoundApi api;
    private final SymbolLookup lookup;
    private final String location;
    private final String failure;
    private NativeFunction versionFunction;
    private NativeFunction errorStringFunction;

    private NativeLibrary(SignalHoundApi api, SymbolLookup lookup, String location, String failure) {
        this.api = api;
        this.lookup = lookup;
        this.location = location;
        this.failure = failure;
    }

    static NativeLibrary loaded(SignalHoundApi api, SymbolLookup lookup, String location) {
        return new NativeLibrary(api, lookup, location, null);
    }

    static NativeLibrary failed(SignalHoundApi api, String failure) {
        return new NativeLibrary(api, null, null, failure);
    }

    /** Returns the library of an API, loading it the first time. */
    public static NativeLibrary of(SignalHoundApi api) {
        synchronized (LIBRARIES) {
            return LIBRARIES.computeIfAbsent(api, NativeLibraryLoader::load);
        }
    }

    public SignalHoundApi api() {
        return api;
    }

    public boolean isLoaded() {
        return lookup != null;
    }

    /** Where the library was loaded from, if it was. */
    public Optional<String> location() {
        return Optional.ofNullable(location);
    }

    /** Why the library could not be loaded, if it could not. */
    public Optional<String> failure() {
        return Optional.ofNullable(failure);
    }

    /** Finds a symbol of the library. */
    Optional<MemorySegment> find(String name) {
        return requireLoaded().find(name);
    }

    private SymbolLookup requireLoaded() {
        if (lookup == null) {
            throw new UnsatisfiedLinkError(failure);
        }
        return lookup;
    }

    /** Returns the version that the library reports, such as {@code 5.0.12}. */
    public String version() {
        requireLoaded();
        synchronized (this) {
            if (versionFunction == null) {
                versionFunction = new NativeFunction(api, api.prefix() + "GetAPIVersion", FunctionDescriptor.of(ADDRESS));
            }
        }
        return NativeFunction.cString(versionFunction.callAddress());
    }

    /** Returns the library's description of a status code. */
    public String errorString(int status) {
        requireLoaded();
        synchronized (this) {
            if (errorStringFunction == null) {
                errorStringFunction = new NativeFunction(api, api.prefix() + "GetErrorString",
                    FunctionDescriptor.of(ADDRESS, JAVA_INT));
            }
        }
        if (!errorStringFunction.isAvailable()) {
            return "status " + status;
        }
        String text = NativeFunction.cString(errorStringFunction.callAddress(status));
        return text == null ? "status " + status : text;
    }
}
