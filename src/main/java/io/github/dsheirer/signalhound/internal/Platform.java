package io.github.dsheirer.signalhound.internal;

import java.util.Locale;

/** Operating system and architecture of the running JVM. */
public enum Platform {
    LINUX_X86_64("linux-x86_64"),
    LINUX_AARCH64("linux-aarch64"),
    MACOS_X86_64("macos-x86_64"),
    MACOS_AARCH64("macos-aarch64"),
    WINDOWS_X86_64("windows-x86_64"),
    WINDOWS_AARCH64("windows-aarch64"),
    UNKNOWN("unknown");

    private static final Platform CURRENT = detect(System.getProperty("os.name"), System.getProperty("os.arch"));

    private final String id;

    Platform(String id) {
        this.id = id;
    }

    /** Returns the platform of this JVM. */
    public static Platform current() {
        return CURRENT;
    }

    /** Identifier such as {@code linux-x86_64}. */
    public String id() {
        return id;
    }

    public boolean isLinux() {
        return this == LINUX_X86_64 || this == LINUX_AARCH64;
    }

    public boolean isMacOs() {
        return this == MACOS_X86_64 || this == MACOS_AARCH64;
    }

    public boolean isWindows() {
        return this == WINDOWS_X86_64 || this == WINDOWS_AARCH64;
    }

    /** Maps the {@code os.name} and {@code os.arch} system properties to a platform. */
    static Platform detect(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String arch = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
        boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
        if (!x64 && !arm64) {
            return UNKNOWN;
        }
        if (os.startsWith("linux")) {
            return x64 ? LINUX_X86_64 : LINUX_AARCH64;
        }
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            return x64 ? MACOS_X86_64 : MACOS_AARCH64;
        }
        if (os.startsWith("windows")) {
            return x64 ? WINDOWS_X86_64 : WINDOWS_AARCH64;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return id;
    }
}
