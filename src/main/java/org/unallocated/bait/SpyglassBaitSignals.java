package org.unallocated.bait;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Server → client hand-off for the screamer's sound and image. In single player the integrated server and the client
 * share this process, so a volatile field is enough and needs no Fabric API networking. On a dedicated server the
 * remote client never sees it: there the scare is only the figure's lunge (multiplayer is secondary, owner's update 5);
 * the director's planned {@code unallocated:effect} packet can replace this later.
 */
public final class SpyglassBaitSignals {
    /**
     * @param player   who must see it
     * @param sequence increases per scare so each one plays exactly once
     * @param nanos    {@link System#nanoTime()} when fired; stale signals (e.g. from a world just left) are ignored
     */
    public record Scare(UUID player, long sequence, long nanos) { }

    private static volatile @Nullable Scare latest;
    private static long sequence;

    private SpyglassBaitSignals() { }

    static synchronized void scare(UUID player) {
        latest = new Scare(player, ++sequence, System.nanoTime());
    }

    public static @Nullable Scare latest() {
        return latest;
    }
}
