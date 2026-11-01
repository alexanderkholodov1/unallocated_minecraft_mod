package org.unallocated.ending;

import net.minecraft.server.level.ServerPlayer;
import org.unallocated.core.Act;
import org.unallocated.core.ActModule;
import org.unallocated.core.Features;
import org.unallocated.core.SessionState;

/** No timers or per-module session fields: reconnects use the core's persisted terminal state. */
public final class EpilogueAct implements ActModule {
    public Act act() { return Act.EPILOGUE; }
    public boolean enabled(Features features) { return features.actEnabled(act()); }
    public void enter(ServerPlayer player, SessionState state) {
        if (player != null && !org.unallocated.story.StoryRuntime.get(player.level().getServer()).closeEnding(player)) EndingController.close(player,
            new EndingController.Ending("SERVER_CLOSED", EndingText.reason()));
    }
    public void tick(ServerPlayer player, SessionState state) {}
    public boolean complete(ServerPlayer player, SessionState state) { return false; }
    public int budgetTicks() { return 600; }
    public void forceAdvance(ServerPlayer player, SessionState state) { enter(player, state); }
    public String checkpointId() { return "lobby.spawn"; }
}
