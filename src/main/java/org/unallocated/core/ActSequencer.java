package org.unallocated.core;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/** Single-player-first, server-thread session spine. No map or narrative content lives here.
 * Debug jumps can select disabled acts; automatic advancement always skips them.
 * One tick can finish at most one act. The final act finishes once, never wraps to CONTRACT.
 */
public final class ActSequencer {
    private final SessionState state;
    private final Features features;
    private final Map<Act, ActModule> modules = new EnumMap<>(Act.class);
    private final ServerEventBus events;
    private final Consumer<String> log;
    private boolean needsEnter = true;
    private boolean transitioning;
    private int revision;
    private Act debugTarget;

    public ActSequencer(SessionState state, Features features, Collection<ActModule> modules,
                        ServerEventBus events, Consumer<String> log) {
        this.state = state; this.features = features; this.events = events; this.log = log;
        for (ActModule module : modules) {
            if (module.budgetTicks() <= 0) throw new IllegalArgumentException("Act needs a positive budget: " + module.act());
            if (this.modules.put(module.act(), module) != null) throw new IllegalArgumentException("Duplicate act: " + module.act());
        }
    }
    public SessionState state() { return state; }
    public String checkpointId() { return module().checkpointId(); }
    public int budgetTicks() { return module().budgetTicks(); }
    private ActModule module() { return Objects.requireNonNull(modules.get(state.act()), "Missing active act module"); }

    /** Called by the launcher after consent, never implicitly in every vanilla world. */
    public void start(ServerPlayer player) {
        if (state.started()) return;
        Act first = nextEnabled(-1);
        if (first == null) return;
        moveTo(first);
        enter(player);
    }

    /** Called once on reconnect/respawn so a content module restores its checkpoint. */
    public void resume(ServerPlayer player) {
        if (!state.started() || state.finished()) return;
        if (!modules.containsKey(state.act()) || (!module().enabled(features) && debugTarget != state.act())) {
            advance(player);
        } else {
            needsEnter = true;
            enter(player);
        }
    }

    public void tick(ServerPlayer player) {
        if (!state.started() || state.finished() || transitioning) return;
        enter(player);
        state.tickClock();
        int before = revision;
        try {
            module().tick(player, state);
            if (before != revision) return; // A module may deliberately call advance itself.
            boolean complete = module().complete(player, state);
            if (before != revision) return;
            if (complete) { advance(player); return; }
        } catch (RuntimeException e) {
            log.accept("UNALLOCATED act=" + state.act() + " callback failed: " + e);
            if (before == revision) forceAdvance(player);
            return;
        }
        if (state.elapsedActTicks() >= module().budgetTicks()) forceAdvance(player);
    }

    public void advance(ServerPlayer player) {
        if (!state.started()) { start(player); return; }
        if (state.finished() || transitioning) return;
        transitioning = true;
        try {
            marker("complete");
            advanceState();
        } finally { transitioning = false; }
        enter(player);
    }

    private void forceAdvance(ServerPlayer player) {
        transitioning = true;
        try {
            marker("forced");
            try { module().forceAdvance(player, state); }
            catch (RuntimeException e) { log.accept("UNALLOCATED forceAdvance failed: " + e); }
            // Even a broken content callback cannot prevent the next act.
            advanceState();
        } finally { transitioning = false; }
        enter(player);
    }

    private void advanceState() {
        Act next = nextEnabled(state.act().ordinal());
        if (next == null) { state.finish(); needsEnter = false; revision++; }
        else moveTo(next);
    }

    /** Console-compatible jump: callbacks wait for a real player, but state/logs change now. */
    public void skipTo(Act act) {
        if (transitioning) return;
        if (!modules.containsKey(act)) throw new IllegalArgumentException("Unregistered act " + act);
        if (state.started() && !state.finished()) marker("forced");
        moveTo(act);
        debugTarget = act;
    }
    public void skipTo(Act act, ServerPlayer player) { skipTo(act); enter(player); }

    private Act nextEnabled(int after) {
        for (Act act : Act.values()) {
            if (act.ordinal() > after && modules.containsKey(act) && modules.get(act).enabled(features)) return act;
        }
        return null;
    }
    private void moveTo(Act next) {
        Act previous = state.act();
        debugTarget = null;
        state.moveTo(next); needsEnter = true; revision++;
        marker("enter");
        boolean wasTransitioning = transitioning;
        transitioning = true;
        try { events.post(new ServerEvents.ActChanged(previous, next)); }
        finally { transitioning = wasTransitioning; }
    }
    private void enter(ServerPlayer player) {
        // Null is used only by console jumps and pure unit-test modules. Runtime never ticks null.
        if (!needsEnter || !state.started() || state.finished()) return;
        needsEnter = false;
        try { module().enter(player, state); }
        catch (RuntimeException e) {
            log.accept("UNALLOCATED act=" + state.act() + " enter failed: " + e);
            // Its finite budget still applies; diegetic fallback is tried at expiry.
        }
    }
    private void marker(String action) {
        log.accept("UNALLOCATED act=" + state.act() + " " + action + " t=" + (state.elapsedSessionTicks() / 20));
    }
}
