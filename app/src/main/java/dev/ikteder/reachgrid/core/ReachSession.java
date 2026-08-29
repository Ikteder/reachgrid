package dev.ikteder.reachgrid.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ReachSession {
    public static final int COLUMNS = 4;
    public static final int ROWS = 6;
    public static final int REPETITIONS = 2;
    public static final int TARGET_COUNT = COLUMNS * ROWS * REPETITIONS;

    public static final class Target {
        public final int row;
        public final int column;

        Target(int cell) {
            this.row = cell / COLUMNS;
            this.column = cell % COLUMNS;
        }

        public double centerX() {
            return (column + 0.5) / COLUMNS;
        }

        public double centerY() {
            return (row + 0.5) / ROWS;
        }

        public int cell() {
            return row * COLUMNS + column;
        }
    }

    public static final class Sample {
        public final int sequence;
        public final int row;
        public final int column;
        public final long latencyMilliseconds;
        public final int misses;

        Sample(int sequence, Target target, long latencyMilliseconds, int misses) {
            this.sequence = sequence;
            this.row = target.row;
            this.column = target.column;
            this.latencyMilliseconds = latencyMilliseconds;
            this.misses = misses;
        }
    }

    public static final class TapResult {
        public final boolean hit;
        public final boolean complete;
        public final int successfulTargets;
        public final int missesOnCurrentTarget;

        TapResult(boolean hit, boolean complete, int successfulTargets, int missesOnCurrentTarget) {
            this.hit = hit;
            this.complete = complete;
            this.successfulTargets = successfulTargets;
            this.missesOnCurrentTarget = missesOnCurrentTarget;
        }
    }

    private final long seed;
    private final int[] route;
    private final List<Sample> samples = new ArrayList<>();
    private boolean started;
    private boolean complete;
    private int routeIndex;
    private int currentMisses;
    private long targetStartedMilliseconds;

    public ReachSession(long seed) {
        this.seed = seed;
        this.route = buildRoute(seed);
    }

    public void start(long nowMilliseconds) {
        if (started) {
            throw new IllegalStateException("session has already started");
        }
        if (nowMilliseconds < 0) {
            throw new IllegalArgumentException("start time must be non-negative");
        }
        started = true;
        targetStartedMilliseconds = nowMilliseconds;
    }

    public TapResult tap(
            double normalizedX,
            double normalizedY,
            double normalizedRadiusX,
            double normalizedRadiusY,
            long nowMilliseconds) {
        requireRunning();
        if (!finiteUnit(normalizedX) || !finiteUnit(normalizedY)) {
            throw new IllegalArgumentException("tap coordinates must be finite values from 0 to 1");
        }
        if (!(normalizedRadiusX > 0.0) || !(normalizedRadiusY > 0.0)) {
            throw new IllegalArgumentException("target radii must be positive");
        }
        if (nowMilliseconds < targetStartedMilliseconds) {
            throw new IllegalArgumentException("tap time cannot move backwards");
        }

        Target target = currentTarget();
        double dx = (normalizedX - target.centerX()) / normalizedRadiusX;
        double dy = (normalizedY - target.centerY()) / normalizedRadiusY;
        boolean hit = dx * dx + dy * dy <= 1.0;
        if (!hit) {
            currentMisses += 1;
            return new TapResult(false, false, samples.size(), currentMisses);
        }

        long latency = nowMilliseconds - targetStartedMilliseconds;
        samples.add(new Sample(routeIndex, target, latency, currentMisses));
        routeIndex += 1;
        currentMisses = 0;
        if (routeIndex == route.length) {
            complete = true;
        } else {
            targetStartedMilliseconds = nowMilliseconds;
        }
        return new TapResult(true, complete, samples.size(), 0);
    }

    public Target currentTarget() {
        requireRunning();
        return new Target(route[routeIndex]);
    }

    public List<Sample> samples() {
        return Collections.unmodifiableList(new ArrayList<>(samples));
    }

    public long seed() {
        return seed;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isComplete() {
        return complete;
    }

    public int successfulTargets() {
        return samples.size();
    }

    public int totalTargets() {
        return route.length;
    }

    public int[] routeCells() {
        return route.clone();
    }

    private void requireRunning() {
        if (!started) {
            throw new IllegalStateException("session has not started");
        }
        if (complete) {
            throw new IllegalStateException("session is complete");
        }
    }

    private static boolean finiteUnit(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0 && value <= 1.0;
    }

    private static int[] buildRoute(long seed) {
        int cells = COLUMNS * ROWS;
        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        for (int cell = 0; cell < cells; cell += 1) {
            first.add(cell);
            second.add(cell);
        }
        Random random = new Random(seed);
        Collections.shuffle(first, random);
        Collections.shuffle(second, random);
        if (first.get(cells - 1).equals(second.get(0))) {
            for (int index = 1; index < second.size(); index += 1) {
                if (!second.get(index).equals(first.get(cells - 1))) {
                    Collections.swap(second, 0, index);
                    break;
                }
            }
        }
        int[] result = new int[cells * REPETITIONS];
        for (int index = 0; index < cells; index += 1) {
            result[index] = first.get(index);
            result[index + cells] = second.get(index);
        }
        return result;
    }
}
