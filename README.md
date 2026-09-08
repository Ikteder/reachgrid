# ReachGrid

[![CI](https://github.com/Ikteder/reachgrid/actions/workflows/ci.yml/badge.svg)](https://github.com/Ikteder/reachgrid/actions/workflows/ci.yml)

ReachGrid is a native Android touch ergonomics lab for exploring one-handed reach on a specific device. It presents a deterministic sequence of targets across a 4 by 6 grid, records successful-tap latency and misses, draws per-zone heatmaps, and shares versioned JSON only when the user asks.

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

## Single-session experience

1. Select left or right hand.
2. Select a target radius of 24, 32, or 40 dp.
3. Tap Start session and follow the highlighted targets.
4. Review the labeled heatmap after 48 successful taps.
5. Use Share JSON to send the report through the Android Sharesheet.

The route is generated from the local `YYYYMMDD` date. Repeating a session on the same day gives the same route, which makes before-and-after layout checks easier to compare.

## Paired target-size comparison

Choose 24/32, 32/40, or 24/40 dp comparison mode to run two matched phases. ReachGrid:

- keeps the date seed, handedness, grid, and 48-target route identical across both phases;
- assigns the first target size deterministically from the seed, so the same input reproduces the order and consecutive seeds exercise both orders;
- pauses after phase 1 so the user can rest and reset their grip;
- draws a delta heatmap after phase 2 with second-minus-first latency and reach-score changes for every cell;
- exports the assigned order, overall descriptive changes, all 24 cell deltas, and both raw session reports.

The order assignment reduces a fixed small-first or large-first bias. It does not remove learning, fatigue, posture drift, or other carryover effects. Deltas are descriptive evidence from one matched pair, not a statistical-significance or causal claim.

## Opt-in local history

Saving is off by default. If `Save completed sessions locally` is checked, completed individual sessions are added to private app preferences. The archive:

- retains the newest 20 sessions;
- stores a save timestamp with each existing session report;
- skips damaged stored entries instead of hiding later valid entries;
- can be shared explicitly as one versioned JSON document;
- can be cleared after a confirmation prompt.

Unchecking saving stops future additions but does not silently delete prior evidence. Use `Clear history` to remove the private local archive.

## Privacy

- No internet permission.
- No account, analytics, advertising, telemetry, or background service.
- No raw touch coordinates in reports.
- Local history is off by default, private to the app, capped at 20 sessions, and removable in the app.
- A report leaves the app only through an explicit Android share action.

Session reports contain the seed, selected hand, target radius, grid dimensions, target cell, latency, misses, accuracy, and score. Comparison reports contain the assigned order and second-minus-first deltas. History exports add only a local save timestamp around each session report.

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

The route, state machine, aggregation, paired protocol, delta calculation, bounded history codec, and JSON exporters are plain Java. They can be checked with only a JDK:

```bash
mkdir -p build/core-verification
javac -source 8 -target 8 -d build/core-verification \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachSession.java \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachReport.java \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachComparisonPlan.java \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachComparison.java \
  app/src/main/java/dev/ikteder/reachgrid/core/ReachHistory.java \
  tools/CoreVerification.java
java -cp build/core-verification CoreVerification
```

Local Windows verification uses JDK 8 for this dependency-free core. Android compilation, unit tests, lint, and APK assembly run in public CI with JDK 17 and the Android 36 SDK. See the [1.1 verification record](docs/experiments/verification-2026-09-07.md) for current evidence and boundaries.

## Project structure

```text
app/src/main/java/dev/ikteder/reachgrid/
  MainActivity.java          Android setup, progress, reset, and sharing
  ReachGridView.java         Target rendering, touch input, and heatmaps
  core/ReachSession.java     Deterministic route and measurement state
  core/ReachReport.java      Aggregation, scoring, and JSON
  core/ReachComparisonPlan.java  Seeded two-phase order assignment
  core/ReachComparison.java      Compatibility checks and paired deltas
  core/ReachHistory.java         Bounded private-history encoding and export
app/src/test/                Android Gradle unit tests
tools/CoreVerification.java  SDK-free verification harness
```

## Current limitations

- Results describe one person, device, posture, hand, target size, and short session. They do not generalize automatically.
- The app is portrait-only in version 1.
- An in-progress session or pair resets if the activity or process is recreated.
- Saved history contains completed sessions only; there is no search, chart across many sessions, CSV export, or cloud synchronization.
- The target sequence is deterministic, but learning and fatigue can still affect repeated sessions.
- Seed-derived order assignment varies which radius runs first but cannot eliminate carryover effects in a two-phase test.
- Direct-touch and assistive-input sessions are not comparable without a separate protocol.
- Heatmap colors are supported by numeric labels, but the custom measurement surface is not a substitute for a complete accessibility study.
- No emulator or physical-device interaction was available on the local Windows machine. Public CI verifies the Android build, lint, and JVM behavior, not real-device touch feel.

## Documentation

- [Approved specification](docs/superpowers/specs/2026-08-29-reachgrid.md)
- [1.1 approved specification](docs/superpowers/specs/2026-09-07-reachgrid-1.1.md)
- [Measurement decision](docs/decisions/0001-transparent-local-measurement.md)
- [Paired protocol and history decision](docs/decisions/0002-paired-protocol-and-opt-in-history.md)
- [Working notes](docs/notes/2026-08-29.md)
- [1.1 working notes](docs/notes/2026-09-07.md)
- [1.1 verification record](docs/experiments/verification-2026-09-07.md)
- [Dataset note](docs/datasets/README.md)
- [Model note](docs/models/README.md)

## License

MIT
