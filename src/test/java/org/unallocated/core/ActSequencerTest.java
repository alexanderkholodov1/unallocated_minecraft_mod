package org.unallocated.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActSequencerTest {
    private final List<String> logs = new ArrayList<>();
    private final ServerEventBus events = new ServerEventBus(e -> fail(e));
    private final Features features = new Features(Map.of("acts.lobby", true, "acts.climax", true, "acts.epilogue", true));
    private ActSequencer sequencer(SessionState state, Module... modules) {
        return new ActSequencer(state, features, List.of(modules), events, logs::add);
    }
    private static class Module implements ActModule {
        final Act act;
        int enters, ticks, forced;
        boolean done, throwForce;
        Module(Act act) { this.act = act; }
        public Act act() { return act; }
        public boolean enabled(Features f) { return f.actEnabled(act); }
        public void enter(ServerPlayer p, SessionState s) { enters++; }
        public void tick(ServerPlayer p, SessionState s) { ticks++; }
        public boolean complete(ServerPlayer p, SessionState s) { return done; }
        public int budgetTicks() { return 3; }
        public void forceAdvance(ServerPlayer p, SessionState s) {
            forced++;
            if (throwForce) throw new IllegalStateException("Broken content fallback");
            s.addFlag("fallback_used");
        }
        public String checkpointId() { return "lobby.spawn"; }
    }
    @Test void budgetExpiresExactlyOnceAndRunsDiegeticFallbackBeforeEnteringNextAct() {
        var state = new SessionState(42);
        var lobby = new Module(Act.LOBBY);
        var climax = new Module(Act.CLIMAX) {
            public void enter(ServerPlayer p, SessionState s) { assertTrue(s.flags().contains("fallback_used")); super.enter(p,s); }
        };
        var seq = sequencer(state, lobby, climax);
        seq.start(null);
        seq.tick(null); seq.tick(null);
        assertEquals(Act.LOBBY, state.act());
        seq.tick(null);
        assertEquals(Act.CLIMAX, state.act());
        assertEquals(1, lobby.forced);
        assertEquals(1, climax.enters);
        assertEquals(List.of("UNALLOCATED act=LOBBY enter t=0", "UNALLOCATED act=LOBBY forced t=0", "UNALLOCATED act=CLIMAX enter t=0"), logs);
    }
    @Test void completionSkipsDisabledActAndPublishesChange() {
        var state = new SessionState(1);
        var lobby = new Module(Act.LOBBY); lobby.done = true;
        var theft = new Module(Act.THEFT);
        var climax = new Module(Act.CLIMAX);
        var changes = new ArrayList<ServerEvents.ActChanged>();
        events.subscribe(ServerEvents.ActChanged.class, changes::add);
        var seq = sequencer(state, lobby, theft, climax);
        seq.start(null); seq.tick(null);
        assertEquals(Act.CLIMAX, state.act());
        assertEquals(0, theft.enters);
        assertEquals(0, lobby.forced);
        assertEquals(new ServerEvents.ActChanged(Act.LOBBY, Act.CLIMAX), changes.getLast());
        assertTrue(logs.contains("UNALLOCATED act=LOBBY complete t=0"));
    }
    @Test void completionOnBudgetBoundaryWinsOverForcedAdvance() {
        var state = new SessionState(0);
        var lobby = new Module(Act.LOBBY);
        var seq = sequencer(state, lobby, new Module(Act.CLIMAX));
        seq.start(null); seq.tick(null); seq.tick(null);
        lobby.done = true;
        seq.tick(null);
        assertEquals(Act.CLIMAX, state.act());
        assertEquals(0, lobby.forced);
        assertTrue(logs.contains("UNALLOCATED act=LOBBY complete t=0"));
    }
    @Test void reloadResumesCheckpointWithRemainingBudget() {
        var state = new SessionState(7);
        var first = sequencer(state, new Module(Act.LOBBY), new Module(Act.CLIMAX));
        first.start(null); first.tick(null); first.tick(null);
        var restored = SessionState.fromTag(state.toTag());
        var lobby = new Module(Act.LOBBY);
        var second = sequencer(restored, lobby, new Module(Act.CLIMAX));
        second.resume(null);
        assertEquals(1, lobby.enters);
        assertEquals(2, restored.elapsedActTicks());
        second.tick(null);
        assertEquals(Act.CLIMAX, restored.act());
        assertEquals(1, lobby.forced);
    }
    @Test void reloadSkipsNewlyDisabledAct() {
        var state = new SessionState(0);
        var seq = sequencer(state, new Module(Act.THEFT), new Module(Act.CLIMAX));
        seq.skipTo(Act.THEFT);
        var restored = SessionState.fromTag(state.toTag());
        var theft = new Module(Act.THEFT);
        var resumed = sequencer(restored, theft, new Module(Act.CLIMAX));
        resumed.resume(null);
        assertEquals(Act.CLIMAX, restored.act());
        assertEquals(0, theft.enters);
    }
    @Test void explicitJumpTargetsDisabledActsAndResetsOnlyActClock() {
        var state = new SessionState(0);
        var theft = new Module(Act.THEFT);
        var seq = sequencer(state, new Module(Act.LOBBY), theft);
        seq.start(null); seq.tick(null); seq.tick(null);
        state.addFlag("knows_name");
        seq.skipTo(Act.THEFT, null);
        assertEquals(Act.THEFT, state.act());
        assertEquals(0, state.elapsedActTicks());
        assertEquals(2, state.elapsedSessionTicks());
        assertEquals(1, theft.enters);
        assertTrue(state.flags().contains("knows_name"));
    }
    @Test void deferredConsoleJumpStillEntersDisabledActWhenPlayerConnects() {
        var state = new SessionState(0);
        var theft = new Module(Act.THEFT);
        var seq = sequencer(state, theft, new Module(Act.CLIMAX));
        seq.skipTo(Act.THEFT);
        assertEquals(0, theft.enters);
        seq.resume(null);
        assertEquals(Act.THEFT, state.act());
        assertEquals(1, theft.enters);
    }
    @Test void moduleMayAdvanceItselfWithoutTickingOrCompletingNextAct() {
        var state = new SessionState(0);
        ActSequencer[] holder = new ActSequencer[1];
        var lobby = new Module(Act.LOBBY) {
            public void tick(ServerPlayer p, SessionState s) { holder[0].advance(p); }
        };
        var climax = new Module(Act.CLIMAX); climax.done = true;
        var seq = sequencer(state, lobby, climax, new Module(Act.EPILOGUE));
        holder[0] = seq;
        seq.start(null); seq.tick(null);
        assertEquals(Act.CLIMAX, state.act());
        assertEquals(0, climax.ticks);
        assertEquals(0, state.elapsedActTicks());
    }
    @Test void terminalActFinishesOnceAndSurvivesReload() {
        var state = new SessionState(0);
        var epilogue = new Module(Act.EPILOGUE); epilogue.done = true;
        var seq = sequencer(state, epilogue);
        seq.start(null); seq.tick(null);
        assertTrue(state.finished());
        var restored = SessionState.fromTag(state.toTag());
        var resumed = sequencer(restored, epilogue);
        resumed.resume(null); resumed.tick(null); resumed.advance(null);
        assertTrue(restored.finished());
        assertEquals(1, epilogue.enters);
        assertEquals(1, restored.ticks());
    }
    @Test void brokenFallbackCannotTrapPlayer() {
        var state = new SessionState(0);
        var lobby = new Module(Act.LOBBY); lobby.throwForce = true;
        var seq = sequencer(state, lobby, new Module(Act.CLIMAX));
        seq.start(null); seq.tick(null); seq.tick(null); seq.tick(null);
        assertEquals(Act.CLIMAX, state.act());
    }
    @Test void sessionIsDormantUntilExplicitStart() {
        var state = new SessionState(0);
        var lobby = new Module(Act.LOBBY);
        var seq = sequencer(state, lobby);
        seq.tick(null);
        assertFalse(state.started()); assertEquals(0, state.ticks()); assertEquals(0, lobby.enters);
    }
    @Test void noEnabledModulesDoesNotLoop() {
        var state = new SessionState(0);
        var seq = sequencer(state, new Module(Act.THEFT));
        seq.start(null);
        assertFalse(state.started());
    }
    @Test void invalidBudgetsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> sequencer(new SessionState(0), new Module(Act.LOBBY) {
            public int budgetTicks() { return 0; }
        }));
    }
}
