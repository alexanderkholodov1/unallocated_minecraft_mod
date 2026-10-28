package org.unallocated.creature;

import java.util.function.Predicate;

/** Conservative steering in small swept steps; world collision and grounding are supplied by the adapter. */
public final class CreatureMotion {
    private CreatureMotion() {}
    public record Point(double x, double y, double z) {
        public double distance(Point other) { return Math.sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z)); }
    }
    public static Point toward(Point from, Point player, double step, double minimum, boolean seen, Predicate<Point> clear) {
        if (seen || step <= 0) return from;
        double dx = player.x - from.x, dz = player.z - from.z, horizontal = Math.hypot(dx, dz);
        if (horizontal < 1.0e-8 || from.distance(player) <= minimum) return from;
        double dy = player.y - from.y;
        double keepHorizontal = Math.sqrt(Math.max(0, minimum*minimum - dy*dy));
        double distance = Math.min(step, Math.max(0, horizontal - keepHorizontal));
        Point last = from; int samples = Math.max(1, (int) Math.ceil(distance / .2));
        for (int i = 1; i <= samples; i++) {
            double fraction = distance * i / samples / horizontal;
            Point next = new Point(from.x + dx * fraction, from.y, from.z + dz * fraction);
            if (!clear.test(next)) break;
            last = next;
        }
        return last;
    }
    public static boolean inView(double dot, double distance, boolean scoping, boolean lineOfSight) {
        return lineOfSight && (distance < .01 || dot >= Math.cos(Math.toRadians(scoping ? 5.5 : 80)));
    }
}
