package io.github.dsheirer.signalhound.internal;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

/**
 * A function of a Signal Hound API library, bound on first use.
 *
 * <p>The SDK ships different versions of a library for each platform, and an
 * older one can lack functions that the header declares. Binding lazily means
 * such a function fails only when it is called, with an
 * {@link UnsupportedOperationException}, rather than preventing the use of the
 * rest of the library. {@link #isAvailable()} tells beforehand.
 */
public final class NativeFunction {
    private static final Linker LINKER = Linker.nativeLinker();

    private final SignalHoundApi api;
    private final String name;
    private final FunctionDescriptor descriptor;
    private volatile MethodHandle handle;
    private volatile boolean resolved;

    NativeFunction(SignalHoundApi api, String name, FunctionDescriptor descriptor) {
        this.api = api;
        this.name = name;
        this.descriptor = descriptor;
    }

    /** A function returning a status code, the {@code bbStatus}, {@code SmStatus} or {@code SpStatus} enum. */
    static NativeFunction status(SignalHoundApi api, String name, MemoryLayout... arguments) {
        return new NativeFunction(api, name, FunctionDescriptor.of(JAVA_INT, arguments));
    }

    /** A function with the given return layout. */
    static NativeFunction returning(SignalHoundApi api, String name, MemoryLayout result, MemoryLayout... arguments) {
        return new NativeFunction(api, name, FunctionDescriptor.of(result, arguments));
    }

    /** The C name of the function, such as {@code bbOpenDevice}. */
    public String name() {
        return name;
    }

    public SignalHoundApi api() {
        return api;
    }

    /** Returns whether the library loads and exports this function. */
    public boolean isAvailable() {
        return NativeLibrary.of(api).isLoaded() && resolve().isPresent();
    }

    private Optional<MethodHandle> resolve() {
        if (!resolved) {
            synchronized (this) {
                if (!resolved) {
                    handle = NativeLibrary.of(api).find(name)
                        .map(symbol -> LINKER.downcallHandle(symbol, descriptor))
                        .orElse(null);
                    resolved = true;
                }
            }
        }
        return Optional.ofNullable(handle);
    }

    /**
     * Returns the downcall handle.
     *
     * @throws UnsatisfiedLinkError when the library cannot be loaded
     * @throws UnsupportedOperationException when the library lacks the function
     */
    public MethodHandle handle() {
        // Not naming the version: the version function may be the missing one.
        return resolve().orElseThrow(() -> new UnsupportedOperationException(
            "The " + api.libraryName() + " library at " + NativeLibrary.of(api).location().orElse("?")
                + " does not provide " + name + "; a newer Signal Hound SDK may"));
    }

    /** Calls the function and returns its raw result as an {@code int}. */
    public int call(Object... arguments) {
        return (int) invoke(arguments);
    }

    /** Calls a function that returns a pointer. */
    public MemorySegment callAddress(Object... arguments) {
        return (MemorySegment) invoke(arguments);
    }

    /**
     * Calls a function that returns a status code and returns it. Negative codes
     * are errors and are thrown; zero is success and positive codes are
     * warnings, which are returned.
     *
     * <p>This boxes its arguments, which costs little next to the work that the
     * Signal Hound functions do, even for the calls that fetch samples.
     */
    public int check(Object... arguments) throws SignalHoundException {
        int status = call(arguments);
        if (status < 0) {
            throw new SignalHoundException(api, name, status, NativeLibrary.of(api).errorString(status));
        }
        return status;
    }

    private Object invoke(Object... arguments) {
        try {
            return handle().invokeWithArguments(arguments);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Native call " + name + " failed", t);
        }
    }

    /**
     * Calls an open function, whose first parameter is {@code int *device},
     * and returns the device handle it stores.
     */
    public int openHandle(Object... arguments) throws SignalHoundException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = arena.allocate(JAVA_INT);
            Object[] all = new Object[arguments.length + 1];
            all[0] = device;
            System.arraycopy(arguments, 0, all, 1, arguments.length);
            check(all);
            return device.get(JAVA_INT, 0);
        }
    }

    /** Reads a NUL-terminated string that the library owns. */
    public static String cString(MemorySegment pointer) {
        return pointer == null || pointer.equals(MemorySegment.NULL)
            ? null : pointer.reinterpret(Long.MAX_VALUE).getString(0);
    }

    @Override
    public String toString() {
        return name;
    }
}
