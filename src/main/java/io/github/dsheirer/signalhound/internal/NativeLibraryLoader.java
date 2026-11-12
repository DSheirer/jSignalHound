package io.github.dsheirer.signalhound.internal;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds and loads a Signal Hound API library that the Signal Hound SDK, or the
 * user, installed. The jar contains no native code.
 *
 * <p>For each API the places below are tried in order, and the first library
 * that loads is used:
 * <ol>
 *   <li>The file named by the system property {@code jsignalhound.<api>.library},
 *       such as {@code -Djsignalhound.bb.library=/opt/sdk/libbb_api.so.5.0.12}.
 *       When it is set, nothing else is tried.</li>
 *   <li>The directories listed in the system property {@value #PATH_PROPERTY}
 *       and then the environment variable {@value #PATH_ENVIRONMENT}, separated
 *       by {@link File#pathSeparator}.</li>
 *   <li>The operating system's own search: {@code LD_LIBRARY_PATH} and the
 *       {@code ldconfig} cache on Linux, {@code DYLD_LIBRARY_PATH} and
 *       {@code /usr/local/lib} on macOS, {@code PATH} on Windows.</li>
 *   <li>The directories in {@code java.library.path}, and the places the SDK
 *       instructions and the Spike installer put the libraries.</li>
 * </ol>
 *
 * <p>In a directory, the plain name, such as {@code libbb_api.so}, is preferred;
 * otherwise the highest version, such as {@code libbb_api.so.5.0.12} or
 * {@code libbb_api.5.0.11.dylib}, as the SDK ships them. A copy of the FTDI
 * library ({@code ftd2xx.dll} or {@code libftd2xx.so}) in the same directory is
 * loaded first, since the BB60 and SA libraries depend on it and Windows does
 * not otherwise look for it next to the library that needs it.
 */
public final class NativeLibraryLoader {
    /** System property listing directories to search before any other. */
    public static final String PATH_PROPERTY = "jsignalhound.library.path";
    /** Environment variable listing directories to search after {@link #PATH_PROPERTY}. */
    public static final String PATH_ENVIRONMENT = "SIGNALHOUND_LIBRARY_PATH";

    private NativeLibraryLoader() {
    }

    /** System property naming the library file of one API, such as {@code jsignalhound.bb.library}. */
    public static String fileProperty(SignalHoundApi api) {
        return "jsignalhound." + api.prefix() + ".library";
    }

    /** Plain file name of an API library on a platform, such as {@code libbb_api.so}. */
    public static String libraryName(SignalHoundApi api, Platform platform) {
        if (platform.isWindows()) {
            return api.libraryName() + ".dll";
        }
        if (platform.isMacOs()) {
            return "lib" + api.libraryName() + ".dylib";
        }
        return "lib" + api.libraryName() + ".so";
    }

    /**
     * Matches the plain and versioned file names of an API library on a
     * platform. Group 1 holds the version, empty for the plain name.
     */
    static Pattern fileNamePattern(SignalHoundApi api, Platform platform) {
        String name = Pattern.quote(api.libraryName());
        if (platform.isWindows()) {
            return Pattern.compile(name + "()\\.dll", Pattern.CASE_INSENSITIVE);
        }
        if (platform.isMacOs()) {
            return Pattern.compile("lib" + name + "((?:\\.\\d+)*)\\.dylib");
        }
        return Pattern.compile("lib" + name + "\\.so((?:\\.\\d+)*)");
    }

    /**
     * Lists the library files of an API in a directory in the order they are
     * tried: the plain name first, then from the highest version down.
     */
    static List<Path> candidates(Path directory, SignalHoundApi api, Platform platform) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        Pattern pattern = fileNamePattern(api, platform);
        record Candidate(Path path, int[] version) {
        }
        List<Candidate> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(file -> {
                Matcher matcher = pattern.matcher(file.getFileName().toString());
                if (matcher.matches() && Files.isRegularFile(file)) {
                    found.add(new Candidate(file, parseVersion(matcher.group(1))));
                }
            });
        } catch (IOException | SecurityException e) {
            return List.of();
        }
        // An empty version, the plain name, sorts first.
        Comparator<Candidate> order = (a, b) -> {
            if (a.version().length == 0 || b.version().length == 0) {
                return Integer.compare(a.version().length, b.version().length);
            }
            return Arrays.compare(b.version(), a.version());
        };
        return found.stream().sorted(order).map(Candidate::path).toList();
    }

    private static int[] parseVersion(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return new int[0];
        }
        return Arrays.stream(suffix.substring(1).split("\\."))
            .mapToInt(part -> {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .toArray();
    }

    /** Directories searched after the operating system's own search. */
    static List<Path> defaultDirectories(Platform platform) {
        Set<Path> directories = new LinkedHashSet<>(split(System.getProperty("java.library.path")));
        if (platform.isLinux()) {
            String multiarch = platform == Platform.LINUX_X86_64 ? "x86_64-linux-gnu" : "aarch64-linux-gnu";
            for (String directory : List.of("/usr/local/lib", "/usr/local/lib64", "/usr/lib/" + multiarch,
                "/usr/lib64", "/usr/lib", "/opt/signalhound/lib")) {
                directories.add(Path.of(directory));
            }
        } else if (platform.isMacOs()) {
            for (String directory : List.of("/usr/local/lib", "/opt/homebrew/lib", "/opt/local/lib")) {
                directories.add(Path.of(directory));
            }
        } else if (platform.isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                directories.add(Path.of(programFiles, "Signal Hound", "Spike"));
                directories.add(Path.of(programFiles, "Signal Hound"));
            }
        }
        return List.copyOf(directories);
    }

    private static List<Path> split(String paths) {
        if (paths == null || paths.isBlank()) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        for (String entry : paths.split(Pattern.quote(File.pathSeparator))) {
            if (!entry.isBlank()) {
                try {
                    result.add(Path.of(entry.trim()));
                } catch (RuntimeException e) {
                    // An entry that is not a valid path cannot hold a library.
                }
            }
        }
        return result;
    }

    /** Loads the library of an API, or records why it could not be loaded. */
    static NativeLibrary load(SignalHoundApi api) {
        Platform platform = Platform.current();
        List<String> attempts = new ArrayList<>();

        String file = System.getProperty(fileProperty(api));
        if (file != null && !file.isBlank()) {
            Path path = Path.of(file.trim());
            NativeLibrary library = tryPath(api, path, attempts);
            return library != null ? library : NativeLibrary.failed(api, failure(api, platform, attempts));
        }

        List<Path> configured = new ArrayList<>(split(System.getProperty(PATH_PROPERTY)));
        configured.addAll(split(System.getenv(PATH_ENVIRONMENT)));
        NativeLibrary library = tryDirectories(api, platform, configured, attempts);
        if (library != null) {
            return library;
        }

        String name = libraryName(api, platform);
        try {
            SymbolLookup lookup = SymbolLookup.libraryLookup(name, Arena.global());
            return NativeLibrary.loaded(api, lookup, "system library search path (" + name + ")");
        } catch (IllegalArgumentException e) {
            attempts.add(name + " on the system library search path: " + e.getMessage());
        }

        library = tryDirectories(api, platform, defaultDirectories(platform), attempts);
        if (library != null) {
            return library;
        }
        return NativeLibrary.failed(api, failure(api, platform, attempts));
    }

    private static NativeLibrary tryDirectories(SignalHoundApi api, Platform platform, List<Path> directories,
        List<String> attempts) {
        for (Path directory : directories) {
            List<Path> candidates = candidates(directory, api, platform);
            if (candidates.isEmpty()) {
                attempts.add(directory + ": no " + libraryName(api, platform));
            }
            for (Path candidate : candidates) {
                NativeLibrary library = tryPath(api, candidate, attempts);
                if (library != null) {
                    return library;
                }
            }
        }
        return null;
    }

    private static NativeLibrary tryPath(SignalHoundApi api, Path path, List<String> attempts) {
        if (!Files.isRegularFile(path)) {
            attempts.add(path + ": not found");
            return null;
        }
        Path absolute = path.toAbsolutePath();
        preloadDependencies(absolute.getParent());
        try {
            SymbolLookup lookup = SymbolLookup.libraryLookup(absolute, Arena.global());
            return NativeLibrary.loaded(api, lookup, absolute.toString());
        } catch (IllegalArgumentException e) {
            // The message names the missing dependency, such as libusb-1.0.
            attempts.add(absolute + ": " + e.getMessage());
            return null;
        }
    }

    /** Loads the FTDI library found next to an API library, so that the API library finds it. */
    private static void preloadDependencies(Path directory) {
        if (directory == null) {
            return;
        }
        Platform platform = Platform.current();
        List<Path> dependencies = new ArrayList<>();
        if (platform.isWindows()) {
            dependencies.add(directory.resolve("ftd2xx.dll"));
        } else if (platform.isLinux()) {
            dependencies.add(directory.resolve("libftd2xx.so"));
        }
        for (Path dependency : dependencies) {
            if (Files.isRegularFile(dependency)) {
                try {
                    SymbolLookup.libraryLookup(dependency, Arena.global());
                } catch (IllegalArgumentException e) {
                    // The API library's own load then reports the problem.
                }
            }
        }
    }

    private static String failure(SignalHoundApi api, Platform platform, List<String> attempts) {
        StringBuilder message = new StringBuilder()
            .append("Unable to load the Signal Hound ").append(api.libraryName())
            .append(" library (").append(api.description()).append(") on ").append(platform)
            .append(". Install it from the Signal Hound SDK, or set -D").append(fileProperty(api))
            .append("=<file> or -D").append(PATH_PROPERTY).append("=<directory>. Tried:");
        for (String attempt : attempts) {
            message.append(System.lineSeparator()).append("  ").append(attempt);
        }
        return message.toString();
    }
}
