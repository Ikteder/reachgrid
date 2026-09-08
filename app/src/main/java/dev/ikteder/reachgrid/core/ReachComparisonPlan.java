package dev.ikteder.reachgrid.core;

public final class ReachComparisonPlan {
    private final long seed;
    private final String handedness;
    private final int firstRadiusDp;
    private final int secondRadiusDp;

    public ReachComparisonPlan(long seed, String handedness, int radiusADp, int radiusBDp) {
        if (!"left".equals(handedness) && !"right".equals(handedness)) {
            throw new IllegalArgumentException("handedness must be left or right");
        }
        if (!allowedRadius(radiusADp) || !allowedRadius(radiusBDp) || radiusADp == radiusBDp) {
            throw new IllegalArgumentException("comparison requires two distinct supported radii");
        }
        this.seed = seed;
        this.handedness = handedness;
        int lower = Math.min(radiusADp, radiusBDp);
        int higher = Math.max(radiusADp, radiusBDp);
        long assignment = mix(seed ^ (((long) lower) << 32) ^ higher ^ handedness.hashCode());
        boolean reverse = (assignment & 1L) != 0L;
        this.firstRadiusDp = reverse ? higher : lower;
        this.secondRadiusDp = reverse ? lower : higher;
    }

    public long seed() {
        return seed;
    }

    public String handedness() {
        return handedness;
    }

    public int radiusForPhase(int zeroBasedPhase) {
        if (zeroBasedPhase == 0) return firstRadiusDp;
        if (zeroBasedPhase == 1) return secondRadiusDp;
        throw new IndexOutOfBoundsException("paired protocol has exactly two phases");
    }

    private static boolean allowedRadius(int radiusDp) {
        return radiusDp == 24 || radiusDp == 32 || radiusDp == 40;
    }

    private static long mix(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
