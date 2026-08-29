package dev.ikteder.reachgrid.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReachReportTest {
    @Test
    public void everyCellHasTwoVisitsAndBoundedScore() {
        ReachReport report = reportWithEvidence();
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachReport.CellSummary summary = report.summary(row, column);
                assertEquals(2, summary.visits);
                assertTrue(summary.medianLatencyMilliseconds >= 250L);
                assertTrue(summary.reachScore >= 0 && summary.reachScore <= 100);
            }
        }
    }

    @Test
    public void jsonIsVersionedAndDoesNotContainRawCoordinates() {
        String json = reportWithEvidence().toJson();
        assertTrue(json.startsWith("{\"schemaVersion\":1"));
        assertTrue(json.contains("\"seed\":20260829"));
        assertTrue(json.contains("\"handedness\":\"right\""));
        assertTrue(json.contains("\"samples\":["));
        assertTrue(json.contains("\"cells\":["));
        assertFalse(json.contains("normalizedX"));
        assertFalse(json.contains("normalizedY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteSessionCannotBecomeAReport() {
        ReachSession session = new ReachSession(1L);
        session.start(0L);
        new ReachReport(session, "left", 32);
    }

    private ReachReport reportWithEvidence() {
        ReachSession session = new ReachSession(20260829L);
        long now = 1_000L;
        session.start(now);
        int index = 0;
        while (!session.isComplete()) {
            ReachSession.Target target = session.currentTarget();
            if (index % 7 == 0) session.tap(0.0, 0.0, 0.005, 0.005, now + 50L);
            now += 250L + index * 5L;
            session.tap(target.centerX(), target.centerY(), 0.01, 0.01, now);
            index += 1;
        }
        return new ReachReport(session, "right", 32);
    }
}
