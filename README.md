# ReachGrid

[![CI](https://github.com/Ikteder/reachgrid/actions/workflows/ci.yml/badge.svg)](https://github.com/Ikteder/reachgrid/actions/workflows/ci.yml)

ReachGrid is a native Android touch ergonomics lab for exploring one-handed reach on a specific device. It presents a deterministic sequence of targets across a 4 by 6 grid, records successful-tap latency and misses, draws a per-zone heatmap, and shares a versioned JSON report only when the user asks.

The app is designed for mobile developers and product designers comparing layouts, grips, target sizes, or devices. It is a small evidence tool, not a medical assessment, accessibility certification, or population study.

## What it measures

A session contains 48 successful targets, two visits to every grid cell. A miss stays on the current target and becomes part of that cell's evidence.

Each result cell shows:

- median latency for its two successful visits;
- total misses attributed to that cell;
- accuracy across hits and misses;
- a reach score from 0 to 100.

The score is transparent:

```text
latencyFactor = clamp(1 - (medianMilliseconds - 250) / 1000, 0, 1)
accuracy = visits / (visits + misses)
reachScore = round(100 * latencyFactor * accuracy)
```

The score is only a visualization convenience. Use the raw latency and miss fields when comparing sessions.

## Android experience

1. Select left or right hand.
2. Select a target radius of 24, 32, or 40 dp.
3. Tap Start session and follow the highlighted targets.
4. Review the labeled heatmap after 48 successful taps.
5. Use Share JSON to send the report through the Android Sharesheet.

The route is generated from the local `YYYYMMDD` date. Repeating a session on the same day gives the same route, which makes before-and-after layout checks easier to compare.

## Privacy

- No internet permission.
- No account, analytics, advertising, telemetry, or background service.
- No raw touch coordinates in reports.
- No persistent local history in version 1.
- A report leaves the app only through an explicit Android share action.

The report contains the seed, selected hand, target radius, grid dimensions, target cell, latency, misses, accuracy, and score.

## Build

Requirements:

- JDK 17
- Android SDK platform 36
- Android SDK Build Tools 36.0.0

The project uses Android Gradle Plugin 9.3.0 and the checked Gradle 9.5.0 wrapper.

On macOS or Linux:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Verify the core without an Android SDK

The route, state machine, aggregation, scoring, and JSON exporter are plain Java. They can be checked with only a JDK:

```bash
mkdir -p build/core-verification
javac -source 8 -target 8 -d build/core-verification \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachSession.java \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachReport.java \
  tools/CoreVerification.java
java -cp build/core-verification CoreVerification
```

Local Windows verification used JDK 8 for this dependency-free core. Android compilation, unit tests, lint, and APK assembly run in public CI with JDK 17 and the Android 36 SDK. See the [verification record](docs/experiments/verification-2026-08-29.md) for exact evidence and boundaries.

## Project structure

```text
app/src/main/java/dev/ikteder/reachgrid/
  MainActivity.java          Android setup, progress, reset, and sharing
  ReachGridView.java         Target rendering, touch input, and heatmap
  core/ReachSession.java     Deterministic route and measurement state
  core/ReachReport.java      Aggregation, scoring, and JSON
app/src/test/                Android Gradle unit tests
tools/CoreVerification.java  SDK-free verification harness
```

## Current limitations

- Results describe one person, device, posture, hand, target size, and short session. They do not generalize automatically.
- The app is portrait-only in version 1.
- Session state resets if the activity or process is recreated.
- There is no saved history, side-by-side comparison, CSV export, or cloud synchronization.
- The target sequence is deterministic, but learning and fatigue can still affect repeated sessions.
- Direct-touch and assistive-input sessions are not comparable without a separate protocol.
- Heatmap colors are supported by numeric labels, but the custom measurement surface is not a substitute for a complete accessibility study.
- No emulator or physical-device interaction was available on the local Windows machine. Public CI verifies the Android build, lint, and JVM behavior, not real-device touch feel.

## Documentation

- [Approved specification](docs/superpowers/specs/2026-08-29-reachgrid.md)
- [Measurement decision](docs/decisions/0001-transparent-local-measurement.md)
- [Working notes](docs/notes/2026-08-29.md)
- [Verification record](docs/experiments/verification-2026-08-29.md)
- [Dataset note](docs/datasets/README.md)
- [Model note](docs/models/README.md)

## License

MIT
