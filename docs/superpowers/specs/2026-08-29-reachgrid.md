# ReachGrid design specification

Date: 2026-08-29

Status: Approved for implementation

## Purpose

ReachGrid is a native Android application that helps mobile designers and developers collect a small, local one-handed touch ergonomics session. It presents targets across a 4 by 6 grid, records successful-target latency and misses, summarizes each screen zone, and exports a versioned JSON report.

The app is a diagnostic aid, not a medical, accessibility-compliance, or population-level usability claim. A session describes one person, device, posture, target size, and moment in time.

## Core experience

1. Choose left or right hand and a target size.
2. Start a deterministic 48-target session containing two visits to each grid cell.
3. Tap the highlighted target. A miss is assigned to the current target and does not advance the sequence.
4. View a heatmap with median successful-tap latency, miss count, and a transparent reach score for every cell.
5. Share the report as JSON through the Android Sharesheet or restart with the same daily seed.

## Measurement model

- Grid: 4 columns by 6 rows.
- Repetitions: 2 per cell, 48 successful targets total.
- Route: Fisher-Yates shuffle using `java.util.Random` and the recorded integer seed.
- Latency: elapsed monotonic milliseconds from target presentation to the successful tap, including time spent on misses.
- Miss: a touch outside the active circular target.
- Cell median: median of successful latencies for that cell.
- Accuracy: successful visits divided by successful visits plus misses attributed to the cell.
- Reach score: `round(100 * latencyFactor * accuracy)`, where `latencyFactor = clamp(1 - (medianMs - 250) / 1000, 0, 1)`.

The score is a visualization convenience, not a standardized human-factors metric. Raw latency and miss evidence remains visible in the report.

## Architecture

- `core/ReachSession.java`: deterministic route, state transitions, hit testing, and immutable samples.
- `core/ReachReport.java`: per-cell aggregation and dependency-free JSON export.
- `ui/ReachGridView.java`: target presentation and result heatmap.
- `MainActivity.java`: setup controls, session lifecycle, progress, and Android sharing.
- `tools/CoreVerification.java`: local JVM verification that needs no Android SDK.
- Android unit tests repeat the core cases in the Gradle build.

## Privacy and safety

- No internet permission, account, analytics, advertising, or background service.
- No touch coordinates are stored beyond normalized target outcome evidence. The report contains target cell, latency, miss count, hand, size, seed, and app schema version.
- Reports leave the app only when the user invokes Android sharing.
- The app does not diagnose dexterity, injury, disability, or accessibility conformance.

## Accessibility and interaction

- High-contrast target and result colors.
- Target radius options of 24, 32, and 40 density-independent pixels.
- Text labels accompany heatmap colors.
- The active target description is announced through Android accessibility services.
- The session intentionally measures direct touch. Results collected with assistive input or screen-reader exploration are not comparable to direct-touch sessions and must be interpreted separately.

## Acceptance criteria

- A fixed seed produces the same route.
- Every cell appears exactly twice, with no immediate repeated cell.
- Misses do not advance the route and are attributed to the active cell.
- Successful taps advance once and completion occurs after exactly 48 hits.
- Aggregation reports median latency, misses, accuracy, and bounded score per cell.
- JSON output is valid, versioned, deterministic for fixed evidence, and contains no raw touch coordinates.
- The app has no network permission.
- Local Java verification passes.
- Public CI passes Android unit tests, lint, and debug APK assembly.
- Every README file contains no em dash character.

## Non-goals

- Cloud accounts, cross-user comparison, background tracking, medical assessment, standardized accessibility certification, gesture recognition, biometric inference, and iOS support are out of scope for version 1.
