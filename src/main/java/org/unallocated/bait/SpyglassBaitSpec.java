package org.unallocated.bait;

import java.util.ArrayList;
import java.util.List;

/**
 * How one spyglass bait behaves; story beats and the director build one and pass it to {@link SpyglassBait#place}.
 * Pure data (no Minecraft types) so the sighting logic can be unit-tested.
 *
 * @param distances          blocks between player and figure for each step, strictly decreasing. The figure starts at
 *                           the first; every spyglass sighting moves it to the next one. The last is "too close": the
 *                           next look at it fires the screamer. Sightings before the scare = {@code distances.size()-1}.
 * @param scare              false: after the last look the figure just disappears (no sound, no image)
 * @param dwellTicks         ticks the figure must stay inside the scope for a look to count as a sighting
 * @param scareDelayTicks    ticks of looking at the too-close figure before the screamer fires
 * @param scareJitterTicks   extra 0..n ticks drawn from {@code seed}, so the moment is not predictable
 * @param fallbackAfterTicks without a spyglass sighting for this long, plain looks also bring it closer (plan §3.3:
 *                           "nadie se va sin vivirlo"); 0 = never
 * @param appearDelayTicks   wait before the figure first appears ("en un momento inesperado")
 * @param maxLifetimeTicks   the figure leaves quietly (while unseen) after this long
 * @param seed               randomness for jitter and placement, fixed for reproducible tests
 */
public record SpyglassBaitSpec(List<Double> distances, boolean scare, int dwellTicks, int scareDelayTicks,
        int scareJitterTicks, int fallbackAfterTicks, int appearDelayTicks, int maxLifetimeTicks, long seed) {
    public static final int MAX_STEPS = 12;
    public static final double MIN_DISTANCE = 4.0;
    public static final double MAX_DISTANCE = 160.0;

    public SpyglassBaitSpec {
        distances = List.copyOf(distances);
        if (distances.isEmpty() || distances.size() > MAX_STEPS) {
            throw new IllegalArgumentException("1.." + MAX_STEPS + " distances required");
        }
        double previous = Double.POSITIVE_INFINITY;
        for (double distance : distances) {
            if (!(distance >= MIN_DISTANCE && distance <= MAX_DISTANCE) || distance >= previous) {
                throw new IllegalArgumentException("distances must decrease within " + MIN_DISTANCE + ".." + MAX_DISTANCE
                        + ": " + distances);
            }
            previous = distance;
        }
        requireRange("dwellTicks", dwellTicks, 1, 200);
        requireRange("scareDelayTicks", scareDelayTicks, 0, 200);
        requireRange("scareJitterTicks", scareJitterTicks, 0, 200);
        requireRange("fallbackAfterTicks", fallbackAfterTicks, 0, 72_000);
        requireRange("appearDelayTicks", appearDelayTicks, 0, 72_000);
        requireRange("maxLifetimeTicks", maxLifetimeTicks, 200, 216_000);
    }

    /** Five looks: four calm sightings at 92, 64, 44 and 28 blocks, then the screamer at 14. */
    public static SpyglassBaitSpec defaults() {
        return new SpyglassBaitSpec(List.of(92.0, 64.0, 44.0, 28.0, 14.0), true, 10, 14, 10, 1800, 0, 6000, 412L);
    }

    /** {@code steps} distances from {@code first} down to {@code closest}, each a constant fraction of the last. */
    public static List<Double> geometric(int steps, double first, double closest) {
        if (steps < 1 || steps > MAX_STEPS) throw new IllegalArgumentException("steps must be 1.." + MAX_STEPS);
        if (steps == 1) return List.of(closest);
        if (!(first > closest)) throw new IllegalArgumentException("first must be farther than closest");
        List<Double> result = new ArrayList<>(steps);
        double ratio = Math.pow(closest / first, 1.0 / (steps - 1));
        for (int i = 0; i < steps; i++) {
            result.add(i == steps - 1 ? closest : Math.round(first * Math.pow(ratio, i) * 10.0) / 10.0);
        }
        return result;
    }

    /**
     * The same approach shrunk proportionally so the first distance is at most {@code cap} (a short render distance
     * must still show the figure). Steps that would collapse below {@link #MIN_DISTANCE} or onto each other merge.
     */
    public SpyglassBaitSpec fittedTo(double cap) {
        double first = distances.getFirst();
        if (first <= cap) return this;
        double factor = Math.max(cap, MIN_DISTANCE) / first;
        List<Double> scaled = new ArrayList<>();
        for (double distance : distances) {
            double value = Math.max(MIN_DISTANCE, Math.round(distance * factor * 10.0) / 10.0);
            if (scaled.isEmpty() || value < scaled.getLast()) scaled.add(value);
        }
        return withDistances(scaled);
    }

    public SpyglassBaitSpec withDistances(List<Double> value) {
        return new SpyglassBaitSpec(value, scare, dwellTicks, scareDelayTicks, scareJitterTicks, fallbackAfterTicks,
                appearDelayTicks, maxLifetimeTicks, seed);
    }

    public SpyglassBaitSpec withScare(boolean value) {
        return new SpyglassBaitSpec(distances, value, dwellTicks, scareDelayTicks, scareJitterTicks, fallbackAfterTicks,
                appearDelayTicks, maxLifetimeTicks, seed);
    }

    public SpyglassBaitSpec withTiming(int dwell, int scareDelay, int scareJitter) {
        return new SpyglassBaitSpec(distances, scare, dwell, scareDelay, scareJitter, fallbackAfterTicks,
                appearDelayTicks, maxLifetimeTicks, seed);
    }

    public SpyglassBaitSpec withFallbackAfterTicks(int value) {
        return new SpyglassBaitSpec(distances, scare, dwellTicks, scareDelayTicks, scareJitterTicks, value,
                appearDelayTicks, maxLifetimeTicks, seed);
    }

    public SpyglassBaitSpec withAppearDelayTicks(int value) {
        return new SpyglassBaitSpec(distances, scare, dwellTicks, scareDelayTicks, scareJitterTicks, fallbackAfterTicks,
                value, maxLifetimeTicks, seed);
    }

    public SpyglassBaitSpec withMaxLifetimeTicks(int value) {
        return new SpyglassBaitSpec(distances, scare, dwellTicks, scareDelayTicks, scareJitterTicks, fallbackAfterTicks,
                appearDelayTicks, value, seed);
    }

    public SpyglassBaitSpec withSeed(long value) {
        return new SpyglassBaitSpec(distances, scare, dwellTicks, scareDelayTicks, scareJitterTicks, fallbackAfterTicks,
                appearDelayTicks, maxLifetimeTicks, value);
    }

    public int lastStep() {
        return distances.size() - 1;
    }

    private static void requireRange(String name, int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException(name + " must be " + min + ".." + max);
    }
}
