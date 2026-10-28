package org.unallocated.world;

import java.util.*;

/** Stable integer traversal and position-based randomness, independent of tick batch sizes. */
public final class WorldPlan {
    private WorldPlan() {}
    public record Region(int x, int y, int z, int width, int height, int depth) {
        public Region {
            if (width <= 0 || height <= 0 || depth <= 0 || (long) width * height * depth > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Region must contain 1..2147483647 blocks");
        }
        public int size() { return Math.multiplyExact(Math.multiplyExact(width, height), depth); }
        public int x(int index) { return x + index % width; }
        public int y(int index) { return y + index / width / depth; }
        public int z(int index) { return z + index / width % depth; }
    }
    /** Source chunk columns, sliced in Y within the current vanilla modification limit. */
    public static List<Region> batches(Region area, int limit, int dx, int dy, int dz) {
        return batches(area, limit, dx, dy, dz, false);
    }
    public static List<Region> batches(Region area, int limit, int dx, int dy, int dz, boolean overlap) {
        if (limit < 1) throw new IllegalArgumentException("Modification limit must be positive");
        List<Region> result = new ArrayList<>();
        for (int x = area.x; x < area.x + area.width;) {
            int w = Math.min(Math.min(16 - Math.floorMod(x, 16), area.x + area.width - x), limit);
            for (int z = area.z; z < area.z + area.depth;) {
                int d = Math.min(Math.min(16 - Math.floorMod(z, 16), area.z + area.depth - z), limit / w);
                if (overlap) d = 1;
                int h = overlap ? 1 : Math.max(1, limit / (w * d));
                for (int y = area.y; y < area.y + area.height; y += h)
                    result.add(new Region(x, y, z, w, Math.min(h, area.y + area.height - y), d));
                z += d;
            }
            x += w;
        }
        // memmove ordering also preserves sources when a same-dimension copy overlaps itself.
        result.sort(Comparator.comparingInt((Region r) -> dy > 0 ? -r.y : r.y)
            .thenComparingInt(r -> dz > 0 ? -r.z : r.z).thenComparingInt(r -> dx > 0 ? -r.x : r.x));
        return result;
    }
    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
    public static double random(long seed, int index, int rule) {
        return (mix(seed ^ mix(index) ^ mix(rule + 0x9e3779b97f4a7c15L)) >>> 11) * 0x1.0p-53;
    }
    public static int replacement(long seed, int index, int rule, double ratio, int choices) {
        if (random(seed, index, rule) >= ratio) return -1;
        return (int) (random(seed ^ 0x632be59bd9b4e019L, index, rule) * choices);
    }
    public static int due(int total, int elapsed, int duration) {
        return duration == 0 ? total : (int) Math.min(total, (long) total * elapsed / duration);
    }
}
