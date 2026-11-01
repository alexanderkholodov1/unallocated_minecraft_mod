package org.unallocated.client.effects;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.unallocated.client.effects.PerceptionOverrides.Perception.*;

class PerceptionOverridesTest {
    @Test void expiresByWallTimeAndClearsWithoutTouchingAnyOptions() {
        var clock = new AtomicLong();
        var state = new PerceptionOverrides(clock::get);
        state.apply(FOV_SCALE, 1.04f, 10);
        state.apply(SENSITIVITY_SCALE, .92f, 10);
        assertEquals(1.04f, state.value(FOV_SCALE));
        assertEquals(.92f, state.value(SENSITIVITY_SCALE));
        clock.set(500_000_000L);
        assertEquals(1, state.value(FOV_SCALE));
        assertEquals(1, state.value(SENSITIVITY_SCALE));
        state.apply(FOV_SCALE, .96f, 100);
        state.clearAll();
        assertEquals(1, state.value(FOV_SCALE));
        assertEquals(0, state.value(DARKNESS));
    }

    @Test void darknessIsBoundedAndAFreshProcessStateHasNoDrift() {
        var clock = new AtomicLong();
        var state = new PerceptionOverrides(clock::get);
        state.apply(DARKNESS, .08f, 100);
        clock.set(250_000_000L);
        assertEquals(.08f, state.value(DARKNESS), .00001f);
        assertEquals(0, new PerceptionOverrides(clock::get).value(DARKNESS));
        clock.set(500_000_000L);
        assertEquals(0, state.value(DARKNESS), .00001f);
    }

    @Test void refusesUnboundedOrNonFiniteRequests() {
        var state = new PerceptionOverrides(() -> 0);
        assertThrows(IllegalArgumentException.class, () -> state.apply(FOV_SCALE, 1.1f, 20));
        assertThrows(IllegalArgumentException.class, () -> state.apply(SENSITIVITY_SCALE, .5f, 20));
        assertThrows(IllegalArgumentException.class, () -> state.apply(DARKNESS, Float.NaN, 20));
        assertThrows(IllegalArgumentException.class, () -> state.apply(DARKNESS, .08f, 101));
    }
}
