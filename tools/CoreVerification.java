import dev.ikteder.reachgrid.core.ReachReport;
import dev.ikteder.reachgrid.core.ReachSession;

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

        if (args.length == 1 && "--json".equals(args[0])) {
            System.out.println(report.toJson());
            return;
        }
        System.out.println("PASS deterministic route");
        System.out.println("PASS balanced 48-target coverage");
        System.out.println("PASS miss attribution and completion");
        System.out.println("PASS summaries and bounded scores");
        System.out.println("4/4 core verification groups passed; misses=" + injectedMisses);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
