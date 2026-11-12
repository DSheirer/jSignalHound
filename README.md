# jSignalHound

Java bindings for the [Signal Hound](https://signalhound.com) device APIs,
built on the Java Foreign Function & Memory API:

| API | Devices |
|-----|---------|
| `bb_api` | BB60A, BB60C, BB60D |
| `sm_api` | SM200A/B/C, SM435B/C (USB and 10GbE) |
| `sp_api` | SP145 |

The jar is pure Java and runs on every platform. It does not contain the
Signal Hound libraries: the Signal Hound API license does not allow merging
them into other software, so they come from the
[Signal Hound SDK](https://signalhound.com/software/signal-hound-software-development-kit-sdk/)
and jSignalHound finds them at runtime. Each API is loaded on its own, so a
missing one does not affect the others.

The SDK provides the libraries for these platforms:

| Platform | BB60 | SM200/SM435 | SP145 |
|----------|:----:|:-----------:|:-----:|
| Linux x86-64 | ✓ | ✓ | ✓ |
| Linux aarch64 (built on NVIDIA Jetson, Ubuntu 20.04) | ✓ ¹ | ✓ | ✓ |
| macOS Apple silicon | ✓ | ✓ ² | ✓ |
| Windows x86-64 | ✓ | ✓ | ✓ |

¹ needs the FTDI ARMv8 D2XX library, which the SDK does not include.
² USB models only.

The SDK has no macOS x86-64 or Windows ARM64 libraries. The ARM builds support
fewer measurement modes and decimations than the x86-64 ones; see the README
in each SDK library folder.

Requires Java 23 or later.

## Getting it

Download `jsignalhound-<version>.jar` from
[Releases](https://github.com/DSheirer/jSignalHound/releases). The rolling
[`nightly`](https://github.com/DSheirer/jSignalHound/releases/tag/nightly)
pre-release tracks `main`.

Then install the libraries for your devices from the SDK, as below, and check
that jSignalHound finds them and your devices:

```
java -jar jsignalhound-<version>.jar        # add -v to list where it looked
```

## Installing the Signal Hound libraries

Each API library is in `device_apis/<family>_series/lib/` of the SDK zip.

- **Linux:** install libusb (`sudo apt install libusb-1.0-0`), then follow the
  README in the library folder: copy the library, such as
  `Ubuntu 18.04/libsm_api.so.2.3.10`, to `/usr/local/lib` and run
  `sudo ldconfig`. `bb_api` also needs the FTDI library `libftd2xx.so`, which
  the SDK includes for x86-64. Install `sh_usb.rules` in `/etc/udev/rules.d/`
  to use the devices without root.
- **macOS:** `brew install libusb`, then copy the `.dylib` files from
  `macos_arm` to `/usr/local/lib`. The libraries load libusb from Homebrew's
  default location, `/opt/homebrew/opt/libusb`.
- **Windows:** install the device drivers, which come with
  [Spike](https://signalhound.com/spike/). Then copy the DLLs from the
  `vs2019\x64` folders, keeping `ftd2xx.dll` next to `bb_api.dll`, to a
  directory on `PATH` or one that you point jSignalHound to.

### How the libraries are found

For each API, jSignalHound uses the first library that loads from:

1. The file named by `-Djsignalhound.bb.library=<file>` (or `.sm.` or `.sp.`).
   When it is set, nothing else is tried.
2. The directories in `-Djsignalhound.library.path=<dirs>`, then in the
   environment variable `SIGNALHOUND_LIBRARY_PATH`.
3. The operating system's own search: `LD_LIBRARY_PATH` and the `ldconfig`
   cache on Linux, `DYLD_LIBRARY_PATH` and `/usr/local/lib` on macOS, `PATH`
   on Windows.
4. `java.library.path`, then `/usr/local/lib`, `/usr/lib` and `/opt/signalhound/lib`
   on Linux; `/usr/local/lib` and `/opt/homebrew/lib` on macOS;
   `%ProgramFiles%\Signal Hound\Spike` on Windows.

In a directory, the plain name (`libbb_api.so`, `libbb_api.dylib`, `bb_api.dll`)
is preferred; otherwise the highest version, as the SDK names its files
(`libbb_api.so.5.0.12`, `libbb_api.5.0.11.dylib`). A `libftd2xx.so` or
`ftd2xx.dll` in the same directory is loaded first. `SignalHoundApi.BB.loadFailure()`
lists everything that was tried.

## Using it

The BB60, SM and SP145 families share one interface for I/Q streaming and
sweeps:

```java
for (DeviceDescriptor descriptor : SignalHound.devices()) {   // every installed API
    try (SignalHoundDevice device = SignalHound.open(descriptor);
         IqBuffer buffer = IqBuffer.allocate(IqDataType.FLOAT32, 16_384)) {
        IqStreamInfo stream = device.startIq(IqConfig.of(100e6, 4, 5e6));
        System.out.println(device.model() + " at " + stream.sampleRate() + " S/s");
        for (int i = 0; i < 1000; i++) {
            IqBlock block = device.readIq(buffer, false);   // blocks for the samples
            float[] iq = buffer.copyFloats();               // interleaved I, Q
            if (block.sampleLoss()) {
                System.out.println("samples were dropped");
            }
        }
    }
}
```

`IqConfig` sets the center frequency, a power-of-two decimation of the
device's base sample rate (40 MS/s for the BB60), the filter bandwidth, the sample type and the reference level, from which gain and
attenuation are chosen automatically. Reuse one `IqBuffer` for every read;
`buffer.segment()` gives zero-copy access to the samples. `FLOAT32` samples are
amplitude corrected; `INT16` samples are full scale, and `device.iqCorrection()`
gives the correction to apply, as the "I/Q data types" section of the API
documentation describes.

Sweeps use a min/max detector in dBm:

```java
SweepInfo info = device.startSweep(SweepConfig.of(1e9, 20e6, 10e3));   // center, span, RBW
Sweep sweep = device.readSweep();
float peak = sweep.max()[0];   // at info.frequency(0) Hz
```

`Bb60`, `Sm` and `Sp145` open devices of one family and return `Bb60Device`,
`SmDevice` and `SpDevice`. These add the family's own settings, such as manual
gain and attenuation, the preselector, the external reference, and opening
networked SMs with `Sm.openNetworked(...)`.

Calls to a device are serialized, so it can be shared between threads, and
`close()` waits for a read in progress. Errors from the APIs throw
`SignalHoundException`, which carries the status code and the library's
description. Warnings, such as an ADC overflow, are reported in
`IqBlock.warning()` and `Sweep.warning()`.

### The full APIs

`io.github.dsheirer.signalhound.internal.BbNative`, `SmNative` and `SpNative`
bind every function of `bb_api.h`, `sm_api.h` and `sp_api.h`, named as in C.
They cover features such as real-time spectrum analysis, audio demodulation,
GPS, segmented and sweep-list I/Q, and the tracking generator. They are not a
stable API, but are usable with the `handle()` of a `Bb60Device`, `SmDevice`
or `SpDevice`:

```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment state = arena.allocate(ValueLayout.JAVA_INT);
    SmNative.smGetGPSState.check(device.handle(), state);
}
```

The SDK ships older libraries on some platforms, which can lack functions the
headers declare. The Linux x86-64 and macOS `sp_api`, for example, lack
`spGetDeviceType` and the preselector functions. Functions are bound when first
called, and a missing one throws `UnsupportedOperationException`;
`NativeFunction.isAvailable()` tells beforehand.

### Native access

Java prints a warning, which future versions will turn into an error, unless
you allow native access:

```
java --enable-native-access=ALL-UNNAMED -cp app.jar:jsignalhound.jar my.App
```

On the module path, use `--enable-native-access=io.github.dsheirer.signalhound`.
In your own executable jar, add `Enable-Native-Access: ALL-UNNAMED` to the
manifest.

## Building

You need JDK 23+, and bash, curl and unzip to fetch the SDK for testing.

```
./gradlew build                                   # jar in build/libs; tests skip missing APIs
./native/fetch-sdk.sh                             # downloads the pinned SDK to sdk/
./native/check-bindings.sh sdk/signal_hound_sdk   # compares the bindings with the headers
./gradlew test -PlibraryPath=$PWD/sdk/lib/linux-x86_64 -PrequireApis=bb,sm,sp
./gradlew test -PhardwareTest=true                # also test connected devices
```

The hardware tests only receive. `sdk/` is ignored by git; never commit it.

## Releases

The SDK that the bindings are checked and tested against is pinned in
[`native/sdk.env`](native/sdk.env), with its checksum. The jSignalHound
version is set in [`gradle.properties`](gradle.properties).

| Trigger | What happens |
|---------|--------------|
| Pull request | Checks the bindings against the pinned SDK and tests all six platforms; publishes nothing |
| Merge to `main` | Replaces the `nightly` pre-release, tested against the SDK currently on Signal Hound's site |
| Daily schedule | The same, if this project or the SDK has changed |
| Tag `vX.Y.Z` | Publishes a release tested against the pinned SDK |

CI downloads the SDK from Signal Hound in each job and keeps it only in the
Actions cache; it never goes into an artifact or the jar. Each platform's
tests load the SDK libraries for that platform and must find every function
the device classes use. macOS x86-64 and Windows ARM64, which have no SDK
libraries, run the tests that need none, and check that the jar starts and
reports the libraries missing.

To release, set `version` in `gradle.properties`, merge, then tag:
`git tag v1.0.0 && git push origin v1.0.0`. The workflow rejects a tag that does
not match. For a pre-release add a suffix, such as `v1.0.0-rc1`.

To adopt a new SDK, set `SDK_URL` and `SDK_SHA256` in `native/sdk.env`. The
nightly build reports when Signal Hound publishes a new one. If it declares
new functions, `check-bindings.sh` lists them; add them to the `*Native` class.

## License

Apache License 2.0. See [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for
the Signal Hound software that jSignalHound uses but does not include.
