package dev.ikteder.reachgrid.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ReachReport {
    public static final int SCHEMA_VERSION = 1;

    public static final class CellSummary {
        public final int row;
        public final int column;
        public final int visits;
        public final long medianLatencyMilliseconds;
        public final int misses;
        public final double accuracy;
        public final int reachScore;

        CellSummary(int row, int column, int visits, long median, int misses, double accuracy, int score) {
            this.row = row;
            this.column = column;
            this.visits = visits;
            this.medianLatencyMilliseconds = median;
            this.misses = misses;
            this.accuracy = accuracy;
            this.reachScore = score;
        }
    }

    private final long seed;
    private final String handedness;
    private final int targetRadiusDp;
    private final List<ReachSession.Sample> samples;
    private final CellSummary[][] summaries;

    public ReachReport(ReachSession session, String handedness, int targetRadiusDp) {
        if (!session.isComplete()) {
            throw new IllegalArgumentException("report requires a completed session");
        }
        if (!"left".equals(handedness) && !"right".equals(handedness)) {
            throw new IllegalArgumentException("handedness must be left or right");
        }
        if (targetRadiusDp <= 0) {
            throw new IllegalArgumentException("target radius must be positive");
        }
        this.seed = session.seed();
        this.handedness = handedness;
        this.targetRadiusDp = targetRadiusDp;
        this.samples = session.samples();
        this.summaries = aggregate(samples);
    }

    public CellSummary summary(int row, int column) {
        if (row < 0 || row >= ReachSession.ROWS || column < 0 || column >= ReachSession.COLUMNS) {
            throw new IndexOutOfBoundsException("cell is outside the grid");
        }
        return summaries[row][column];
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        field(json, "schemaVersion", SCHEMA_VERSION).append(',');
        field(json, "seed", seed).append(',');
        quotedField(json, "handedness", handedness).append(',');
        field(json, "targetRadiusDp", targetRadiusDp).append(',');
        field(json, "columns", ReachSession.COLUMNS).append(',');
        field(json, "rows", ReachSession.ROWS).append(',');
        json.append("\"samples\":[");
        for (int index = 0; index < samples.size(); index += 1) {
            if (index > 0) json.append(',');
            ReachSession.Sample sample = samples.get(index);
            json.append('{');
            field(json, "sequence", sample.sequence).append(',');
            field(json, "row", sample.row).append(',');
            field(json, "column", sample.column).append(',');
            field(json, "latencyMilliseconds", sample.latencyMilliseconds).append(',');
            field(json, "misses", sample.misses);
            json.append('}');
        }
        json.append("],\"cells\":[");
        boolean first = true;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                if (!first) json.append(',');
                first = false;
                CellSummary summary = summaries[row][column];
                json.append('{');
                field(json, "row", summary.row).append(',');
                field(json, "column", summary.column).append(',');
                field(json, "visits", summary.visits).append(',');
                field(json, "medianLatencyMilliseconds", summary.medianLatencyMilliseconds).append(',');
                field(json, "misses", summary.misses).append(',');
                json.append("\"accuracy\":").append(String.format(Locale.ROOT, "%.6f", summary.accuracy)).append(',');
                field(json, "reachScore", summary.reachScore);
                json.append('}');
            }
        }
        json.append("]}");
        return json.toString();
    }

    private static CellSummary[][] aggregate(List<ReachSession.Sample> samples) {
        CellSummary[][] result = new CellSummary[ReachSession.ROWS][ReachSession.COLUMNS];
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                List<Long> latencies = new ArrayList<>();
                int misses = 0;
                for (ReachSession.Sample sample : samples) {
                    if (sample.row == row && sample.column == column) {
                        latencies.add(sample.latencyMilliseconds);
                        misses += sample.misses;
                    }
                }
                Collections.sort(latencies);
                long median = median(latencies);
                int visits = latencies.size();
                double accuracy = visits == 0 ? 0.0 : visits / (double) (visits + misses);
                double latencyFactor = clamp(1.0 - (median - 250.0) / 1000.0, 0.0, 1.0);
                int score = (int) Math.round(100.0 * latencyFactor * accuracy);
                result[row][column] = new CellSummary(row, column, visits, median, misses, accuracy, score);
            }
        }
        return result;
    }

    private static long median(List<Long> sorted) {
        if (sorted.isEmpty()) return 0;
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(middle);
        return Math.round((sorted.get(middle - 1) + sorted.get(middle)) / 2.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static StringBuilder field(StringBuilder json, String name, long value) {
        return json.append('"').append(name).append("\":").append(value);
    }

    private static StringBuilder quotedField(StringBuilder json, String name, String value) {
        return json.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
