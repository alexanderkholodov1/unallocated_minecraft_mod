package org.unallocated.core;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionStateTest {
    @Test void savedDataCodecRoundTripPreservesEveryFieldAndIndependentCollections() {
        var state = new SessionState(987654321);
        state.moveTo(Act.LOBBY);
        state.tickClock(); state.tickClock();
        state.moveTo(Act.POCKET);
        state.tickClock();
        state.addFlag("knows_name"); state.addFlag("sees_threads");
        state.setCounter("loop_laps", 4); state.setCounter("screamers", 2);
        state.setProfile("fear", 0.73); state.setProfile("confidence", 0.9);
        state.setTemperature(1.4);
        var encoded = SessionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        var restored = SessionState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(state.toTag(), restored.toTag());
        assertFalse(restored.isDirty());
        restored.addFlag("ending_b");
        assertFalse(state.flags().contains("ending_b"));
        assertThrows(UnsupportedOperationException.class, () -> restored.flags().add("bypass"));
        assertThrows(UnsupportedOperationException.class, () -> restored.counters().put("bypass", 1));
        assertThrows(UnsupportedOperationException.class, () -> restored.profile().put("bypass", 1.0));
    }
    @Test void allPublicMutatorsMarkStateDirty() {
        var state = new SessionState(0);
        state.addFlag("name"); assertTrue(state.isDirty()); state.setDirty(false);
        state.removeFlag("name"); assertTrue(state.isDirty()); state.setDirty(false);
        state.setCounter("count", 1); assertTrue(state.isDirty()); state.setDirty(false);
        state.setProfile("fear", 0.5); assertTrue(state.isDirty()); state.setDirty(false);
        state.setTemperature(1.2); assertTrue(state.isDirty());
    }
    @Test void badTimestampsAreClampedAndUnknownSchemasRejected() {
        var tag = new SessionState(0).toTag();
        tag.putLong("ticks", 10); tag.putLong("actStartTick", 900); tag.putLong("sessionStartTick", -10);
        var restored = SessionState.fromTag(tag);
        assertEquals(0, restored.sessionStartTick()); assertEquals(10, restored.actStartTick());
        tag.putInt("schema", 999);
        assertThrows(IllegalArgumentException.class, () -> SessionState.fromTag(tag));
    }
    @Test void profileAndTemperatureCannotPersistNonFiniteNumbers() {
        var state = new SessionState(0);
        assertThrows(IllegalArgumentException.class, () -> state.setProfile("fear", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> state.setTemperature(Double.POSITIVE_INFINITY));
    }
}
