package org.unallocated.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapLayoutTest {
    @Test void checkpointsFitTheirSectionsAndWorldEnvelope() {
        for (var cp : MapLayout.checkpoints().values()) {
            assertTrue(MapLayout.section(cp.section()).bounds().contains(cp.x(), cp.y(), cp.z()));
            assertTrue(MapLayout.ISLAND.contains(cp.x(), cp.y(), cp.z()));
        }
        assertTrue(MapLayout.trigger("base.theft_zone").contains(61.5, 64, 12.5));
        assertTrue(MapLayout.trigger("hill.bait_path").contains(34.5, 72, -23.5));
    }
    @Test void sectionOwnershipNeverOverlaps() {
        var sections = MapLayout.sections().values().stream().toList();
        for (int i = 0; i < sections.size(); i++) for (int j = i + 1; j < sections.size(); j++) {
            var a = sections.get(i).bounds(); var b = sections.get(j).bounds();
            assertTrue(a.maxX() < b.minX() || b.maxX() < a.minX() || a.maxZ() < b.minZ() || b.maxZ() < a.minZ(),
                sections.get(i).id() + " overlaps " + sections.get(j).id());
        }
    }
    @Test void inclusiveBlockBoxesHandlePlayerCentersAtEdges() {
        var box = new MapLayout.BlockBox(1, 2, 3, 4, 5, 6);
        assertTrue(box.contains(1, 2, 3)); assertTrue(box.contains(4.99, 5.99, 6.99));
        assertFalse(box.contains(5, 3, 4)); assertFalse(box.contains(0.99, 3, 4));
        assertThrows(IllegalArgumentException.class, () -> new MapLayout.BlockBox(3, 0, 0, 2, 0, 0));
    }
    @Test void unknownNamesFailClearlyAndContractsAreReadOnly() {
        assertThrows(IllegalArgumentException.class, () -> MapLayout.checkpoint("missing"));
        assertThrows(IllegalArgumentException.class, () -> MapLayout.section("missing"));
        assertThrows(UnsupportedOperationException.class, () -> MapLayout.checkpoints().clear());
        assertThrows(UnsupportedOperationException.class, () -> MapLayout.sections().clear());
        assertThrows(UnsupportedOperationException.class, () -> MapLayout.triggers().clear());
        assertThrows(UnsupportedOperationException.class, () -> MapLayout.landmarks().clear());
    }
}
