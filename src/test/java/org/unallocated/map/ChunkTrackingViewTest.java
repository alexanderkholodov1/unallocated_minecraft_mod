package org.unallocated.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

/** Regression for the map tour's unreachable square-download predicate in Minecraft 26.3. */
class ChunkTrackingViewTest {
    @Test
    void radiusEightStreamsARoundedViewIncludingChunksOutsideTheHelpersSquare() {
        var view = ChunkTrackingView.of(new ChunkPos(0, 0), 8);
        var streamed = new HashSet<ChunkPos>();
        view.forEach(streamed::add);
        assertEquals(329, streamed.size());
        for (int x : new int[] {-8, 8}) for (int z : new int[] {-8, 8}) {
            assertFalse(view.contains(x, z), "Vanilla never sends a radius-eight square corner");
            assertFalse(streamed.contains(new ChunkPos(x, z)));
        }
        assertTrue(view.contains(9, 0), "The actual streamed buffer extends beyond the helper's square");
        assertTrue(streamed.contains(new ChunkPos(9, 0)));
        assertTrue(streamed.contains(new ChunkPos(0, 0)));
    }

    @Test
    void theFabricHarnessDefaultRadiusDoesNotExposeTheSquareMismatch() {
        var view = ChunkTrackingView.of(new ChunkPos(0, 0), 5);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            assertTrue(view.contains(x, z));
        }
    }
}
