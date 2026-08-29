# Decision 0001: Transparent local measurement

Date: 2026-08-29

Status: Accepted

## Context

A touch heatmap can look authoritative even when it comes from very little evidence. Hidden smoothing or a learned score would make a one-session result easy to overinterpret. Collecting raw touch coordinates would also add privacy cost without being necessary for a zone-level experiment.

## Decision

ReachGrid uses a fixed 4 by 6 grid, two successful visits per cell, a seeded route, monotonic latency, and misses attributed to the active cell. It stores target cells rather than raw touch coordinates.

Each cell exposes visits, median latency, misses, accuracy, and a documented score. The score combines a clamped latency factor with accuracy and is explicitly labeled as a visualization convenience.

## Consequences

- A report can be inspected and reproduced without a model.
- The JSON is small and does not disclose exact touch locations.
- Two visits per cell are enough for a quick diagnostic, not a stable population estimate.
- The grid cannot reveal within-cell reach gradients.
- Repeated sessions can still be influenced by learning, posture, fatigue, and device grip.
