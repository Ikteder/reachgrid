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

[GitHub Actions run 33279739328](https://github.com/Ikteder/reachgrid/actions/runs/33279739328) completed successfully on the public repository.

| Check | Actual result |
|---|---|
| SDK setup | Android platform 36 and Build Tools 36.0.0 installed successfully |
| Independent core check | 4/4 verification groups passed; 6 misses preserved |
| JVM unit tests | 7/7 tests passed through `testDebugUnitTest` |
| Android lint | `lintDebug` passed |
| APK assembly | `assembleDebug` passed; `app-debug.apk` existed |
| Manifest privacy check | Internet permission absent |
| Debug APK SHA-256 | `fea8ecb4ad2fdd98151457fd90c92ef59804e958c164bf201ab7b602e6d21342` |

The first public run exposed a missing `sdkmanager` path in the workflow and stopped before project compilation. The workflow was corrected to install Android tooling explicitly. The replacement run passed, and the final run above also passed after current accessibility signaling replaced a deprecated announcement call.

## Interpretation boundary

The core behavior is locally executed evidence. Android UI compilation, resource linking, lint, unit tests, and APK assembly require public CI. Neither environment supplies physical-device touch testing, and no statement about reach comfort or accessibility compliance should be inferred from build success.
