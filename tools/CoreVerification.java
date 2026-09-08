import dev.ikteder.reachgrid.core.ReachReport;
import dev.ikteder.reachgrid.core.ReachSession;
import dev.ikteder.reachgrid.core.ReachComparison;
import dev.ikteder.reachgrid.core.ReachComparisonPlan;
import dev.ikteder.reachgrid.core.ReachHistory;

import java.util.Arrays;

public final class CoreVerification {
    public static void main(String[] args) {
        ReachSession first = new ReachSession(20260829L);
        ReachSession repeat = new ReachSession(20260829L);
        require(Arrays.equals(first.routeCells(), repeat.routeCells()), "same seed must reproduce route");

        int[] counts = new int[ReachSession.COLUMNS * ReachSession.ROWS];
        int previous = -1;
        for (int cell : first.routeCells()) {
            require(cell != previous, "route must not repeat a cell immediately");
            counts[cell] += 1;
            previous = cell;
        }
        for (int count : counts) require(count == 2, "every cell must appear twice");

        long now = 1_000L;
        first.start(now);
        int injectedMisses = 0;
        while (!first.isComplete()) {
            ReachSession.Target target = first.currentTarget();
            if (first.successfulTargets() % 8 == 0) {
                ReachSession.TapResult miss = first.tap(0.0, 0.0, 0.005, 0.005, now + 25L);
                require(!miss.hit, "outside touch must miss");
                injectedMisses += 1;
            }
            now += 280L + first.successfulTargets() * 3L;
            ReachSession.TapResult hit = first.tap(target.centerX(), target.centerY(), 0.01, 0.01, now);
            require(hit.hit, "target center must hit");
        }
        require(first.successfulTargets() == 48, "session must contain 48 successful targets");

        ReachReport report = new ReachReport(first, "right", 32);
        int summaryMisses = 0;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachReport.CellSummary summary = report.summary(row, column);
                require(summary.visits == 2, "each summary must contain two visits");
                require(summary.reachScore >= 0 && summary.reachScore <= 100, "score must be bounded");
                summaryMisses += summary.misses;
            }
        }
        require(summaryMisses == injectedMisses, "summary misses must match injected evidence");

        ReachComparisonPlan plan = new ReachComparisonPlan(20260907L, "right", 24, 40);
        ReachComparisonPlan repeatPlan = new ReachComparisonPlan(20260907L, "right", 40, 24);
        require(plan.radiusForPhase(0) == repeatPlan.radiusForPhase(0),
                "counterbalanced order must be deterministic and input-order independent");
        int lowerFirst = 0;
        for (long seed = 20260000L; seed < 20261000L; seed += 1L) {
            if (new ReachComparisonPlan(seed, "right", 24, 40).radiusForPhase(0) == 24) {
                lowerFirst += 1;
            }
        }
        require(lowerFirst > 400 && lowerFirst < 600,
                "seed-derived assignment must exercise both orders without a large imbalance");

        ReachReport slower = completedReport(99L, "left", 24, 500L);
        ReachReport faster = completedReport(99L, "left", 40, 400L);
        ReachComparison comparison = new ReachComparison(slower, faster);
        require(comparison.meanMedianLatencyDeltaMilliseconds() == -100.0,
                "paired mean latency must be second minus first");
        require(comparison.meanReachScoreDelta() == 10.0,
                "paired mean score must be second minus first");
        require(comparison.totalMissDelta() == 0, "matched fixture must have zero miss delta");

        String history = "broken";
        for (int index = 0; index < 5; index += 1) {
            history = ReachHistory.append(history, 1_000L + index, report, 3);
        }
        require(ReachHistory.count(history) == 3, "history must retain only its newest entries");
        require(ReachHistory.entries(history).get(0).savedAtEpochMilliseconds == 1_002L,
                "history must discard oldest entries first");
        String historyJson = ReachHistory.toJson(history);
        require(!historyJson.contains("normalizedX") && !historyJson.contains("normalizedY"),
                "history must preserve the no-coordinate privacy boundary");

        if (args.length == 1 && "--json".equals(args[0])) {
            System.out.println(comparison.toJson());
            return;
        }
        System.out.println("PASS deterministic route");
        System.out.println("PASS balanced 48-target coverage");
        System.out.println("PASS miss attribution and completion");
        System.out.println("PASS summaries and bounded scores");
        System.out.println("PASS deterministic counterbalanced pair assignment; lowerFirst=" + lowerFirst);
        System.out.println("PASS paired cell and overall deltas");
        System.out.println("PASS bounded, corruption-tolerant local history");
        System.out.println("PASS history privacy and comparison JSON");
        System.out.println("8/8 core verification groups passed; misses=" + injectedMisses);
    }

    private static ReachReport completedReport(
            long seed, String handedness, int radiusDp, long latencyMilliseconds) {
        ReachSession session = new ReachSession(seed);
        long now = 1_000L;
        session.start(now);
        while (!session.isComplete()) {
            ReachSession.Target target = session.currentTarget();
            now += latencyMilliseconds;
            session.tap(target.centerX(), target.centerY(), 0.01, 0.01, now);
        }
        return new ReachReport(session, handedness, radiusDp);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
