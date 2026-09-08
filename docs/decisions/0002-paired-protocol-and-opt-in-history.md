# Decision 0002: Paired protocol and opt-in history

Date: 2026-09-07

Status: Accepted

## Context

Comparing independent reach sessions can confound target size with route, hand, and presentation order. Always running the smaller target first adds another systematic choice. Saving every session automatically would also change ReachGrid's original no-persistence privacy boundary without explicit consent.

## Decision

Paired mode fixes the date seed, route, handedness, and grid across two different target radii. A deterministic mixed-seed bit assigns which radius runs first. The UI exposes that order, inserts a deliberate rest transition, and defines all deltas as second minus first.

Local saving remains off by default. When enabled, private app preferences retain at most the newest 20 completed session reports. Each stored item adds a save timestamp and SHA-256 integrity digest but preserves the existing versioned session JSON and no-coordinate boundary. Sharing and clearing require explicit user actions.

## Consequences

- Matching the route and hand removes two avoidable sources of variation.
- Reproducible order assignment prevents the implementation from silently choosing one size first every time.
- A same-seed rerun receives the same order, which is useful for auditability but is not full experimental counterbalancing for one person.
- Learning, fatigue, rest duration, posture, and grip can still affect the second phase.
- Private bounded history supports later inspection without accounts, networking, or unbounded storage.
- The archive records completed sessions, not in-progress recovery, and malformed stored entries are skipped.
