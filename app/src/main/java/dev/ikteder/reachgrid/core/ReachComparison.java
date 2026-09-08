package dev.ikteder.reachgrid.core;

import java.util.Locale;

public final class ReachComparison {
    public static final int SCHEMA_VERSION = 1;

    public static final class CellDelta {
        public final int row;
        public final int column;
        public final long medianLatencyDeltaMilliseconds;
        public final int missDelta;
        public final int reachScoreDelta;

        CellDelta(int row, int column, long latencyDelta, int missDelta, int scoreDelta) {
            this.row = row;
            this.column = column;
            this.medianLatencyDeltaMilliseconds = latencyDelta;
            this.missDelta = missDelta;
            this.reachScoreDelta = scoreDelta;
        }
    }

    private final ReachReport first;
    private final ReachReport second;
    private final CellDelta[][] deltas;
    private final double meanMedianLatencyDeltaMilliseconds;
    private final int totalMissDelta;
    private final double meanReachScoreDelta;

    public ReachComparison(ReachReport first, ReachReport second) {
        if (first.seed() != second.seed()) {
            throw new IllegalArgumentException("paired reports must use the same route seed");
        }
        if (!first.handedness().equals(second.handedness())) {
            throw new IllegalArgumentException("paired reports must use the same handedness");
        }
        if (first.targetRadiusDp() == second.targetRadiusDp()) {
            throw new IllegalArgumentException("paired reports must use different target radii");
        }
        this.first = first;
        this.second = second;
        this.deltas = new CellDelta[ReachSession.ROWS][ReachSession.COLUMNS];

        long latencyTotal = 0L;
        int missTotal = 0;
        int scoreTotal = 0;
        int cells = ReachSession.ROWS * ReachSession.COLUMNS;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachReport.CellSummary before = first.summary(row, column);
                ReachReport.CellSummary after = second.summary(row, column);
                long latencyDelta = after.medianLatencyMilliseconds - before.medianLatencyMilliseconds;
                int missDelta = after.misses - before.misses;
                int scoreDelta = after.reachScore - before.reachScore;
                deltas[row][column] = new CellDelta(row, column, latencyDelta, missDelta, scoreDelta);
                latencyTotal += latencyDelta;
                missTotal += missDelta;
                scoreTotal += scoreDelta;
            }
        }
        meanMedianLatencyDeltaMilliseconds = latencyTotal / (double) cells;
        totalMissDelta = missTotal;
        meanReachScoreDelta = scoreTotal / (double) cells;
    }

    public ReachReport firstReport() {
        return first;
    }

    public ReachReport secondReport() {
        return second;
    }

    public CellDelta delta(int row, int column) {
        if (row < 0 || row >= ReachSession.ROWS || column < 0 || column >= ReachSession.COLUMNS) {
            throw new IndexOutOfBoundsException("cell is outside the grid");
        }
        return deltas[row][column];
    }

    public double meanMedianLatencyDeltaMilliseconds() {
        return meanMedianLatencyDeltaMilliseconds;
    }

    public int totalMissDelta() {
        return totalMissDelta;
    }

    public double meanReachScoreDelta() {
        return meanReachScoreDelta;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        field(json, "schemaVersion", SCHEMA_VERSION).append(',');
        field(json, "seed", first.seed()).append(',');
        quotedField(json, "handedness", first.handedness()).append(',');
        json.append("\"interpretation\":\"second-minus-first\",");
        json.append("\"orderRadiusDp\":[").append(first.targetRadiusDp()).append(',')
                .append(second.targetRadiusDp()).append("],");
        json.append("\"summary\":{");
        decimalField(json, "meanMedianLatencyDeltaMilliseconds", meanMedianLatencyDeltaMilliseconds).append(',');
        field(json, "totalMissDelta", totalMissDelta).append(',');
        decimalField(json, "meanReachScoreDelta", meanReachScoreDelta).append("},");
        json.append("\"cells\":[");
        boolean firstCell = true;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                if (!firstCell) json.append(',');
                firstCell = false;
                CellDelta delta = deltas[row][column];
                json.append('{');
                field(json, "row", row).append(',');
                field(json, "column", column).append(',');
                field(json, "medianLatencyDeltaMilliseconds", delta.medianLatencyDeltaMilliseconds).append(',');
                field(json, "missDelta", delta.missDelta).append(',');
                field(json, "reachScoreDelta", delta.reachScoreDelta);
                json.append('}');
            }
        }
        json.append("],\"firstReport\":").append(first.toJson()).append(',');
        json.append("\"secondReport\":").append(second.toJson());
        json.append('}');
        return json.toString();
    }

    private static StringBuilder field(StringBuilder json, String name, long value) {
        return json.append('"').append(name).append("\":").append(value);
    }

    private static StringBuilder decimalField(StringBuilder json, String name, double value) {
        return json.append('"').append(name).append("\":")
                .append(String.format(Locale.ROOT, "%.3f", value));
    }

    private static StringBuilder quotedField(StringBuilder json, String name, String value) {
        return json.append('"').append(name).append("\":\"").append(value).append('"');
    }
}
