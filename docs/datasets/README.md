# Dataset note

ReachGrid does not download, bundle, train on, or evaluate against a dataset. A user creates one small local session through direct touch.

The report contains target cell identifiers, successful-tap latency, misses, selected hand, target radius, seed, and transparent summaries. It does not contain raw touch coordinates, identity, account information, or network-derived data. Reports leave the application only through an explicit Android share action.

A single session is not representative data for other people, devices, grips, or accessibility needs.

Version 1.1 can retain up to 20 completed sessions in private app preferences after explicit opt-in. A paired comparison holds route seed and handedness constant across two target radii and reports second-minus-first cell deltas. This remains a small local convenience sample, not a research dataset. Order assignment cannot remove learning, fatigue, posture drift, or other carryover effects.
