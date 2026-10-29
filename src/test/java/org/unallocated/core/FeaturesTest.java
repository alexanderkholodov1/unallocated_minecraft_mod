package org.unallocated.core;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeaturesTest {
    @Test void nestedSwitchesAndMissingKeys() {
        var features = Features.read(new StringReader("{\"acts\":{\"theft\":false,\"loop\":true}}"));
        assertFalse(features.actEnabled(Act.THEFT)); assertTrue(features.actEnabled(Act.LOOP));
        assertFalse(features.enabled("effects.options"));
    }
    @Test void malformedSwitchesFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> Features.read(new StringReader("{\"acts\":{\"theft\":\"true\"}}")));
    }
    @Test void bundledStorySlotsAreEnabled() {
        var features = Features.load();
        assertTrue(features.actEnabled(Act.THEFT)); assertTrue(features.actEnabled(Act.LOOP));
        assertTrue(features.actEnabled(Act.POCKET)); assertTrue(features.actEnabled(Act.LOBBY));
        assertTrue(features.actEnabled(Act.CLIMAX));
        // The shipped story uses brief darkness and the spyglass screamer; the contract warns of both.
        assertTrue(features.enabled("effects.options")); assertTrue(features.enabled("effects.screamer"));
    }
}
