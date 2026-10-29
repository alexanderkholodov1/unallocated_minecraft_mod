package org.unallocated.world;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldPlanTest {
    @Test void copyBatchesCoverNegativeChunkBoundariesAndRespectEvenTinyLimits() {
        var area = new WorldPlan.Region(-19, -64, -17, 39, 140, 35);
        for (int limit : new int[]{1, 17, 255, 32768}) {
            BitSet visited = new BitSet(area.size());
            for (var batch : WorldPlan.batches(area, limit, 0, 0, 0)) {
                assertTrue(batch.size() <= limit);
                assertEquals(batch.x() >> 4, (batch.x() + batch.width() - 1) >> 4);
                assertEquals(batch.z() >> 4, (batch.z() + batch.depth() - 1) >> 4);
                for (int i = 0; i < batch.size(); i++) {
                    int index = (batch.y(i) - area.y()) * area.width() * area.depth()
                        + (batch.z(i) - area.z()) * area.width() + batch.x(i) - area.x();
                    assertFalse(visited.get(index)); visited.set(index);
                }
            }
            assertEquals(area.size(), visited.cardinality());
        }
    }
    @Test void overlappingCopyOrderingPreservesEverySourceAcrossBatchBoundaries() {
        var area = new WorldPlan.Region(-17, 0, -2, 36, 4, 5);
        for (int dx : new int[]{-2, 0, 2}) for (int dy : new int[]{-1, 0, 1}) for (int dz : new int[]{-1, 0, 1}) {
            Map<String, Integer> blocks = new HashMap<>(), expected = new HashMap<>();
            for (int i = 0; i < area.size(); i++) blocks.put(key(area.x(i), area.y(i), area.z(i)), i);
            expected.putAll(blocks);
            for (int i = 0; i < area.size(); i++) expected.put(key(area.x(i) + dx, area.y(i) + dy, area.z(i) + dz), i);
            for (var batch : WorldPlan.batches(area, 17, dx, dy, dz, true)) {
                List<Integer> snapshot = new ArrayList<>();
                for (int i = 0; i < batch.size(); i++) snapshot.add(blocks.get(key(batch.x(i), batch.y(i), batch.z(i))));
                for (int i = 0; i < batch.size(); i++) blocks.put(key(batch.x(i) + dx, batch.y(i) + dy, batch.z(i) + dz), snapshot.get(i));
            }
            assertEquals(expected, blocks, "offset " + dx + "," + dy + "," + dz);
        }
    }
    private static String key(int x, int y, int z) { return x + ":" + y + ":" + z; }
    @Test void corruptionIsDeterministicAcrossSeedsBatchesAndResumeWithRespectedRatio() {
        int[] first = new int[10000], resumed = new int[10000]; int selected = 0;
        for (int i = 0; i < first.length; i++) {
            first[i] = WorldPlan.replacement(17, i, 0, .25, 3); if (first[i] >= 0) selected++;
        }
        for (int start = 0; start < resumed.length; start += 137)
            for (int i = start; i < Math.min(start + 137, resumed.length); i++) resumed[i] = WorldPlan.replacement(17, i, 0, .25, 3);
        assertArrayEquals(first, resumed); assertTrue(selected > 2300 && selected < 2700, "Selected " + selected);
        assertTrue(java.util.stream.IntStream.range(0, 10000).anyMatch(i -> first[i] != WorldPlan.replacement(18, i, 0, .25, 3)));
        for (int i = 0; i < 100; i++) { assertEquals(-1, WorldPlan.replacement(17, i, 0, 0, 3)); assertTrue(WorldPlan.replacement(17, i, 0, 1, 3) >= 0); }
    }
    @Test void timedCorruptionSpreadsEvenlyAndFinishesAtTheRequestedActiveTick() {
        assertEquals(0, WorldPlan.due(101, 0, 20)); assertEquals(50, WorldPlan.due(101, 10, 20));
        assertEquals(101, WorldPlan.due(101, 20, 20)); assertEquals(101, WorldPlan.due(101, 200, 20));
        assertEquals(101, WorldPlan.due(101, 0, 0));
    }
}
