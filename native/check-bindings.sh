#!/usr/bin/env bash
# Compares the functions declared in the Signal Hound API headers with the
# bindings in src/main/java/.../internal/{Bb,Sm,Sp}Native.java, so that a new
# SDK function fails the build rather than being silently unavailable.
#
# Usage: native/check-bindings.sh <sdk-directory>
#
# <sdk-directory> is the signal_hound_sdk directory of an extracted SDK, as
# native/fetch-sdk.sh creates under sdk/. Deprecated functions, those marked
# with the header's *_DEPRECATED macro or listed after its "Deprecated
# functions" comment, need no binding.
set -euo pipefail
# sort and comm must agree on collation order whatever the caller's locale.
export LC_ALL=C

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="$1"
BINDINGS="$ROOT/src/main/java/io/github/dsheirer/signalhound/internal"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Prints the names of the non-deprecated functions declared in a header.
declared() {
    python3 -I - "$1" <<'PY'
import re
import sys

text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
# Drop the deprecated section that closes bb_api.h, up to the end of extern "C".
text = re.sub(r"//\s*Deprecated functions.*?#ifdef __cplusplus\s*\}", "", text, flags=re.S)
text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
text = re.sub(r"//[^\n]*", "", text)
pattern = r"((?:[A-Z]+_DEPRECATED\([^)]*\)\s*)?)[A-Z]+_API\s+[^;{()]*?\b([a-z]{2}[A-Za-z0-9_]+)\s*\("
for match in re.finditer(pattern, text):
    if not match.group(1):
        print(match.group(2))
PY
}

status=0
check() {
    local header="$1" bindings="$2"
    declared "$header" | sort -u > "$WORK/header"
    # Bindings are created by status("name", ...) or returning(API, "name", ...).
    grep -oE '(status\(|returning\(API, )"[A-Za-z0-9_]+"' "$bindings" | cut -d'"' -f2 | sort -u > "$WORK/bindings"
    local missing stale
    missing="$(comm -23 "$WORK/header" "$WORK/bindings")"
    stale="$(comm -13 "$WORK/header" "$WORK/bindings")"
    if [ -n "$missing" ] || [ -n "$stale" ]; then
        [ -z "$missing" ] || printf 'Functions in %s without a binding:\n%s\n' "$(basename "$header")" "$missing" >&2
        [ -z "$stale" ] || printf 'Bindings not in %s:\n%s\n' "$(basename "$header")" "$stale" >&2
        status=1
    else
        echo "Bindings match $(basename "$header") ($(wc -l < "$WORK/header" | tr -d ' ') functions)"
    fi
}

APIS="$SDK/device_apis"
check "$APIS/bb_series/include/bb_api.h" "$BINDINGS/BbNative.java"
check "$APIS/sm_series/include/sm_api.h" "$BINDINGS/SmNative.java"
# sp_series has no top-level include directory; each platform has its own
# header. The Windows one is the newest and declares the most functions.
check "$APIS/sp_series/lib/windows_x64/include/sp_api.h" "$BINDINGS/SpNative.java"
exit $status
