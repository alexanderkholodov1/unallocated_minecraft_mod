package org.unallocated.story;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.unallocated.core.*;
import static org.junit.jupiter.api.Assertions.*;
class StoryJumpTest {
    private static class Module implements ActModule {
        final Act act; Module(Act act) { this.act = act; }
        public Act act() { return act; } public boolean enabled(Features f) { return true; }
        public void enter(ServerPlayer p, SessionState s) {} public void tick(ServerPlayer p, SessionState s) {}
        public boolean complete(ServerPlayer p, SessionState s) { return false; } public int budgetTicks() { return 1; }
        public void forceAdvance(ServerPlayer p, SessionState s) {} public String checkpointId() { return "test"; }
    }
    @Test void jumpHonorsDormantFinishedEnabledAndReentrantGuards() {
        SessionState s = new SessionState(0); List<String> logs = new ArrayList<>();
        var bus = new ServerEventBus(e -> fail(e));
        var q = new ActSequencer(s,new Features(Map.of("acts.lobby",true,"acts.epilogue",true)),List.of(new Module(Act.LOBBY),new Module(Act.EPILOGUE)),bus,logs::add);
        q.jump(Act.EPILOGUE,null); assertFalse(s.started()); s.arm();
        bus.subscribe(ServerEvents.ActChanged.class,e -> { q.advance(null); q.skipTo(Act.LOBBY); q.jump(Act.EPILOGUE,null); });
        q.start(null); assertEquals(Act.LOBBY,s.act());
        q.jump(Act.EPILOGUE,null); assertEquals(Act.EPILOGUE,s.act());
        assertTrue(logs.contains("UNALLOCATED act=LOBBY complete t=0"));
        q.advance(null); assertTrue(s.finished()); q.jump(Act.EPILOGUE,null); assertTrue(s.finished());
    }
}
