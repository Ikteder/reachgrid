package dev.ikteder.reachgrid.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReachComparisonTest {
    @Test
    public void assignmentIsDeterministicAndUsesBothOrders() {
        ReachComparisonPlan first = new ReachComparisonPlan(20260907L, "right", 24, 40);
        ReachComparisonPlan repeat = new ReachComparisonPlan(20260907L, "right", 40, 24);
        assertEquals(first.radiusForPhase(0), repeat.radiusForPhase(0));
        assertEquals(first.radiusForPhase(1), repeat.radiusForPhase(1));
        assertNotEquals(first.radiusForPhase(0), first.radiusForPhase(1));

        int lowerFirst = 0;
        for (long seed = 20260000L; seed < 20261000L; seed += 1L) {
            if (new ReachComparisonPlan(seed, "right", 24, 40).radiusForPhase(0) == 24) {
                lowerFirst += 1;
            }
        }
        assertTrue(lowerFirst > 400 && lowerFirst < 600);
    }

    @Test
    public void pairedDeltasAreSecondMinusFirst() {
        ReachReport first = completedReport(77L, "left", 24, 500L);
        ReachReport second = completedReport(77L, "left", 40, 400L);
        ReachComparison comparison = new ReachComparison(first, second);

        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachComparison.CellDelta delta = comparison.delta(row, column);
                assertEquals(-100L, delta.medianLatencyDeltaMilliseconds);
                assertEquals(0, delta.missDelta);
                assertEquals(10, delta.reachScoreDelta);
            }
        }
        assertEquals(-100.0, comparison.meanMedianLatencyDeltaMilliseconds(), 0.0);
        assertEquals(10.0, comparison.meanReachScoreDelta(), 0.0);
        assertEquals(0, comparison.totalMissDelta());
        assertTrue(comparison.toJson().contains("\"interpretation\":\"second-minus-first\""));
        assertTrue(comparison.toJson().contains("\"orderRadiusDp\":[24,40]"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mismatchedSeedsCannotBeCompared() {
        new ReachComparison(
                completedReport(1L, "right", 24, 400L),
                completedReport(2L, "right", 40, 400L));
    }

    static ReachReport completedReport(
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
}
