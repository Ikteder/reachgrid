package dev.ikteder.reachgrid.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReachSessionTest {
    @Test
    public void routeIsDeterministicBalancedAndHasNoImmediateRepeats() {
        ReachSession first = new ReachSession(20260829L);
        ReachSession same = new ReachSession(20260829L);
        ReachSession different = new ReachSession(20260830L);
        assertArrayEquals(first.routeCells(), same.routeCells());
        assertFalse(java.util.Arrays.equals(first.routeCells(), different.routeCells()));
        int[] counts = new int[ReachSession.COLUMNS * ReachSession.ROWS];
        int previous = -1;
        for (int cell : first.routeCells()) {
            counts[cell] += 1;
            assertNotEquals(previous, cell);
            previous = cell;
        }
        for (int count : counts) assertEquals(2, count);
    }

    @Test
    public void missDoesNotAdvanceAndHitRecordsEvidence() {
        ReachSession session = new ReachSession(10L);
        session.start(1_000L);
        ReachSession.Target target = session.currentTarget();
        ReachSession.TapResult miss = session.tap(0.0, 0.0, 0.01, 0.01, 1_050L);
        assertFalse(miss.hit);
        assertEquals(0, session.successfulTargets());
        assertEquals(target.cell(), session.currentTarget().cell());
        ReachSession.TapResult hit = session.tap(target.centerX(), target.centerY(), 0.01, 0.01, 1_200L);
        assertTrue(hit.hit);
        assertEquals(1, session.successfulTargets());
        assertEquals(200L, session.samples().get(0).latencyMilliseconds);
        assertEquals(1, session.samples().get(0).misses);
    }

    @Test
    public void exactlyFortyEightHitsCompleteTheSession() {
        ReachSession session = completedSession(20260829L);
        assertTrue(session.isComplete());
        assertEquals(48, session.successfulTargets());
    }

    @Test(expected = IllegalStateException.class)
    public void cannotTapAfterCompletion() {
        ReachSession session = completedSession(4L);
        session.tap(0.5, 0.5, 0.1, 0.1, 20_000L);
    }

    private ReachSession completedSession(long seed) {
        ReachSession session = new ReachSession(seed);
        long now = 100L;
        session.start(now);
        while (!session.isComplete()) {
            ReachSession.Target target = session.currentTarget();
            now += 300L;
            session.tap(target.centerX(), target.centerY(), 0.01, 0.01, now);
        }
        return session;
    }
}
