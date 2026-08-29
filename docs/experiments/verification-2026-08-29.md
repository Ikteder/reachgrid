# Verification record, 2026-08-29

## Local environment

- Operating system: Windows
- Available JDK: Java 8
- Android SDK, emulator, Android Studio, Gradle installation, Flutter, and Dart: unavailable
- Core implementation language: Java compatible with Java 8
- Android build language level: Java 17

## Local evidence

| Check | Actual result |
|---|---|
| Core compilation | `ReachSession`, `ReachReport`, and `CoreVerification` compiled with `javac -source 1.8 -target 1.8` |
| Core verification | 4/4 groups passed |
| Route coverage | 48 targets; all 24 cells appeared exactly twice; no immediate repeated cell |
| Miss evidence | 6 injected misses were preserved in summaries |
| JSON parse | Valid JSON; schema 1; 48 samples; 24 cell summaries; 6 misses |
| Privacy assertion | No `normalizedX` or `normalizedY` fields in exported samples |
| Android resource parse | All 4 XML files parsed successfully |
| Manifest privacy check | `android.permission.INTERNET` absent |
| README punctuation check | 3 README files checked; 0 em dash characters |
| Wrapper integrity | Wrapper JAR SHA-256 `497C8C2A7E5031F6AA847F88104AA80A93532EC32EE17BDB8D1D2F67A194A9C7` |

## Public Android verification

The exact GitHub Actions result will be added after the public repository and CI run are verified. CI is required to provide the Android SDK boundary that the local machine lacks.

## Interpretation boundary

The core behavior is locally executed evidence. Android UI compilation, resource linking, lint, unit tests, and APK assembly require public CI. Neither environment supplies physical-device touch testing, and no statement about reach comfort or accessibility compliance should be inferred from build success.
