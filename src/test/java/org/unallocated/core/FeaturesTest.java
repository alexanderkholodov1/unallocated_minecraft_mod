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
    @Test void bundledOptionalContentIsOff() {
        var features = Features.load();
        assertFalse(features.actEnabled(Act.THEFT)); assertFalse(features.actEnabled(Act.LOOP));
        assertFalse(features.actEnabled(Act.POCKET)); assertTrue(features.actEnabled(Act.LOBBY));
    }
}
