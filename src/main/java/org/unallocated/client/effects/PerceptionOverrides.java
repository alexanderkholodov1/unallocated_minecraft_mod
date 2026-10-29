package org.unallocated.client.effects;

import java.util.EnumMap;
import java.util.function.LongSupplier;

/** Process-local perception only. Never owns, changes, saves or restores game options. */
public final class PerceptionOverrides {
    public enum Perception { FOV_SCALE, SENSITIVITY_SCALE, DARKNESS }
    public static final PerceptionOverrides INSTANCE = new PerceptionOverrides(System::nanoTime);
    private record Override(float value, long start, long end) { }
    private final EnumMap<Perception, Override> overrides = new EnumMap<>(Perception.class);
    private final LongSupplier clock;

    PerceptionOverrides(LongSupplier clock) { this.clock = clock; }

    public void apply(Perception kind, float value, int ticks) {
        float min = kind == Perception.DARKNESS ? 0 : kind == Perception.FOV_SCALE ? .96f : .92f;
        float max = kind == Perception.DARKNESS ? .08f : kind == Perception.FOV_SCALE ? 1.04f : 1.08f;
        if (kind == null || !Float.isFinite(value) || value < min || value > max || ticks < 1 || ticks > 100) {
            throw new IllegalArgumentException("Invalid perception override");
        }
        long now = clock.getAsLong();
        overrides.put(kind, new Override(value, now, now + ticks * 50_000_000L));
    }

    public float value(Perception kind) {
        Override override = overrides.get(kind);
        long now = clock.getAsLong();
        if (override == null || now >= override.end) {
            overrides.remove(kind);
            return kind == Perception.DARKNESS ? 0 : 1;
        }
        if (kind != Perception.DARKNESS) return override.value;
        return override.value * (float) ((1 - Math.cos((now - override.start) / 1_000_000_000.0 * Math.PI * 4)) * .5);
    }

    public void clearAll() { overrides.clear(); }
}
