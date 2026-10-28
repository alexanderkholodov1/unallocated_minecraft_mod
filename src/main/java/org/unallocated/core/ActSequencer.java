package org.unallocated.core;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread session spine. Only armed worlds may start. CONTRACT is a client boundary,
 * never a timed server act. All content callbacks are isolated; progression is bounded.
 */
public final class ActSequencer {
    public static final int RESCUE_DELAY_TICKS = 100;
    private final SessionState state;
    private final Features features;
    private final Map<Act, ActModule> modules = new EnumMap<>(Act.class);
    private final ServerEventBus events;
    private final Consumer<String> log;
    private boolean needsEnter = true;
    private boolean transitioning;
    private boolean forcing;
    private Act requestedJump;
    private int revision;
    private Act debugTarget;
    private int activeBudget;
    private String checkpoint = "unavailable";

    public ActSequencer(SessionState state, Features features, Collection<ActModule> modules,
                        ServerEventBus events, Consumer<String> log) {
        this.state = state; this.features = features; this.events = events; this.log = log;
        for (ActModule module : modules) {
            Act act;
            try { act = java.util.Objects.requireNonNull(module.act()); }
            catch (RuntimeException e) { failed("act", e); continue; }
            if (this.modules.put(act, module) != null) throw new IllegalArgumentException("Duplicate act: " + act);
        }
        if (state.active()) configureActive();
    }
    public SessionState state() { return state; }
    /** Cached values: status cannot invoke broken content callbacks. */
    public String checkpointId() { return checkpoint; }
    public int budgetTicks() { return activeBudget; }
    private ActModule module() { return modules.get(state.act()); }

    /** Called on the server thread, normally from the first JOIN in an armed world. */
    public void start(ServerPlayer player) {
        if (!state.armed() || state.started() || transitioning) return;
        Act first = nextEnabled(Act.CONTRACT.ordinal());
        if (first == null) return;
        marker(Act.CONTRACT, "complete");
        moveTo(first);
        enter(player);
    }

    /** Restore the checkpoint once on reconnect/respawn; never renew the budget. */
    public void resume(ServerPlayer player) {
        if (!state.active() || transitioning) return;
        if (state.act() == Act.CONTRACT) {
            // Compatibility with old saves: no server-side consent wait, no fake timeout.
            transition(player, "complete", false);
        } else if (debugTarget != state.act() && !enabled(state.act())) {
            transition(player, "skipped", false);
        } else {
            configureActive();
            needsEnter = true;
            enter(player);
        }
    }

    public void tick(ServerPlayer player) {
        if (!state.active() || transitioning) return;
        int before = revision;
        enter(player);
        if (before != revision || !state.active()) return;
        state.tickClock();
        try {
            module().tick(player, state);
            if (before != revision) return;
            boolean complete = module().complete(player, state);
            if (before != revision) return;
            if (complete) { advance(player); return; }
        } catch (RuntimeException e) {
            failed("tick/complete", e);
            if (before == revision) transition(player, "forced", true);
            return;
        }
        if (state.elapsedActTicks() >= activeBudget) transition(player, "forced", true);
    }

    /** Content completion only. This must never start a dormant session. */
    public void advance(ServerPlayer player) { transition(player, "complete", false); }

    /** No-cheats rescue: active session only, with a five-second delay after every act change.
     * Run its diegetic escape, but distinguish the tester/player skip from real completion. */
    public boolean rescue(ServerPlayer player) {
        if (!state.active() || transitioning || state.elapsedActTicks() < RESCUE_DELAY_TICKS) return false;
        transition(player, "skipped", true);
        return true;
    }

    private void transition(ServerPlayer player, String action, boolean force) {
        if (!state.active() || transitioning) return;
        transitioning = true;
        try {
            marker(state.act(), action);
            if (force && module() != null) {
                try { forcing = true; module().forceAdvance(player, state); }
                catch (RuntimeException e) { failed("forceAdvance", e); }
                finally { forcing = false; }
            }
            Act next = requestedJump != null ? requestedJump : nextEnabled(state.act().ordinal());
            requestedJump = null;
            if (next == null) { state.finish(); needsEnter = false; revision++; }
            else moveTo(next);
        } finally { transitioning = false; }
        enter(player);
    }

    /** Content jump: active sessions only, enabled forward targets, truthful completion marker.
     * Escape may select a destination; ActChanged listeners retain the reentrancy guard. */
    public void jump(Act target, ServerPlayer player) {
        if (!state.active() || target.ordinal() <= state.act().ordinal()) return;
        if (transitioning) {
            if (forcing && enabled(target)) requestedJump = target;
            return;
        }
        if (!enabled(target)) return;
        transitioning = true;
        try { marker(state.act(), "complete"); moveTo(target); }
        finally { transitioning = false; }
        enter(player);
    }

    /** Admin jump within an armed world. Console callbacks wait for a real participant. */
    public void skipTo(Act act) {
        if (!state.armed() || transitioning) return;
        if (!modules.containsKey(act)) throw new IllegalArgumentException("Unregistered act " + act);
        if (state.active()) marker(state.act(), "skipped");
        if (act == Act.CONTRACT) {
            marker(Act.CONTRACT, "complete");
            act = nextEnabled(Act.CONTRACT.ordinal());
            if (act == null) return;
        }
        moveTo(act);
        debugTarget = act;
    }
    public void skipTo(Act act, ServerPlayer player) {
        if (!state.armed() || transitioning) return;
        skipTo(act);
        enter(player);
    }

    private boolean enabled(Act act) {
        if (act == Act.CONTRACT || !features.actEnabled(act) || !modules.containsKey(act)) return false;
        try { return modules.get(act).enabled(features); }
        catch (RuntimeException e) { failed("enabled(" + act + ")", e); return false; }
    }
    private Act nextEnabled(int after) {
        for (Act act : Act.values()) {
            if (act.ordinal() <= after) continue;
            if (enabled(act)) return act;
            if (modules.containsKey(act)) marker(act, "skipped");
        }
        return null;
    }
    private void configureActive() {
        activeBudget = 1; // Broken/missing modules get at most one tick before forced escape.
        checkpoint = "unavailable";
        if (module() == null) return;
        try {
            int budget = module().budgetTicks();
            if (budget <= 0) throw new IllegalArgumentException("Act needs a positive budget");
            activeBudget = budget;
        } catch (RuntimeException e) { failed("budgetTicks", e); }
        try { checkpoint = java.util.Objects.requireNonNull(module().checkpointId()); }
        catch (RuntimeException e) { failed("checkpointId", e); }
    }
    private void moveTo(Act next) {
        Act previous = state.act();
        debugTarget = null;
        state.moveTo(next); needsEnter = true; revision++;
        configureActive();
        marker(next, "enter");
        boolean wasTransitioning = transitioning;
        transitioning = true;
        try { events.post(new ServerEvents.ActChanged(previous, next)); }
        finally { transitioning = wasTransitioning; }
    }
    private void enter(ServerPlayer player) {
        if (!needsEnter || !state.active()) return;
        needsEnter = false;
        try { module().enter(player, state); }
        catch (RuntimeException e) { failed("enter", e); activeBudget = 1; }
    }
    private void failed(String callback, RuntimeException e) {
        log.accept("UNALLOCATED act=" + state.act() + " callback=" + callback + " failed: " + e);
    }
    private void marker(Act act, String action) {
        log.accept("UNALLOCATED act=" + act + " " + action + " t=" + (state.elapsedSessionTicks() / 20));
    }
}
