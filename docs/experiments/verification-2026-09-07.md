# Verification record, 2026-09-07

## Local environment

- Operating system: Windows
- Available JDK: Java 8 update 381
- Android SDK and emulator: unavailable
- Android build target: JDK 17, SDK platform 36, Build Tools 36.0.0 in public CI

## Local evidence

| Check | Actual result |
|---|---|
| Core compilation | Five core classes and `CoreVerification` compiled with `javac -source 1.8 -target 1.8 -Xlint:all` |
| Core harness | 8/8 verification groups passed |
| Original behavior | Deterministic route, balanced 48-target coverage, miss attribution, completion, summaries, and bounded scores passed |
| Order reproducibility | Reversed constructor inputs produced the same assigned order for seed `20260907` |
| Order sweep | Across seeds `20260000` through `20260999`, lower radius ran first 526/1,000 times |
| Paired fixture | All 24 cells produced second-minus-first values of `-100 ms`, `0 misses`, and `+10 score` |
| Comparison JSON | Parsed schema 1 with 24 cells and 48 samples in each nested report |
| History behavior | Malformed entries were skipped; a five-append fixture with limit 3 retained timestamps 1002 through 1004 |
| Privacy | Comparison and history fixtures contained no `normalizedX` or `normalizedY` fields |

## Public Android verification

Pending the publication commit and GitHub Actions run. This section must be updated with the actual run URL, test count, lint result, APK result, and artifact hash before the improvement is reported complete.

## Interpretation boundary

The synthetic fixture verifies calculations and serialization, not human performance. The order sweep verifies deterministic use of both orders, not elimination of learning or fatigue. Android build evidence will not substitute for emulator or physical-device touch testing.
