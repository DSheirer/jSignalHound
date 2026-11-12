# Third-party notices

jSignalHound is licensed under the Apache License 2.0. The jar contains only
jSignalHound's own code.

## Signal Hound device APIs

jSignalHound calls the Signal Hound `bb_api`, `sm_api` and `sp_api` libraries,
which are not part of jSignalHound and are not distributed with it. They are
part of the
[Signal Hound SDK](https://signalhound.com/software/signal-hound-software-development-kit-sdk/),
are licensed by Signal Hound, Inc. under its API license (`LICENSE.rtf` in the
SDK), and must be obtained from Signal Hound. The function signatures and
constants in jSignalHound's bindings follow the public headers of that SDK.

Signal Hound is a registered trademark of Signal Hound, Inc. jSignalHound is an
independent project and is not endorsed by Signal Hound.

The Signal Hound libraries use libusb and, for the BB60, the FTDI D2XX
library, which are licensed by their own authors.
