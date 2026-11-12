package io.github.dsheirer.signalhound.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.signalhound.SignalHoundApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests of library discovery that need no Signal Hound library. */
class NativeLibraryLoaderTest {
    @TempDir
    Path directory;

    @Test
    void detectsPlatforms() {
        assertEquals(Platform.LINUX_X86_64, Platform.detect("Linux", "amd64"));
        assertEquals(Platform.LINUX_AARCH64, Platform.detect("Linux", "aarch64"));
        assertEquals(Platform.MACOS_AARCH64, Platform.detect("Mac OS X", "aarch64"));
        assertEquals(Platform.MACOS_X86_64, Platform.detect("Mac OS X", "x86_64"));
        assertEquals(Platform.WINDOWS_X86_64, Platform.detect("Windows 11", "amd64"));
        assertEquals(Platform.WINDOWS_AARCH64, Platform.detect("Windows 11", "aarch64"));
        assertEquals(Platform.UNKNOWN, Platform.detect("Linux", "riscv64"));
        assertEquals(Platform.UNKNOWN, Platform.detect("FreeBSD", "amd64"));
    }

    @Test
    void namesLibraries() {
        assertEquals("libbb_api.so", NativeLibraryLoader.libraryName(SignalHoundApi.BB, Platform.LINUX_AARCH64));
        assertEquals("libsm_api.dylib", NativeLibraryLoader.libraryName(SignalHoundApi.SM, Platform.MACOS_AARCH64));
        assertEquals("sp_api.dll", NativeLibraryLoader.libraryName(SignalHoundApi.SP, Platform.WINDOWS_X86_64));
        assertEquals("jsignalhound.bb.library", NativeLibraryLoader.fileProperty(SignalHoundApi.BB));
    }

    @Test
    void prefersPlainNameThenHighestVersionOnLinux() throws IOException {
        create("libbb_api.so.5.0.9", "libbb_api.so.5.0.12", "libbb_api.so.5", "libbb_api.so",
            "libbb_api.so.txt", "libsm_api.so", "libftd2xx.so");
        assertEquals(List.of("libbb_api.so", "libbb_api.so.5.0.12", "libbb_api.so.5.0.9", "libbb_api.so.5"),
            names(NativeLibraryLoader.candidates(directory, SignalHoundApi.BB, Platform.LINUX_X86_64)));
    }

    @Test
    void findsVersionedMacLibraries() throws IOException {
        create("libsp_api.1.0.5.dylib", "libsp_api.1.0.10.dylib", "libsp_api.so");
        assertEquals(List.of("libsp_api.1.0.10.dylib", "libsp_api.1.0.5.dylib"),
            names(NativeLibraryLoader.candidates(directory, SignalHoundApi.SP, Platform.MACOS_AARCH64)));
    }

    @Test
    void findsWindowsLibrariesWhateverTheCase() throws IOException {
        create("SM_API.DLL", "sm_api.lib");
        assertEquals(List.of("SM_API.DLL"),
            names(NativeLibraryLoader.candidates(directory, SignalHoundApi.SM, Platform.WINDOWS_X86_64)));
    }

    @Test
    void ignoresMissingDirectories() {
        assertTrue(NativeLibraryLoader.candidates(directory.resolve("absent"), SignalHoundApi.BB,
            Platform.LINUX_X86_64).isEmpty());
    }

    @Test
    void reportsWhereItLookedWhenTheConfiguredFileIsMissing() {
        String property = NativeLibraryLoader.fileProperty(SignalHoundApi.SP);
        String previous = System.getProperty(property);
        Path missing = directory.resolve("libsp_api.so");
        System.setProperty(property, missing.toString());
        try {
            NativeLibrary library = NativeLibraryLoader.load(SignalHoundApi.SP);
            assertFalse(library.isLoaded());
            String failure = library.failure().orElseThrow();
            assertTrue(failure.contains(missing.toString()), failure);
            assertTrue(failure.contains(property), failure);
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }

    @Test
    void structureLayoutsMatchHeaders() {
        // Sizes on every 64-bit platform the SDK supports.
        assertEquals(48, BbNative.BB_IQ_PACKET.byteSize());
        assertEquals(16, SmNative.SM_GPIO_STEP.byteSize());
        assertEquals(40, SmNative.SM_DEVICE_DIAGNOSTICS.byteSize());
    }

    private void create(String... names) throws IOException {
        for (String name : names) {
            Files.createFile(directory.resolve(name));
        }
    }

    private static List<String> names(List<Path> paths) {
        return paths.stream().map(path -> path.getFileName().toString()).toList();
    }
}
