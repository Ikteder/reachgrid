# Model note

ReachGrid contains no machine-learning model. Route generation uses seeded shuffling, and result summaries use documented arithmetic over latency and misses.

The reach score is not a probability, diagnosis, accessibility grade, ergonomic standard, or learned prediction. It is a bounded visualization value for one session. Raw latency and miss counts remain the primary evidence.

Paired mode adds arithmetic second-minus-first deltas and a deterministic seed-derived phase order. Neither is a statistical model, causal estimate, confidence interval, or significance test.
