package org.unallocated.smoke;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.unallocated.ending.EndingText;

/** Deterministically inspects the real connection at the close-before-reason race window. */
public final class EndingDisconnectProbe {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final AtomicInteger FAILURES = new AtomicInteger();
    private EndingDisconnectProbe() {}

    public static void afterChannelClose(Connection connection, DisconnectionDetails requested) {
        if (!(connection.getPacketListener() instanceof ClientCommonPacketListenerImpl)
                || !(requested.reason().getContents() instanceof TranslatableContents text)
                || !EndingText.REASON_KEY.equals(text.getKey())) return;
        if (connection.getDisconnectionDetails() != requested) FAILURES.incrementAndGet();
        CHECKS.incrementAndGet();
    }

    static int checks() { return CHECKS.get(); }

    static void requirePublishedSince(int before) {
        if (CHECKS.get() <= before || FAILURES.get() != 0) {
            throw new AssertionError("Ending reason was not published before client channel closure: checks="
                    + CHECKS.get() + " failures=" + FAILURES.get());
        }
    }
}
