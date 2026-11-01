package org.unallocated.core;

import net.minecraft.server.level.ServerPlayer;

/** Server-thread callbacks. enter must be idempotent: reconnect/reload restores its checkpoint.
 * Persist progress in SessionState; a new process cannot restore module instance fields.
 * forceAdvance performs the diegetic escape synchronously; the sequencer then advances itself.
 */
public interface ActModule {
    Act act();
    boolean enabled(Features f);
    void enter(ServerPlayer p, SessionState s);
    void tick(ServerPlayer p, SessionState s);
    boolean complete(ServerPlayer p, SessionState s);
    int budgetTicks();
    void forceAdvance(ServerPlayer p, SessionState s);
    String checkpointId();
}
