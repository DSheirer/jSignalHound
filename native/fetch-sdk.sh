#!/usr/bin/env bash
# Downloads the Signal Hound SDK pinned in native/sdk.env, extracts it to
# sdk/signal_hound_sdk, and copies the bb_api, sm_api and sp_api libraries
# for the host platform to sdk/lib/<platform>, ready for
# ./gradlew test -PlibraryPath=sdk/lib/<platform>.
#
# Usage: native/fetch-sdk.sh [sdk-zip-url]
#
# The SDK is only for checking and testing the bindings. It must not be
# committed, published or bundled: the Signal Hound API license does not allow
# merging it into other software. A URL other than the pinned one, as the
# nightly build passes for the current SDK, is used without a checksum.
# Works on Linux, macOS and Windows (Git Bash). Set JSIGNALHOUND_PLATFORM, such
# as windows-aarch64, to override the detected platform.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=sdk.env
source "$ROOT/native/sdk.env"
URL="${1:-$SDK_URL}"
SDK="${JSIGNALHOUND_SDK_DIR:-$ROOT/sdk}"

case "$(uname -s)" in
    Linux)  OS=linux ;;
    Darwin) OS=macos ;;
    MINGW*|MSYS*|CYGWIN*) OS=windows ;;
    *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
    x86_64|amd64)  ARCH=x86_64 ;;
    aarch64|arm64) ARCH=aarch64 ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac
# A shell running under emulation, such as x64 Git Bash on Windows ARM64,
# reports the emulated architecture, so the workflows name the platform.
PLATFORM="${JSIGNALHOUND_PLATFORM:-$OS-$ARCH}"

mkdir -p "$SDK"
ZIP="$SDK/$(basename "$URL")"
if [ ! -f "$ZIP" ]; then
    echo "=== Downloading $URL ==="
    curl -fsSL --retry 3 -o "$ZIP.part" "$URL"
    mv "$ZIP.part" "$ZIP"
fi
if command -v sha256sum >/dev/null; then
    ACTUAL="$(sha256sum "$ZIP" | cut -d' ' -f1)"
else
    ACTUAL="$(shasum -a 256 "$ZIP" | cut -d' ' -f1)"
fi
if [ "$URL" = "$SDK_URL" ]; then
    if [ "$ACTUAL" != "$SDK_SHA256" ]; then
        echo "SDK checksum mismatch: $ACTUAL, expected $SDK_SHA256" >&2
        exit 1
    fi
else
    echo "Unpinned SDK $URL has SHA-256 $ACTUAL"
fi

# Only the headers and libraries of the three bound APIs are needed. The zip is
# extracted whole and pruned afterwards, because unzip's member patterns are
# not portable: the unzip.exe on the Windows runners matches none of them and
# extracts nothing.
rm -rf "$SDK/signal_hound_sdk"
unzip -q -o "$ZIP" -d "$SDK"
APIS="$SDK/signal_hound_sdk/device_apis"
for series in bb_series sm_series sp_series; do
    if [ ! -d "$APIS/$series/lib" ]; then
        echo "$(basename "$ZIP") has no signal_hound_sdk/device_apis/$series/lib" >&2
        exit 1
    fi
done
find "$SDK/signal_hound_sdk" -mindepth 1 -maxdepth 1 ! -name device_apis -exec rm -rf {} +
find "$APIS" -mindepth 1 -maxdepth 1 ! -name bb_series ! -name sm_series \
    ! -name sp_series ! -name LICENSE.rtf -exec rm -rf {} +
find "$APIS" -mindepth 2 -maxdepth 2 -type d -name examples -exec rm -rf {} +

OUT="$SDK/lib/$PLATFORM"
rm -rf "$OUT"
mkdir -p "$OUT"

# Copies the files matching a find -path pattern under a directory to $OUT.
copy() {
    local found
    found="$(find "$1" -type f -path "$2" | sort | tail -n 1)"
    if [ -z "$found" ]; then
        echo "No $2 under $1" >&2
        return 1
    fi
    echo "  $(basename "$found")  <- ${found#"$APIS"/}"
    cp "$found" "$OUT/"
}

echo "=== Libraries for $PLATFORM ==="
case "$PLATFORM" in
    linux-x86_64)
        # The Ubuntu 18.04 builds are the newest x86-64 ones that need only
        # glibc, libstdc++, libusb-1.0 and, for bb_api, the bundled FTDI library.
        copy "$APIS/bb_series/lib/linux_x64" '*/Ubuntu 18.04/libbb_api.so*'
        copy "$APIS/bb_series/lib/linux_x64" '*/Ubuntu 18.04/libftd2xx.so'
        copy "$APIS/sm_series/lib/linux_x64" '*/Ubuntu 18.04/libsm_api.so*'
        copy "$APIS/sp_series/lib/linux_x64" '*/Ubuntu 18.04/libsp_api.so*'
        ;;
    linux-aarch64)
        # bb_api also needs the FTDI ARMv8 D2XX library, which the SDK does not
        # ship; see its README_aarch64.txt.
        copy "$APIS/bb_series/lib/aarch64" '*/libbb_api.so*'
        copy "$APIS/sm_series/lib/aarch64" '*/libsm_api.so*'
        copy "$APIS/sp_series/lib/aarch64" '*/libsp_api.so*'
        ;;
    macos-aarch64)
        copy "$APIS/bb_series/lib/macos_arm" '*/libbb_api*.dylib'
        copy "$APIS/sm_series/lib/macos_arm" '*/libsm_api*.dylib'
        copy "$APIS/sp_series/lib/macos_arm" '*/libsp_api*.dylib'
        ;;
    windows-x86_64)
        copy "$APIS/bb_series/lib/win_x64" '*/vs2019/x64/bb_api.dll'
        copy "$APIS/bb_series/lib/win_x64" '*/vs2019/x64/ftd2xx.dll'
        copy "$APIS/sm_series/lib/windows" '*/vs2019_*/x64/sm_api.dll'
        copy "$APIS/sp_series/lib/windows_x64" '*/vs2019/x64/sp_api.dll'
        ;;
    *)
        echo "The SDK has no libraries for $PLATFORM"
        ;;
esac
echo "$OUT"
