package dev.ikteder.reachgrid.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReachHistoryTest {
    @Test
    public void archiveIsBoundedAndKeepsNewestValidSessions() {
        ReachReport report = ReachComparisonTest.completedReport(7L, "right", 32, 450L);
        String stored = "damaged entry\n-4:AAAA";
        for (int index = 0; index < 5; index += 1) {
            stored = ReachHistory.append(stored, 1_000L + index, report, 3);
        }

        String corrupted = stored.replaceFirst(":", ":0");
        assertEquals(2, ReachHistory.count(corrupted));

        assertEquals(3, ReachHistory.count(stored));
        assertEquals(1_002L, ReachHistory.entries(stored).get(0).savedAtEpochMilliseconds);
        assertEquals(1_004L, ReachHistory.entries(stored).get(2).savedAtEpochMilliseconds);
        String json = ReachHistory.toJson(stored);
        assertTrue(json.startsWith("{\"schemaVersion\":1,\"sessionCount\":3"));
        assertTrue(json.contains("\"report\":{\"schemaVersion\":1"));
        assertFalse(json.contains("normalizedX"));
        assertFalse(json.contains("normalizedY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void archiveLimitMustBePositive() {
        ReachHistory.append("", 1L,
                ReachComparisonTest.completedReport(7L, "right", 32, 450L), 0);
    }
}
