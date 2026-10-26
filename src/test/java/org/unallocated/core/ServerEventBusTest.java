package org.unallocated.core;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventBusTest {
    @Test void oneFailedListenerDoesNotHideTransitionFromOtherPackages() throws Exception {
        var errors = new ArrayList<RuntimeException>();
        var received = new ArrayList<ServerEvents.ActChanged>();
        var bus = new ServerEventBus(errors::add);
        bus.subscribe(ServerEvents.ActChanged.class, e -> { throw new IllegalStateException("content failure"); });
        var subscription = bus.subscribe(ServerEvents.ActChanged.class, received::add);
        var change = new ServerEvents.ActChanged(Act.LOBBY, Act.CLIMAX);
        bus.post(change);
        assertEquals(1, errors.size()); assertEquals(java.util.List.of(change), received);
        subscription.close();
        bus.post(change);
        assertEquals(1, received.size());
    }
}
